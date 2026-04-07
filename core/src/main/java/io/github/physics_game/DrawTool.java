package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import io.github.physics_game.object_types.PhysicsObject;
import io.github.physics_game.object_types.DynamicObject;
import io.github.physics_game.object_types.StaticObject;

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

    private ReentrantLock lock = new ReentrantLock();

    private boolean drawing; // check if it's drawing or not
    private final Camera camera;
    private int nextId; // give id to objects
    private List<Vector2> circleShape;
    private final float toolWidth; // the width of the tool (the radius of the circle)
    private final float density = 0.1f;
    private float mass = 0f;
    private float inertia = 0f;
    private Vector2 com = new Vector2();
    private Vector2 prevPosition;
    private List<List<Float>> gridField;
    private Vector2 referencePoint;
    private List<Vector2> exteriorLoop = new ArrayList<>();
    private List<List<Vector2>> interiorLoops = new ArrayList<>();
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

    public DrawTool(Camera camera, float toolWidth) {
        this.camera = camera;
        this.drawing = false;
        this.nextId = 100;  // object drew by player = start from 100
        this.toolWidth = toolWidth;
    }

    // call the method each frame
    public PhysicsObject update() {
        if(lock.tryLock()) {
            try {
                // press mouse = start to draw
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    return startDrawing();
                }

                // continue press mouse = continue to draw
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing) {
                    return addPoint(false);
                }

                // release mouse = done + create object
                if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing) {
                    return finishDrawing();
                }
            } finally {
                lock.unlock();
            }
        }

        return null;
    }

    // start drawing method (make a circle)
    private StaticObject startDrawing() {

        drawing = true;
        exteriorLoop.clear();
        interiorLoops.clear();
        mass = 0f;
        inertia = 0f;
        com.setZero();
        minX = -50;
        maxX = 50;
        minY = -50;
        maxY = 50;
        resetGridField();
        Vector2 pos = getMousePosition();

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

        List<Vector2> contour = generateLocalContours();

        referencePoint = pos;

        prevPosition = pos;
        PhysicsObject preview = buildCurrentObject(contour, false);
        if(preview instanceof StaticObject) {
            return (StaticObject) preview;
        }
        return null;
    }

    // add point method
    private PhysicsObject addPoint(boolean buildDynamic) {
        Vector2 pos = getMousePosition();
        Vector2 delta = new Vector2(pos).sub(prevPosition);
        addPixelValues(pos, delta);
        updateDrawingMetrics(new Vector2(prevPosition).sub(referencePoint), new Vector2(pos).sub(referencePoint));
        prevPosition = pos;
        List<Vector2> contour = generateLocalContours();
        return buildCurrentObject(contour, buildDynamic);
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

    private List<Vector2> performMarchingSquares() {
        return generateLocalContours();
    }

    public List<Vector2> generateLocalContours() {
        createPixelatedEdges();
        if(exteriorLoop == null || exteriorLoop.size() < 3) {
            return null;
        }
        return exteriorLoop;
    }


    private void createPixelatedEdges() {
        List<MarchingSegment> segments = new ArrayList<>();
        Map<String, List<Integer>> adjacency = new HashMap<>();

        for(int y = 0; y < gridField.size() - 1; y++) {
            for(int x = 0; x < gridField.get(y).size() - 1; x++) {
                Vector2 p = new Vector2((float)(x + minX)*resolutionScale, (float)(y + minY)*resolutionScale);
                float v0 = gridField.get(y).get(x);
                float v1 = gridField.get(y).get(x + 1);
                float v2 = gridField.get(y + 1).get(x + 1);
                float v3 = gridField.get(y + 1).get(x);
                int c = getCaseId(v0, v1, v2, v3);
                if(c == 0 || c == 15) {
                    continue;
                }

                int[] edges = edgeTable[c];
                if(c == 5 || c == 10) {
                    addSegment(edgePos(edges[0], p, v0, v1, v2, v3), edgePos(edges[1], p, v0, v1, v2, v3), segments, adjacency);
                    addSegment(edgePos(edges[2], p, v0, v1, v2, v3), edgePos(edges[3], p, v0, v1, v2, v3), segments, adjacency);
                } else {
                    addSegment(edgePos(edges[0], p, v0, v1, v2, v3), edgePos(edges[1], p, v0, v1, v2, v3), segments, adjacency);
                }
            }
        }

        exteriorLoop = new ArrayList<>();
        interiorLoops = new ArrayList<>();

        if(segments.isEmpty()) {
            return;
        }

        Set<Integer> travelledEdges = new HashSet<>();
        List<List<Vector2>> foundLoops = new ArrayList<>();
        for(int i = 0; i < segments.size(); i++) {
            if(travelledEdges.contains(i)) {
                continue;
            }
            List<Vector2> loop = traceLoopFromSegment(i, segments, adjacency, travelledEdges);
            if(loop.size() >= 3) {
                ArrayList<Vector2> tessellatedLoop = new ArrayList<>(loop);
                tessellateContour(tessellatedLoop);
                if(tessellatedLoop.size() >= 3) {
                    foundLoops.add(tessellatedLoop);
                }
            }
        }

        if(foundLoops.isEmpty()) {
            return;
        }

        int outerIdx = 0;
        float largestArea = -1f;
        for(int i = 0; i < foundLoops.size(); i++) {
            float area = abs(signedArea(foundLoops.get(i)));
            if(area > largestArea) {
                largestArea = area;
                outerIdx = i;
            }
        }

        exteriorLoop = new ArrayList<>(foundLoops.get(outerIdx));
        for(int i = 0; i < foundLoops.size(); i++) {
            if(i != outerIdx) {
                interiorLoops.add(new ArrayList<>(foundLoops.get(i)));
            }
        }

        exteriorLoop = connectInteriorLoopsToExterior(exteriorLoop, interiorLoops);
    }

    private void tessellateContour(ArrayList<Vector2> contour) {
        for(int i = 2; i < contour.size() + 2 && contour.size() >= 3; i++) {
            Vector2 prevPoint = new Vector2(contour.get(((i - 2) + contour.size()) % contour.size()));
            Vector2 curPoint = new Vector2(contour.get(((i - 1) + contour.size()) % contour.size()));
            Vector2 newPoint = new Vector2(contour.get(i % contour.size()));
            Vector2 edge1 = new Vector2(curPoint).sub(prevPoint);
//            if(edge1.len() < 0.01f) {
//                Vector2 midPoint =  new Vector2(prevPoint).add(new Vector2(edge1).scl(1/2f));
//                contour.remove(((i - 2) + contour.size())%contour.size());
//                i--;
//                contour.set(((i - 2) + contour.size()) %contour.size(), midPoint);
//                prevPoint = new Vector2(contour.get(((i - 2) + contour.size()) % contour.size()));
//                curPoint = new Vector2(contour.get(((i - 1) + contour.size()) % contour.size()));
//                newPoint = new Vector2(contour.get((i + contour.size()) % contour.size()));
//            }

            edge1 = new Vector2(curPoint).sub(prevPoint);
            Vector2 edge2 = new Vector2(newPoint).sub(curPoint);

            if(Math.abs(edge1.len() * edge2.len() - edge1.dot(edge2)) < 0.001f) {
                contour.remove((i - 1)%contour.size());
                i--;
            }
        }
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
            Gdx.app.log("ERROR", "contour is null");
            return null;
        }
        if(dynamicObject) {
            DynamicObject obj = new DynamicObject(nextId, 0.5f, 0.4f, 1f, contour,
                referencePoint.x, referencePoint.y, 0, mass, inertia, com);
            nextId++;
            return obj;
        }

        return new StaticObject(1000, 0.5f, 0.4f, contour, referencePoint.x, referencePoint.y, 0);
    }

    private void addSegment(Vector2 a, Vector2 b, List<MarchingSegment> segments, Map<String, List<Integer>> adjacency) {
        if(pointKey(a).equals(pointKey(b))) {
            return;
        }
        int idx = segments.size();
        MarchingSegment segment = new MarchingSegment(a, b);
        segments.add(segment);
        adjacency.computeIfAbsent(segment.keyA, k -> new ArrayList<>()).add(idx);
        adjacency.computeIfAbsent(segment.keyB, k -> new ArrayList<>()).add(idx);
    }

    private List<Vector2> traceLoopFromSegment(int startSegment,
                                               List<MarchingSegment> segments,
                                               Map<String, List<Integer>> adjacency,
                                               Set<Integer> travelledEdges) {
        List<Vector2> loop = new ArrayList<>();
        MarchingSegment start = segments.get(startSegment);

        String startKey = start.keyA;
        String prevKey = start.keyA;
        String currentKey = start.keyB;

        loop.add(new Vector2(start.a));
        loop.add(new Vector2(start.b));
        travelledEdges.add(startSegment);

        int guard = 0;
        int maxStep = Math.max(segments.size() * 2, 64);
        while(!currentKey.equals(startKey) && guard++ < maxStep) {
            int nextSegment = findNextSegment(currentKey, prevKey, segments, adjacency, travelledEdges);
            if(nextSegment < 0) {
                break;
            }

            MarchingSegment seg = segments.get(nextSegment);
            String nextKey = seg.otherKey(currentKey);
            loop.add(new Vector2(seg.pointForKey(nextKey)));

            travelledEdges.add(nextSegment);
            prevKey = currentKey;
            currentKey = nextKey;
        }

        if(loop.size() > 1) {
            Vector2 first = loop.get(0);
            Vector2 last = loop.get(loop.size() - 1);
            if(first.epsilonEquals(last, 1e-5f)) {
                loop.remove(loop.size() - 1);
            }
        }

        return loop;
    }

    private int findNextSegment(String currentKey,
                                String prevKey,
                                List<MarchingSegment> segments,
                                Map<String, List<Integer>> adjacency,
                                Set<Integer> travelledEdges) {
        List<Integer> candidates = adjacency.getOrDefault(currentKey, Collections.emptyList());
        int fallback = -1;
        for(int segmentIdx : candidates) {
            if(travelledEdges.contains(segmentIdx)) {
                continue;
            }
            MarchingSegment seg = segments.get(segmentIdx);
            String otherKey = seg.otherKey(currentKey);
            if(!otherKey.equals(prevKey)) {
                return segmentIdx;
            }
            fallback = segmentIdx;
        }
        return fallback;
    }

    private float signedArea(List<Vector2> loop) {
        float area = 0f;
        for(int i = 0; i < loop.size(); i++) {
            Vector2 a = loop.get(i);
            Vector2 b = loop.get((i + 1) % loop.size());
            area += a.x * b.y - b.x * a.y;
        }
        return 0.5f * area;
    }

    private List<Vector2> connectInteriorLoopsToExterior(List<Vector2> outerLoop, List<List<Vector2>> innerLoops) {
        if(outerLoop == null || outerLoop.size() < 3 || innerLoops.isEmpty()) {
            return outerLoop;
        }

        List<Vector2> merged = new ArrayList<>(outerLoop);
        for(List<Vector2> innerLoop : innerLoops) {
            if(innerLoop == null || innerLoop.size() < 3) {
                continue;
            }
            int[] bridge = findClosestBridge(merged, innerLoop);
            merged = spliceLoopAtBridge(merged, innerLoop, bridge[0], bridge[1]);
        }

        return merged;
    }

    private int[] findClosestBridge(List<Vector2> outerLoop, List<Vector2> innerLoop) {
        int outerIndex = 0;
        int innerIndex = 0;
        float bestDist2 = Float.MAX_VALUE;

        for(int i = 0; i < outerLoop.size(); i++) {
            Vector2 outer = outerLoop.get(i);
            for(int j = 0; j < innerLoop.size(); j++) {
                float dist2 = outer.dst2(innerLoop.get(j));
                if(dist2 < bestDist2) {
                    bestDist2 = dist2;
                    outerIndex = i;
                    innerIndex = j;
                }
            }
        }

        return new int[]{outerIndex, innerIndex};
    }

    private List<Vector2> spliceLoopAtBridge(List<Vector2> outerLoop, List<Vector2> innerLoop, int outerIndex, int innerIndex) {
        List<Vector2> orderedInner = new ArrayList<>(innerLoop);
        if(Math.signum(signedArea(outerLoop)) == Math.signum(signedArea(orderedInner))) {
            Collections.reverse(orderedInner);
            innerIndex = orderedInner.size() - 1 - innerIndex;
        }

        List<Vector2> merged = new ArrayList<>();

        for(int i = 0; i <= outerIndex; i++) {
            merged.add(new Vector2(outerLoop.get(i)));
        }

        Vector2 bridgeOuter = outerLoop.get(outerIndex);
        Vector2 bridgeInner = orderedInner.get(innerIndex);
        merged.add(new Vector2(bridgeInner));

        for(int i = 1; i < orderedInner.size(); i++) {
            int idx = (innerIndex + i) % orderedInner.size();
            merged.add(new Vector2(orderedInner.get(idx)));
        }

        merged.add(new Vector2(bridgeInner));
        merged.add(new Vector2(bridgeOuter));

        for(int i = outerIndex + 1; i < outerLoop.size(); i++) {
            merged.add(new Vector2(outerLoop.get(i)));
        }

        return merged;
    }

    private String pointKey(Vector2 point) {
        int x = Math.round(point.x * 100000f);
        int y = Math.round(point.y * 100000f);
        return x + "," + y;
    }

    private Vector2 edgePos(int edge, Vector2 p, float v0, float v1, float v2, float v3) {
        switch(edge) {

            case 0:
                return interpolation(p, new Vector2(p.x + resolutionScale, p.y), v0, v1);
            case 1:
                return interpolation(new Vector2(p.x + resolutionScale, p.y), new Vector2(p.x + resolutionScale, p.y + resolutionScale),
                    v1, v2);
            case 2:
                return interpolation(new Vector2(p.x + resolutionScale, p.y + resolutionScale), new Vector2(p.x, p.y + resolutionScale),
                    v2, v3);
            case 3:
                return interpolation(new Vector2(p.x, p.y + resolutionScale), new Vector2(p.x, p.y),
                    v3, v0);
            default:
                return interpolation(p, new Vector2(p.x + resolutionScale, p.y), v0, v1);
        }
    }

    private Vector2 interpolation(Vector2 a, Vector2 b, float va, float vb) {
        if(Math.abs(va - vb) < 1e-6f) {
            return new Vector2(a).add(new Vector2(b).sub(a).scl(0.5f));
        } else {
            return new Vector2(a).add(new Vector2(b).sub(a).scl((0.9f - va) / (vb - va)));
        }
    }

    private int getCaseId(float v0, float v1, float v2, float v3) {
        int caseIndex = 0;
        if(v0 > 0.9f) caseIndex |= 1;
        if(v1 > 0.9f) caseIndex |= 2;
        if(v2 > 0.9f) caseIndex |= 4;
        if(v3 > 0.9f) caseIndex |= 8;

        return caseIndex;
    }

    private void resetGridField() {
        gridField = new ArrayList<>();
        int height = maxY - minY + 1;
        int width = maxX - minX + 1;
        for(int y = 0; y < height; y++) {
            gridField.add(new ArrayList<>(Collections.nCopies(width, 0f)));
        }
    }

    private static class MarchingSegment {
        private final Vector2 a;
        private final Vector2 b;
        private final String keyA;
        private final String keyB;

        private MarchingSegment(Vector2 a, Vector2 b) {
            this.a = new Vector2(a);
            this.b = new Vector2(b);
            this.keyA = toKey(this.a);
            this.keyB = toKey(this.b);
        }

        private String otherKey(String key) {
            if(keyA.equals(key)) {
                return keyB;
            }
            return keyA;
        }

        private Vector2 pointForKey(String key) {
            if(keyA.equals(key)) {
                return a;
            }
            return b;
        }

        private static String toKey(Vector2 point) {
            int x = Math.round(point.x * 100000f);
            int y = Math.round(point.y * 100000f);
            return x + "," + y;
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
    private DynamicObject finishDrawing() {
        drawing = false;

        return (DynamicObject)addPoint(true);
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

    // check if it is drawing or not
    public boolean isDrawing() {
        return drawing;
    }
}

