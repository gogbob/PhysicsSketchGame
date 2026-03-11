package io.github.physics_game.collision;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom concave polygon contact detector.
 *
 * <p>Pipeline:
 * 1) Transform local polygon vertices into world space.
 * 2) Decompose each (possibly concave) polygon into triangles via ear clipping.
 * 3) Run broadphase AABB pruning on triangle pairs.
 * 4) Run SAT on surviving pairs and keep the deepest contact.
 *
 * <p>This avoids Box2D narrowphase while still producing a simulation-friendly contact normal.</p>
 */
public final class CustomContactHandler {
    private CustomContactHandler() {
    }

    public static ContactResult detect(PolygonBody a, PolygonBody b) {
        if (a == null || b == null) {
            return ContactResult.NO_CONTACT;
        }

        List<Vector2> worldA = a.toWorldVertices();
        List<Vector2> worldB = b.toWorldVertices();
        if (worldA.size() < 3 || worldB.size() < 3) {
            return ContactResult.NO_CONTACT;
        }

        List<List<Vector2>> trianglesA = EarClippingDecomposer.decomposeToTriangles(worldA);
        List<List<Vector2>> trianglesB = EarClippingDecomposer.decomposeToTriangles(worldB);
        if (trianglesA.isEmpty() || trianglesB.isEmpty()) {
            return ContactResult.NO_CONTACT;
        }

        ContactResult best = ContactResult.NO_CONTACT;

        for (List<Vector2> triA : trianglesA) {
            Aabb aabbA = Aabb.fromPolygon(triA);
            for (List<Vector2> triB : trianglesB) {
                Aabb aabbB = Aabb.fromPolygon(triB);
                if (!aabbA.overlaps(aabbB)) {
                    continue;
                }

                ContactResult result = SatCollision.detect(triA, triB);
                if (result.isColliding() && result.getPenetrationDepth() > best.getPenetrationDepth()) {
                    best = result;
                }
            }
        }

        return best;
    }

    public static final class PolygonBody {
        private final List<Vector2> localVertices;
        private final Vector2 position = new Vector2();
        private float rotationRadians;

        public PolygonBody(List<Vector2> localVertices) {
            this.localVertices = copyVertices(localVertices);
        }

        public PolygonBody setPosition(float x, float y) {
            position.set(x, y);
            return this;
        }

        public PolygonBody setRotationRadians(float rotationRadians) {
            this.rotationRadians = rotationRadians;
            return this;
        }

        public Vector2 getPosition() {
            return new Vector2(position);
        }

        public float getRotationRadians() {
            return rotationRadians;
        }

        public List<Vector2> toWorldVertices() {
            if (localVertices.size() < 3) {
                return Collections.emptyList();
            }

            float cos = (float) Math.cos(rotationRadians);
            float sin = (float) Math.sin(rotationRadians);

            List<Vector2> out = new ArrayList<>(localVertices.size());
            for (Vector2 v : localVertices) {
                float x = v.x * cos - v.y * sin + position.x;
                float y = v.x * sin + v.y * cos + position.y;
                out.add(new Vector2(x, y));
            }
            return out;
        }

        private static List<Vector2> copyVertices(List<Vector2> vertices) {
            List<Vector2> copied = new ArrayList<>();
            if (vertices == null) {
                return copied;
            }
            for (Vector2 v : vertices) {
                if (v != null) {
                    copied.add(new Vector2(v));
                }
            }
            return copied;
        }
    }

    private static final class Aabb {
        final float minX;
        final float minY;
        final float maxX;
        final float maxY;

        private Aabb(float minX, float minY, float maxX, float maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        static Aabb fromPolygon(List<Vector2> polygon) {
            float minX = polygon.get(0).x;
            float minY = polygon.get(0).y;
            float maxX = minX;
            float maxY = minY;

            for (int i = 1; i < polygon.size(); i++) {
                Vector2 v = polygon.get(i);
                minX = Math.min(minX, v.x);
                minY = Math.min(minY, v.y);
                maxX = Math.max(maxX, v.x);
                maxY = Math.max(maxY, v.y);
            }

            return new Aabb(minX, minY, maxX, maxY);
        }

        boolean overlaps(Aabb other) {
            return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY;
        }
    }
}

