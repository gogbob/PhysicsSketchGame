package io.github.physics_game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class DebugForce {
    private Vector2 position = new Vector2();
    private Vector2 force = new Vector2();
    private Color color = Color.RED;

    public DebugForce(float x, float y, float forceX, float forceY) {
        this.position.set(x, y);
        this.force.set(forceX, forceY);
    }

    public DebugForce(Vector2 position, float forceX, float forceY) {
        this.position.set(position);
        this.force.set(forceX, forceY);
    }

    public DebugForce(float x, float y, Vector2 force) {
        this.position.set(x, y);
        this.force.set(force);
    }

    public DebugForce(Vector2 position, Vector2 force) {
        this.position.set(position);
        this.force.set(force);
    }

    public Vector2 getPosition() {
        return new Vector2(position);
    }

    public Vector2 getForce() {
        return new Vector2(force);
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
