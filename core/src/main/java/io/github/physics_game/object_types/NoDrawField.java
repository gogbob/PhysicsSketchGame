package io.github.physics_game.object_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class NoDrawField extends UncollidableObject {
    public NoDrawField(int id, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, vertices, startX, startY, rotation);
        setColor(new Color(1.0f, 0.5f, 0.5f, 0.2f));
    }
}
