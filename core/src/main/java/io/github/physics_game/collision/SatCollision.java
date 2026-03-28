package io.github.physics_game.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;

/**
 * Convex polygon collision test using the Separating Axis Theorem (SAT).
 */
public final class SatCollision {
    private static final float EPSILON = 1e-6f;

    private SatCollision() {
    }

    public static ContactManifold detect(List<Vector2> polygonA, List<Vector2> polygonB) {
        if (polygonA == null || polygonB == null || polygonA.size() < 3 || polygonB.size() < 3) {
            return ContactManifold.NO_CONTACT;
        }

        //make a test to see if both polygons are convex
        for(int i = 0; i < polygonA.size(); ++i) {
            Vector2 v1 = polygonA.get(i);
            Vector2 v2 = polygonA.get((i + 1) % polygonA.size());
            Vector2 v3 = polygonA.get((i + 2) % polygonA.size());

            Vector2 edge1 = new Vector2(v2).sub(v1);
            Vector2 edge2 = new Vector2(v3).sub(v2);

            if (new Vector2(edge1).nor().crs(edge2) < -EPSILON) {
                //Gdx.app.log("SatCollision", "Polygon A is not convex by " + new Vector2(edge1).nor().crs(edge2));
            }
        }

        for(int i = 0; i < polygonB.size(); ++i) {
            Vector2 v1 = polygonB.get(i);
            Vector2 v2 = polygonB.get((i + 1) % polygonB.size());
            Vector2 v3 = polygonB.get((i + 2) % polygonB.size());

            Vector2 edge1 = new Vector2(v2).sub(v1);
            Vector2 edge2 = new Vector2(v3).sub(v2);

            if (new Vector2(edge1).nor().crs(edge2) < -EPSILON) {
                //Gdx.app.log("SatCollision", "Polygon B is not convex by " + new Vector2(edge1).nor().crs(edge2));
            }
        }

        float minOverlap = Float.MAX_VALUE;
        int refEdgeA = -1;
        Vector2 bestAxis = null;

        EdgeTestResult fromA = testAxesWithEdge(polygonA, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromA.hasCollision) {
            return ContactManifold.NO_CONTACT;
        }
        minOverlap = fromA.overlap;
        bestAxis = fromA.axis;
        refEdgeA = fromA.edgeIndex;

        EdgeTestResult fromB = testAxesWithEdge(polygonB, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromB.hasCollision) {
            return ContactManifold.NO_CONTACT;
        }

        float minOverlapB = fromB.overlap;
        Vector2 bestAxisB = fromB.axis;
        int refEdgeB = fromB.edgeIndex;

        Vector2 centerA = centroid(polygonA);
        Vector2 centerB = centroid(polygonB);
        Vector2 centerDelta = new Vector2(centerB).sub(centerA);

        // Choose reference polygon: use one with minimum overlap if significantly smaller
        boolean useB = (minOverlapB < minOverlap * 0.95f);
        List<Vector2> refPoly = useB ? polygonB : polygonA;
        List<Vector2> incPoly = useB ? polygonA : polygonB;
        Vector2 normal = useB ? new Vector2(bestAxisB) : new Vector2(bestAxis);
        int refEdgeIdx = useB ? refEdgeB : refEdgeA;  // USE THE EDGE THAT PRODUCED MIN OVERLAP
        float pen = useB ? minOverlapB : minOverlap;

        // Ensure normal convention is always A -> B.
        if (normal.dot(centerDelta) < 0f) {
            normal.scl(-1f);
        }

        // Generate manifold with edge clipping
        return findEdgeClipping(polygonA, polygonB, normal, pen);
    }

    /**
     * Test edges and return both the axis and the edge index for reference face selection.
     */
    private static class EdgeTestResult {
        boolean hasCollision;
        Vector2 axis;
        float overlap;
        int edgeIndex;

        EdgeTestResult(boolean collision, Vector2 axis, float overlap, int edgeIdx) {
            this.hasCollision = collision;
            this.axis = new Vector2(axis);
            this.overlap = overlap;
            this.edgeIndex = edgeIdx;
        }
    }

