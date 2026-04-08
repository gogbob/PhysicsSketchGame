package io.github.physics_game.levels;

import io.github.physics_game.object_types.PhysicsObject;

import java.util.ArrayList;

public class Level1 extends Level {
    public Level1(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects, float viewPortWidth, float viewPortHeight) {
        super(levelId, levelName, internalObjects, viewPortWidth, viewPortHeight);
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
