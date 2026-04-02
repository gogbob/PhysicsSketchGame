package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.List;

public class DynamicObject extends PhysicsObject {
    private Vector2 currentVelocity = new Vector2();
    private float currentAngularVelocity = 0f;
    List<Float> densities;
    float mass = 0f;
    float inertia = 0f;
    public DynamicObject(int id, float friction, float restitution, List<Float> densities, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
        this.densities = densities;
        mass = PhysicsResolver.getMassOfPolygon(getConcaveLocalTriangles(), densities);
        inertia = PhysicsResolver.getMomentOfInertiaPolygon(getConcaveLocalTriangles(), densities);
    }

    public DynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, vertices, startX, startY, rotation);

        for(int i = 0; i < getConcaveLocalTriangles().size(); i++) {
            densities.add(density);
        }
        mass = PhysicsResolver.getMassOfPolygon(getConcaveLocalTriangles(),  densities);
        inertia = PhysicsResolver.getMomentOfInertiaPolygon(getConcaveLocalTriangles(), densities);
    }



    public float getMass() {
        return mass;
    }
    public float getDensity() {
        return density;
    }
    public float getInertia() {
        return inertia;
    }
    @Override
    public Vector2 getLinearVelocity() {
        return currentVelocity;
    }
    @Override
    public float getAngularVelocity() {
        return currentAngularVelocity;
    }
    public void setLinearVelocity(Vector2 velocity) {
        if (velocity == null) {
            this.currentVelocity.setZero();
        } else {
            this.currentVelocity.set(velocity);
        }
    }
    public void setAngularVelocity(float velocity) {
        this.currentAngularVelocity = velocity;
    }
    public void updatePosition(float delta) {
        Vector2 linearVelocity = new Vector2(getLinearVelocity());
        float angle = getRotation() + getAngularVelocity() * delta;
        Vector2 position = new Vector2(getPosition()).add(linearVelocity.scl(delta));
        setPosition(position);
        setRotation(angle);
    }
    public void reinitialize() {
            setPosition(new Vector2(getStartX(), getStartY()));
            setRotation(0f);
            currentVelocity.setZero();
            currentAngularVelocity = 0f;
    }
}
