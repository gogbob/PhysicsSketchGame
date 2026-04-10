package io.github.physics_game.object_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.DebugForce;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.ArrayList;
import java.util.List;

public class DynamicObject extends PhysicsObject {
    private Vector2 currentVelocity = new Vector2();
    private float currentAngularVelocity = 0f;
    private float density;
    private float mass = 0f;
    private float inertia = 0f;


    public DynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices,
                         float startX, float startY, float rotation) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
        this.density = density;
        this.mass = PhysicsResolver.getMassOfPolygon(getConcaveLocalTriangles(), density);
        this.inertia = PhysicsResolver.getMomentOfInertiaPolygon(getConcaveLocalTriangles(), density);

    }

    public DynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices,
                         float startX, float startY, float rotation, float mass, float inertia, Vector2 com,  List<Vector2> pointSegments, List<Float> massSegments) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation, com, pointSegments, massSegments);
        this.density = density;
        this.mass = mass;
        this.inertia = inertia;
    }



    public float getMass() {
        return mass;
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

    public DebugForce applyForce(Vector2 force, Vector2 point, Color color, boolean isRun, boolean isDebug) {
        if(isRun) {
            Vector2 r = new Vector2(point).sub(this.getCenter());
            Vector2 deltaLinearVel = new Vector2(force).scl(1f / this.getMass());
            float deltaAngularVel = r.crs(force) / this.getInertia();

            Vector2 newLinearVel = new Vector2(this.getLinearVelocity()).add(deltaLinearVel);
            float newAngularVel = this.getAngularVelocity() + deltaAngularVel;

            this.setLinearVelocity(newLinearVel);
            this.setAngularVelocity(newAngularVel);
        }
        if(isDebug) {
            DebugForce impulseForce = new DebugForce(point, force);
            impulseForce.setColor(color);
            return impulseForce;
        }
        return null;
    }
}
