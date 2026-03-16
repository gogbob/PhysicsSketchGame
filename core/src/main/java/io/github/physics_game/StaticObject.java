package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.List;

public class StaticObject extends PhysicsObject {
    private Body body;
    public StaticObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation, World world) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
        body = createBodyFromEarClippedTriangles(vertices, startX, startY, world, friction, restitution);
        setLocalPosition(new Vector2(startX, startY));
        setLocalRotation(rotation);
    }

    private Body createBodyFromEarClippedTriangles(List<Vector2> localVertices,
                                                   float worldX,
                                                   float worldY,
                                                   World world,
                                                   float friction,
                                                   float restitution) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(worldX, worldY);
        Body body = world.createBody(bodyDef);

        List<List<Vector2>> triangles = EarClippingDecomposer.decomposeToTriangles(localVertices);
        for (List<Vector2> triangle : triangles) {
            PolygonShape polygon = new PolygonShape();
            polygon.set(new Vector2[]{triangle.get(0), triangle.get(1), triangle.get(2)});

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = polygon;
            fixtureDef.density = 0f;
            fixtureDef.friction = friction;
            fixtureDef.restitution = restitution;
            body.createFixture(fixtureDef);
            polygon.dispose();
        }

        return body;
    }
    public Vector2 getPosition() {
        return body.getPosition();
    }

    public float getAngle() {
        return body.getAngle();
    }
    @Override
    public void setPosition(Vector2 position) {
        body.setTransform(position, body.getAngle());
        setLocalPosition(position);
    }
    @Override
    public void setRotation(float angle) {
        body.setTransform(body.getPosition(), angle);
        setLocalRotation(angle);
    }

    public Body getBody() {
        return body;
    }
    @Override
    public Vector2 getCenter() {
        return body.getWorldCenter();
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
    public void reinitialize() {
        body.setTransform(new Vector2(getStartX(), getStartY()), 0f);
        setLocalPosition(body.getPosition());
        setLocalRotation(0f);
    }

    @Override
    public Vector2 getLinearVelocity() {
        return new Vector2(0, 0);
    }

    @Override
    public float getAngularVelocity() {
        return 0;
    }
}
