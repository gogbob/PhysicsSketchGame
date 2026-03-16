package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
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
    private float startX;
    private float startY;

    public PhysicsObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation) {
        this.id = id;
        this.friction = friction;
        this.restitution = restitution;
        this.vertices = new ArrayList<>(vertices);
        this.localBody = new CustomContactHandler.PolygonBody(vertices);
        this.concaveLocalTriangles = EarClippingDecomposer.decomposeToTriangles(vertices);
        this.startX = startX;
        this.startY = startY;
    }
    public abstract Vector2 getCenter();
    public void setLocalPosition(Vector2 localPosition) {
        localBody.setPosition(localPosition.x, localPosition.y);
    }
    public void setLocalRotation(float angle) {
        localBody.setRotationRadians(angle);
    }
    public Vector2 getLocalPosition() {
        return new Vector2(localBody.getPosition());
    }
    public float getLocalRotation() {
        return localBody.getRotationRadians();
    }
    public abstract void setPosition(Vector2 position);
    public abstract void setRotation(float angle);
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
    public float getStartX() {
        return startX;
    }
    public float getStartY() {
        return startY;
    }
    public abstract void reinitialize();
    public abstract Vector2 getLinearVelocity();
    public abstract float getAngularVelocity();
}
