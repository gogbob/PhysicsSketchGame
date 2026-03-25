package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Convex polygon collision test using the Separating Axis Theorem (SAT).
 */
public final class SatCollision {
    private static final float EPSILON = 1e-6f;

    private SatCollision() {
    }

    public static ContactPoint detect(List<Vector2> polygonA, List<Vector2> polygonB) {
        if (polygonA == null || polygonB == null || polygonA.size() < 3 || polygonB.size() < 3) {
            return ContactPoint.NO_CONTACT_POINT;
        }

        float minOverlap = Float.MAX_VALUE;
        int refEdgeA = -1;
        Vector2 bestAxis = null;

        EdgeTestResult fromA = testAxesWithEdge(polygonA, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromA.hasCollision) {
            return ContactPoint.NO_CONTACT_POINT;
        }
        minOverlap = fromA.overlap;
        bestAxis = fromA.axis;
        refEdgeA = fromA.edgeIndex;

        EdgeTestResult fromB = testAxesWithEdge(polygonB, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromB.hasCollision) {
            return ContactPoint.NO_CONTACT_POINT;
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
        ContactPoint findIntersectionMidpoints = findEdgeIntersectionMidPoint(refPoly, incPoly, normal, pen);
        return findIntersectionMidpoints;
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

            float overlap = pA.max - pB.min;
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

    public static ContactPoint findEdgeIntersectionMidPoint(List<Vector2> polyA, List<Vector2> polyB, Vector2 normal, float penetrationDepth) {
        List<Vector2> contactPoints = new ArrayList<>();

        for (int i = 0; i < polyA.size(); i++) {
            Vector2 a1 = polyA.get(i);
            Vector2 a2 = polyA.get((i + 1) % polyA.size());

            for(int j = 0; j < polyB.size(); j++) {
                Vector2 b1 = polyB.get(j);
                Vector2 b2 = polyB.get((j + 1) % polyB.size());

                Vector2 intersection = lineSegmentIntersection(a1, a2, b1, b2);
                if(intersection != null) {

                    contactPoints.add(new Vector2());

                    // Only keep up to 2 contacts
                    if (contactPoints.size() >= 2) {
                        Vector2 midPoint = new Vector2((contactPoints.get(0).x + contactPoints.get(1).x)/2, (contactPoints.get(0).y + contactPoints.get(1).y)/2);
                        return new ContactPoint(midPoint, penetrationDepth, normal);
                    }
                }
            }
        }

        if(contactPoints.size() >= 2) {
            Vector2 midPoint = new Vector2((contactPoints.get(0).x + contactPoints.get(1).x)/2, (contactPoints.get(0).y + contactPoints.get(1).y)/2);
            return new ContactPoint(midPoint, penetrationDepth, normal);
        } else if (contactPoints.size() == 1) {
            return new ContactPoint(contactPoints.get(0), penetrationDepth, normal);
        } else {
            // No intersection points found, return a contact point at the centroid of the overlap region
            return ContactPoint.NO_CONTACT_POINT;
        }
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

