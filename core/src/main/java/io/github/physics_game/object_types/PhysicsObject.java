package io.github.physics_game.object_types;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.*;

public abstract class PhysicsObject {
    private final int id;
    private float friction;
    private float restitution;
    private float density;
    private final List<Vector2> vertices;
    private final CustomContactHandler.PolygonBody localBody;
    private final List<List<Vector2>> concaveLocalTriangles;
    private List<List<Vector2>> concaveLocalBest;
    private float startX;
    private float startY;
    private float startRotation;
    private List<Float> massSegments;
    private List<Vector2> pointSegments;
    private Vector2 localCenterOffset = new Vector2();
    private final Color color = new Color();

    public PhysicsObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation) {

        this(id, friction, restitution, density, vertices, startX, startY, rotation
            , PhysicsResolver.getCenterOfMassPolygon(EarClippingDecomposer.decomposeToTriangles(vertices)));
        for(List<Vector2> tri : this.getConcaveLocalTriangles()) {
            float massPoint = PhysicsResolver.getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), density);
            Vector2 center = new Vector2(PhysicsResolver.getCenterOfMassTriangle(tri.get(0), tri.get(1), tri.get(2)));
            addMassSegment(massPoint, center);
        }
    }

    public PhysicsObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, Vector2 com, List<Vector2> pointSegments, List<Float> massSegments) {
        this(id, friction, restitution, density, vertices, startX, startY, rotation
            , com);
        this.massSegments = massSegments;
        this.pointSegments = pointSegments;
    }

    public PhysicsObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, Vector2 com) {
        super();
        this.id = id;
        this.friction = friction;
        this.restitution = restitution;
        this.density = density;
        this.massSegments = new ArrayList<>();
        this.pointSegments = new ArrayList<>();

        Vector2 localCom = (com == null) ? new Vector2() : new Vector2(com);
        this.localCenterOffset = new Vector2(localCom);
        List<Vector2> centeredVertices = recenterVertices(vertices, localCom);

        this.vertices = new ArrayList<>(centeredVertices);
        this.localBody = new CustomContactHandler.PolygonBody(centeredVertices);
        this.concaveLocalTriangles = EarClippingDecomposer.decomposeToTriangles(centeredVertices);
        int prevSize = concaveLocalTriangles.size();


        concaveLocalBest = EarClippingDecomposer.mergePolygons(concaveLocalTriangles);
        int currentSize = concaveLocalBest.size();

        while(concaveLocalBest.size() < prevSize) {
            prevSize = currentSize;
            //continuously find if you can simplify
            concaveLocalBest = EarClippingDecomposer.mergePolygons(concaveLocalBest);
            currentSize =  concaveLocalBest.size();
        }

        // Keep the simulation transform at COM so impulses/rotation use the same origin.
        Vector2 rotatedLocalCom = new Vector2(localCenterOffset).rotateRad(rotation);
        this.startX = startX + rotatedLocalCom.x;
        this.startY = startY + rotatedLocalCom.y;

        this.startRotation = rotation;
        setRotation(rotation);
        setPosition(new Vector2(this.startX, this.startY));
        color.set(Color.WHITE);
    }

    private List<Vector2> recenterVertices(List<Vector2> source, Vector2 com) {
        List<Vector2> centered = new ArrayList<>();
        if (source == null) {
            return centered;
        }
        for (Vector2 v : source) {
            if (v != null) {
                centered.add(new Vector2(v).sub(com));
            }
        }
        return centered;
    }

    public boolean isPointInsideObject(Vector2 point) {
        for(List<Vector2> convexPolygon : concaveLocalBest) {
            //check if it is on the left side of all edges
            boolean isInside = true;
            for(int i = 0; i < convexPolygon.size(); i++) {
                Vector2 a = CustomContactHandler.toWorld(this.getPosition(), this.getRotation(), new Vector2(convexPolygon.get(i)));
                Vector2 b = CustomContactHandler.toWorld(this.getPosition(), this.getRotation(), new Vector2(convexPolygon.get((i + 1) % convexPolygon.size())));
                Vector2 edge = new Vector2(b).sub(a);

                if(edge.crs(new Vector2(point).sub(a)) < 0) {
                    isInside = false;
                }
            }
            if(isInside) {
                return true;
            }
        }
        return false;
    }


    public Vector2 getCenter()  {
        return getPosition();
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color.set(color);
    }

    public float getDensity() {
        return density;
    }

    public void addMassSegment(float mass, Vector2 point) {
        this.massSegments.add(mass);
        this.pointSegments.add(point);
    }

    public void setMassSegments(List<Float> mass, List<Vector2> points) {
        massSegments = new ArrayList<>(mass);
        pointSegments = new ArrayList<>(points);
    }

    public List<Float> getMassSegments() {
        return new ArrayList<>(massSegments);
    }
    public List<Vector2> getPointSegments() {
        return new ArrayList<>(pointSegments);
    }

    public float getMassSegment(int i) {
        return this.massSegments.get(i);
    }

    public Vector2 getWorldPointSegment(int i) {
        return CustomContactHandler.toWorld(getPosition(), getRotation(), new Vector2(this.pointSegments.get(i)).sub(localCenterOffset));
    }

    public Vector2 getAnchorPosition() {
        Vector2 rotatedOffset = new Vector2(localCenterOffset).rotateRad(getRotation());
        return new Vector2(getPosition()).sub(rotatedOffset);
    }

    public void setAnchorPosition(Vector2 anchorPosition) {
        Vector2 rotatedOffset = new Vector2(localCenterOffset).rotateRad(getRotation());
        setPosition(new Vector2(anchorPosition).add(rotatedOffset));
    }

    public void setPosition(Vector2 localPosition) {
        localBody.setPosition(localPosition.x, localPosition.y);
    }
    public void setRotation(float angle) {
        localBody.setRotationRadians(angle);
    }
    public Vector2 getPosition() {
        return new Vector2(localBody.getPosition());
    }
    public float getRotation() {
        return localBody.getRotationRadians();
    }
    public int getId() {
        return id;
    }
    public float getFriction() {
        return friction;
    }
    public float getRestitution() {
        return restitution;
    }
    public List<Vector2> getVertices() {
        return new ArrayList<>(vertices);
    }
    public CustomContactHandler.PolygonBody getLocalBody() {
        return localBody;
    }
    public List<List<Vector2>> getConcaveLocalTriangles() {
        return new ArrayList<>(concaveLocalTriangles);
    }
    public List<List<Vector2>> getConcaveLocalBest() {
        return new ArrayList<>(concaveLocalBest);
    }
    public float getStartX() {
        return startX;
    }
    public float getStartY() {
        return startY;
    }
    public void reinitialize() {
        setPosition(new Vector2(startX, startY));
        setRotation(startRotation);
    }
    public abstract Vector2 getLinearVelocity();
    public abstract float getAngularVelocity();
}
