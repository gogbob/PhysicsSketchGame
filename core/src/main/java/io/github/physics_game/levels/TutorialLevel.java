package io.github.physics_game.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TutorialLevel extends Level {
    private final float timeToComplete = 0.8f; // Time in seconds to complete the level
    private float elapsedTimeInside = 0f;
    //private float elapsedTimeOutside = timeToComplete;
    private FollowingUncollidableField cupInside;
    private DynamicObject ball;
    private boolean isComplete;
    private static final int ShapePenalty = 10; // Each shape took off 10 points
    private static final float TIME_PENALTY = 1.0f;

    public TutorialLevel(float viewPortWidth, float viewPortHeight) {
        super(0, "Tutorial Level", new ArrayList<>(), viewPortWidth, viewPortHeight);

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

        this.cupInside = new FollowingUncollidableField(2, cupInsideVertices, 5f, 2f, 0f, cup);
        addPhysicsObject(cupInside);

        setBackground(new Texture("background_forest.png"));
    }

    @Override
    public void reset() {
        super.reset();
        isComplete = false;
        elapsedTimeInside = 0f;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public LevelTickData tick(float deltaTime) {
        if (cupInside.getTriggerIds().contains(ball.getId())) {
            elapsedTimeInside += deltaTime;
            System.out.println("Ball is inside the cup!");

            if (elapsedTimeInside >= timeToComplete) {
                System.out.println("Ball stayed inside the cup long enough! You win");
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
