package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

/**
 * A single contact point with world position and penetration depth.
 */
public final class ContactPoint {
    public static final ContactPoint NO_CONTACT_POINT = new ContactPoint(new Vector2(), 0f, new Vector2());
    public final Vector2 point;
    public final float penetration;
    public final Vector2 normal;

    public ContactPoint(Vector2 p, float pen, Vector2 normal) {
        this.point = new Vector2(p);
        this.penetration = pen;
        this.normal = new Vector2(normal).nor();
    }

    public ContactPoint(float x, float y, float pen, Vector2 normal) {
        this.point = new Vector2(x, y);
        this.penetration = pen;
        this.normal = new Vector2(normal).nor();
    }

    @Override
    public String toString() {
        return String.format("ContactPoint(%.2f,%.2f) penetration=%.4f", point.x, point.y, penetration);
    }
}

