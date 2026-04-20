package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class FollowingUncollidableField extends TriggerField implements Following {
    private PhysicsObject followedObj;
    private Vector2 relativeAnchorLocal;
    private float relativeRotation;

    public FollowingUncollidableField(int id, List<Vector2> vertices, float startX, float startY, float rotation, PhysicsObject followedObj) {
        super(id, vertices, startX, startY, rotation);
        this.followedObj = followedObj;

        Vector2 followerAnchor = getAnchorPosition();
        Vector2 followedAnchor = followedObj.getAnchorPosition();

        this.relativeAnchorLocal = new Vector2(followerAnchor)
            .sub(followedAnchor)
            .rotateRad(-followedObj.getRotation());
        this.relativeRotation = getRotation() - followedObj.getRotation();
    }

    @Override
    public PhysicsObject getFollowedObj() {
        return followedObj;
    }

    @Override
    public void setFollowedObj(PhysicsObject obj) {
        followedObj = obj;
    }

    @Override
    public void updatePosition() {
        if (followedObj != null) {
            Vector2 followedAnchor = followedObj.getAnchorPosition();
            Vector2 nextAnchor = new Vector2(relativeAnchorLocal)
                .rotateRad(followedObj.getRotation())
                .add(followedAnchor);

            setRotation(followedObj.getRotation() + relativeRotation);
            setAnchorPosition(nextAnchor);
        }
    }
}
