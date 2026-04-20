package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class TriggerField extends UncollidableObject implements Triggerable {
    ArrayList<Integer> triggerIds = new ArrayList<>();

    public TriggerField(int id, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, vertices, startX, startY, rotation);
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
}
