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

    public TutorialLevel() {
        super(0, "Tutorial Level", new ArrayList<>(), 20f, 15f);

        List<Vector2> cupVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(0, 0),
                new Vector2(3f, 0),
                new Vector2(3.6f, 4f),
                new Vector2(3.3f, 4f),
                new Vector2(2.7f, 1f),
                new Vector2(0.3f, 1f),
                new Vector2(-0.3f, 4f),
                new Vector2(-0.6f, 4f)
            )
        );

        List<Vector2> cupInsideVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(2.7f, 1f),
                new Vector2(3.3f, 4f),
                new Vector2(-0.3f, 4f),
                new Vector2(0.3f, 1f)
            )
        );

        List<Vector2> platformVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(0, 0f),
                new Vector2(5f, 0f),
                new Vector2(5f, 1f),
                new Vector2(0, 1f)
            )
        );

        List<Vector2> circleVertices = PhysicsResolver.getCircleVertices(12, 0.5f);
        this.ball = new DynamicObject(0, 0.5f, 0.5f, 1f, circleVertices, 10f, 8f, 0f);
        addPhysicsObject(ball);



        DynamicObject cup = new DynamicObject(1, 0.01f, 0.5f, 1f, cupVertices, 10f, 2f, 0f);
        addPhysicsObject(cup);

        this.cupInside = new FollowingTriggerObject(2, 0.01f, 0.5f, cupInsideVertices, 10f, 2f, 0f, cup);
        addPhysicsObject(cupInside);

        StaticObject platform = new StaticObject(3, 0.01f, 0.5f, platformVertices, 0f, 5f, 0f);
        addPhysicsObject(platform);
    }

    @Override
    public void isComplete() {
        // Implement logic to check if the tutorial level is complete
    }

    @Override
    public void tick(float deltaTime) {
        // Implement any necessary updates for the tutorial level
        if(cupInside.getTriggeredObjectIds().contains(ball)) {
            System.out.println("Ball found");
            elapsedTimeOutside -= deltaTime;
            if (elapsedTimeOutside <= 0) {
                System.out.println("Ball has been outside the cup for long enough! You win");
            }
        } else {
            System.out.println("Ball is inside the cup!");
            elapsedTimeOutside = timeToComplete; // Reset the timer if the ball is inside the cup
        }
    }

}
