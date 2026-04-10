package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.DebugForce;

import java.util.List;

public interface Charged {
    float getChargeDensity();
    void setChargeDensity(float chargeDensity);
    List<DebugForce> applyChargeForcePair(PhysicsObject charged, boolean isRun);
}
