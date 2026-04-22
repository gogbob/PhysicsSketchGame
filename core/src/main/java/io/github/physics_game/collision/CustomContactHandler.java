package io.github.physics_game.collision;

import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.object_types.PhysicsObject;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static ContactManifold detect(PhysicsObject a, PhysicsObject b) {
        if (a == null || b == null) {
            return ContactManifold.NO_CONTACT;
        }

        List<List<Vector2>> polysA = toWorld(a);
        List<List<Vector2>> polysB = toWorld(b);
        if (polysA.isEmpty() || polysB.isEmpty()) {
            return ContactManifold.NO_CONTACT;
        }

        // Collect best manifold across all triangle pairs, filtering internal edges
        Vector2 bestNormal = new Vector2();
        float bestDepth = -Float.MAX_VALUE;
        float bestCorrection = -Float.MAX_VALUE;
        List<ContactPoint> contacts = new ArrayList<>();
        ContactPoint bestPoint = null;
        for (List<Vector2> pA : polysA) {
            Aabb aabbA = Aabb.fromPolygon(pA);
            for (List<Vector2> pB : polysB) {
                Aabb aabbB = Aabb.fromPolygon(pB);
                if (!aabbA.overlaps(aabbB)) {
                    continue;
                }

                ContactManifold manifold = SatCollision.detect(pA, pB);

                if (manifold.isColliding() && manifold.getPoints().isEmpty()) {
                    System.out.println("WARNING: collision without contacts");
                }

                if(manifold.isColliding()) {
                    if(manifold.getPenetration() > bestDepth) {
                        bestNormal = manifold.getNormal();
                        bestDepth = manifold.getPenetration();
                    }
                    for(ContactPoint point : manifold.getPoints()) {
                        boolean isUnique = true;
                        for(ContactPoint p : contacts)
                        {
                            if(point.point.epsilonEquals(p.point, 0.01f)) {
                                isUnique = false;
                                break;
                            }
                        }
                        if(isUnique) {
                            contacts.add(point);
                            if(bestPoint == null || point.penetration > bestCorrection) {
                                bestCorrection = point.penetration;
                                bestPoint = point;
                            }
                        }
                    }
                }
            }
        }

        if(contacts.isEmpty()) {
            return new ContactManifold(true, bestNormal, new ArrayList<>(), bestDepth, a, b);
        } else if(contacts.size() == 1) {
            return new ContactManifold(true, bestNormal, contacts, bestDepth, a, b);
        } else {
            //find farthest point from best
            ContactPoint farthest = null;
            float maxDistance = -Float.MAX_VALUE;
            for(ContactPoint point : contacts) {
                float distance = point.point.dst(bestPoint.point);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    farthest = point;
                }
            }

            return new ContactManifold(true, bestNormal, Arrays.asList(bestPoint, farthest), bestDepth, a, b);
        }
    }

    public static List<List<Vector2>> toWorld(PhysicsObject object) {
        float rotationRadians = object.getRotation();
        Vector2 position = object.getPosition();

        List<List<Vector2>> newObject = new ArrayList<>();
        for(List<Vector2> poly : object.getConcaveLocalBest()) {
            if (poly.size() < 3) {
                return Collections.emptyList();
            }

            float cos = (float) Math.cos(rotationRadians);
            float sin = (float) Math.sin(rotationRadians);

            List<Vector2> out = new ArrayList<>(poly.size());
            for (Vector2 v : poly) {
                float x = v.x * cos - v.y * sin + position.x;
                float y = v.x * sin + v.y * cos + position.y;
                out.add(new Vector2(x, y));
            }
            newObject.add(out);
        }

        return newObject;
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

        public List<Vector2> getLocalVertices() {
            return localVertices;
        }
        public static List<Vector2> copyVertices(List<Vector2> vertices) {
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

    public static Vector2 toWorld(Vector2 position, float rotation, Vector2 local) {
        float x = local.x * ((float) Math.cos(rotation)) - local.y * ((float) Math.sin(rotation)) + position.x;
        float y = local.x * ((float) Math.sin(rotation)) + local.y * ((float) Math.cos(rotation)) + position.y;

        return new Vector2(x, y);
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
            //set a bounding box around the object
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

