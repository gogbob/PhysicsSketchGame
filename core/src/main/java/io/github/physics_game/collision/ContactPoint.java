package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

/**
 * A single contact point with world position and penetration depth.
 */
public final class ContactPoint {
    public final Vector2 point;
    public final float penetration;

    public ContactPoint(Vector2 p, float pen) {
        this.point = new Vector2(p);
        this.penetration = pen;
    }

    public ContactPoint(float x, float y, float pen) {
        this.point = new Vector2(x, y);
        this.penetration = pen;
    }

    @Override
    public String toString() {
        return String.format("ContactPoint(%.2f,%.2f) penetration=%.4f", point.x, point.y, penetration);
    }
}

