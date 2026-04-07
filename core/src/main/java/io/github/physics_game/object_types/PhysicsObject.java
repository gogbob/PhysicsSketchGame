package io.github.physics_game.object_types;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.ArrayList;
import java.util.List;

public abstract class PhysicsObject {
    private final int id;
    private float friction;
    private float restitution;
    private List<Vector2> vertices;
    private CustomContactHandler.PolygonBody localBody;
    private List<List<Vector2>> concaveLocalTriangles;
    private List<List<Vector2>> concaveLocalBest;
    private float startX;
    private float startY;
    private float startRotation;
    private Vector2 localCenterOffset = new Vector2();

    public PhysicsObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation) {
        this(id, friction, restitution, vertices, startX, startY, rotation
            , PhysicsResolver.getCenterOfMassPolygon(EarClippingDecomposer.decomposeToTriangles(vertices)));
    }

    public PhysicsObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation, Vector2 com) {
        super();
        this.id = id;
        this.friction = friction;
        this.restitution = restitution;

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


    public Vector2 getCenter()  {
        return getPosition();
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
