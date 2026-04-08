package io.github.physics_game.object_types;

public interface Following {
    PhysicsObject getFollowedObj();
    void setFollowedObj(PhysicsObject obj);
    void updatePosition();
}
