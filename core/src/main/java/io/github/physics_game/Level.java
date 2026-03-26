package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import io.github.physics_game.collision.Ball;
import io.github.physics_game.collision.GoalArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Level {
    private int levelId;
    private String levelName;
    private ArrayList<PhysicsObject> physicsObjects = new ArrayList<>();

    private Ball ball;
    private GoalArea goalArea;
    private int maxShapes = 5;
    private int shapesDrawn = 0;
    private float timeElapsed = 0f;
    private boolean isCompleted = false;

    public Level(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects) {
        this.levelId = levelId;
        this.levelName = levelName;
        physicsObjects.addAll(internalObjects);

        List<Vector2> floorPoly = Arrays.asList(
            new Vector2(0, 0), new Vector2(10, 0),
            new Vector2(10, 1), new Vector2(0, 1)
        );
        StaticObject floor = new StaticObject(-1, 0.5f, 0.5f, floorPoly, 0, 0, 0);

        List<Vector2> wallPoly = Arrays.asList(
            new Vector2(0, 0), new Vector2(1, 0),
            new Vector2(1, 10), new Vector2(0, 10)
        );
        StaticObject leftWall = new StaticObject(-2, 0.5f, 0.5f, wallPoly, 0, 0, 0);
        StaticObject rightWall = new StaticObject(-3, 0.5f, 0.5f, wallPoly, 9, 0, 0);

        physicsObjects.add(floor);
        physicsObjects.add(leftWall);
        physicsObjects.add(rightWall);

        ball = new Ball(2, 5);
        physicsObjects.add(ball);

        goalArea = new GoalArea(7, 1.5f, 2, 1.5f);
        createGoalCup();
    }

    // Create the Cup
    private void createGoalCup() {
        // left wall
        List<Vector2> leftWall = Arrays.asList(
            new Vector2(0, 0), new Vector2(0.1f, 0),
            new Vector2(0.1f, 1.5f), new Vector2(0, 1.5f)
        );
        StaticObject left = new StaticObject(-10, 0.5f, 0.2f, leftWall, 7, 1.5f, 0);

        // bottom
        List<Vector2> bottom = Arrays.asList(
            new Vector2(0, 0), new Vector2(2, 0),
            new Vector2(2, 0.1f), new Vector2(0, 0.1f)
        );
        StaticObject base = new StaticObject(-11, 0.5f, 0.2f, bottom, 7, 1.5f, 0);

        // right wall
        List<Vector2> rightWall = Arrays.asList(
            new Vector2(0, 0), new Vector2(0.1f, 0),
            new Vector2(0.1f, 1.5f), new Vector2(0, 1.5f)
        );
        StaticObject right = new StaticObject(-12, 0.5f, 0.2f, rightWall, 8.9f, 1.5f, 0);

        physicsObjects.add(left);
        physicsObjects.add(base);
        physicsObjects.add(right);
    }

    public void update(float delta) {
        timeElapsed += delta;

        if (!isCompleted && ball != null) {
            Vector2 ballPos = ball.getPosition();
            float ballSpeed = ball.getLinearVelocity().len();

            if (goalArea.update(ballPos, ballSpeed, delta)) {
                isCompleted = true;
                System.out.println("LEVEL COMPLETE!");
            }
        }
    }

    public boolean addDrawnShape(DynamicObject shape) {
        if (shapesDrawn >= maxShapes) {
            System.out.println(" You have reached Max shapes !");
            return false;
        }
        physicsObjects.add(shape);
        shapesDrawn++;
        System.out.println("Shape " + shapesDrawn + "/" + maxShapes);
        return true;
    }

    public ArrayList<PhysicsObject> getPhysicsObjects() {
        return physicsObjects;
    }

    /*public ArrayList<PhysicsObject> getPhysicsObjects() {
        return new ArrayList<>(physicsObjects);
    }

     */
    public void addPhysicsObject(PhysicsObject obj) {
        physicsObjects.add(obj);
    }
    public int getLevelId() {
        return levelId;
    }
    public String getLevelName() {
        return levelName;
    }

    public GoalArea getGoalArea() {
        return goalArea;
    }

    public Ball getBall() {
        return ball;
    }

    public int getShapesDrawn() {
        return shapesDrawn;
    }

    public int getMaxShapes() {
        return maxShapes;
    }

    public float getTimeElapsed() {
        return timeElapsed;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public int calculateStars() {
        if (shapesDrawn <= 2) return 3;
        if (shapesDrawn <= 4) return 2;
        return 1;
    }
}
