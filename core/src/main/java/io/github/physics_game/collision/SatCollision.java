package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

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
        List<ContactPoint> manifoldPoints = clipEdges(refPoly, incPoly, normal, refEdgeIdx, pen);
        return new ContactManifold(true, normal, manifoldPoints);
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

    /**
     * Clip incident edge against reference face to generate up to 2 contact points.
     */
    private static List<ContactPoint> clipEdges(List<Vector2> refPoly, List<Vector2> incPoly,
                                                 Vector2 normal, int refEdgeIdx, float totalPenetration) {
        List<ContactPoint> result = new ArrayList<>();

        // Reference edge vertices
        Vector2 refA = refPoly.get(refEdgeIdx);
        Vector2 refB = refPoly.get((refEdgeIdx + 1) % refPoly.size());
        Vector2 refEdge = new Vector2(refB).sub(refA);

        // Find incident edge: opposite normal direction
        Vector2 negNormal = new Vector2(normal).scl(-1f);
        int incEdgeIdx = findIncidentEdge(incPoly, negNormal);
        Vector2 incA = incPoly.get(incEdgeIdx);
        Vector2 incB = incPoly.get((incEdgeIdx + 1) % incPoly.size());

        // Clip incident edge against reference edge side planes
        List<Vector2> clipped = new ArrayList<>();
        clipped.add(new Vector2(incA));
        clipped.add(new Vector2(incB));

        // Clip against left plane of reference edge
        Vector2 leftNormal = new Vector2(-refEdge.y, refEdge.x).nor();
        clipped = clipSegmentToPlane(clipped, refA, leftNormal);
        if (clipped.size() < 2) return result;

        // Clip against right plane of reference edge
        Vector2 rightNormal = new Vector2(refEdge.y, -refEdge.x).nor();
        clipped = clipSegmentToPlane(clipped, refB, rightNormal);
        if (clipped.size() < 2) return result;

        // Clip against reference face plane (normal pointing into incident poly)
        clipped = clipSegmentToPlane(clipped, refA, normal);

        // Convert clipped points to ContactPoints; keep points behind reference face
        for (Vector2 p : clipped) {
            Vector2 toPoint = new Vector2(p).sub(refA);
            float depth = -toPoint.dot(normal) + totalPenetration * 0.1f; // small bias
            if (depth >= -EPSILON) {
                result.add(new ContactPoint(p, Math.max(0f, depth)));
            }
        }

        // If we got too many or too few, pick the deepest point + one more
        if (result.size() > 2) {
            ContactPoint deepest = result.get(0);
            int deepestIdx = 0;
            for (int i = 1; i < result.size(); i++) {
                if (result.get(i).penetration > deepest.penetration) {
                    deepest = result.get(i);
                    deepestIdx = i;
                }
            }
            // Keep deepest and one neighbor if available
            List<ContactPoint> pruned = new ArrayList<>();
            pruned.add(deepest);
            if (deepestIdx > 0) {
                pruned.add(result.get(deepestIdx - 1));
            } else if (result.size() > 1) {
                pruned.add(result.get(1));
            }
            result = pruned;
        }

        // Fallback: if no points, use original edge center estimate
        if (result.isEmpty()) {
            Vector2 cp = new Vector2(incA).add(incB).scl(0.5f);
            result.add(new ContactPoint(cp, totalPenetration));
        }

        return result;
    }

    /**
     * Find the incident edge: the edge whose outward normal is most opposite to the given direction.
     */
    private static int findIncidentEdge(List<Vector2> poly, Vector2 direction) {
        int best = 0;
        float bestDot = Float.MAX_VALUE;

        for (int i = 0; i < poly.size(); i++) {
            Vector2 v1 = poly.get(i);
            Vector2 v2 = poly.get((i + 1) % poly.size());
            Vector2 edge = new Vector2(v2).sub(v1);
            Vector2 edgeNormal = new Vector2(-edge.y, edge.x).nor();

            float dot = edgeNormal.dot(direction);
            //use the fact that it is convex to say that the most opposite normal will be the one with the smallest dot product
            if (dot > bestDot) {
                bestDot = dot;
                best = i;
            }
        }

        return best;
    }

    /**
     * Clip a line segment against a half-plane defined by a point and inward normal.
     * Returns the clipped segment (0, 1, or 2 points).
     */
    private static List<Vector2> clipSegmentToPlane(List<Vector2> segment, Vector2 planePoint, Vector2 planeNormal) {
        if (segment.size() < 2) return segment;

        List<Vector2> result = new ArrayList<>();
        Vector2 start = segment.get(0);
        Vector2 end = segment.get(1);

        float dStart = new Vector2(start).sub(planePoint).dot(planeNormal);
        float dEnd = new Vector2(end).sub(planePoint).dot(planeNormal);

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

    private static Vector2 supportPoint(List<Vector2> polygon, Vector2 direction) {
        Vector2 best = polygon.get(0);
        float bestDot = best.dot(direction);

        for (int i = 1; i < polygon.size(); i++) {
            Vector2 candidate = polygon.get(i);
            float dot = candidate.dot(direction);
            if (dot > bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }

        return new Vector2(best);
    }

    private static Vector2 estimateContactPoint(List<Vector2> polygonA, List<Vector2> polygonB, Vector2 normal) {
        List<Vector2> overlap = intersectConvexPolygons(polygonA, polygonB);
        if (!overlap.isEmpty()) {
            return polygonCentroid(overlap);
        }

        // Fallback for degenerate intersections (parallel/near-zero area overlap).
        Vector2 supportA = supportPoint(polygonA, normal);
        Vector2 supportB = supportPoint(polygonB, new Vector2(normal).scl(-1f));
        return new Vector2(supportA).add(supportB).scl(0.5f);
    }

    /**
     * Sutherland-Hodgman clipping for convex polygons in world space.
     * The result is the convex overlap polygon (possibly empty).
     */
    private static List<Vector2> intersectConvexPolygons(List<Vector2> subject, List<Vector2> clip) {
        List<Vector2> output = new ArrayList<>();
        for (Vector2 v : subject) {
            output.add(new Vector2(v));
        }

        boolean clipIsCcw = signedArea(clip) >= 0f;

        for (int i = 0; i < clip.size(); i++) {
            Vector2 clipA = clip.get(i);
            Vector2 clipB = clip.get((i + 1) % clip.size());

            List<Vector2> input = output;
            output = new ArrayList<>();
            if (input.isEmpty()) {
                break;
            }

            Vector2 s = input.get(input.size() - 1);
            for (Vector2 e : input) {
                boolean eInside = isInsideHalfPlane(e, clipA, clipB, clipIsCcw);
                boolean sInside = isInsideHalfPlane(s, clipA, clipB, clipIsCcw);

                if (eInside) {
                    if (!sInside) {
                        output.add(lineIntersection(s, e, clipA, clipB));
                    }
                    output.add(new Vector2(e));
                } else if (sInside) {
                    output.add(lineIntersection(s, e, clipA, clipB));
                }

                s = e;
            }
        }

        return output;
    }

    private static boolean isInsideHalfPlane(Vector2 p, Vector2 edgeA, Vector2 edgeB, boolean edgeCcw) {
        Vector2 edge = new Vector2(edgeB).sub(edgeA);
        Vector2 ap = new Vector2(p).sub(edgeA);
        float c = cross(edge, ap);
        return edgeCcw ? c >= -EPSILON : c <= EPSILON;
    }

    private static Vector2 lineIntersection(Vector2 s, Vector2 e, Vector2 a, Vector2 b) {
        Vector2 r = new Vector2(e).sub(s);
        Vector2 q = new Vector2(b).sub(a);
        float denom = cross(r, q);

        if (Math.abs(denom) <= EPSILON) {
            // Near-parallel case; midpoint keeps output bounded for debug rendering.
            return new Vector2(s).add(e).scl(0.5f);
        }

        Vector2 aMinusS = new Vector2(a).sub(s);
        float t = cross(aMinusS, q) / denom;
        return new Vector2(s).mulAdd(r, t);
    }

    private static float cross(Vector2 u, Vector2 v) {
        return u.x * v.y - u.y * v.x;
    }

    private static float signedArea(List<Vector2> polygon) {
        float areaTwice = 0f;
        for (int i = 0; i < polygon.size(); i++) {
            Vector2 c = polygon.get(i);
            Vector2 n = polygon.get((i + 1) % polygon.size());
            areaTwice += c.x * n.y - n.x * c.y;
        }
        return 0.5f * areaTwice;
    }

    private static Vector2 polygonCentroid(List<Vector2> polygon) {
        if (polygon.size() == 1) {
            return new Vector2(polygon.get(0));
        }

        float areaTwice = 0f;
        float cx = 0f;
        float cy = 0f;

        for (int i = 0; i < polygon.size(); i++) {
            Vector2 c = polygon.get(i);
            Vector2 n = polygon.get((i + 1) % polygon.size());
            float cross = c.x * n.y - n.x * c.y;
            areaTwice += cross;
            cx += (c.x + n.x) * cross;
            cy += (c.y + n.y) * cross;
        }

        if (Math.abs(areaTwice) <= EPSILON) {
            return centroid(polygon);
        }

        float factor = 1f / (3f * areaTwice);
        return new Vector2(cx * factor, cy * factor);
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

