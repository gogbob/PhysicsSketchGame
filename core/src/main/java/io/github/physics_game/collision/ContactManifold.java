package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact manifold: collision normal + 0-2 contact points.
 * Represents stable multi-point resting contact or single collision.
 */
public final class ContactManifold {
    public static final ContactManifold NO_CONTACT = new ContactManifold(false, new Vector2(), new ArrayList<>());

    private final boolean colliding;
    private final Vector2 normal; // A -> B
    private final List<ContactPoint> points; // 0..2 points

    public ContactManifold(boolean colliding, Vector2 normal, List<ContactPoint> points) {
        this.colliding = colliding;
        this.normal = new Vector2(normal);
        this.points = new ArrayList<>(points);
    }

    public boolean isColliding() {
        return colliding;
    }

    public Vector2 getNormal() {
        return new Vector2(normal);
    }

    public List<ContactPoint> getPoints() {
        return new ArrayList<>(points);
    }

    public int getPointCount() {
        return points.size();
    }

    public float getMaxPenetration() {
        float max = 0f;
        for (ContactPoint p : points) {
            max = Math.max(max, p.penetration);
        }
        return max;
    }

    public float getAveragePenetration() {
        if (points.isEmpty()) return 0f;
        float sum = 0f;
        for (ContactPoint p : points) {
            sum += p.penetration;
        }
        return sum / points.size();
    }

    @Override
    public String toString() {
        return String.format("ContactManifold(colliding=%s, points=%d, depth=%.4f)", colliding, points.size(), getMaxPenetration());
    }
}

