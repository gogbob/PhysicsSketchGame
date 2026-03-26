package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class TutorialLevel extends  Level {
    private final float timeToComplete = 5f; // Time in seconds to complete the level
    private float elapsedTimeOutside = timeToComplete;
    private FollowingTriggerObject cupInside;
    private DynamicObject ball;
    private boolean isComplete;

    public TutorialLevel() {
        super(0, "Tutorial Level", new ArrayList<>(), 20f, 15f);

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

        List<Vector2> circleVertices = PhysicsResolver.getCircleVertices(12, 0.5f);
        this.ball = new DynamicObject(0, 0.5f, 0.5f, 1f, circleVertices, 6f, 4f, 0f);
        addPhysicsObject(ball);

        DynamicObject cup = new DynamicObject(1, 0.5f, 0.5f, 1f, cupVertices, 5f, 2f, 0f);
        addPhysicsObject(cup);

        this.cupInside = new FollowingTriggerObject(2, 0.5f, 0.5f, cupInsideVertices, 5f, 2f, 0f, cup);
        addPhysicsObject(cupInside);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public void tick(float deltaTime) {
        // Implement any necessary updates for the tutorial level
        if(!cupInside.getTriggeredObjectIds().contains(ball.getId())) {
            elapsedTimeOutside -= deltaTime;
            if (elapsedTimeOutside <= 0) {
                System.out.println("Ball has been outside the cup for long enough! You win");
                isComplete = true;
            }
        } else {
            System.out.println("Ball is inside the cup!");
            elapsedTimeOutside = timeToComplete; // Reset the timer if the ball is inside the cup
        }
    }

}
