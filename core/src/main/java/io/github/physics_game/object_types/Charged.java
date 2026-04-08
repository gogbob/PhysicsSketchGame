package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

public interface Charged {
    float getChargeDensity();
    void setChargeDensity(float chargeDensity);
    Vector2 findChargeForce(PhysicsObject charged);
}
