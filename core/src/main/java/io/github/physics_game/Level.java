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
        List<Vector2> floorPoly = Arrays.asList(new Vector2(0, 0), new Vector2(10, 0), new Vector2(10, 1), new Vector2(0, 1));
        StaticObject floor = new StaticObject(-1, 0.5f, 0.5f, floorPoly, 0, 0, 0, world);

        List<Vector2> wallPoly = Arrays.asList(new Vector2(0, 0), new Vector2(2, 0), new Vector2(2, 10), new Vector2(0, 10));
        StaticObject leftWall = new StaticObject(-2, 0.5f, 0.5f, wallPoly, 0, 0, 0, world);
        StaticObject rightWall = new StaticObject(-3, 0.5f, 0.5f, wallPoly, 5, 0, 0, world);

        physicsObjects.add(floor);
        physicsObjects.add(leftWall);
        physicsObjects.add(rightWall);
    }

    public ArrayList<PhysicsObject> getPhysicsObjects() {
        return new ArrayList<>(physicsObjects);
    }


}
