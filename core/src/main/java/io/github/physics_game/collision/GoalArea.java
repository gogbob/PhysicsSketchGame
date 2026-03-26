package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Rectangle;

public class GoalArea {
    private Rectangle bounds;
    private float requiredTime = 1.0f;
    private float currentTime = 0f;

    public GoalArea(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public boolean update(Vector2 ballPosition, float ballVelocity, float delta) {
        if (bounds.contains(ballPosition.x, ballPosition.y) && ballVelocity < 0.1f) {
            currentTime += delta;
            if (currentTime >= requiredTime) {
                return true;
            }
        } else {
            currentTime = 0f;
        }
        return false;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public float getProgress() {
        return currentTime / requiredTime;
    }
}
