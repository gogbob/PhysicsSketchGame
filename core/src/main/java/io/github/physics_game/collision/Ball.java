package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import io.github.physics_game.DynamicObject;
import java.util.Arrays;
import java.util.List;

public class Ball extends DynamicObject {
    private static final float R = 0.25f;

    public Ball(float startX, float startY) {
        super(
            -100,
            0.4f,
            0.3f,
            1.0f,
            createCircle(),
            startX,
            startY,
            0
            //world
        );
    }

    private static List<Vector2> createCircle() {
        return Arrays.asList(
            new Vector2(R, 0),
            new Vector2(0.866f*R, 0.5f*R),
            new Vector2(0.5f*R, 0.866f*R),
            new Vector2(0, R),
            new Vector2(-0.5f*R, 0.866f*R),
            new Vector2(-0.866f*R, 0.5f*R),
            new Vector2(-R, 0),
            new Vector2(-0.866f*R, -0.5f*R),
            new Vector2(-0.5f*R, -0.866f*R),
            new Vector2(0, -R),
            new Vector2(0.5f*R, -0.866f*R),
            new Vector2(0.866f*R, -0.5f*R)
        );
    }

}