    public static ContactManifold findEdgeClipping(List<Vector2> polygonA, List<Vector2> polygonB, Vector2 normal, float penetration) {
        List<ContactPoint> manifoldPoints = new ArrayList<>();

        int edgeIdxA = findBestEdge(polygonA, normal);
        int edgeIdxB = findBestEdge(polygonB, new Vector2(normal).scl(-1f));

        Vector2 edgeA = new Vector2(polygonA.get((edgeIdxA + 1) % polygonA.size())).sub(polygonA.get(edgeIdxA));
        Vector2 edgeB = new Vector2(polygonB.get((edgeIdxB + 1) % polygonB.size())).sub(polygonB.get(edgeIdxB));
        Vector2 edgeNormalA = new Vector2(edgeA.y, -edgeA.x).nor();
        Vector2 edgeNormalB = new Vector2(edgeB.y, -edgeB.x).nor();
        int refEdgeIdx = edgeIdxA;
        int incEdgeIdx = edgeIdxB;
        List<Vector2> refPoly = polygonA;
        List<Vector2> incPoly = polygonB;
        Vector2 newNormal = new Vector2(normal);
        //unless the edge from B is significantly better, use the edge from A as reference
        if (Math.abs(normal.dot(edgeNormalA)) < Math.abs(normal.dot(edgeNormalB))) {
            refEdgeIdx = edgeIdxB;
            incEdgeIdx = edgeIdxA;
            refPoly = polygonB;
            incPoly = polygonA;
            newNormal.scl(-1f);
        }

        Vector2 refV1 = refPoly.get(refEdgeIdx);
        Vector2 refV2 = refPoly.get((refEdgeIdx + 1) % refPoly.size());
        Vector2 incV1 = incPoly.get(incEdgeIdx);
        Vector2 incV2 = incPoly.get((incEdgeIdx + 1) % incPoly.size());

        // Clip incident edge to reference edge side planes
        Vector2 refEdge = new Vector2(refV2).sub(refV1).nor();
        Vector2 sideNormal1 = new Vector2(refEdge);
        Vector2 sideNormal2 = new Vector2(refEdge).scl(-1f);

        float offset1 = sideNormal1.dot(refV1);
        float offset2 = sideNormal2.dot(refV2);

        List<Vector2> clipPoints = clipSegmentToLine(incV1, incV2, sideNormal1, offset1);
        if(clipPoints.size()<2){
            //use fallback
            ContactPoint fallback = new ContactPoint(new Vector2(incV1)
                .add(incV2)
                .add(refV1)
                .add(refV2)
                .scl(0.25f), penetration);
            return new ContactManifold(true, normal, Arrays.asList(fallback), penetration);
        }

        clipPoints = clipSegmentToLine(clipPoints.get(0), clipPoints.get(1), sideNormal2, offset2);

        if(clipPoints.size()<2){
            ContactPoint fallback = new ContactPoint(new Vector2(incV1)
                .add(incV2)
                .add(refV1)
                .add(refV2)
                .scl(0.25f), penetration);
            return new ContactManifold(true, normal, Arrays.asList(fallback), penetration);
        }

        Vector2 refNormal = new Vector2(refEdge.y, -refEdge.x);
        float refOffset = refNormal.dot(refV1);

        for (Vector2 cp : clipPoints) {
            float penetrationDepth = refNormal.dot(cp) - refOffset;
            if (penetrationDepth <= 0.01f) {
                manifoldPoints.add(new ContactPoint(cp, -penetrationDepth));
            }
        }

        return new ContactManifold(true, normal, manifoldPoints, penetration);
    }

    private static int findBestEdge(List<Vector2> polygon, Vector2 normal) {
        int bestEdge = -1;
        float maxDot = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < polygon.size(); i++) {
            Vector2 v1 = polygon.get(i);
            Vector2 v2 = polygon.get((i + 1) % polygon.size());
            Vector2 edge = new Vector2(v2).sub(v1);
            Vector2 edgeNormal = new Vector2(edge.y, -edge.x).nor();

            float dot = edgeNormal.dot(normal);
            if (dot > maxDot) {
                maxDot = dot;
                bestEdge = i;
            }
        }

