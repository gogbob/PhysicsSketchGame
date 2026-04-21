package io.github.physics_game;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.collision.ContactManifold;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.levels.Level;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.*;

public class DrawTool {
    private static final float resolutionScale = 0.05f;
    private static final float rangeMax = 0.05f;
    public static final float minDist = 0.5f;

    private long elapsedTime = 0;
    private ReentrantLock lock = new ReentrantLock();
    public float chargeDensity = 2.0f;
    private boolean drawing; // check if it's drawing or not
    private Vector2 worldPos = new Vector2();
    private DrawType drawType = DrawType.NORMAL;
    private int nextId; // give id to objects
    private final float toolWidth; // the width of the tool (the radius of the circle)
    private final float density = 0.1f;
    private float mass = 0f;
    private float inertia = 0f;
    private Vector2 com = new Vector2();
    private Vector2 prevPosition;
    private List<List<Float>> gridField;
    private List<Vector2> pointSegments;
    private List<Float> massSegments;
    private Vector2 referencePoint;
    private List<Vector2> exteriorLoop = new ArrayList<>();
    private List<List<Vector2>> interiorLoops = new ArrayList<>();
    private int minX = -50;
    private int maxX = 50;
    private int minY = -50;
    private int maxY = 50;
    //make it in anticlockwise order


    public DrawTool(float toolWidth) {
        this.drawing = false;
        this.nextId = 100;  // object drew by player = start from 100
        this.toolWidth = toolWidth;
    }

    public void reset() {
        nextId = 100;
        drawing = false;
    }

    // call the method each frame
    public synchronized void update(DrawType drawType, Level currentLevel, int inputKey, float worldPosX, float worldPosY) {
        if(lock.tryLock()) {
            try {
                elapsedTime = System.currentTimeMillis();
                PhysicsObject obj = null;
                worldPos.set(worldPosX, worldPosY);
                // release mouse = done + create object
                if ((!(inputKey == 2) || currentLevel.getDrawLeft() <= 0.0001f || (inputKey == 1)) && drawing) {
                    if(currentLevel.getDrawLeft() <= 0.0001f) System.out.println("No more Draw");
                    obj = finishDrawing(drawType, currentLevel);
                }

                // press mouse = start to draw
                if ((inputKey == 1) && currentLevel.getDrawLeft() >= toolWidth) {
                    currentLevel.getCurrentDrawnAmounts().set(currentLevel.getSelectedPaint(),
                        currentLevel.getCurrentDrawnAmounts().get(currentLevel.getSelectedPaint()) + toolWidth);
                    obj = startDrawing(drawType, currentLevel);
                }

                // continue press mouse = continue to draw
                if ((inputKey == 2) && drawing && new Vector2(worldPos).sub(prevPosition).len() > minDist) {
                    obj = addPoint(false, currentLevel);
                }

                if(obj != null) {
                    currentLevel.setRunPhysics(true);
                    if (obj instanceof DynamicObject) {
                        currentLevel.getPhysicsObjects().removeIf(o -> o.getId() >= 1000);
                        currentLevel.addPhysicsObject(obj);
                        currentLevel.setNumDrawnObjects(currentLevel.getNumDrawnObjects() + 1);
                    } else {
                        currentLevel.getPhysicsObjects().removeIf(o -> o.getId() >= 1000);
                        currentLevel.addPhysicsObject(obj);
                    }
                }

            } finally {
                lock.unlock();
            }
        }
    }

