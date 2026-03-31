package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class DynamicTriggerObject extends DynamicObject {
    List<Integer> triggeredObjectIds;
    public DynamicTriggerObject(int id, float friction, float restitution, List<Vector2> vertices, float density, float startX, float startY, float rotation) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
    }

    public void resetTriggeredObjectIds() {
        triggeredObjectIds.clear();
    }

    public void addTriggered(int id) {
        triggeredObjectIds.add(id);
    }

    public List<Integer> getTriggeredObjectIds() {
        return triggeredObjectIds;
    }
}
