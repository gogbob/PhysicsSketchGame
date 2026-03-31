package io.github.physics_game;

import java.util.List;

public interface Triggerable {
    List<Integer> getTriggerIds();
    void resetTriggerIds();
    void addTriggerId(int id);
}
