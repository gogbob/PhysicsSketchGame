package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class FollowingTriggerObject extends TriggerObject {
    private PhysicsObject followingObject;
    private Vector2 relativePosition = new Vector2();
    private float relativeRotation = 0f;

    public FollowingTriggerObject(int id, float friction, float restitution, List<Vector2> vertices, float startX, float startY, float rotation, PhysicsObject followingObject) {
        super(id, friction, restitution, vertices, startX, startY, rotation);
        this.followingObject = followingObject;
        this.relativePosition = new Vector2(followingObject.getPosition()).sub(getPosition()).nor();
        this.relativeRotation = followingObject.getRotation() - getRotation();

    }

    public PhysicsObject getFollowingObject() {
        return followingObject;
    }

    public void updatePosition() {
        setPosition(new Vector2(followingObject.getPosition()).add(relativePosition));
        setRotation(followingObject.getRotation() + relativeRotation);
    }
}
