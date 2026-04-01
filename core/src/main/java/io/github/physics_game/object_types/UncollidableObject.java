package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

public class UncollidableObject extends PhysicsObject {


    public UncollidableObject(int id, java.util.List<com.badlogic.gdx.math.Vector2> vertices, float startX, float startY, float rotation) {
        super(id, 0f, 0f, vertices, startX, startY, rotation);
    }

    @Override
    public Vector2 getLinearVelocity() {
        return new Vector2();
    }

    @Override
    public float getAngularVelocity() {
        return 0;
    }
}
