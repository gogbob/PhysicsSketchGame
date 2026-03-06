package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

/**
 * Immutable collision query result.
 *
 * <p>The normal always points from shape A to shape B so response code can rely on a
 * single convention for impulse and position correction.</p>
 */
public class ContactResult {
    public static final ContactResult NO_CONTACT = new ContactResult(false, new Vector2(), 0f, new Vector2());

    private final boolean colliding;
    private final Vector2 normal;
    private final float penetrationDepth;
    private final Vector2 contactPoint;

    public ContactResult(boolean colliding, Vector2 normal, float penetrationDepth) {
        this(colliding, normal, penetrationDepth, new Vector2());
    }

    public ContactResult(boolean colliding, Vector2 normal, float penetrationDepth, Vector2 contactPoint) {
        this.colliding = colliding;
        this.normal = new Vector2(normal);
        this.penetrationDepth = penetrationDepth;
        this.contactPoint = new Vector2(contactPoint);
    }

    public boolean isColliding() {
        return colliding;
    }

    public Vector2 getNormal() {
        return new Vector2(normal);
    }

    public float getPenetrationDepth() {
        return penetrationDepth;
    }

    public Vector2 getContactPoint() {
        return new Vector2(contactPoint);
    }
}
