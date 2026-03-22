package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;

public class DrawTool {
    private ArrayList<Vector2> points; // store drew points
    private boolean drawing; // check if it's drawing or not
    private Camera camera;
    private Level level;
    private int nextId; // give id to objects

    public DrawTool(Camera camera, Level level) {
        this.camera = camera;
        this.level = level;
        this.points = new ArrayList<>();
        this.drawing = false;
        this.nextId = 100;  // object drew by player = start from 100
    }

    // call the method each frame
    public void update() {
        // press mouse = start to draw
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            startDrawing();
        }

        // continue press mouse = continue to draw
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing) {
            addPoint();
        }

        // release mouse = done + create object
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing) {
            finishDrawing();
        }
    }

    // start drawing method
    private void startDrawing() {
        drawing = true;
        points.clear();
        Vector2 pos = getMousePosition();
        points.add(pos);
    }

    // add point method
    private void addPoint() {
        Vector2 pos = getMousePosition();

        // add when it is far from the las point
        if (points.size() == 0) {
            points.add(pos);
        } else {
            Vector2 lastPoint = points.get(points.size() - 1);
            float distance = pos.dst(lastPoint);
            if (distance > 0.1f) {  // add when the distance is bigger than 0.1
                points.add(pos);
            }
        }
    }

    // finish drawing + create object method
    private void finishDrawing() {
        drawing = false;

        // check if there's enough points (big enough or not)
        if (points.size() < 3) {
            System.out.println("NOT BIG ENOUGH, PLEASE DRAW MORE POINTS");
            points.clear();
            return;
        }

        // create object
        createObject();
        points.clear();
    }

    // create object methoooood
    private void createObject() {
        // find the center of object
        Vector2 center = new Vector2(0, 0);
        for (Vector2 p : points) {
            center.add(p);
        }
        center.scl(1.0f / points.size());  // divide by point = average = center

        // change all point to position relative to center
        ArrayList<Vector2> localPoints = new ArrayList<>();
        for (Vector2 p : points) {
            Vector2 local = new Vector2(p.x - center.x, p.y - center.y);
            localPoints.add(local);
        }

        // create DynamicObject
        DynamicObject obj = new DynamicObject(
            nextId, // ID
            0.5f, // friction
            0.3f, // bouncy
            1f, // density
            localPoints, // shape's point
            center.x, // x position
            center.y, // y position
            0 // rotation angle
        );

        nextId++;  // next object ID + 1
        level.addPhysicsObject(obj);

        System.out.println("You have created the object! The point size is " + localPoints.size());
    }

    // get mouse position in world methode
    private Vector2 getMousePosition() {
        // the position of mouse on screen
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // change into the position in game world
        Vector3 screenPos = new Vector3(screenX, screenY, 0);
        Vector3 worldPos = camera.unproject(screenPos);

        return new Vector2(worldPos.x, worldPos.y);
    }

    // get the current drawing point ( to draw line )
    public ArrayList<Vector2> getPoints() {
        return points;
    }

    // check if it is drawing or not
    public boolean isDrawing() {
        return drawing;
    }
}

