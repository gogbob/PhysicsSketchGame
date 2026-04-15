package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EarClippingDecomposer {
    private static final float EPSILON = 1e-8f;
    private static final float CONVEX_EPSILON = 1e-6f;

    private EarClippingDecomposer() {
    }

    public static List<List<Vector2>> decomposeToTriangles(List<Vector2> polygon) {
        List<Vector2> cleaned = sanitizePolygon(polygon);
        if (cleaned.size() < 3) {
            return Collections.emptyList();
        }

        // Ear clipping assumes a consistent winding. We enforce CCW so convexity checks have
        // a single sign convention: cross(prev->curr, curr->next) > 0 means a convex corner.
        if (signedArea(cleaned) < 0f) {
            Collections.reverse(cleaned);
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            indices.add(i);
        }

        List<List<Vector2>> triangles = new ArrayList<>();

        // Keep removing one "ear" at a time until only a base triangle remains.
        int guard = 0;
        int maxIterations = cleaned.size() * cleaned.size();
        while (indices.size() > 3 && guard < maxIterations) {
            boolean earFound = false;

            for (int i = 0; i < indices.size(); i++) {
                int prevIndex = indices.get((i - 1 + indices.size()) % indices.size());
                int currIndex = indices.get(i);
                int nextIndex = indices.get((i + 1) % indices.size());

                Vector2 prev = cleaned.get(prevIndex);
                Vector2 curr = cleaned.get(currIndex);
                Vector2 next = cleaned.get(nextIndex);

                if (!isConvex(prev, curr, next)) {
                    continue;
                }

                // A valid ear triangle contains no other remaining polygon vertex.
                if (containsAnyVertex(indices, cleaned, prevIndex, currIndex, nextIndex, prev, curr, next)) {
                    continue;
                }

                triangles.add(copyTriangle(prev, curr, next));
                indices.remove(i);
                earFound = true;
                break;
            }

            if (!earFound) {
                // If strict ear selection fails, clip the least-bad local ear instead of arbitrary fan splitting.
                int bestFallbackEar = selectBestFallbackEar(indices, cleaned);
                if (bestFallbackEar < 0) {
                    fanTriangulateFallback(indices, cleaned, triangles);
                    return triangles;
                }

                int prevIndex = indices.get((bestFallbackEar - 1 + indices.size()) % indices.size());
                int currIndex = indices.get(bestFallbackEar);
                int nextIndex = indices.get((bestFallbackEar + 1) % indices.size());
                triangles.add(copyTriangle(cleaned.get(prevIndex), cleaned.get(currIndex), cleaned.get(nextIndex)));
                indices.remove(bestFallbackEar);
            }

            guard++;
        }

        if (indices.size() == 3) {
            Vector2 a = cleaned.get(indices.get(0));
            Vector2 b = cleaned.get(indices.get(1));
            Vector2 c = cleaned.get(indices.get(2));
            triangles.add(copyTriangle(a, b, c));
        }

        return triangles;
    }

    private static List<Vector2> sanitizePolygon(List<Vector2> polygon) {
        List<Vector2> out = new ArrayList<>();
        if (polygon == null) {
            return out;
        }

        for (Vector2 v : polygon) {
            if (v == null) {
                continue;
            }

            if (out.isEmpty() || !out.get(out.size() - 1).epsilonEquals(v, EPSILON)) {
                out.add(new Vector2(v));
            }
        }

        if (out.size() > 1 && out.get(0).epsilonEquals(out.get(out.size() - 1), EPSILON)) {
            out.remove(out.size() - 1);
        }

        // Remove almost-collinear points; they create zero-area ears and unstable normals.
        int i = 0;
        while (out.size() >= 3 && i < out.size()) {
            Vector2 prev = out.get((i - 1 + out.size()) % out.size());
            Vector2 curr = out.get(i);
            Vector2 next = out.get((i + 1) % out.size());

            float cross = cross(prev, curr, next);
            if (Math.abs(cross) <= EPSILON) {
                out.remove(i);
                continue;
            }

            i++;
        }

        return out;
    }

    private static boolean containsAnyVertex(List<Integer> indices,
                                             List<Vector2> vertices,
                                             int aIndex,
                                             int bIndex,
                                             int cIndex,
                                             Vector2 a,
                                             Vector2 b,
                                             Vector2 c) {
        return countContainedVertices(indices, vertices, aIndex, bIndex, cIndex, a, b, c) > 0;
    }

    private static int countContainedVertices(List<Integer> indices,
                                              List<Vector2> vertices,
                                              int aIndex,
                                              int bIndex,
                                              int cIndex,
                                              Vector2 a,
                                              Vector2 b,
                                              Vector2 c) {
        int count = 0;
        for (int idx : indices) {
            if (idx == aIndex || idx == bIndex || idx == cIndex) {
                continue;
            }

            Vector2 p = vertices.get(idx);
            if (isPointInsideOrOnTriangle(p, a, b, c)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isConvex(Vector2 prev, Vector2 curr, Vector2 next) {
        return new Vector2(curr).sub(prev).crs(new Vector2(next).sub(curr)) > CONVEX_EPSILON;
    }

    private static float cross(Vector2 a, Vector2 b, Vector2 c) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float bcx = c.x - b.x;
        float bcy = c.y - b.y;
        return abx * bcy - aby * bcx;
    }

    private static float signedArea(List<Vector2> polygon) {
        float areaTwice = 0f;
        for (int i = 0; i < polygon.size(); i++) {
            Vector2 current = polygon.get(i);
            Vector2 next = polygon.get((i + 1) % polygon.size());
            areaTwice += current.x * next.y - next.x * current.y;
        }
        return areaTwice * 0.5f;
    }

    private static boolean isPointInsideOrOnTriangle(Vector2 p, Vector2 a, Vector2 b, Vector2 c) {
        // Barycentric sign test: p is inside/on edges when all oriented areas have same sign.
        float c1 = orientedArea(a, b, p);
        float c2 = orientedArea(b, c, p);
        float c3 = orientedArea(c, a, p);

        boolean hasNegative = c1 < -EPSILON || c2 < -EPSILON || c3 < -EPSILON;
        boolean hasPositive = c1 > EPSILON || c2 > EPSILON || c3 > EPSILON;
        return !(hasNegative && hasPositive);
    }

    private static float orientedArea(Vector2 a, Vector2 b, Vector2 c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static int selectBestFallbackEar(List<Integer> indices, List<Vector2> vertices) {
        int bestEar = -1;
        int bestContained = Integer.MAX_VALUE;
        int bestOutsidePenalty = Integer.MAX_VALUE;
        float bestAreaAbs = -1f;

        for (int i = 0; i < indices.size(); i++) {
            int prevIndex = indices.get((i - 1 + indices.size()) % indices.size());
            int currIndex = indices.get(i);
            int nextIndex = indices.get((i + 1) % indices.size());

            Vector2 prev = vertices.get(prevIndex);
            Vector2 curr = vertices.get(currIndex);
            Vector2 next = vertices.get(nextIndex);

            float triArea = Math.abs(orientedArea(prev, curr, next));
            if (triArea <= CONVEX_EPSILON) {
                continue;
            }

            int contained = countContainedVertices(indices, vertices, prevIndex, currIndex, nextIndex, prev, curr, next);
            int outsidePenalty = computeOutsidePenalty(vertices, prev, curr, next);

            if (contained < bestContained
                || (contained == bestContained && outsidePenalty < bestOutsidePenalty)
                || (contained == bestContained && outsidePenalty == bestOutsidePenalty && triArea > bestAreaAbs)) {
                bestContained = contained;
                bestOutsidePenalty = outsidePenalty;
                bestAreaAbs = triArea;
                bestEar = i;
            }
        }

        return bestEar;
    }

    private static int computeOutsidePenalty(List<Vector2> polygon, Vector2 a, Vector2 b, Vector2 c) {
        int penalty = 0;

        // Penalize candidate ears whose diagonal likely exits the polygon boundary.
        if (segmentIntersectsPolygonEdges(a, c, polygon)) {
            penalty += 8;
        }

        Vector2 centroid = new Vector2(a).add(b).add(c).scl(1f / 3f);
        if (!isPointInsidePolygon(centroid, polygon)) {
            penalty += 4;
        }

        Vector2 abMid = new Vector2(a).add(b).scl(0.5f);
        Vector2 bcMid = new Vector2(b).add(c).scl(0.5f);
        Vector2 caMid = new Vector2(c).add(a).scl(0.5f);
        if (!isPointInsidePolygon(abMid, polygon)) penalty++;
        if (!isPointInsidePolygon(bcMid, polygon)) penalty++;
        if (!isPointInsidePolygon(caMid, polygon)) penalty++;

        return penalty;
    }

    private static boolean segmentIntersectsPolygonEdges(Vector2 a, Vector2 b, List<Vector2> polygon) {
        for (int i = 0; i < polygon.size(); i++) {
            Vector2 p = polygon.get(i);
            Vector2 q = polygon.get((i + 1) % polygon.size());

            if (a.epsilonEquals(p, EPSILON) || a.epsilonEquals(q, EPSILON)
                || b.epsilonEquals(p, EPSILON) || b.epsilonEquals(q, EPSILON)) {
                continue;
            }

            if (segmentsIntersect(a, b, p, q)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPointInsidePolygon(Vector2 point, List<Vector2> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Vector2 vi = polygon.get(i);
            Vector2 vj = polygon.get(j);
            boolean crosses = ((vi.y > point.y) != (vj.y > point.y))
                && (point.x < (vj.x - vi.x) * (point.y - vi.y) / ((vj.y - vi.y) + EPSILON) + vi.x);
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean segmentsIntersect(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        float o1 = orientedArea(a, b, c);
        float o2 = orientedArea(a, b, d);
        float o3 = orientedArea(c, d, a);
        float o4 = orientedArea(c, d, b);

        if (Math.abs(o1) <= EPSILON && onSegment(a, b, c)) return true;
        if (Math.abs(o2) <= EPSILON && onSegment(a, b, d)) return true;
        if (Math.abs(o3) <= EPSILON && onSegment(c, d, a)) return true;
        if (Math.abs(o4) <= EPSILON && onSegment(c, d, b)) return true;

        return (o1 > 0f) != (o2 > 0f) && (o3 > 0f) != (o4 > 0f);
    }

    private static boolean onSegment(Vector2 a, Vector2 b, Vector2 p) {
        return p.x >= Math.min(a.x, b.x) - EPSILON
            && p.x <= Math.max(a.x, b.x) + EPSILON
            && p.y >= Math.min(a.y, b.y) - EPSILON
            && p.y <= Math.max(a.y, b.y) + EPSILON;
    }

    private static List<Vector2> copyTriangle(Vector2 a, Vector2 b, Vector2 c) {
        List<Vector2> tri = new ArrayList<>(3);
        tri.add(new Vector2(a));
        tri.add(new Vector2(b));
        tri.add(new Vector2(c));
        return tri;
    }

    private static void fanTriangulateFallback(List<Integer> indices,
                                               List<Vector2> cleaned,
                                               List<List<Vector2>> triangles) {
        if (indices.size() < 3) {
            return;
        }

        int root = indices.get(0);
        for (int i = 1; i < indices.size() - 1; i++) {
            Vector2 a = cleaned.get(root);
            Vector2 b = cleaned.get(indices.get(i));
            Vector2 c = cleaned.get(indices.get(i + 1));
            if (Math.abs(orientedArea(a, b, c)) <= CONVEX_EPSILON) {
                continue;
            }
            triangles.add(copyTriangle(a, b, c));
        }
    }

    public static List<List<Vector2>> mergePolygons(List<List<Vector2>> polys) {
        List<List<Vector2>> polygons = new ArrayList<>(polys.size());
        for (List<Vector2> poly : polys) {
            polygons.add(new ArrayList<>(poly));
        }

        boolean mergedAny = true;
        while (mergedAny) {
            mergedAny = false;

            for (int i = 0; i < polygons.size() && !mergedAny; i++) {
                List<Vector2> currentPoly = polygons.get(i);
                for (int j = i + 1; j < polygons.size() && !mergedAny; j++) {
                    List<Vector2> nextPoly = polygons.get(j);
                    List<Vector2> merged = tryMergeAlongSharedEdge(currentPoly, nextPoly);
                    if (merged != null) {
                        polygons.set(i, merged);
                        polygons.remove(j);
                        mergedAny = true;
                    }
                }
            }
        }

        return polygons;
    }

    private static List<Vector2> tryMergeAlongSharedEdge(List<Vector2> a, List<Vector2> b) {
        for (int i = 0; i < a.size(); i++) {
            Vector2 a0 = a.get(i);
            Vector2 a1 = a.get((i + 1) % a.size());

            for (int j = 0; j < b.size(); j++) {
                Vector2 b0 = b.get(j);
                Vector2 b1 = b.get((j + 1) % b.size());

                if (!a0.epsilonEquals(b1, EPSILON) || !a1.epsilonEquals(b0, EPSILON)) {
                    continue;
                }

                List<Vector2> merged = buildMergedLoop(a, b, i, j);
                merged = sanitizePolygon(merged);
                if (merged.size() < 3) {
                    continue;
                }
                if (signedArea(merged) < 0f) {
                    Collections.reverse(merged);
                }
                if (isStrictlyConvex(merged)) {
                    return merged;
                }
            }
        }

        return null;
    }

    private static List<Vector2> buildMergedLoop(List<Vector2> a, List<Vector2> b, int edgeA, int edgeB) {
        List<Vector2> merged = new ArrayList<>();

        // Walk A from edgeA+1 back to edgeA (exclusive of shared edge endpoints on one side).
        for (int m = edgeA + 1; m <= edgeA + a.size() - 1; m++) {
            merged.add(new Vector2(a.get(m % a.size())));
        }

        // Walk B from edgeB+1 back to edgeB.
        for (int m = edgeB + 1; m <= edgeB + b.size() - 1; m++) {
            merged.add(new Vector2(b.get(m % b.size())));
        }

        return merged;
    }

    private static boolean isStrictlyConvex(List<Vector2> poly) {
        if (poly.size() < 3) {
            return false;
        }

        for (int i = 0; i < poly.size(); i++) {
            Vector2 prev = poly.get((i - 1 + poly.size()) % poly.size());
            Vector2 curr = poly.get(i);
            Vector2 next = poly.get((i + 1) % poly.size());
            if (orientedArea(prev, curr, next) <= CONVEX_EPSILON) {
                return false;
            }
        }

        return true;
    }
}
