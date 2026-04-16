package io.github.physics_game.levels;

import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Level1 extends Level {
    private final float timeToComplete = 0.8f;
    private float elapsedTimeInside = 0f;
    private boolean hasEnteredCup = false;

    private FollowingUncollidableField cupInside;
    private DynamicObject ball;
    private boolean isComplete;


    public Level1(float viewPortWidth, float viewPortHeight) {
        super(2, "Drop Into the Cup", new ArrayList<>(), viewPortWidth, viewPortHeight);

        List<Vector2> platformVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(-2.5f, -0.25f),
                new Vector2(2.5f, -0.25f),
                new Vector2(2.5f, 0.25f),
                new Vector2(-2.5f, 0.25f)
            )
        );

        StaticObject platform = new StaticObject(
            10,
            0.5f,
            0.5f,
            platformVertices,
            4.0f,
            8.0f,
            -0.25f   // slight slope so the ball naturally rolls right
        );
        addPhysicsObject(platform);


        List<Vector2> circleVertices = PhysicsResolver.getCircleVertices(12, 0.5f);

        this.ball = new DynamicObject(
            11,
            0.5f,
            0.5f,
            1f,
            circleVertices,
            2.8f,
            9.0f,
            0f
        );
        addPhysicsObject(ball);


        List<Vector2> cupVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(0, 0),
                new Vector2(3f, 0),
                new Vector2(3.6f, 5f),
                new Vector2(3.3f, 5f),
                new Vector2(2.7f, 0.4f),
                new Vector2(0.3f, 0.4f),
                new Vector2(-0.3f, 5f),
                new Vector2(-0.6f, 5f)
            )
        );

        List<Vector2> cupInsideVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(2.7f, 0.4f),
                new Vector2(3.5f, 5f),
                new Vector2(-0.5f, 5f),
                new Vector2(0.3f, 0.4f)
            )
        );

        DynamicObject cup = new DynamicObject(
            12,
            0.5f,
            0.5f,
            1f,
            cupVertices,
            14.0f,
            1.5f,
            0f
        );
        addPhysicsObject(cup);

        this.cupInside = new FollowingUncollidableField(
            13,
            cupInsideVertices,
            14.0f,
            1.5f,
            0f,
            cup
        );
        addPhysicsObject(cupInside);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public LevelTickData tick(float deltaTime) {
        if (cupInside.getTriggerIds().contains(ball.getId())) {
            if (!hasEnteredCup) {
                System.out.println("Ball entered the cup!");
                hasEnteredCup = true;
            }

            elapsedTimeInside += deltaTime;

            if (elapsedTimeInside >= timeToComplete) {
                if (!isComplete) {
                    System.out.println("Level complete!");
                }
                isComplete = true;
            }
        } else {
            elapsedTimeInside = 0f;
        }

        for (PhysicsObject obj : getPhysicsObjects()) {
            if (obj instanceof Triggerable) {
                ((Triggerable) obj).resetTriggerIds();
            }
        }

        if (elapsedTimeInside > 0f && elapsedTimeInside < timeToComplete) {
            return new LevelTickData(timeToComplete - elapsedTimeInside);
        } else {
            return new LevelTickData();
        }
    }
}

