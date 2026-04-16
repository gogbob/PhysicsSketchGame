package io.github.physics_game.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.object_types.DynamicObject;
import io.github.physics_game.object_types.PhysicsObject;

import java.util.ArrayList;
import java.util.List;

public class Level1 extends Level {
    public Level1(float viewPortWidth, float viewPortHeight) {
        super(1, "Level 1", new ArrayList<>(), viewPortWidth, viewPortHeight);
        List<Vector2> circleVertices = PhysicsResolver.getCircleVertices(12, 0.5f);
        DynamicObject ball = new DynamicObject(0, 0.5f, 0.5f, 1f, circleVertices, 6f, 4f, 0f);
        addPhysicsObject(ball);

        setBackground(new Texture("background_forest.png"));
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public LevelTickData tick(float deltaTime) {
        return null;
    }
}
