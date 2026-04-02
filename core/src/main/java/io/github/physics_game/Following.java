package io.github.physics_game;

public interface Following {
    PhysicsObject getFollowedObj();
    void setFollowedObj(PhysicsObject obj);
    void updatePosition();
}
