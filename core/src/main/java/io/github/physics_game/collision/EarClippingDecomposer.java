package io.github.physics_game.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Decomposes a simple (possibly concave) polygon into triangles using ear clipping.
 *
 * <p>Ear clipping is O(n^2) and robust for gameplay-sized polygons. We keep the implementation
 * explicit and heavily documented because this class is intended as a reference for the geometry
 * pipeline used by custom collision detection.</p>
 */
public final class EarClippingDecomposer {
    private static final float EPSILON = 1e-6f;

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
                // Degenerate or near-self-intersecting input can fail strict ear selection.
                // We gracefully finish with a fan triangulation from the first surviving vertex.
                fanTriangulateFallback(indices, cleaned, triangles);
                return triangles;
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
        for (int idx : indices) {
            if (idx == aIndex || idx == bIndex || idx == cIndex) {
                continue;
            }

            Vector2 p = vertices.get(idx);
            if (isPointInsideOrOnTriangle(p, a, b, c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConvex(Vector2 prev, Vector2 curr, Vector2 next) {
        return cross(prev, curr, next) > EPSILON;
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
            triangles.add(copyTriangle(a, b, c));
        }
    }

    public static List<List<Vector2>> mergePolygons(List<List<Vector2>> polys) {
        List<List<Vector2>> polygons = new ArrayList<>(polys.size());
        for (int i = 0; i < polys.size(); i++) {
            List<Vector2> poly = polys.get(i);
            polygons.add(new ArrayList<>(poly));
            if(poly.size() > 3) {
                Gdx.app.log("DynamicObject", "Merged poly: " + poly.size());
            }
        }

        for (int i = 0; i < polygons.size(); i++) {
            List<Vector2> currentPoly = polygons.get(i);
            for (int j = i + 1; j < polygons.size(); j++) {
                List<Vector2> nextPoly = polygons.get(j);
                boolean found = false;
                for (int k = 0; k < currentPoly.size() && !found; k++) {
                    Vector2 a = currentPoly.get(k);
                    Vector2 b = currentPoly.get((k + 1) % currentPoly.size());
                    for (int l = 0; l < nextPoly.size() && !found; l++) {
                        Vector2 c = nextPoly.get(l);
                        Vector2 d = nextPoly.get((l + 1) % nextPoly.size());
                        if (a.epsilonEquals(d, EPSILON) && b.epsilonEquals(c, EPSILON)) {
                            // Found a shared edge
                            found = true;

                            //all you have to check for convexness is whether a is at convex
                            //and b is at convex
                            int prevIndex = ((k - 1) + currentPoly.size()) % currentPoly.size();
                            int nextIndex = ((l - 1) + nextPoly.size()) % nextPoly.size();
                            //keep in mind of counterClockwise order:
                            //since a arrives before b
                            //the point before a arrives before in the merged polygon
                            boolean isACon = isConvex(currentPoly.get(prevIndex), a, nextPoly.get(nextIndex));
                            prevIndex = (k + 2) % currentPoly.size();
                            nextIndex = (l + 2) % nextPoly.size();
                            boolean isBCon = isConvex(nextPoly.get(nextIndex), b, currentPoly.get(prevIndex));

                            // if they are convex merge
                            if(isACon && isBCon) {
                                List<Vector2> merged = new ArrayList<>();
                                //first go through the vertices from k + 1 until k
                                for (int m = k + 1; m <= k + currentPoly.size() - 1; m++) {
                                    merged.add(currentPoly.get(m % currentPoly.size()));
                                }

                                //then go from the point that is equivalent to k around the next poly until the point that is equivalent to k + 1
                                for (int m = l + 1; m <= l + nextPoly.size() - 1; m++) {
                                    merged.add(nextPoly.get(m % nextPoly.size()));
                                }
                                polygons.set(i, merged);
                                polygons.remove(j);
                                j--;
                            }

                        }
                    }
                }
            }
        }
        return polygons;
    }
}

