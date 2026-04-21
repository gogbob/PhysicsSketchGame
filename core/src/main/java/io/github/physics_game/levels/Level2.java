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
    private final float timeToComplete = 1.2f;
    private float elapsedTimeInside = 0f;
    private boolean hasEnteredCup = false;

    private FollowingUncollidableField cupInside;
    private DynamicObject ball;
    private boolean isComplete;

    public Level2(float viewPortWidth, float viewPortHeight) {
        super(
            3,
            "Over the Ridge",
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(DrawType.NORMAL, DrawType.POSITIVE)),
            new ArrayList<>(Arrays.asList(80f, 30f)),
            viewPortWidth,
            viewPortHeight
        );

        // Long starting platform on the left
        List<Vector2> startPlatformVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(-3.2f, -0.25f),
                new Vector2(3.2f, -0.25f),
                new Vector2(3.2f, 0.25f),
                new Vector2(-3.2f, 0.25f)
            )
        );

        StaticObject startPlatform = new StaticObject(
            20,
            0.5f,
            0.5f,
            startPlatformVertices,
            5.2f,
            22.0f,
            -0.08f
        );
        addPhysicsObject(startPlatform);

        // Central tilted blocker / ridge
        List<Vector2> ridgeVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(-2.8f, -0.3f),
                new Vector2(2.8f, -0.3f),
                new Vector2(2.8f, 0.3f),
                new Vector2(-2.8f, 0.3f)
            )
        );

        StaticObject ridge = new StaticObject(
            21,
            0.8f,
            0.5f,
            ridgeVertices,
            18.5f,
            16.0f,
            0.55f
        );
        addPhysicsObject(ridge);

        // Small landing shelf before the cup
        List<Vector2> shelfVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(-2.2f, -0.22f),
                new Vector2(2.2f, -0.22f),
                new Vector2(2.2f, 0.22f),
                new Vector2(-2.2f, 0.22f)
            )
        );

        StaticObject shelf = new StaticObject(
            22,
            0.6f,
            0.5f,
            shelfVertices,
            29.0f,
            10.0f,
            -0.05f
        );
        addPhysicsObject(shelf);

        // Ball starts high and left
        List<Vector2> circleVertices = PhysicsResolver.getCircleVertices(14, 0.5f);

        this.ball = new DynamicObject(
            23,
            0.5f,
            0.5f,
            1f,
            circleVertices,
            4.0f,
            23.4f,
            0f
        );
        addPhysicsObject(ball);

        // Cup placed farther right and lower
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
                new Vector2(3.2f, 0.4f + 4.6f * (5f / 6f)),
                new Vector2(-0.2f, 0.4f + 4.6f * (5f / 6f)),
                new Vector2(0.3f, 0.4f)
            )
        );

        DynamicObject cup = new DynamicObject(
            24,
            0.5f,
            0.5f,
            1f,
            cupVertices,
            33.0f,
            6.0f,
            0f
        );
        addPhysicsObject(cup);

        // Narrow obstacle near the cup to force a cleaner approach
        List<Vector2> blockerVertices = new ArrayList<>(
            Arrays.asList(
                new Vector2(-0.25f, -2.0f),
                new Vector2(0.25f, -2.0f),
                new Vector2(0.25f, 2.0f),
                new Vector2(-0.25f, 2.0f)
            )
        );

        StaticObject blocker = new StaticObject(
            25,
            0.7f,
            0.4f,
            blockerVertices,
            27.0f,
            8.5f,
            0f
        );
        addPhysicsObject(blocker);

        // No-draw region on the right half, like Level1, but stricter
        List<Vector2> noDrawZone = new ArrayList<>(
            Arrays.asList(
                new Vector2(getWorldBounds().x / 2.4f, floorHeight),
                new Vector2(getWorldBounds().x - wallWidth, floorHeight),
                new Vector2(getWorldBounds().x - wallWidth, getWorldBounds().y - floorHeight),
                new Vector2(getWorldBounds().x / 2.4f, getWorldBounds().y - floorHeight)
            )
        );

        NoDrawField noDrawField = new NoDrawField(
            26,
            noDrawZone,
            0f,
            0f,
            0f
        );
        addPhysicsObject(noDrawField);

        this.cupInside = new FollowingUncollidableField(
            27,
            cupInsideVertices,
            33.0f,
            6.0f,
            0f,
            cup
        );
        addPhysicsObject(cupInside);

        setBackground(new Texture("background_forest.png"));
        setDescription(
            "A harder follow-up.\n" +
                "The ball must cross the ridge\n" +
                "and approach the cup cleanly.\n" +
                "You have less charged material,\n" +
                "so use your shapes carefully.\n" +
                "Keep the ball in the cup a bit longer."
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
