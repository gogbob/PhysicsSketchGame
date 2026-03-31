package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class TriggerObject extends PhysicsObject {
    List<Integer> triggeredObjectIds = new ArrayList<>();
    public TriggerObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
    }

    public void addTriggered(int id) {
        triggeredObjectIds.add(id);
    }

    public List<Integer> getTriggeredObjectIds() {
        return triggeredObjectIds;
    }

    public void resetTriggeredObjectIds() {
        triggeredObjectIds.clear();
    }

    @Override
    public void reinitialize() {
        setPosition(new Vector2(getStartX(), getStartY()));
        setRotation(0f);
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
