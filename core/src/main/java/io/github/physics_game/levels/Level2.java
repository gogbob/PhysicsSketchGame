package io.github.physics_game.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.DrawType;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Level2 extends Level {
    private final float timeToComplete = 0.8f;
    private float elapsedTimeInside = 0f;
    private boolean hasEnteredCup = false;

    private FollowingUncollidableField cupInside;
    private ChargedDynamicObject ball;
    private boolean isComplete;

    public Level2(float viewPortWidth, float viewPortHeight) {
        super(3, "Coulomb's Challenge",
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(DrawType.POSITIVE)),
            new ArrayList<>(Arrays.asList(20f)),
            viewPortWidth, viewPortHeight);

        // Platform the ball rests on (left side)
        List<Vector2> platformVerts = Arrays.asList(
            new Vector2(-3f, -0.3f), new Vector2(3f, -0.3f),
            new Vector2(3f,  0.3f), new Vector2(-3f,  0.3f)
        );
        StaticObject platform = new StaticObject(10, 0.5f, 0.3f, platformVerts, 7f, 20f, 0f);
        addPhysicsObject(platform);

        // Positively charged ball — constructor auto-colors it red
        List<Vector2> circleVerts = PhysicsResolver.getCircleVertices(12, 0.5f);
        this.ball = new ChargedDynamicObject(11, 0.5f, 0.4f, 1.5f, circleVerts, 7f, 20.8f, 0f, 1.0f);
        addPhysicsObject(ball);

        // Tall wall blocking direct path (top at y=24, above the ball's starting height)
        List<Vector2> wallVerts = Arrays.asList(
            new Vector2(-0.5f,  0f), new Vector2(0.5f,  0f),
            new Vector2(0.5f, 23f), new Vector2(-0.5f, 23f)
        );
        StaticObject wall = new StaticObject(15, 0.3f, 0.2f, wallVerts, 20f, 1f, 0f);
        addPhysicsObject(wall);

        // Cup (dynamic, heavy so it barely moves)
        List<Vector2> cupVerts = Arrays.asList(
            new Vector2(0,    0),
            new Vector2(3f,   0),
            new Vector2(3.6f, 5f),
            new Vector2(3.3f, 5f),
            new Vector2(2.7f, 0.4f),
            new Vector2(0.3f, 0.4f),
            new Vector2(-0.3f, 5f),
            new Vector2(-0.6f, 5f)
        );
        DynamicObject cup = new DynamicObject(12, 0.5f, 0.3f, 1f, cupVerts, 28f, 1.5f, 0f);
        addPhysicsObject(cup);

        // Trigger field inside the cup
        List<Vector2> cupInsideVerts = Arrays.asList(
            new Vector2(2.7f, 0.4f),
            new Vector2(3.2f, 0.4f + 4.6f * (5f / 6f)),
            new Vector2(-0.2f, 0.4f + 4.6f * (5f / 6f)),
            new Vector2(0.3f, 0.4f)
        );
        this.cupInside = new FollowingUncollidableField(13, cupInsideVerts, 28f, 1.5f, 0f, cup);
        addPhysicsObject(cupInside);

        setBackground(new Texture("background_forest.png"));
        setDescription(
            "The red ball is POSITIVELY charged!\n" +
            "Draw RED (+) behind it to repel it forward.\n" +
            "Draw BLUE (-) above the wall to attract\n" +
            "it up and over.\n" +
            "Get the ball into the cup!"
        );
    }

    @Override
    public void reset() {
        super.reset();
        isComplete = false;
        hasEnteredCup = false;
        elapsedTimeInside = 0f;
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
                isComplete = true;
            }
        } else {
            if (hasEnteredCup) {
                System.out.println("Ball outside the cup!");
                hasEnteredCup = false;
            }
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
