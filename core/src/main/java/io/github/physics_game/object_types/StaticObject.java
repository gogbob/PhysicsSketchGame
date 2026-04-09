package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class StaticObject extends PhysicsObject {
    public StaticObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, 1f, vertices, startX, startY, rotation);
    }

    public StaticObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY,
                        float rotation, Vector2 com, List<Vector2> pointSegments, List<Float> massSegments) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation, com, pointSegments, massSegments);
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
