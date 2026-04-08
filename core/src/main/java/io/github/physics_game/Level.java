package io.github.physics_game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.object_types.PhysicsObject;
import io.github.physics_game.object_types.StaticObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Level {
    private int levelId;
    private String levelName;
    private ArrayList<PhysicsObject> physicsObjects = new ArrayList<>();

    public Level(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects, float viewPortWidth, float viewPortHeight) {
        this.levelId = levelId;
        this.levelName = levelName;
        physicsObjects.addAll(internalObjects);
        List<Vector2> floorPoly = Arrays.asList(new Vector2(0, 0), new Vector2(viewPortWidth, 0), new Vector2(viewPortWidth, 1), new Vector2(0, 1));
        StaticObject floor = new StaticObject(-1, 0.5f, 0.5f, floorPoly, 0, 0, 0);

        List<Vector2> wallPoly = Arrays.asList(new Vector2(0, 0), new Vector2(1, 0), new Vector2(1, viewPortHeight*2), new Vector2(0, viewPortHeight*2));
        StaticObject leftWall = new StaticObject(-2, 0.5f, 0.5f, wallPoly, 0, 0, 0);
        StaticObject rightWall = new StaticObject(-3, 0.5f, 0.5f, wallPoly, viewPortWidth - 1, 0, 0);

        floor.setColor(Color.GRAY);
        leftWall.setColor(Color.GRAY);
        rightWall.setColor(Color.GRAY);

        physicsObjects.add(floor);
        physicsObjects.add(leftWall);
        physicsObjects.add(rightWall);
    }

    public ArrayList<PhysicsObject> getPhysicsObjects() {
        return new ArrayList<>(physicsObjects);
    }
    public void addPhysicsObject(PhysicsObject obj) {
        physicsObjects.add(obj);
    }
     public int getLevelId() {
        return levelId;
    }
    public String getLevelName() {
        return levelName;
    }

    public abstract boolean isComplete();
    public abstract LevelTickData tick(float deltaTime);

    public class LevelTickData {
        private Float timeLeft = null;
        public LevelTickData() {}
        public LevelTickData(Float timeLeft) {
            this.timeLeft = timeLeft;
        }
        public Float getTimeLeft() {
            return timeLeft;
        }
    }
}
