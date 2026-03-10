package io.github.physics_game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Level {
    private int levelId;
    private String levelName;
    private ArrayList<PhysicsObject> physicsObjects = new ArrayList<>();

    public Level(int levelId, String levelName, ArrayList<PhysicsObject> internalObjects, World world) {
        this.levelId = levelId;
        this.levelName = levelName;
        physicsObjects.addAll(internalObjects);
        List<Vector2> floorPoly = Arrays.asList(new Vector2(-50, -1), new Vector2(50, -1), new Vector2(50, 0), new Vector2(-50, 0));
        StaticObject floor = new StaticObject(-1, 0.5f, 0.5f, floorPoly, 0, 0, 0, null);

        List<Vector2> wallPoly = Arrays.asList(new Vector2(-1, -50), new Vector2(0, -50), new Vector2(0, 50), new Vector2(-1, 50));
        StaticObject leftWall = new StaticObject(-2, 0.5f, 0.5f, wallPoly, -50, 0, 0, world);
        StaticObject rightWall = new StaticObject(-3, 0.5f, 0.5f, wallPoly, 50, 0, 0, world);

        physicsObjects.add(floor);
        physicsObjects.add(leftWall);
        physicsObjects.add(rightWall);
    }

    public void reinitialize() {
        for(PhysicsObject obj : physicsObjects) {
            obj.reinitialize();
        }
    }

    public int getNumberOfObjects() {
        return physicsObjects.size();
    }

    public PhysicsObject getPhysicsObject(int index)
    {
        if (index < 0 || index >= physicsObjects.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
        return physicsObjects.get(index);
    }


}
