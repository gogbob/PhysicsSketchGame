package io.github.physics_game;


import com.badlogic.gdx.math.Vector2;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class MarchingSquares {
    private static final int[][] edgeTable = {
        {-1,-1,-1,-1},        // 0 0000

        {0,3,-1,-1},          // 1 0001
        {1,0,-1,-1},          // 2 0010
        {1,3,-1,-1},          // 3 0011

        {2,1,-1,-1},          // 4 0100
        {0, 3, 2,1},           // 5 0101 (ambiguous → 2 segments)
        {2,0,-1,-1},          // 6 0110
        {2,3,-1,-1},          // 7 0111

        {3,2,-1,-1},          // 8 1000
        {0,2,-1,-1},          // 9 1001
        {3,2, 1,0},           // 10 1010 (ambiguous)
        {1,2,-1,-1},          // 11 1011

        {3,1,-1,-1},          // 12 1100
        {0,1,-1,-1},          // 13 1101
        {3,0,-1,-1},          // 14 1110

        {-1,-1,-1,-1}         // 15 1111
    };

    private static final float ISO_LEVEL = 0.9f;

    public static ContourData generateLocalContours(boolean debug, List<List<Float>> gridField, int minX, int minY, float resolutionScale) {
        ContourData contourData = createPixelatedEdges(debug, gridField, minX, minY, resolutionScale);
        if (contourData.contour == null || contourData.contour.size() < 3) {
            return null;
        }
        return contourData;
    }

    private static ContourData createPixelatedEdges(boolean debug, List<List<Float>> gridField, int minX, int minY, float resolutionScale) {
        List<Vector2> exteriorLoop = new ArrayList<>();
        List<List<Vector2>> interiorLoops = new ArrayList<>();

        List<List<Float>> travelledCells = new ArrayList<>();
        for(int y = 0; y < gridField.size(); y++){
            travelledCells.add(new ArrayList<>());
            for(int x = 0; x < gridField.get(0).size(); x++){
                travelledCells.get(y).add(0f);
            }
        }
        List<List<Vector2>> foundLoops = new ArrayList<>();

        int numLoops = 0;

        for(int y = 0; y < gridField.size() - 1; y++){
            for(int x = 0; x < gridField.get(0).size() - 1; x++){
                if(travelledCells.get(y).get(x) < 1.0f) {
                    //not travelled through yet
                    List<Float> values = new ArrayList<>();

                    values.add(gridField.get(y).get(x));
                    values.add(gridField.get(y).get(x + 1));
                    values.add(gridField.get(y + 1).get(x + 1));
                    values.add(gridField.get(y + 1).get(x));
                    int c = getCaseId(values.get(0), values.get(1), values.get(2), values.get(3));
                    if(c != 0 && c != 15) {
                        numLoops++;
                        int edgeIndex = edgeTable[c][0];

                        if((c == 5 || c == 10) && (travelledCells.get(y).get(x) < 0.3f && travelledCells.get(y).get(x) > 0.2f)) {
                            edgeIndex = edgeTable[c][2];
                        }

                        List<Vector2> loop = sanitizeLoop(traceLoopFromEdge(x, y, edgeIndex, gridField, travelledCells, resolutionScale, minX, minY));

                        if(loop != null) {
                            foundLoops.add(loop);
                        } else {
                            System.out.println("Removed bad loop");
                        }
                    }
                }
            }
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

        List<List<Integer>> verticePairs = new ArrayList<>();

        if(!interiorLoops.isEmpty()) {
            exteriorLoop = connectInteriorLoopsToExterior(exteriorLoop, interiorLoops, verticePairs, resolutionScale);
        }



        if(debug) {
            PhysicsResolver.printShape(exteriorLoop);
        }

        exteriorLoop = normalizeMergedLoop(exteriorLoop);
        ContourData contourData = new ContourData(exteriorLoop, verticePairs);
        return contourData;
    }


    private static List<Vector2> traceLoopFromEdge(int startSegmentX,
                                               int startSegmentY,
                                               int edgeIndex,
                                               List<List<Float>> gridField,
                                               List<List<Float>> travelledCells,
                                                      float resolutionScale,
                                                      int minX,
                                                      int minY) {
        List<Vector2> loop = new ArrayList<>();

        int x = startSegmentX;
        int y = startSegmentY;

        int numLoops = 0;

        List<Float> values = new ArrayList<>();

        values.add(gridField.get(y).get(x));
        values.add(gridField.get(y).get(x + 1));
        values.add(gridField.get(y + 1).get(x + 1));
        values.add(gridField.get(y + 1).get(x));
        int c = getCaseId(values.get(0), values.get(1), values.get(2), values.get(3));
        if(c == 0 || c == 15) {
            System.out.println("Error case");
            return null;
        }

        List<Vector2> edgeBounds = edgeBoundsFromEdgeType(edgeIndex, y, x, minX, minY, resolutionScale);

        //add the first vertice
        loop.add(interpolation(
            edgeBounds.get(0),
            edgeBounds.get(1),
            values.get(edgeIndex),
            values.get((edgeIndex + 1) % 4)));
        int prevIndex = (edgeIndex + 2) % 4;

        //go through until you get back to the previous vertice
        boolean foundStartVertex = false;
        while(!foundStartVertex) {
            numLoops++;
            if(numLoops > 10000) {
                System.out.println("Too many loops");
                return null;
            }
            int currentEdgeIndex;
            values = new ArrayList<>();

            values.add(gridField.get(y).get(x));
            values.add(gridField.get(y).get(x + 1));
            values.add(gridField.get(y + 1).get(x + 1));
            values.add(gridField.get(y + 1).get(x));
            c = getCaseId(values.get(0), values.get(1), values.get(2), values.get(3));

            if(c == 0 || c == 15) {
                System.out.println("Error case");
                return null;
            }

            currentEdgeIndex = edgeTable[c][1];

            if(c == 5 || c == 10) {
                //find the edge that corresponds the correct type
                if(!(prevIndex != edgeTable[c][0] && prevIndex % 2 == edgeTable[c][0] % 2)) {
                    currentEdgeIndex = edgeTable[c][3];
                    travelledCells.get(y).set(x, 0.75f + travelledCells.get(y).get(x));
                } else {
                    travelledCells.get(y).set(x, 0.25f + travelledCells.get(y).get(x));
                }
            } else {
                travelledCells.get(y).set(x, 1f);
            }

            edgeBounds = edgeBoundsFromEdgeType(currentEdgeIndex, y, x, minX, minY, resolutionScale);

            Vector2 p = interpolation(
                edgeBounds.get(0),
                edgeBounds.get(1),
                values.get(currentEdgeIndex),
                values.get((currentEdgeIndex + 1) % 4));

            //update the x and y value according to edge type
            x += (currentEdgeIndex == 0 || currentEdgeIndex == 2) ? 0 : ((currentEdgeIndex == 1) ? 1 : -1);
            y += (currentEdgeIndex == 1 || currentEdgeIndex == 3) ? 0 : ((currentEdgeIndex == 2) ? 1 : -1);

            //check if already reached start edge
            if(x == startSegmentX && y == startSegmentY && (edgeIndex != currentEdgeIndex && edgeIndex % 2 == currentEdgeIndex % 2)) {
                foundStartVertex = true;
            } else {
                loop.add(p);
            }

            if(travelledCells.get(y).get(x) > 0.8f && !foundStartVertex) {
                return null;
            }

            prevIndex = currentEdgeIndex;
        }

        if (loop.size() > 1 && loop.get(0).epsilonEquals(loop.get(loop.size() - 1), 1e-5f)) {
            loop.remove(loop.size() - 1);
        }

        return loop;
    }

    private static List<Vector2> edgeBoundsFromEdgeType(int edgeType, int i, int j, int minX, int minY, float resolutionScale) {
        List<Vector2> result = new ArrayList<>();
        result.add(globalPositionFromArray((edgeType == 0 || edgeType == 1) ? i : i + 1,(edgeType == 0 || edgeType == 3) ? j : j + 1, minX, minY, resolutionScale));
        result.add(globalPositionFromArray((edgeType == 0 || edgeType == 3) ? i : i + 1,(edgeType == 2 || edgeType == 3) ? j : j + 1, minX, minY, resolutionScale));
        return result;
    }

    private static Vector2 globalPositionFromArray(int i, int j, int minX, int minY, float resolutionScale) {
        return new Vector2(resolutionScale * (j + minX), resolutionScale * (i + minY));
    }

    private static float signedArea(List<Vector2> loop) {
        float area = 0f;
        for (int i = 0; i < loop.size(); i++) {
            Vector2 a = loop.get(i);
            Vector2 b = loop.get((i + 1) % loop.size());
            area += a.x * b.y - b.x * a.y;
        }
        return 0.5f * area;
    }

    private static List<Vector2> ensureLoopWinding(List<Vector2> loop, boolean expectCounterClockwise) {
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

    private static List<Vector2> sanitizeLoop(List<Vector2> loop) {
        if (loop == null || loop.size() < 3) {
            return null;
        }

        List<Vector2> out = new ArrayList<>();
        for (Vector2 p : loop) {
            if (p == null) {
                continue;
            }
            if (out.isEmpty() || !out.get(out.size() - 1).epsilonEquals(p, 0.05f)) {
                out.add(new Vector2(p));
            }
        }

        if (out.size() > 1 && out.get(0).epsilonEquals(out.get(out.size() - 1), 0.05f)) {
            out.remove(out.size() - 1);
        }

        // Remove near-collinear vertices to avoid near-zero-area ears.
        int i = 0;
        while (out.size() >= 3 && i < out.size()) {
            Vector2 prev = out.get((i - 1 + out.size()) % out.size());
            Vector2 curr = out.get(i);
            Vector2 next = out.get((i + 1) % out.size());
            if (Math.abs(orientedArea(prev, curr, next)) <= 0.01f) {
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

//        if (hasSelfIntersection(out)) {
//            return null;
//        }

        return out;
    }

    // Post-merge cleanup: keep bridge-compatible loops while removing obvious duplicates/degenerates.
    private static List<Vector2> normalizeMergedLoop(List<Vector2> loop) {
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

    private static boolean hasSelfIntersection(List<Vector2> loop) {
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

    private static boolean segmentsIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
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

    private static boolean onSegment(Vector2 a, Vector2 b, Vector2 p) {
        return p.x >= Math.min(a.x, b.x) - 1e-6f
            && p.x <= Math.max(a.x, b.x) + 1e-6f
            && p.y >= Math.min(a.y, b.y) - 1e-6f
            && p.y <= Math.max(a.y, b.y) + 1e-6f;
    }

    private static float orientedArea(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static List<Vector2> connectInteriorLoopsToExterior(List<Vector2> outerLoop, List<List<Vector2>> innerLoops, List<List<Integer>> verticePairs, float resolutionScale) {
        if(outerLoop == null || outerLoop.size() < 3 || innerLoops.isEmpty()) {
            return outerLoop;
        }

        List<Vector2> baseOuterLoop = ensureLoopWinding(new ArrayList<>(outerLoop), true);
        List<Vector2> merged = new ArrayList<>(baseOuterLoop);

        while(!innerLoops.isEmpty()) {
            boolean foundCandidate = false;
            for(int innerLoopIndex = 0; innerLoopIndex < innerLoops.size() && !foundCandidate; innerLoopIndex++) {
                List<Vector2> innerLoop = innerLoops.get(innerLoopIndex);
                if(innerLoop == null || innerLoop.size() < 3) {
                    continue;
                }

                List<Vector2> orientedInner = ensureLoopWinding(new ArrayList<>(innerLoop), false);
                int[] bridge = findClosestBridge(merged, orientedInner, innerLoops, innerLoopIndex);

                if(bridge[0] == -1) {
                    continue;
                }

                verticePairs.add(Arrays.asList(bridge[0], bridge[1]));
                foundCandidate = true;

                innerLoops.remove(innerLoopIndex);

                merged = spliceLoopAtBridge(merged, orientedInner, bridge[0], bridge[1], resolutionScale);
            }

            if(!foundCandidate) {
                return merged;
            }

        }
        return merged;
    }

    private static int[] findClosestBridge(List<Vector2> outerLoop,
                                    List<Vector2> innerLoop,
                                    List<List<Vector2>> allInnerLoops,
                                    int targetInnerLoopIndex) {
        int outerIndex = -1;
        int innerIndex = -1;
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

    private static int findVertexIndex(List<Vector2> loop, Vector2 vertex) {
        for (int i = 0; i < loop.size(); i++) {
            if (loop.get(i).epsilonEquals(vertex, 1e-5f)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isValidBridge(List<Vector2> outerLoop,
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

    private static boolean bridgeCrossesLoop(Vector2 a, Vector2 b, List<Vector2> loop, int endpointIndex) {
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

    private static boolean isPointInsideLoop(Vector2 point, List<Vector2> loop) {
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

    private static List<Vector2> spliceLoopAtBridge(List<Vector2> outerLoop, List<Vector2> innerLoop, int outerIndex, int innerIndex, float resolutionScale) {
        List<Vector2> forward = buildSplicedLoop(outerLoop, innerLoop, outerIndex, innerIndex, true, resolutionScale);
        List<Vector2> backward = buildSplicedLoop(outerLoop, innerLoop, outerIndex, innerIndex, false, resolutionScale);

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

    private static List<Vector2> buildSplicedLoop(List<Vector2> outerLoop,
                                           List<Vector2> innerLoop,
                                           int outerIndex,
                                           int innerIndex,
                                           boolean forward, float resolutionScale) {
        List<Vector2> merged = new ArrayList<>();

        for (int i = 0; i <= outerIndex; i++) {
            merged.add(new Vector2(outerLoop.get(i)));
        }

        Vector2 bridgeOuter = outerLoop.get(outerIndex);
        Vector2 bridgeInner = innerLoop.get(innerIndex);
        BridgeCorridor corridor = buildBridgeCorridor(bridgeOuter, bridgeInner, resolutionScale);

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

    private static BridgeCorridor buildBridgeCorridor(Vector2 outer, Vector2 inner, float resolutionScale) {
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

    public static class ContourData {
        public List<Vector2> contour;
        public List<List<Integer>> pairedVerticies;
        public ContourData(List<Vector2> contour, List<List<Integer>> pairedVerticies) {
            this.contour = contour;
            this.pairedVerticies = pairedVerticies;
        }
    }

    private static Vector2 interpolation(Vector2 a, Vector2 b, float va, float vb) {
        if(Math.abs(va - vb) < 1e-6f) {
            return new Vector2(a).add(new Vector2(b).sub(a).scl(0.5f));
        } else {
            return new Vector2(a).add(new Vector2(b).sub(a).scl((0.9f - va) / (vb - va)));
        }
    }

    private static int getCaseId(float v0, float v1, float v2, float v3) {
        int caseIndex = 0;
        if(v0 > 0.9f) caseIndex |= 1;
        if(v1 > 0.9f) caseIndex |= 2;
        if(v2 > 0.9f) caseIndex |= 4;
        if(v3 > 0.9f) caseIndex |= 8;
        return caseIndex;
    }

    public static void printField(List<List<Float>> gridField) {
        for(int i = 0; i < gridField.size(); i++) {
            System.out.print(i + (i < 10 ? "   " : (i < 100) ? "  " : (i < 1000) ? " " : ""));
            for(int j = 0; j < gridField.get(i).size(); j++) {
                System.out.print(gridField.get(i).get(j) > 0.8 ? "#" : (gridField.get(i).get(j) > 0.5 ? "/" : (gridField.get(i).get(j) > 0.1 ? ":" : "_")));
            }
            System.out.println();
        }
    }

    public static void printField(List<List<Integer>> gridField, boolean a) {
        for(int i = 0; i < gridField.size(); i++) {
            for(int j = 0; j < gridField.get(i).size(); j++) {
                System.out.print(gridField.get(i).get(j) == 0 ? "_" : gridField.get(i).get(j));
            }
            System.out.println();
        }
    }
}
