package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class DynamicTriggerObject extends DynamicObject implements Triggerable {
    List<Integer> triggerIds;
    public DynamicTriggerObject(int id, float friction, float restitution, List<Vector2> vertices, float density, float startX, float startY, float rotation) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
    }

    @Override
    public List<Integer> getTriggerIds() {
        return triggerIds;
    }

    @Override
    public void resetTriggerIds() {
        triggerIds.clear();
    }

    @Override
    public void addTriggerId(int id) {
        triggerIds.add(id);
    }

    @Override
    public void reinitialize() {
        super.reinitialize();
        triggerIds = new ArrayList<>();
    }
}
