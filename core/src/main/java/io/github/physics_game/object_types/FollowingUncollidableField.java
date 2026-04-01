package io.github.physics_game.object_types;

import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class FollowingUncollidableField extends TriggerField implements Following {
    private PhysicsObject followedObj;
    private Vector2 relativePosition;
    private float relativeRotation;

    public FollowingUncollidableField(int id, List<Vector2> vertices, float startX, float startY, float rotation, PhysicsObject followedObj) {
        super(id, vertices, startX, startY, rotation);
        this.relativePosition = new Vector2(followedObj.getPosition()).sub(getPosition());
        this.relativeRotation = followedObj.getRotation() - getRotation();
        this.followedObj = followedObj;
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
            float cos = (float) Math.cos(followedObj.getRotation() - relativeRotation);
            float sin = (float) Math.sin(followedObj.getRotation() - relativeRotation);

            float x = relativePosition.x * cos - relativePosition.y * sin + followedObj.getPosition().x;
            float y = relativePosition.x * sin + relativePosition.y * cos + followedObj.getPosition().y;
            setPosition(new Vector2(x, y));
            setRotation(followedObj.getRotation());
        }
    }
}
