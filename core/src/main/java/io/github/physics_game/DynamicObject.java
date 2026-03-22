package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.List;

public class DynamicObject extends PhysicsObject {
    private Body body;
    private Vector2 currentVelocity = new Vector2();
    private float currentAngularVelocity = 0f;
    public DynamicObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation, World world) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
        body = createBodyFromEarClippedTriangles(vertices, startX, startY, world, friction, restitution);

        //sync initial position and rotation with local body
        setLocalPosition(new Vector2(startX, startY));
        setLocalRotation(body.getAngle());
    }

    private Body createBodyFromEarClippedTriangles(List<Vector2> localVertices,
                                                            float worldX,
                                                            float worldY,
                                                            World world,
                                                            float friction,
                                                            float restitution) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(worldX, worldY);
        Body body = world.createBody(bodyDef);

        List<List<Vector2>> triangles = EarClippingDecomposer.decomposeToTriangles(localVertices);
        for (List<Vector2> triangle : triangles) {
            PolygonShape polygon = new PolygonShape();
            polygon.set(new Vector2[]{triangle.get(0), triangle.get(1), triangle.get(2)});

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = polygon;

            fixtureDef.density = 1f;
            fixtureDef.friction = friction;
            fixtureDef.restitution = restitution;
            body.createFixture(fixtureDef);
            polygon.dispose();
        }

        return body;
    }
    public Body getBody() {
        return body;
    }
    @Override
    public Vector2 getCenter() {
        return body.getWorldCenter();
    }

    @Override
    public void setPosition(Vector2 position) {
        body.setTransform(new Vector2(body.getPosition()).add(new Vector2(position).sub(getLocalPosition())), body.getAngle());
        setLocalPosition(position);
    }

    @Override
    public void setRotation(float angle) {
        body.setTransform(body.getPosition(), angle);
        setLocalRotation(angle);
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }
    public float getRotation() {
        return body.getAngle();
    }
    public float getMass() {
        return body.getMass();
    }
    public float getInertia() {
        return body.getInertia();
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
        float angle = body.getAngle() + getAngularVelocity() * delta;
        Vector2 position = new Vector2(body.getPosition()).add(linearVelocity.scl(delta));

        if (position.y < 2.0f) {
            position.y = 2.0f;
            currentVelocity.setZero();
            currentAngularVelocity = 0f;
            System.out.println("Object stopped at y=2.0");
        }

        setPosition(position);
        setRotation(angle);

    }


    public void reinitialize() {
            body.setTransform(new Vector2(getStartX(), getStartY()), 0f);
            setLocalPosition(body.getPosition());
            setLocalRotation(0f);
            currentVelocity.setZero();
            currentAngularVelocity = 0f;
    }
}