        return bestEdge;
    }

    private static List<Vector2> clipSegmentToLine(Vector2 v1, Vector2 v2, Vector2 edgeNormal, float offset) {
        List<Vector2> result = new ArrayList<>();

        float d1 = edgeNormal.dot(v1) - offset;
        float d2 = edgeNormal.dot(v2) - offset;

        if (d1 >= -EPSILON) {
            result.add(v1);
        }
        if (d2 >= -EPSILON) {
            result.add(v2);
        }

        if (d1 * d2 < 0) {
            float t = d1 / (d1 - d2);
            Vector2 intersection = new Vector2(v1).mulAdd(new Vector2(v2).sub(v1), t);
            if (intersection != null) {
                result.add(intersection);
            }
        }

        return result;
    }


    private static Vector2 lineSegmentIntersection(Vector2 a1, Vector2 a2, Vector2 b1, Vector2 b2) {
        float x1 =  a1.x;
        float y1 = a1.y;
        float x2 =  a2.x;
        float y2 = a2.y;
        float x3 =  b1.x;
        float y3 = b1.y;
        float x4 =  b2.x;
        float y4 = b2.y;

        float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);

        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        float u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denom;

        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            return new Vector2(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
        }

        return null;
    }

    private static float distancePointToPolygonBoundary(Vector2 point, List<Vector2> poly) {
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < poly.size(); i++) {
            Vector2 v1 = poly.get(i);
            Vector2 v2 = poly.get((i + 1) % poly.size());

            // Distance from point to edge
            float dist = distancePointToSegment(point, v1, v2);
            minDist = Math.min(minDist, dist);
        }

        return minDist;
    }

    private static float distancePointToSegment(Vector2 p, Vector2 a, Vector2 b) {
        Vector2 ab = new Vector2(b).sub(a);
        Vector2 ap = new Vector2(p).sub(a);

        float abLen2 = ab.len2();
        if (abLen2 < EPSILON) {
            return ap.len(); // a and b are the same
        }

        float t = ap.dot(ab) / abLen2;
        t = Math.max(0, Math.min(1, t)); // Clamp to segment

        Vector2 closest = new Vector2(a).add(new Vector2(ab).scl(t));
        return new Vector2(p).sub(closest).len();
    }
    /**
     * Clip a line segment against a half-plane defined by a point and inward normal.
     * Returns the clipped segment (0, 1, or 2 points).
     */
    private static List<Vector2> clipSegmentToPlane(List<Vector2> segment, Vector2 planePoint, Vector2 planeTangent) {
        if (segment.size() < 2) return segment;

        List<Vector2> result = new ArrayList<>();
        Vector2 start = segment.get(0);
        Vector2 end = segment.get(1);

        //the plane tangent is the tangent pointing inwards from the edge, so we can use it to determine whether the points are on the correct side
        float dStart = new Vector2(start).sub(planePoint).dot(planeTangent);
        float dEnd = new Vector2(end).sub(planePoint).dot(planeTangent);

        if (dStart >= -EPSILON) {
            result.add(new Vector2(start));
        }
        if (dEnd >= -EPSILON) {
            result.add(new Vector2(end));
        }

        if ((dStart > EPSILON && dEnd < -EPSILON) || (dStart < -EPSILON && dEnd > EPSILON)) {
            // Segment crosses plane; interpolate intersection
            float t = dStart / (dStart - dEnd);
            Vector2 intersection = new Vector2(start).mulAdd(new Vector2(end).sub(start), t);
            if (result.isEmpty() || !result.get(result.size() - 1).epsilonEquals(intersection, EPSILON)) {
                result.add(intersection);
            }
        }

        return result;
    }

    public static EdgeTestResult testAxesWithEdge(List<Vector2> axisSource,
                                                  List<Vector2> polygonA,
                                                  List<Vector2> polygonB,
                                                  float currentMinOverlap,
                                                  Vector2 currentBestAxis) {
        float minOverlap = currentMinOverlap;
        Vector2 bestAxis = currentBestAxis == null ? new Vector2() : new Vector2(currentBestAxis);
        int bestEdgeIdx = -1;

        for (int i = 0; i < axisSource.size(); i++) {
            Vector2 v1 = axisSource.get(i);
            Vector2 v2 = axisSource.get((i + 1) % axisSource.size());
            Vector2 edge = new Vector2(v2).sub(v1);

            if (edge.len2() <= EPSILON) {
                continue;
            }

            Vector2 axis = new Vector2(edge.y, -edge.x).nor();

            Projection pA = projectPolygon(axis, polygonA);
            Projection pB = projectPolygon(axis, polygonB);

            float overlap = Math.min(pA.max, pB.max) - Math.max(pB.min, pA.min);
            if (overlap <= 0f) {
                return new EdgeTestResult(false, axis, overlap, i);
            }

            if (overlap < minOverlap) {
                minOverlap = overlap;
                bestAxis = axis;
                bestEdgeIdx = i;
            }
        }

        return new EdgeTestResult(true, bestAxis, minOverlap, bestEdgeIdx);
    }

    private static Projection projectPolygon(Vector2 axis, List<Vector2> polygon) {
        float min = axis.dot(polygon.get(0));
        float max = min;

        for (int i = 1; i < polygon.size(); i++) {
            float p = axis.dot(polygon.get(i));
            if (p < min) {
                min = p;
            }
            if (p > max) {
                max = p;
            }
        }

        return new Projection(min, max);
    }

    private static Vector2 centroid(List<Vector2> polygon) {
        float x = 0f;
        float y = 0f;
        for (Vector2 v : polygon) {
            x += v.x;
            y += v.y;
        }
        return new Vector2(x / polygon.size(), y / polygon.size());
    }



    private static final class Projection {
        final float min;
        final float max;

        Projection(float min, float max) {
            this.min = min;
            this.max = max;
        }
    }
}

