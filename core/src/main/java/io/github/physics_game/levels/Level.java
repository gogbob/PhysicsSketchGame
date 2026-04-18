package io.github.physics_game.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
    private int numDrawnObjects;
    private Texture background;

    public Level(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects, float viewPortWidth, float viewPortHeight) {
        this.levelId = levelId;
        this.levelName = levelName;
        physicsObjects.addAll(internalObjects);
        float wallWidth = 1f;
        float floorHeight = 1;
        List<Vector2> floorPoly = Arrays.asList(new Vector2(0, 0), new Vector2(viewPortWidth, 0), new Vector2(viewPortWidth, floorHeight), new Vector2(0, floorHeight));
        StaticObject floor = new StaticObject(-1, 0.5f, 0.5f, floorPoly, 0, 0, 0);
        StaticObject ceiling = new StaticObject(-2, 0.5f, 0.5f, floorPoly, 0, viewPortHeight - floorHeight, 0);

        List<Vector2> wallPoly = Arrays.asList(new Vector2(0, 0), new Vector2(wallWidth, 0), new Vector2(wallWidth, viewPortHeight), new Vector2(0, viewPortHeight));
        StaticObject leftWall = new StaticObject(-3, 0.5f, 0.5f, wallPoly, 0, 0, 0);
        StaticObject rightWall = new StaticObject(-4, 0.5f, 0.5f, wallPoly, viewPortWidth - wallWidth, 0, 0);

        floor.setColor(Color.GRAY);
        ceiling.setColor(Color.GRAY);
        leftWall.setColor(Color.GRAY);
        rightWall.setColor(Color.GRAY);

        physicsObjects.add(floor);
        physicsObjects.add(ceiling);
        physicsObjects.add(leftWall);
        physicsObjects.add(rightWall);
    }

    public ArrayList<PhysicsObject> getPhysicsObjects() {
        return physicsObjects;
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
    public Texture getBackground() {
        return background;
    }
    public void setBackground(Texture background) {
        this.background = background;
    }
    public void setNumDrawnObjects(int numDrawnObjects) {
        this.numDrawnObjects = numDrawnObjects;
    }
    public int getNumDrawnObjects() {
        return numDrawnObjects;
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
