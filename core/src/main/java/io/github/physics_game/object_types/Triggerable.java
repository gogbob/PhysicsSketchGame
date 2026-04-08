package io.github.physics_game.object_types;

import java.util.List;

public interface Triggerable {
    List<Integer> getTriggerIds();
    void resetTriggerIds();
    void addTriggerId(int id);
}
