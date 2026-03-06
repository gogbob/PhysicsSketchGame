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

    public static ContactResult detect(List<Vector2> polygonA, List<Vector2> polygonB) {
        if (polygonA == null || polygonB == null || polygonA.size() < 3 || polygonB.size() < 3) {
            return ContactResult.NO_CONTACT;
        }

        float minOverlap = Float.MAX_VALUE;
        Vector2 bestAxis = null;

        ContactResult fromA = testAxes(polygonA, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromA.isColliding()) {
            return ContactResult.NO_CONTACT;
        }
        minOverlap = fromA.getPenetrationDepth();
        bestAxis = fromA.getNormal();

        ContactResult fromB = testAxes(polygonB, polygonA, polygonB, minOverlap, bestAxis);
        if (!fromB.isColliding()) {
            return ContactResult.NO_CONTACT;
        }

        minOverlap = fromB.getPenetrationDepth();
        bestAxis = fromB.getNormal();

        Vector2 centerA = centroid(polygonA);
        Vector2 centerB = centroid(polygonB);
        Vector2 centerDelta = new Vector2(centerB).sub(centerA);

        // Ensure normal convention is always A -> B.
        if (bestAxis.dot(centerDelta) < 0f) {
            bestAxis.scl(-1f);
        }

        // Approximate contact location from opposing support points along the final normal.
        // This is stable enough for debug visualization, but may drift if penetration is very deep.
        Vector2 contactPoint = estimateContactPoint(polygonA, polygonB, bestAxis);
        return new ContactResult(true, bestAxis, minOverlap, contactPoint);
    }

    private static ContactResult testAxes(List<Vector2> axisSource,
                                          List<Vector2> polygonA,
                                          List<Vector2> polygonB,
                                          float currentMinOverlap,
                                          Vector2 currentBestAxis) {
        float minOverlap = currentMinOverlap;
        Vector2 bestAxis = currentBestAxis == null ? new Vector2() : new Vector2(currentBestAxis);

        for (int i = 0; i < axisSource.size(); i++) {
            Vector2 v1 = axisSource.get(i);
            Vector2 v2 = axisSource.get((i + 1) % axisSource.size());
            Vector2 edge = new Vector2(v2).sub(v1);

            if (edge.len2() <= EPSILON) {
                continue;
            }

            Vector2 axis = new Vector2(-edge.y, edge.x).nor();

            Projection pA = projectPolygon(axis, polygonA);
            Projection pB = projectPolygon(axis, polygonB);

            float overlap = Math.min(pA.max, pB.max) - Math.max(pA.min, pB.min);
            if (overlap <= 0f) {
                return ContactResult.NO_CONTACT;
            }

            if (overlap < minOverlap) {
                minOverlap = overlap;
                bestAxis = axis;
            }
        }

        return new ContactResult(true, bestAxis, minOverlap);
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