    // start drawing method (make a circle)
    private StaticObject startDrawing(DrawType drawType, Level currentLevel) {
        Vector2 pos = new Vector2(worldPos);

        PhysicsObject tempCircle = new StaticObject(2000, 1.0f, 1.0f, PhysicsResolver.getCircleVertices(12, toolWidth), pos.x, pos.y, 0f);
        for(PhysicsObject object : currentLevel.getPhysicsObjects()) {
            if(!(object instanceof UncollidableObject) || (object instanceof NoDrawField)) {
                ContactManifold manifold = CustomContactHandler.detect(tempCircle, object);
                if(manifold.isColliding() && manifold.getPointCount() > 0) {
                    //invalid place to draw
                    return null;
                }
            }
        }
        drawing = true;
        if(drawType != null) this.drawType = drawType;
        exteriorLoop.clear();
        interiorLoops.clear();
        mass = (toolWidth * toolWidth * (float)PI)*density;
        inertia = 1/2f * mass * toolWidth * toolWidth;
        massSegments = new ArrayList<>();
        massSegments.add(mass);
        pointSegments = new ArrayList<>();
        pointSegments.add(new Vector2());
        com.setZero();
        minX = -50;
        maxX = 50;
        minY = -50;
        maxY = 50;
        resetGridField();

        referencePoint = new Vector2(pos.x, pos.y);

        List<Integer> bounds = getBounds(new Vector2(), new Vector2());
        updateGlobalBounds(bounds.get(0) - 2, bounds.get(2) - 2, bounds.get(1) + 2, bounds.get(3) + 2);
        for(int i = bounds.get(0); i < bounds.get(1); i++) {
            for(int j = bounds.get(2); j < bounds.get(3); j++) {
                Vector2 pixelPos = new Vector2(i * resolutionScale, j * resolutionScale);
                float dist = new Vector2(pixelPos).len();
                if(dist <= toolWidth + rangeMax) {

                    float addValue = dist <= toolWidth ? 1f : max(0f, (toolWidth + rangeMax - dist) / rangeMax);
                    int gridX = i - minX;
                    int gridY = j - minY;
                    gridField.get(gridY).set(gridX, Math.max(gridField.get(gridY).get(gridX), addValue));
                }
            }
        }

        System.out.println("Elapsed time draw circle: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();

        List<Vector2> contour = MarchingSquares.generateLocalContours(false, gridField, minX, minY, resolutionScale);

        referencePoint = pos;

        prevPosition = pos;
        PhysicsObject preview = buildCurrentObject(contour, false);
        System.out.println("Elapsed time build object: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();
        if(preview instanceof StaticObject) {
            return (StaticObject) preview;
        }
        return null;
    }

    // add point method
    private PhysicsObject addPoint(boolean buildDynamic, Level currentLevel) {
        Vector2 pos = new Vector2(worldPos);
        Vector2 delta = new Vector2(pos).sub(prevPosition);

        //update delta if there is too much being drawn
        if(delta.len() > currentLevel.getDrawLeft()) {
            delta.nor().scl(currentLevel.getDrawLeft());
            pos = new Vector2(prevPosition).add(delta);
        }

        List<Vector2> circleVertTemp = PhysicsResolver.getCircleVertices(12, toolWidth);
        List<Vector2> segmentTemp = new ArrayList<>();

        System.out.println("Elapsed time create init: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();

        for(int i = 0; i < circleVertTemp.size(); i++) {
            Vector2 edge = new Vector2(circleVertTemp.get(i)).sub(circleVertTemp.get((i + 1) % circleVertTemp.size()));
            //check if the edge normal points with the delta
            Vector2 normal = new Vector2(edge.y, -edge.x);
            if(delta.dot(normal) >= 0) {
                if(segmentTemp.size() == 0) {
                    segmentTemp.add(new Vector2(circleVertTemp.get(i)).add(delta));
                    segmentTemp.add(new Vector2(circleVertTemp.get((i + 1) % circleVertTemp.size())).add(delta));
                } else {
                    segmentTemp.add(new Vector2(circleVertTemp.get((i + 1) % circleVertTemp.size())).add(delta));
                }
            }
        }
        segmentTemp.add(new Vector2(-delta.y, delta.x).nor().scl(toolWidth));
        segmentTemp.add(new Vector2(delta.y, -delta.x).nor().scl(toolWidth));

        System.out.println("Elapsed time create temp segment: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();

        PhysicsObject tempSegment = new StaticObject(2000, 1.0f, 1.0f, segmentTemp, prevPosition.x, prevPosition.y, 0f);
        for(PhysicsObject object : currentLevel.getPhysicsObjects()) {
            if((!(object instanceof UncollidableObject) || (object instanceof NoDrawField)) && object.getId() < 1000) {
                ContactManifold manifold = CustomContactHandler.detect(tempSegment, object);
                if(manifold.isColliding() && manifold.getPointCount() > 0) {
                    //invalid place to draw
                    return null;
                }
            }
        }

        currentLevel.getCurrentDrawnAmounts().set(currentLevel.getSelectedPaint(),
            currentLevel.getCurrentDrawnAmounts().get(currentLevel.getSelectedPaint()) + delta.len());

        System.out.println("Elapsed time find intersection: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();

        addPixelValues(pos, delta);
        System.out.println("Elapsed time draw segment: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();
        updateDrawingMetrics(new Vector2(prevPosition).sub(referencePoint), new Vector2(pos).sub(referencePoint));
        prevPosition = pos;
        List<Vector2> contour = MarchingSquares.generateLocalContours(false, gridField, minX, minY, resolutionScale);
        System.out.println("Elapsed time draw contour: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();
        PhysicsObject temp = buildCurrentObject(contour, buildDynamic);

        System.out.println("Elapsed time build object: " + ((float)(System.currentTimeMillis() - elapsedTime) / 1000f));
        elapsedTime = System.currentTimeMillis();

        return temp;
    }

    public void testAddPoint(boolean buildDynamic) {
        List<Vector2> contour = MarchingSquares.generateLocalContours(true, gridField, minX, minY, resolutionScale);
        buildCurrentObject(contour, buildDynamic);
    }

    private void addPixelValues(Vector2 pos, Vector2 delta) {

        Vector2 localEnd = new Vector2(pos).sub(referencePoint);
        Vector2 localStart = new Vector2(prevPosition).sub(referencePoint);
        Vector2 localDelta = new Vector2(localEnd).sub(localStart);
        List<Integer> bounds = getBounds(localStart, delta);
        updateGlobalBounds(bounds.get(0) - 2, bounds.get(2) - 2, bounds.get(1) + 2, bounds.get(3) + 2);

        float deltaLen2 = localDelta.dot(localDelta);

        for(int i = bounds.get(0) - 1; i <= bounds.get(1) + 1; i++) {
            for(int j =  bounds.get(2) - 1; j <= bounds.get(3) + 1; j++) {
                Vector2 pixelLocal = new Vector2(i * resolutionScale, j * resolutionScale);
                Vector2 AP = new Vector2(pixelLocal).sub(localStart);
                //this rectBound variable tells you whether the point between the start and end of the line segment
                //if it is between 0 and 1
                float rectBound = deltaLen2 > 1e-6f ? AP.dot(localDelta) / deltaLen2 : 0f;
                float distToStart = new Vector2(pixelLocal).sub(localStart).len();
                float distToEnd = new Vector2(pixelLocal).sub(localEnd).len();

                //if within the rect or within the end
                //but not within the start because it should already be drawn
                if(((rectBound >= 0 && rectBound <= 1) ||
                    (distToEnd <= toolWidth + rangeMax)) &&
                    !(distToStart <= toolWidth)) {
                    //if the point is within the tool width of the line segment, add it to the grid field
                    float clampedBound = max(0f, min(1f, rectBound));
                    Vector2 closestPoint = new Vector2(localStart).add(new Vector2(localDelta).scl(clampedBound));
                    Vector2 perpendicular = new Vector2(pixelLocal).sub(closestPoint);
                    if(perpendicular.len() <= toolWidth || distToEnd <= toolWidth + rangeMax) {
                        int gridX = i - minX;
                        int gridY = j - minY;
                        float addValue = (distToEnd <= toolWidth || perpendicular.len() <= toolWidth)
                            ? 1f
                            : max(0f, (toolWidth + rangeMax - distToEnd) / rangeMax);
                        gridField.get(gridY).set(gridX, Math.max(gridField.get(gridY).get(gridX), addValue));
                    }
                }
            }
        }
    }

    private void updateGlobalBounds(int miX, int miY, int maX, int maY) {

        if(miX < minX) {
            int shift = minX - miX;
            for(int i = 0; i < gridField.size(); i++) {
                for(int j = 0; j < shift; j++) {
                    gridField.get(i).add(0, 0f);
                }
            }
            minX = miX;
        }
        if(miY < minY) {
            for(int i = miY; i < minY; i++) {
                gridField.add(0, new ArrayList<>(Collections.nCopies(maxX - minX + 1, 0f)));
            }
            minY = miY;
        }
        if(maX > maxX) {
            int shift = maX - maxX;
            for(int i = 0; i < gridField.size(); i++) {
                for(int j = 0; j < shift; j++) {
                    gridField.get(i).add(0f);
                }
            }
            maxX = maX;
        }
        if(maY > maxY) {
            for(int i = maxY + 1; i <= maY; i++) {
                gridField.add(new ArrayList<>(Collections.nCopies(maxX - minX + 1, 0f)));
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

    private void updateDrawingMetrics(Vector2 start, Vector2 end) {
        Vector2 delta = new Vector2(end).sub(start);
        float length = delta.len();
        if(length < 1e-8f) {
            return;
        }

        float radius = toolWidth;
        float rectArea = length * (2f * radius);
        float capArea = (float)(0.5f * PI * radius * radius);
        float rectMass = rectArea * density;
        float capMass = capArea * density;
        float addedMass = rectMass + capMass;

        Vector2 dir = new Vector2(delta).nor();
        Vector2 rectCenter = new Vector2(start).add(end).scl(0.5f);
        Vector2 capCenter = new Vector2(end).sub(new Vector2(dir).scl((float)(4f * radius / (3f * PI))));

        pointSegments.add((new Vector2(rectCenter).scl(rectMass))
            .add(new Vector2(capCenter).scl(capMass)).scl(1f / addedMass));
        massSegments.add(addedMass);

        float totalMass = mass + addedMass;
        Vector2 prevCom = new Vector2(com);
        if(totalMass > 1e-8f) {
            Vector2 weighted = new Vector2(com).scl(mass)
                .add(new Vector2(rectCenter).scl(rectMass))
                .add(new Vector2(capCenter).scl(capMass));
            com = weighted.scl(1f / totalMass);
        }

        inertia -= totalMass * (new Vector2(com).sub(prevCom).len() * new Vector2(com).sub(prevCom).len());

        float rectInertia = rectMass * (length * length + (2f * radius) * (2f * radius)) / 12f;
        float capInertia = capMass * radius * radius / 2f;
        inertia += rectInertia + capInertia + rectMass * rectCenter.dst2(com) + capMass * capCenter.dst2(com);
        mass = totalMass;
    }

    private PhysicsObject buildCurrentObject(List<Vector2> contour, boolean dynamicObject) {
        if(contour == null) {
            System.out.println("Error: contour is null");
            return null;
        }

        if(dynamicObject) {
            DynamicObject obj;
            if(drawType == DrawType.ANTIGRAVITY) {
                obj = new AntigravityObject(nextId, 0.5f, 0.4f, density, contour,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                System.out.println("Creating Antigravity object");
            } else if(drawType == DrawType.POSITIVE ||  drawType == DrawType.NEGATIVE) {
                obj = new ChargedDynamicObject(nextId, 0.5f, 0.4f, density, contour,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments, (drawType == DrawType.POSITIVE)? chargeDensity : -chargeDensity);
                System.out.println("Creating Charged object");
            } else if(drawType == DrawType.ICY) {
                obj = new DynamicObject(nextId, 0.00f, 0.4f, density, contour,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                System.out.println("Creating Icy object");
                obj.setColor(new Color(0.8f, 0.8f, 1.0f, 1));
            } else {
                obj = new DynamicObject(nextId, 0.5f, 0.4f, density, contour,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                System.out.println("Creating Normal object");
            }
            nextId++;
            return obj;
        }

        StaticObject staticObject;
        if(drawType == DrawType.ANTIGRAVITY) {
            staticObject = new StaticObject(1000, 0.5f, 0.4f, density, contour, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
            staticObject.setColor(Color.CORAL);
        } else if(drawType == DrawType.POSITIVE ||  drawType == DrawType.NEGATIVE) {
            staticObject = new ChargedStaticObject(1000, 0.5f, 0.4f, density, contour, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments, (drawType == DrawType.POSITIVE)? chargeDensity : -chargeDensity);
        } else if(drawType == DrawType.ICY) {
            staticObject = new StaticObject(1000, 0.0f, 0.4f, density, contour, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
            staticObject.setColor(new Color(0.8f, 0.8f, 1.0f, 1));
        } else {
            staticObject = new StaticObject(1000, 0.5f, 0.4f, density, contour, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
        }
        return staticObject;
    }

    private void resetGridField() {
        gridField = new ArrayList<>();
        int height = maxY - minY + 1;
        int width = maxX - minX + 1;
        for(int y = 0; y < height; y++) {
            gridField.add(new ArrayList<>(Collections.nCopies(width, 0f)));
        }
    }


    // finish drawing + create object method
    private DynamicObject finishDrawing(DrawType drawType, Level currentLevel) {
        DynamicObject temp = (DynamicObject)addPoint(true, currentLevel);
        if(temp == null) return null;
        drawing = false;
        if(drawType != null) this.drawType = drawType;
        return (DynamicObject)addPoint(true, currentLevel);
    }

    // check if it is drawing or not
    public boolean isDrawing() {
        return drawing;
    }
}
