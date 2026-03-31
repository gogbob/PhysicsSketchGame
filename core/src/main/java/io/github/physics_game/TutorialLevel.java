package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.collision.ScoreCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class TutorialLevel extends Level {
    private final float timeToComplete = 5f; // Time in seconds to complete the level
    private float elapsedTimeOutside = timeToComplete;
    private FollowingTriggerObject cupInside;
    private DynamicObject ball;
    private boolean isComplete;

    private static final int SHAPE_PENALTY = 10; // Each shape took off 10 points
    private static final float TIME_PENALTY = 1.0f;

    private int maxShapes = 5;
    private int shapesDrawn = 0;
    private float totalTime = 0f;

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
        totalTime += deltaTime;

        // Implement any necessary updates for the tutorial level
        if(!cupInside.getTriggeredObjectIds().contains(ball.getId())) {
            elapsedTimeOutside -= deltaTime;
            if (elapsedTimeOutside <= 0) {
                System.out.println(" Level Complete!");
                System.out.println(" Shapes used: " + shapesDrawn + "/" + maxShapes);
                System.out.println(" Time: " + String.format("%.1f", totalTime) + "s");

                // calculate score
                int score = ScoreCalculator.calculateScore(
                    shapesDrawn, totalTime,
                    SHAPE_PENALTY, TIME_PENALTY
                );

                int stars = ScoreCalculator.calculateStars(score);

                System.out.println(" Your Final Score is: " + score);
                System.out.println(" Stars: " + stars);

                isComplete = true;
            }
        } else {
            System.out.println("Ball is inside the cup!");
            elapsedTimeOutside = timeToComplete; // Reset the timer if the ball is inside the cup
        }
    }

    public boolean addDrawnShape(DynamicObject shape) {
        if (shapesDrawn >= maxShapes) {
            System.out.println(" Maximum shapes reached! (" + maxShapes + ")");
            return false;
        }
        addPhysicsObject(shape);
        shapesDrawn++;
        System.out.println(" Shape " + shapesDrawn + "/" + maxShapes + " added");
        return true;
    }

    public int getShapesDrawn() {
        return shapesDrawn;
    }

    public int getMaxShapes() {
        return maxShapes;
    }

    public float getTotalTime() {
        return totalTime;
    }
}
