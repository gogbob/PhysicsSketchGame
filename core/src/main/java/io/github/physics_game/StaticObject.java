package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.List;

public class StaticObject extends PhysicsObject {
    public StaticObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
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
