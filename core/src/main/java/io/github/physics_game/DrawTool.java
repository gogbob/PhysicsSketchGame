package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
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
    private static final float ISO_LEVEL = 0.9f;
    public static final float minDist = 0.5f;

    private ReentrantLock lock = new ReentrantLock();
    public float chargeDensity = 2.0f;
    private boolean drawing; // check if it's drawing or not
    private final Camera camera;
    private DrawType drawType = DrawType.NORMAL;
    private final Viewport viewport;
    private int nextId; // give id to objects
    private List<Vector2> circleShape;
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

    public DrawTool(Camera camera, Viewport viewport, float toolWidth) {
        this.camera = camera;
        this.viewport = viewport;
        this.drawing = false;
        this.nextId = 100;  // object drew by player = start from 100
        this.toolWidth = toolWidth;
    }

    public void reset() {
        drawing = false;
        nextId = 100;
    }

    // call the method each frame
    public synchronized PhysicsObject update(DrawType drawType) {
        if(lock.tryLock()) {
            try {
                // press mouse = start to draw
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    return startDrawing(drawType);
                }

                // continue press mouse = continue to draw
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing && getMousePosition().sub(prevPosition).len() > minDist) {
                    return addPoint(false);
                }

                // draw tool
                // release mouse = done + create object
                if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && drawing) {
                    return finishDrawing(drawType);
                }
            } finally {
                lock.unlock();
            }
        }

        return null;
    }

    // start drawing method (make a circle)
    private StaticObject startDrawing(DrawType drawType) {
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

        List<Vector2> contour = generateLocalContours(false);

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
        List<Vector2> contour = generateLocalContours(false);
        return buildCurrentObject(contour, buildDynamic);
    }

    public void testAddPoint(boolean buildDynamic) {
        List<Vector2> contour = generateLocalContours(true);
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

    public List<Vector2> generateLocalContours(boolean debug) {
        createPixelatedEdges(debug);
        if (exteriorLoop == null || exteriorLoop.size() < 3) {
            return null;
        }
        return new ArrayList<>(exteriorLoop);
    }

    private void createPixelatedEdges(boolean debug) {
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
                    addAmbiguousCellSegments(c, p, v0, v1, v2, v3, segments, adjacency);
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
            if(debug) {
                System.out.println("Tracing loop from segment " + i + " with endpoints " + segments.get(i).a + " and " + segments.get(i).b);
            }
            List<Vector2> loop = traceLoopFromSegment(i, segments, adjacency, travelledEdges);
            if(loop.size() >= 3) {
                ArrayList<Vector2> tessellatedLoop = new ArrayList<>(loop);
                tessellateContour(tessellatedLoop);
                List<Vector2> sanitized = sanitizeLoop(tessellatedLoop);
                if(sanitized != null) {
                    foundLoops.add(sanitized);
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
        exteriorLoop = ensureLoopWinding(exteriorLoop, true);
        if(debug) {
            PhysicsResolver.printShape(exteriorLoop);
        }
        for(int i = 0; i < foundLoops.size(); i++) {
            if(i != outerIdx) {
                interiorLoops.add(ensureLoopWinding(new ArrayList<>(foundLoops.get(i)), false));
            }
        }

        if(debug) {
            List<List<Vector2>> allLoops = new ArrayList<>();
            allLoops.add(exteriorLoop);
            allLoops.addAll(interiorLoops);
            PhysicsResolver.printListShape(allLoops);
        }

        if(!interiorLoops.isEmpty()) {
            exteriorLoop = connectInteriorLoopsToExterior(exteriorLoop, interiorLoops);
        }

        if(debug) {
            PhysicsResolver.printShape(exteriorLoop);
        }

        exteriorLoop = normalizeMergedLoop(exteriorLoop);
    }

    private void addSegment(Vector2 a, Vector2 b, List<MarchingSegment> segments, Map<String, List<Integer>> adjacency) {
        if (a == null || b == null || a.epsilonEquals(b, 1e-6f)) {
            return;
        }

        int idx = segments.size();
        MarchingSegment segment = new MarchingSegment(a, b);
        segments.add(segment);

        adjacency.computeIfAbsent(segment.keyA, k -> new ArrayList<>()).add(idx);
        adjacency.computeIfAbsent(segment.keyB, k -> new ArrayList<>()).add(idx);
    }

    private void addAmbiguousCellSegments(int caseId,
                                          Vector2 p,
                                          float v0,
                                          float v1,
                                          float v2,
                                          float v3,
                                          List<MarchingSegment> segments,
                                          Map<String, List<Integer>> adjacency) {
        // Bilinear saddle decider: choose one of the two valid topologies for 5/10.
        float center = 0.25f * (v0 + v1 + v2 + v3);
        boolean centerInside = center >= ISO_LEVEL;

        int[][] pairs;
        if (caseId == 5) {
            // case 5 corners: v0,v2 inside. Switch pairing based on center sign.
            pairs = centerInside
                ? new int[][]{{0, 1}, {2, 3}}
                : new int[][]{{0, 3}, {1, 2}};
        } else {
            // case 10 corners: v1,v3 inside. Opposite pairing choice from case 5.
            pairs = centerInside
                ? new int[][]{{0, 3}, {1, 2}}
                : new int[][]{{0, 1}, {2, 3}};
        }

        for (int[] pair : pairs) {
            addSegment(
                edgePos(pair[0], p, v0, v1, v2, v3),
                edgePos(pair[1], p, v0, v1, v2, v3),
                segments,
                adjacency
            );
        }
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

        boolean closed = false;
        int guard = 0;
        int maxSteps = Math.max(segments.size() * 2, 64);
        while (guard++ < maxSteps) {
            if (currentKey.equals(startKey)) {
                closed = true;
                break;
            }

            int nextSegment = findNextSegment(currentKey, prevKey, segments, adjacency, travelledEdges);
            if (nextSegment < 0) {
                break;
            }

            MarchingSegment seg = segments.get(nextSegment);
            String nextKey = seg.otherKey(currentKey);
            loop.add(new Vector2(seg.pointForKey(nextKey)));

            travelledEdges.add(nextSegment);
            prevKey = currentKey;
            currentKey = nextKey;
        }

        if (!closed) {
            // Open chains are unstable during in-progress drawing and cause bad transient triangulation.
            return Collections.emptyList();
        }

        if (loop.size() > 1 && loop.get(0).epsilonEquals(loop.get(loop.size() - 1), 1e-5f)) {
            loop.remove(loop.size() - 1);
        }

        return loop;
    }

    private int findNextSegment(String currentKey,
                                String prevKey,
                                List<MarchingSegment> segments,
                                Map<String, List<Integer>> adjacency,
                                Set<Integer> travelledEdges) {
        List<Integer> candidates = adjacency.getOrDefault(currentKey, Collections.emptyList());
        Vector2 currentPoint = keyToPoint(currentKey);
        Vector2 prevPoint = keyToPoint(prevKey);

        int best = -1;
        float bestScore = -Float.MAX_VALUE;
        int fallback = -1;

        for (int segmentIdx : candidates) {
            if (travelledEdges.contains(segmentIdx)) {
                continue;
            }

            MarchingSegment seg = segments.get(segmentIdx);
            String otherKey = seg.otherKey(currentKey);
            if (otherKey.equals(prevKey)) {
                fallback = segmentIdx;
                continue;
            }

            Vector2 nextPoint = seg.pointForKey(otherKey);
            Vector2 dirOut = new Vector2(nextPoint).sub(currentPoint);
            if (dirOut.len2() <= 1e-12f) {
                continue;
            }

            float score;
            if (prevPoint == null || currentPoint == null || new Vector2(currentPoint).sub(prevPoint).len2() <= 1e-12f) {
                // First turn: prefer longer continuation over tiny stubs.
                score = dirOut.len2();
            } else {
                Vector2 dirIn = new Vector2(currentPoint).sub(prevPoint).nor();
                score = dirIn.dot(new Vector2(dirOut).nor()); // maximal dot => minimal turn
            }

            if (score > bestScore || (Math.abs(score - bestScore) <= 1e-6f && segmentIdx < best)) {
                bestScore = score;
                best = segmentIdx;
            }
        }

        return best >= 0 ? best : fallback;
    }

    private Vector2 keyToPoint(String key) {
        if (key == null) {
            return null;
        }
        int sep = key.indexOf(',');
        if (sep <= 0 || sep >= key.length() - 1) {
            return null;
        }
        try {
            int x = Integer.parseInt(key.substring(0, sep));
            int y = Integer.parseInt(key.substring(sep + 1));
            return new Vector2(x / 100000f, y / 100000f);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private float signedArea(List<Vector2> loop) {
        float area = 0f;
        for (int i = 0; i < loop.size(); i++) {
            Vector2 a = loop.get(i);
            Vector2 b = loop.get((i + 1) % loop.size());
            area += a.x * b.y - b.x * a.y;
        }
        return 0.5f * area;
    }

    private List<Vector2> ensureLoopWinding(List<Vector2> loop, boolean expectCounterClockwise) {
        if (loop == null || loop.size() < 3) {
            return loop;
        }

        float area = signedArea(loop);
        if (Math.abs(area) <= 1e-8f) {
            return loop;
        }

        boolean isCounterClockwise = area > 0f;
        if (isCounterClockwise != expectCounterClockwise) {
            Collections.reverse(loop);
        }
        return loop;
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
            Gdx.app.log("ERROR", "contour is null");
            return null;
        }

        List<Vector2> cleaned = normalizeMergedLoop(contour);
        if (cleaned == null) {
            return null;
        }

        if(dynamicObject) {
            DynamicObject obj;
            if(drawType == DrawType.ANTIGRAVITY) {
                obj = new AntigravityObject(nextId, 0.5f, 0.4f, density, cleaned,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                Gdx.app.log("Draw Tool", "Creating Antigravity object");
            } else if(drawType == DrawType.POSITIVE ||  drawType == DrawType.NEGATIVE) {
                obj = new ChargedDynamicObject(nextId, 0.5f, 0.4f, density, cleaned,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments, (drawType == DrawType.POSITIVE)? chargeDensity : -chargeDensity);
                Gdx.app.log("Draw Tool", "Creating Charged object");
            } else if(drawType == DrawType.ICY) {
                obj = new DynamicObject(nextId, 0.00f, 0.4f, density, cleaned,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                Gdx.app.log("Draw Tool", "Creating Icy object");
                obj.setColor(new Color(0.8f, 0.8f, 1.0f, 1));
            } else {
                obj = new DynamicObject(nextId, 0.5f, 0.4f, density, cleaned,
                    referencePoint.x, referencePoint.y, 0, mass, inertia, com, pointSegments, massSegments);
                Gdx.app.log("Draw Tool", "Creating Normal object");
            }
            nextId++;
            return obj;
        }

        StaticObject staticObject;
        if(drawType == DrawType.ANTIGRAVITY) {
            staticObject = new StaticObject(1000, 0.5f, 0.4f, density, cleaned, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
            staticObject.setColor(Color.CORAL);
        } else if(drawType == DrawType.POSITIVE ||  drawType == DrawType.NEGATIVE) {
            staticObject = new ChargedStaticObject(1000, 0.5f, 0.4f, density, cleaned, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments, (drawType == DrawType.POSITIVE)? chargeDensity : -chargeDensity);
        } else if(drawType == DrawType.ICY) {
            staticObject = new StaticObject(1000, 0.0f, 0.4f, density, cleaned, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
            staticObject.setColor(new Color(0.8f, 0.8f, 1.0f, 1));
        } else {
            staticObject = new StaticObject(1000, 0.5f, 0.4f, density, cleaned, referencePoint.x, referencePoint.y,
                0, com, pointSegments, massSegments);
        }
        return staticObject;
    }

    private List<Vector2> sanitizeLoop(List<Vector2> loop) {
        if (loop == null || loop.size() < 3) {
            return null;
        }

        List<Vector2> out = new ArrayList<>();
        for (Vector2 p : loop) {
            if (p == null) {
                continue;
            }
            if (out.isEmpty() || !out.get(out.size() - 1).epsilonEquals(p, 1e-5f)) {
                out.add(new Vector2(p));
            }
        }

        if (out.size() > 1 && out.get(0).epsilonEquals(out.get(out.size() - 1), 1e-5f)) {
            out.remove(out.size() - 1);
        }

        // Remove near-collinear vertices to avoid near-zero-area ears.
        int i = 0;
        while (out.size() >= 3 && i < out.size()) {
            Vector2 prev = out.get((i - 1 + out.size()) % out.size());
            Vector2 curr = out.get(i);
            Vector2 next = out.get((i + 1) % out.size());
            if (Math.abs(orientedArea(prev, curr, next)) <= 1e-6f) {
                out.remove(i);
                continue;
            }
            i++;
        }

        if (out.size() < 3) {
            return null;
        }

        if (Math.abs(signedArea(out)) < 1e-4f) {
            return null;
        }

        if (hasSelfIntersection(out)) {
            return null;
        }

        return out;
    }

    // Post-merge cleanup: keep bridge-compatible loops while removing obvious duplicates/degenerates.
    private List<Vector2> normalizeMergedLoop(List<Vector2> loop) {
        if (loop == null || loop.size() < 3) {
            return null;
        }

        List<Vector2> out = new ArrayList<>();
        for (Vector2 p : loop) {
            if (p == null) {
                continue;
            }
            if (out.isEmpty() || !out.get(out.size() - 1).epsilonEquals(p, 1e-5f)) {
                out.add(new Vector2(p));
            }
        }

        if (out.size() > 1 && out.get(0).epsilonEquals(out.get(out.size() - 1), 1e-5f)) {
            out.remove(out.size() - 1);
        }

        int i = 0;
        while (out.size() >= 3 && i < out.size()) {
            Vector2 prev = out.get((i - 1 + out.size()) % out.size());
            Vector2 curr = out.get(i);
            Vector2 next = out.get((i + 1) % out.size());
            if (Math.abs(orientedArea(prev, curr, next)) <= 1e-6f) {
                out.remove(i);
                continue;
            }
            i++;
        }

        if (out.size() < 3 || Math.abs(signedArea(out)) < 1e-4f) {
            return null;
        }

        return out;
    }

    private boolean hasSelfIntersection(List<Vector2> loop) {
        int n = loop.size();
        for (int i = 0; i < n; i++) {
            Vector2 a1 = loop.get(i);
            Vector2 a2 = loop.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                // Adjacent edges share endpoints and should be ignored.
                if (Math.abs(i - j) <= 1 || (i == 0 && j == n - 1)) {
                    continue;
                }
                Vector2 b1 = loop.get(j);
                Vector2 b2 = loop.get((j + 1) % n);
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean segmentsIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        float o1 = orientedArea(a, b, c);
        float o2 = orientedArea(a, b, d);
        float o3 = orientedArea(c, d, a);
        float o4 = orientedArea(c, d, b);

        if (Math.abs(o1) <= 1e-6f && onSegment(a, b, c)) return true;
        if (Math.abs(o2) <= 1e-6f && onSegment(a, b, d)) return true;
        if (Math.abs(o3) <= 1e-6f && onSegment(c, d, a)) return true;
        if (Math.abs(o4) <= 1e-6f && onSegment(c, d, b)) return true;

        return (o1 > 0f) != (o2 > 0f) && (o3 > 0f) != (o4 > 0f);
    }

    private boolean onSegment(Vector2 a, Vector2 b, Vector2 p) {
        return p.x >= Math.min(a.x, b.x) - 1e-6f
            && p.x <= Math.max(a.x, b.x) + 1e-6f
            && p.y >= Math.min(a.y, b.y) - 1e-6f
            && p.y <= Math.max(a.y, b.y) + 1e-6f;
    }

    private float orientedArea(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private List<Vector2> connectInteriorLoopsToExterior(List<Vector2> outerLoop, List<List<Vector2>> innerLoops) {
        if(outerLoop == null || outerLoop.size() < 3 || innerLoops.isEmpty()) {
            return outerLoop;
        }

        List<Vector2> baseOuterLoop = ensureLoopWinding(new ArrayList<>(outerLoop), true);
        List<Vector2> merged = new ArrayList<>(baseOuterLoop);
        for(int innerLoopIndex = 0; innerLoopIndex < innerLoops.size(); innerLoopIndex++) {
            List<Vector2> innerLoop = innerLoops.get(innerLoopIndex);
            if(innerLoop == null || innerLoop.size() < 3) {
                continue;
            }

            List<Vector2> orientedInner = ensureLoopWinding(new ArrayList<>(innerLoop), false);
            int[] bridge = findClosestBridge(baseOuterLoop, orientedInner, innerLoops, innerLoopIndex);

            int mergedOuterIndex = findVertexIndex(merged, baseOuterLoop.get(bridge[0]));
            if (mergedOuterIndex < 0) {
                continue;
            }

            merged = spliceLoopAtBridge(merged, orientedInner, mergedOuterIndex, bridge[1]);
        }

        return merged;
    }

    private int[] findClosestBridge(List<Vector2> outerLoop,
                                    List<Vector2> innerLoop,
                                    List<List<Vector2>> allInnerLoops,
                                    int targetInnerLoopIndex) {
        int outerIndex = 0;
        int innerIndex = 0;
        float bestDist2 = Float.MAX_VALUE;

        for(int i = 0; i < outerLoop.size(); i++) {
            Vector2 outer = outerLoop.get(i);
            for(int j = 0; j < innerLoop.size(); j++) {
                Vector2 inner = innerLoop.get(j);
                float dist2 = outer.dst2(inner);
                if (dist2 >= bestDist2) {
                    continue;
                }
                if (!isValidBridge(outerLoop, innerLoop, i, j, allInnerLoops, targetInnerLoopIndex)) {
                    continue;
                }
                bestDist2 = dist2;
                outerIndex = i;
                innerIndex = j;
            }
        }

        return new int[]{outerIndex, innerIndex};
    }

    private int findVertexIndex(List<Vector2> loop, Vector2 vertex) {
        for (int i = 0; i < loop.size(); i++) {
            if (loop.get(i).epsilonEquals(vertex, 1e-5f)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidBridge(List<Vector2> outerLoop,
                                  List<Vector2> innerLoop,
                                  int outerIndex,
                                  int innerIndex,
                                  List<List<Vector2>> allInnerLoops,
                                  int targetInnerLoopIndex) {
        Vector2 a = outerLoop.get(outerIndex);
        Vector2 b = innerLoop.get(innerIndex);

        if (bridgeCrossesLoop(a, b, outerLoop, outerIndex)) {
            return false;
        }
        if (bridgeCrossesLoop(a, b, innerLoop, innerIndex)) {
            return false;
        }

        for (int i = 0; i < allInnerLoops.size(); i++) {
            if (i == targetInnerLoopIndex) {
                continue;
            }
            List<Vector2> otherInner = allInnerLoops.get(i);
            if (otherInner == null || otherInner.size() < 3) {
                continue;
            }
            if (bridgeCrossesLoop(a, b, otherInner, -1)) {
                return false;
            }
        }

        Vector2 midpoint = new Vector2(a).add(b).scl(0.5f);
        return isPointInsideLoop(midpoint, outerLoop);
    }

    private boolean bridgeCrossesLoop(Vector2 a, Vector2 b, List<Vector2> loop, int endpointIndex) {
        for (int i = 0; i < loop.size(); i++) {
            Vector2 p = loop.get(i);
            Vector2 q = loop.get((i + 1) % loop.size());

            // Skip edges touching the chosen endpoint on that loop.
            if (i == endpointIndex || ((i + 1) % loop.size()) == endpointIndex) {
                continue;
            }

            if (segmentsIntersect(a, b, p, q)) {
                if (a.epsilonEquals(p, 1e-5f) || a.epsilonEquals(q, 1e-5f)
                    || b.epsilonEquals(p, 1e-5f) || b.epsilonEquals(q, 1e-5f)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isPointInsideLoop(Vector2 point, List<Vector2> loop) {
        boolean inside = false;
        for (int i = 0, j = loop.size() - 1; i < loop.size(); j = i++) {
            Vector2 vi = loop.get(i);
            Vector2 vj = loop.get(j);
            boolean intersects = ((vi.y > point.y) != (vj.y > point.y))
                && (point.x < (vj.x - vi.x) * (point.y - vi.y) / ((vj.y - vi.y) + 1e-12f) + vi.x);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private List<Vector2> spliceLoopAtBridge(List<Vector2> outerLoop, List<Vector2> innerLoop, int outerIndex, int innerIndex) {
        List<Vector2> forward = buildSplicedLoop(outerLoop, innerLoop, outerIndex, innerIndex, true);
        List<Vector2> backward = buildSplicedLoop(outerLoop, innerLoop, outerIndex, innerIndex, false);

        float targetArea = Math.max(0f, Math.abs(signedArea(outerLoop)) - Math.abs(signedArea(innerLoop)));
        float forwardError = Math.abs(Math.abs(signedArea(forward)) - targetArea);
        float backwardError = Math.abs(Math.abs(signedArea(backward)) - targetArea);

        boolean forwardValid = !hasSelfIntersection(forward);
        boolean backwardValid = !hasSelfIntersection(backward);

        if (forwardValid && !backwardValid) {
            return forward;
        }
        if (backwardValid && !forwardValid) {
            return backward;
        }

        return forwardError <= backwardError ? forward : backward;
    }

    private List<Vector2> buildSplicedLoop(List<Vector2> outerLoop,
                                           List<Vector2> innerLoop,
                                           int outerIndex,
                                           int innerIndex,
                                           boolean forward) {
        List<Vector2> merged = new ArrayList<>();

        for (int i = 0; i <= outerIndex; i++) {
            merged.add(new Vector2(outerLoop.get(i)));
        }

        Vector2 bridgeOuter = outerLoop.get(outerIndex);
        Vector2 bridgeInner = innerLoop.get(innerIndex);
        BridgeCorridor corridor = buildBridgeCorridor(bridgeOuter, bridgeInner);

        // Enter the hole boundary through one side of a tiny corridor.
        merged.add(corridor.outerEnter);
        merged.add(corridor.innerEnter);

        for (int i = 1; i < innerLoop.size(); i++) {
            int idx;
            if (forward) {
                idx = (innerIndex + i) % innerLoop.size();
            } else {
                idx = (innerIndex - i + innerLoop.size()) % innerLoop.size();
            }
            merged.add(new Vector2(innerLoop.get(idx)));
        }

        // Exit through the other side of the corridor to avoid overlapping bridge edges.
        merged.add(corridor.innerExit);
        merged.add(corridor.outerExit);

        for (int i = outerIndex + 1; i < outerLoop.size(); i++) {
            merged.add(new Vector2(outerLoop.get(i)));
        }

        return merged;
    }

    private BridgeCorridor buildBridgeCorridor(Vector2 outer, Vector2 inner) {
        Vector2 dir = new Vector2(inner).sub(outer);
        float len = dir.len();
        if (len <= 1e-6f) {
            return new BridgeCorridor(new Vector2(outer), new Vector2(inner), new Vector2(inner), new Vector2(outer));
        }

        // Keep a finite slit for simple-polygon validity, but make it visually negligible.
        float halfWidth = Math.max(0.0005f, Math.min(resolutionScale * 0.08f, len * 0.005f));
        Vector2 normal = new Vector2(-dir.y, dir.x).nor().scl(halfWidth);

        return new BridgeCorridor(
            new Vector2(outer).add(normal),
            new Vector2(inner).add(normal),
            new Vector2(inner).sub(normal),
            new Vector2(outer).sub(normal)
        );
    }

    private static class BridgeCorridor {
        private final Vector2 outerEnter;
        private final Vector2 innerEnter;
        private final Vector2 innerExit;
        private final Vector2 outerExit;

        private BridgeCorridor(Vector2 outerEnter, Vector2 innerEnter, Vector2 innerExit, Vector2 outerExit) {
            this.outerEnter = outerEnter;
            this.innerEnter = innerEnter;
            this.innerExit = innerExit;
            this.outerExit = outerExit;
        }
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
                return interpolation(new Vector2(p.x + resolutionScale, p.y), new Vector2(p.x + resolutionScale, p.y + resolutionScale), v1, v2);
            case 2:
                return interpolation(new Vector2(p.x + resolutionScale, p.y + resolutionScale), new Vector2(p.x, p.y + resolutionScale), v2, v3);
            case 3:
                return interpolation(new Vector2(p.x, p.y + resolutionScale), new Vector2(p.x, p.y), v3, v0);
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
    // finish drawing + create object method
    private DynamicObject finishDrawing(DrawType drawType) {
        drawing = false;
        DynamicObject temp = (DynamicObject)addPoint(true);
        if(drawType != null) this.drawType = drawType;
        return (DynamicObject)addPoint(true);
    }
    // get mouse position in world methode
    private Vector2 getMousePosition() {
        // the position of mouse on screen
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // change into the position in game world
        Vector3 screenPos = new Vector3(screenX, screenY, 0);
        Vector3 worldPos = viewport.unproject(screenPos);

        return new Vector2(worldPos.x, worldPos.y);
    }

    // check if it is drawing or not
    public boolean isDrawing() {
        return drawing;
    }
}
