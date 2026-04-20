package io.github.physics_game.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.DrawType;
import io.github.physics_game.object_types.DynamicObject;
import io.github.physics_game.object_types.PhysicsObject;
import io.github.physics_game.object_types.StaticObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class Level {
    private int levelId;
    private String levelName;
    private ArrayList<PhysicsObject> physicsObjects = new ArrayList<>();
    private boolean runPhysics;
    private final ArrayList<DrawType> drawTypes;
    private final ArrayList<Float> drawAmounts;
    private ArrayList<Float> currentDrawnAmounts;
    private int selectedPaint = 0;
    private int numDrawnObjects;
    private float freeProp = 0.5f;
    private Texture background;
    private float levelTimer = 0f;
    private String description;

    public Level(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects, ArrayList<DrawType> drawTypes, ArrayList<Float> drawAmounts, float viewPortWidth, float viewPortHeight) {
        if(drawTypes.size() != drawAmounts.size()) {
            throw new IllegalArgumentException("drawTypes and drawAmounts must be the same size");
        }

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
        this.drawTypes = drawTypes;
        this.drawAmounts = drawAmounts;
        this.currentDrawnAmounts = new ArrayList<>(Collections.nCopies(drawAmounts.size(), 0f));
        this.description = "";
    }

    public ArrayList<PhysicsObject> getPhysicsObjects() {
        return physicsObjects;
    }
    public void addPhysicsObject(PhysicsObject obj) {
        physicsObjects.add(obj);
    }
    public boolean getRunPhysics() {
        return runPhysics;
    }
    public void setRunPhysics(boolean runPhysics) {
        this.runPhysics = runPhysics;
    }
    public ArrayList<DrawType> getDrawTypes() {
        return drawTypes;
    }
    public ArrayList<Float> getDrawAmounts() {
        return drawAmounts;
    }
    public ArrayList<Float> getCurrentDrawnAmounts() {
        return currentDrawnAmounts;
    }
    public float getCurrentDrawnProportion() {
        float total = 0f;
        for(int i = 0; i < drawAmounts.size(); i++) {
            total += ((currentDrawnAmounts.get(i) / drawAmounts.get(i)) / currentDrawnAmounts.size());
        }
        return total;
    }
    public int getSelectedPaint() {
        return selectedPaint;
    }
    public void setSelectedPaint(int selectedPaint) {
        this.selectedPaint = selectedPaint;
    }
    public float getDrawLeft() {
        return getDrawAmounts().get(getSelectedPaint()) - getCurrentDrawnAmounts().get(getSelectedPaint());
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
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
    public void setLevelTimer(float levelTimer) {
        this.levelTimer = levelTimer;
    }
    public float getLevelTimer() {
        return levelTimer;
    }
    public void setFreeProp(float freeProp) {
        this.freeProp = freeProp;
    }
    public float getFreeProp() {
        return freeProp;
    }

    public abstract boolean isComplete();
    public abstract LevelTickData tick(float deltaTime);

    public void reset() {
        reinitialize();
        numDrawnObjects = 0;
    }

    public void reinitialize() {
        setRunPhysics(false);
        for(int i = 0; i < physicsObjects.size(); i++) {
            if(physicsObjects.get(i).getId() >= 100) {
                physicsObjects.remove(i);
                i--;
            } else {
                physicsObjects.get(i).reinitialize();
            }
        }

        for(int i = 0; i < currentDrawnAmounts.size(); i++) {
            currentDrawnAmounts.set(i, 0f);
        }

        this.levelTimer = 0f;
    }

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
