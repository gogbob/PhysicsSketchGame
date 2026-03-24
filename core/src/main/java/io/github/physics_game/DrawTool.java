package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static java.lang.Math.*;

public class DrawTool {
    private static final float resolutionScale = 0.01f;

    private ArrayList<Vector2> points; // store drew points
    private boolean drawing; // check if it's drawing or not
    private Camera camera;
    private Level level;
    private int nextId; // give id to objects
    private List<Vector2> circleShape;
    private float toolWidth = 0.1f; // the width of the tool (the radius of the circle)
    private Vector2 prevPosition;
    private List<List<Float>> gridField;
    private Vector2 referencePoint;
    private int minX = -50;
    private int maxX = 50;
    private int minY = -50;
    private int maxY = 50;
    //make it in anticlockwise order
    private static final int[][] edgeTable = {
        {-1,-1,-1,-1},        // 0 0000

        {0,3,-1,-1},          // 1 0001
        {1,0,-1,-1},          // 2 0010
        {1,3,-1,-1},          // 3 0011

        {2,1,-1,-1},          // 4 0100
        {1,0, 2,3},           // 5 0101 (ambiguous → 2 segments)
        {2,0,-1,-1},          // 6 0110
        {2,3,-1,-1},          // 7 0111

        {3,2,-1,-1},          // 8 1000
        {2,0,-1,-1},          // 9 1001
        {2,0, 3,1},           // 10 1010 (ambiguous)
        {1,2,-1,-1},          // 11 1011

        {3,1,-1,-1},          // 12 1100
        {1,0,-1,-1},          // 13 1101
        {3,0,-1,-1},          // 14 1110

        {-1,-1,-1,-1}         // 15 1111
    };

    public DrawTool(Camera camera, Level level, float toolWidth) {
        this.camera = camera;
        this.level = level;
        this.points = new ArrayList<>();
        this.drawing = false;
        this.nextId = 100;  // object drew by player = start from 100
        this.toolWidth = toolWidth;
    }

    public DrawTool(Camera camera, Level level) {
        this(camera, level, 0.1f);
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

    // start drawing method (make a circle)
    private void startDrawing() {

        drawing = true;
        points.clear();
        Vector2 pos = getMousePosition();
        for (Vector2 localPoint : circleShape) {
            points.add(new Vector2(pos).add(localPoint));
        }

        referencePoint = pos;

        prevPosition = pos;
    }

    // add point method
    private void addPoint() {
        Vector2 pos = getMousePosition();
        Vector2 delta = new Vector2(pos).sub(prevPosition);
        addPixelValues(pos, delta);
    }

    private void addPixelValues(Vector2 pos, Vector2 delta) {

        Vector2 localPos = new Vector2(pos).sub(referencePoint);
        List<Integer> bounds = getBounds(pos, delta);
        updateGlobalBounds(bounds.get(0), bounds.get(1), bounds.get(2), bounds.get(3));

        for(int i = bounds.get(0); i < bounds.get(2); i++) {
            for(int j =  bounds.get(1); j < bounds.get(3); j++) {
                Vector2 pixelPos = new Vector2(i / resolutionScale, j / resolutionScale);
                Vector2 AP = new Vector2(pixelPos).sub(localPos);
                //this rectBound variable tells you whether the point between the start and end of the line segment
                //if it is between 0 and 1
                float rectBound = AP.dot(delta) / delta.dot(delta);

                //if within the rect or within the end
                //but not within the start because it should already be drawn
                if(((rectBound >= 0 && rectBound <= 1) ||
                    (new Vector2(pixelPos).sub(new Vector2(localPos.add(delta))).len() <= toolWidth) &&
                    !((new Vector2(pixelPos).sub(localPos)).len() <= toolWidth))) {
                    //if the point is within the tool width of the line segment, add it to the grid field
                    Vector2 tangent = new Vector2(delta).scl(AP.dot(delta)/delta.len());
                    Vector2 perpendicular = new Vector2(pixelPos).sub(new Vector2(localPos).add(tangent));
                    if(perpendicular.len() <= toolWidth) {
                        int gridX = i - minX;
                        int gridY = j - minY;
                        gridField.get(gridY).set(gridX, gridField.get(gridY).get(gridX) + 1f);
                    }
                }
            }
        }
    }

    private void updateGlobalBounds(int miX, int miY, int maX, int maY) {

        if(miX < minX) {
            for(int i = minY; i <= maxY; i++) {
                for(int j = miX; j < minX; j++) {
                    gridField.get(i).add(0, 0f);
                }
            }
            minX = miX;
        }
        if(miY < minY) {
            for(int i = miY; i < minY; i++) {
                gridField.add(0, new ArrayList<>(maxX - minX + 1));
            }
            minY = miY;
        }
        if(maX > maxX) {
            for(int i = minY; i <= maxY; i++) {
                for(int j = maxX + 1; j <= maX; j++) {
                    gridField.get(i).add(0f);
                }
            }
            maxX = maX;
        }
        if(maY > maxY) {
            for(int i = maxY + 1; i <= maY; i++) {
                gridField.add(new ArrayList<>(maxX - minX + 1));
            }
            maxY = maY;
        }
    }

    private List<Integer> getBounds(Vector2 pos, Vector2 delta) {
        int minX = (int)(min(pos.x - toolWidth - 1, pos.x + delta.x - toolWidth - 1)/ resolutionScale);
        int maxX = (int)(max(pos.x + toolWidth + 1, pos.x + delta.x + toolWidth + 1) / resolutionScale);
        int minY = (int)(min(pos.y - toolWidth - 1, pos.y + delta.y - toolWidth - 1) / resolutionScale);
        int maxY = (int)(max(pos.y + toolWidth + 1, pos.y + delta.y + toolWidth + 1) / resolutionScale);

        return Arrays.asList(minX, maxX, minY, maxY);
    }

    private List<Vector2> performMarchingSquares() {
        return null;
    }

    private void createPixelatedEdges() {
        for(int y = 0; y < gridField.size() - 1; y++) {
            for(int x = 0; x < gridField.get(y).size() - 1; x++) {
                float v0 = gridField.get(y).get(x);
                float v1 = gridField.get(y).get(x+1);
                float v2 = gridField.get(y+1).get(x+1);
                float v3 = gridField.get(y+1).get(x);

                int caseIndex = 0;
                if(v0 > 0) caseIndex |= 1;
                if(v1 > 0) caseIndex |= 2;
                if(v2 > 0) caseIndex |= 4;
                if(v3 > 0) caseIndex |= 8;


            }
        }
    }

    private List<Vector2> drawFromLookUp(int caseIndex, int x, int y) {
        int[] edgeKey = edgeTable[caseIndex];
        List<Vector2> edgePoints = new ArrayList<>();
        for(int i = 0; i < edgeKey.length; i+=2) {
            if(edgeKey[i] == -1) break;
            switch(edgeKey[i]) {
                case 0:
                    edgePoints.add(new Vector2(x + 0.5f, y));
                    break;
                case 1:
                    edgePoints.add(new Vector2(x + 1, y + 0.5f));
                    break;
                case 2:
                    edgePoints.add(new Vector2(x + 0.5f, y + 1));
                    break;
                case 3:
                    edgePoints.add(new Vector2(x, y + 0.5f));
                    break;
            }
        }
        return edgePoints;
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

