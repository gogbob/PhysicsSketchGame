package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class DynamicTriggerObject extends DynamicObject implements Triggerable {
    List<Integer> triggeredObjectIds;
    public DynamicTriggerObject(int id, float friction, float restitution, List<Vector2> vertices, float density, float startX, float startY, float rotation) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
    }

    @Override
    public List<Integer> getTriggerIds() {
        return triggeredObjectIds;
    }

    @Override
    public void resetTriggerIds() {
        triggeredObjectIds.clear();
    }

    @Override
    public void addTriggerId(int id) {
        triggeredObjectIds.add(id);
    }
}
