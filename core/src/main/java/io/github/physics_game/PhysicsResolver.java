package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import io.github.physics_game.collision.ContactManifold;
import io.github.physics_game.collision.ContactPoint;
import io.github.physics_game.collision.CustomContactHandler;

import java.util.ArrayList;
import java.util.List;

public class PhysicsResolver {
    final static float fixedStep = 1f / 60f;
    final static int NUM_VEL_ITERATIONS = 6;
    final static int NUM_POS_ITERATIONS = 10;
    final static Vector2 GRAVITY = new Vector2(0, -6f);
    public static void step(ArrayList<PhysicsObject> objects) {
        while(Main.accumulator >= fixedStep) {
            // Step the physics simulation with a fixed time step. This ensures consistent behavior regardless of frame rate.

            //1. Integrate forces → update velocity
            //2. Detect collisions
            //3. Build contact constraints
            //4. Solve velocity constraints (impulses)
            //5. Solve position constraints
            //6. Update positions

            //RESOLVING ALL FORCES
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    //apply gravity

                    Vector2 currentVelocity = new Vector2(dynObj.getLinearVelocity());
                    Vector2 newVelocity = currentVelocity.add(new Vector2(GRAVITY).scl(fixedStep));
                    dynObj.setLinearVelocity(newVelocity);
                }
            }

            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {
                boolean anyCollision = false;
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(iteration == 0) {
                            if(((obj1 instanceof TriggerObject || obj1 instanceof DynamicTriggerObject) && obj2 instanceof DynamicObject)
                                || ((obj2 instanceof TriggerObject || obj2 instanceof DynamicTriggerObject) && obj1 instanceof DynamicObject)) {
                                //check if a body is triggering a field
                                ContactManifold manifold = CustomContactHandler.detect(obj1, obj2);
                                if(manifold.isColliding()) {
                                    if (obj1 instanceof TriggerObject) {
                                        ((TriggerObject) obj1).addTriggered(obj2.getId());
                                    } else if(obj2 instanceof TriggerObject) {
                                        ((TriggerObject) obj2).addTriggered(obj1.getId());
                                    }
                                    //dynamic trigger objects can also trigger other trigger objects so are independent
                                    if(obj1 instanceof DynamicTriggerObject) {
                                        ((DynamicTriggerObject) obj1).addTriggered(obj2.getId());
                                    }
                                    if(obj2 instanceof DynamicTriggerObject) {
                                        ((DynamicTriggerObject) obj2).addTriggered(obj1.getId());
                                    }
                                }
                            }
                        }
                        if(resolveCollision(obj1, obj2, false, null, iteration, true)) {
                            anyCollision = true;
                        }
                    }
                }
                if(!anyCollision) {
                    break;
                }
            }
            // move the objects
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    dynObj.updatePosition(fixedStep);
                }
                if(obj instanceof FollowingTriggerObject) {
                    ((FollowingTriggerObject) obj).updatePosition();
                }
            }

            //correct the positions to prevent sinking due to numerical errors

            for(int iteration = 0; iteration < NUM_POS_ITERATIONS; iteration++) {
                boolean anyCorrection = false;
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(resolvePenetrationCorrection(obj1, obj2)) {
                            anyCorrection = true;
                        }
                    }
                }
                if(!anyCorrection) {
                    break;
                }
            }

            Main.accumulator -= fixedStep;
        }
    }

    public static ArrayList<DebugForce> stepWithDebug(ArrayList<PhysicsObject> objects) {

        ArrayList<DebugForce> forces = new ArrayList<>();

        for(PhysicsObject obj : objects) {
            if (obj instanceof DynamicObject) {
                DebugForce velForce = new DebugForce(((DynamicObject) obj).getCenter(), new Vector2(((DynamicObject) obj).getLinearVelocity()).scl(2f));
                velForce.setColor(Color.GREEN);
                forces.add(velForce);
            }
        }
        if(Main.accumulator < fixedStep) {
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    forces.add(new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass())));
                }
            }
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {
                boolean anyCollision = false;
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(resolveCollision(obj1, obj2, true, forces, iteration, false)) {
                            anyCollision = true;
                        }
                    }
                }
                if(!anyCollision) {
                    break;
                }
            }
        }

        while(Main.accumulator >= fixedStep) {
            //1. Integrate forces → update velocity
            //2. Detect collisions
            //3. Build contact constraints
            //4. Solve velocity constraints (impulses)
            //5. Solve position constraints
            //6. Update positions

            //RESOLVING ALL FORCES
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    Vector2 currentVelocity = dynObj.getLinearVelocity();
                    Vector2 newVelocity = currentVelocity.add(new Vector2(GRAVITY).scl(fixedStep));
                    dynObj.setLinearVelocity(newVelocity);
                    forces.add(new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass())));
                }
            }


            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {
                boolean anyCollision = false;
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(iteration == 0) {
                            if(((obj1 instanceof TriggerObject || obj1 instanceof DynamicTriggerObject) && obj2 instanceof DynamicObject)
                                || ((obj2 instanceof TriggerObject || obj2 instanceof DynamicTriggerObject) && obj1 instanceof DynamicObject)) {
                                //check if a body is triggering a field
                                ContactManifold manifold = CustomContactHandler.detect(obj1, obj2);
                                if(manifold.isColliding()) {
                                    if (obj1 instanceof TriggerObject) {
                                        ((TriggerObject) obj1).addTriggered(obj2.getId());
                                    } else if(obj2 instanceof TriggerObject) {
                                        ((TriggerObject) obj2).addTriggered(obj1.getId());
                                    }
                                    //dynamic trigger objects can also trigger other trigger objects so are independent
                                    if(obj1 instanceof DynamicTriggerObject) {
                                        ((DynamicTriggerObject) obj1).addTriggered(obj2.getId());
                                    }
                                    if(obj2 instanceof DynamicTriggerObject) {
                                        ((DynamicTriggerObject) obj2).addTriggered(obj1.getId());
                                    }
                                }
                            }
                        }
                        if(resolveCollision(obj1, obj2, true, forces, iteration, true)) {
                            anyCollision = true;
                        }
                    }
                }
                if(!anyCollision) {
                    break;
                }
            }
            // move the objects
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    dynObj.updatePosition(fixedStep);
                }
                if(obj instanceof FollowingTriggerObject) {
                    ((FollowingTriggerObject) obj).updatePosition();
                }
            }

            //correct the positions to prevent sinking due to numerical errors
            boolean anyCorrection = false;
            for(int iteration = 0; iteration < NUM_POS_ITERATIONS; iteration++) {
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if((resolvePenetrationCorrection(obj1, obj2))) {
                            anyCorrection = true;
                        }
                    }
                }
                if(!anyCorrection) {
                    //Gdx.app.log("PhysicsResolver", "Collision iterations ended");
                    break;
                }
            }

            Main.accumulator -= fixedStep;
        }
        return forces;
    }

    public static boolean resolveCollision(PhysicsObject obj1, PhysicsObject obj2, boolean isDebug, ArrayList<DebugForce> debugForces, int iteration, boolean isRun) {
        if ((!((obj1 instanceof StaticObject) && obj2 instanceof StaticObject)) && !(obj1 instanceof TriggerObject || obj2 instanceof TriggerObject)) {

            ContactManifold manifold = CustomContactHandler.detect(obj1, obj2);
            if (manifold.isColliding() && manifold.getPointCount() > 0) {
                Vector2 n = manifold.getNormal().nor();
                // Solve impulse for each contact point
                for (ContactPoint contact : manifold.getPoints()) {

                    Vector2 impulseN = resolveNormalMotion(obj1, obj2, contact, n, contact.penetration, debugForces, iteration, isRun, isDebug);
                    if(impulseN == null) {
                        continue; // No impulse applied, skip friction
                    }
                    resolveFrictionMotion(obj1, obj2, contact, n, debugForces, iteration, isRun, isDebug, impulseN.len());
                    // Recompute relative velocity after

                }
                return true;
            }
        }
        return false;
    }

    public static boolean resolvePenetrationCorrection(PhysicsObject obj1, PhysicsObject obj2) {
        if((obj1 instanceof StaticObject && obj2 instanceof StaticObject) || (obj1 instanceof TriggerObject || obj2 instanceof TriggerObject)) {
            return false;
        }

        ContactManifold contact = CustomContactHandler.detect(obj1, obj2);
        if (!contact.isColliding()) {
            return false;
        }

        if(contact.getPenetration() < 0.001f) {
            return false; // Ignore very small penetrations to prevent jitter
        }

        Vector2 n = contact.getNormal();
        float penetrationDepth = contact.getPenetration();
        float slop = 0.01f;
        float percent = 0.2f;
        float correction = Math.max(penetrationDepth - slop, 0f) * percent;

        float invMassA = (obj1 instanceof DynamicObject) ? 1f / ((DynamicObject) obj1).getMass() : 0f;
        float invMassB = (obj2 instanceof DynamicObject) ? 1f / ((DynamicObject) obj2).getMass() : 0f;
        float totalInvMass = invMassA + invMassB;

        if (totalInvMass <= 0f) {
            return false;
        }

        if (obj1 instanceof DynamicObject) {
            DynamicObject dynObj1 = (DynamicObject) obj1;
            Vector2 newPositionA = new Vector2(dynObj1.getPosition()).sub(new Vector2(n).scl(correction * invMassA / totalInvMass));
            dynObj1.setPosition(newPositionA);
        }

        if (obj2 instanceof DynamicObject) {
            DynamicObject dynObj2 = (DynamicObject) obj2;
            Vector2 newPositionB = new Vector2(dynObj2.getPosition()).add(new Vector2(n).scl(correction * invMassB / totalInvMass));
            dynObj2.setPosition(newPositionB);
        }

        return true;
    }

    public static DebugForce applyImpulse(PhysicsObject obj, Vector2 impulse, Vector2 contactPoint, Color  color, boolean isRun, boolean isDebug) {
        if (obj instanceof DynamicObject) {
            if(isRun) {
                DynamicObject dynObj = (DynamicObject) obj;
                Vector2 r = new Vector2(contactPoint).sub(dynObj.getCenter());
                Vector2 deltaLinearVel = new Vector2(impulse).scl(1f / dynObj.getMass());
                float deltaAngularVel = r.crs(impulse) / dynObj.getInertia();

                Vector2 newLinearVel = new Vector2(dynObj.getLinearVelocity()).add(deltaLinearVel);
                float newAngularVel = dynObj.getAngularVelocity() + deltaAngularVel;

                dynObj.setLinearVelocity(newLinearVel);
                dynObj.setAngularVelocity(newAngularVel);
            }
            if(isDebug) {
                DebugForce impulseForce = new DebugForce(contactPoint, impulse);
                impulseForce.setColor(color);
                return impulseForce;
            }
        }
        return null;
    }

    public static Vector2 resolveNormalMotion(PhysicsObject obj1, PhysicsObject obj2, ContactPoint contact, Vector2 n, float penetration, List<DebugForce> debugForces, int iteration, boolean isRun, boolean isDebug) {
        Vector2 cp = contact.point;
        float restitution = Math.max(obj1.getRestitution(), obj2.getRestitution());
        Vector2 rA = new Vector2(cp).sub(obj1.getCenter());
        Vector2 rB = new Vector2(cp).sub(obj2.getCenter());

        Vector2 vA = getContactVelocity(obj1, rA, obj1.getLinearVelocity(), obj1.getAngularVelocity());
        Vector2 vB = getContactVelocity(obj2, rB, obj2.getLinearVelocity(), obj2.getAngularVelocity());
        Vector2 relativeVel = new Vector2(vB).sub(vA);

        // Check if moving apart
        float velN = relativeVel.dot(n);
        if (velN > 0f || contact.penetration < 0.01f) {
            return null;
        }

        float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
        float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
        float invInertiaA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getInertia();
        float invInertiaB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getInertia();

        // Compute normal impulse
        float rACrossN = n.crs(rA);
        float rBCrossN = n.crs(rB);
        float kN = invMassA + invMassB + Math.abs((2*n.y*n.x*rA.y*rA.x - n.x*n.x*rA.y*rA.y  - n.y*n.y*rA.x*rA.x)*invInertiaA + (2*n.y*n.x*rB.y*rB.x - n.x*n.x*rB.y*rB.y  - n.y*n.y*rB.x*rB.x)*invInertiaB);
        if (kN <= 0f) return null;
        float slop = 0.01f;
        float beta = 0.1f;
        float vBias = beta /  fixedStep * Math.max(0, penetration - slop);

        float jn = (-(1f + restitution) * velN + vBias) / kN;



        // Distribute impulse over contact count for stability

        Vector2 impulseN = new Vector2(n).scl(jn);

        Vector2 oldRelativeVel = new Vector2(relativeVel);

        if(obj1.getId() == 0 || obj2.getId() == 0) {
            int a = 0; // is ball
        }

        // Apply impulse to obj1
        DebugForce df = applyImpulse(obj1, new Vector2(impulseN).scl(-1f), cp, new Color(1f, 0f, 0f, 1f / (iteration + 1)), isRun, isDebug);
        if(df != null) debugForces.add(df);
        df = applyImpulse(obj2, new Vector2(impulseN), cp, new Color(1f, 0f, 0f, 1f / (iteration + 1)), isRun, isDebug);
        if(df != null) debugForces.add(df);

        //do check of general equation to see if the impulse value fits
        vA = getContactVelocity(obj1, rA, obj1.getLinearVelocity(), obj1.getAngularVelocity());
        vB = getContactVelocity(obj2, rB, obj2.getLinearVelocity(), obj2.getAngularVelocity());
        relativeVel = new Vector2(vB).sub(vA);

        if(relativeVel.dot(n) > -restitution*(oldRelativeVel.dot(n)) + 0.01f || relativeVel.dot(n) < -restitution*(oldRelativeVel.dot(n)) - 0.01f)
            return null;

        return impulseN;
    }

    public static void resolveFrictionMotion(PhysicsObject obj1, PhysicsObject obj2, ContactPoint contact, Vector2 n, List<DebugForce> debugForces, int iteration, boolean isRun, boolean isDebug, float jn) {
        Vector2 cp = contact.point;
        Vector2 rA = new Vector2(cp).sub(obj1.getCenter());
        Vector2 rB = new Vector2(cp).sub(obj2.getCenter());

        // Recompute current velocities at contact point
        Vector2 vA = getContactVelocity(obj1, rA, obj1.getLinearVelocity(), obj1.getAngularVelocity());
        Vector2 vB = getContactVelocity(obj2, rB, obj2.getLinearVelocity(), obj2.getAngularVelocity());
        Vector2 relativeVel = new Vector2(vB).sub(vA);

        float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
        float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
        float invInertiaA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getInertia();
        float invInertiaB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getInertia();


        // resolve friction
        Vector2 tangent = new Vector2(relativeVel).sub(new Vector2(n).scl(relativeVel.dot(n)));
        if (tangent.len2() > 1e-8f) tangent = tangent.nor();
        else tangent.setZero();

        if (!tangent.isZero(1e-6f)) {
            float effectiveTangentialMass = invMassA + invMassB + Math.abs((2*rA.x*rA.y*tangent.y*tangent.x - rA.y*rA.y*tangent.x* tangent.x - rA.x * rA.x*tangent.y * tangent.y) * invInertiaA +
                (2*rB.x*rB.y*tangent.y*tangent.x - rB.y*rB.y*tangent.x* tangent.x - rB.x * rB.x*tangent.y * tangent.y) * invInertiaB);
            //impulse which would bring the relative tangential velocity to zero
            float jt = -relativeVel.dot(tangent) / effectiveTangentialMass;
            //Clamping due to Coulomb's law of friction: |jt| <= μ * j
            float mu = (obj1.getFriction() + obj2.getFriction()) / 2f;
            if (Math.abs(jt) > mu * jn) {
                jt = mu * jn * Math.signum(jt);
            }
            //Gdx.app.log("Physics Resolver", "Applying friction impulse with magnitude " + jt);
            Vector2 frictionImpulse = new Vector2(tangent).scl(jt);

            DebugForce df = applyImpulse(obj1, new Vector2(frictionImpulse).scl(-1f), cp, new Color(0f, 0f, 1f, 1f / (iteration + 1)), isRun, isDebug);
            if(df != null) debugForces.add(df);
            df = applyImpulse(obj2, new Vector2(frictionImpulse), cp, new Color(0f, 0f, 1f, 1f / (iteration + 1)), isRun, isDebug);
            if(df != null) debugForces.add(df);
        }
    }

    public static Vector2 getContactVelocity(PhysicsObject obj, Vector2 r, Vector2 linearVel, float angularVel) {
        // v + w x r
        Vector2 tangentialVel = new Vector2(-r.y, r.x).scl(angularVel);
        return new Vector2(linearVel).add(tangentialVel);
    }

    public static Vector2 getCenterOfMassTriangle(Vector2 a, Vector2 b, Vector2 c) {
        return new Vector2((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f);
    }
    public static float getMassOfTriangle(Vector2 a, Vector2 b, Vector2 c, float density) {
        //Area of the triangle is A = bh/2
        //which converts to A = |(x1(y2 - y3) + x2(y3 - y1) + x3(y1 - y2)) / 2| for vertices (x1, y1), (x2, y2), (x3, y3)
        //and M = A * density
        return Math.abs((a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) / 2f) * density;
    }
    public static float getMomentOfInertiaTriangle(Vector2 a, Vector2 b, Vector2 c, float mass) {
        //I_z = (m/6) * (a^2 + b^2 + c^2) where a, b, c are the distances from the center of mass to the vertices
        float inertia = (mass *
            (a.dst2(getCenterOfMassTriangle(a, b, c)) + b.dst2(getCenterOfMassTriangle(a, b, c)) +
                c.dst2(getCenterOfMassTriangle(a, b, c)))) / 6f;
        return inertia;
    }

    public static Vector2 getCenterOfMassPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        Vector2 sum = new Vector2();
        //make a weight average of the centers of mass of triangles
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                sum.add(new Vector2(getCenterOfMassTriangle(tri.get(0), tri.get(1), tri.get(2)))
                    .scl(getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(triangles.indexOf(tri)))));
            }
        }
        return sum.scl(1f / triangles.size());
    }

    public static Vector2 getCenterOfMassPolygon(List<List<Vector2>> triangles) {
        Vector2 sum = new Vector2();
        //make a weight average of the centers of mass of triangles
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                sum.add(new Vector2(getCenterOfMassTriangle(tri.get(0), tri.get(1), tri.get(2)))
                    .scl(getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), 1f)));
            }
        }
        return sum.scl(1f / triangles.size());
    }

    public static float getMassOfPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        float area = 0f;
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                area +=getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(triangles.indexOf(tri)));
            }
        }
        return area;
    }

    public static float getMassOfPolygon(List<List<Vector2>> triangles, float density) {
        float area = 0f;
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                area +=getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), density);
            }
        }
        return area;
    }

    public static float getMomentOfInertiaPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        float inertia = 0f;
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                //use parallel axis theorem to calculate the total moment of inertia:
                //I_total = I_triangle + m_triangle * d^2
                inertia += getMomentOfInertiaTriangle(tri.get(0), tri.get(1), tri.get(2),
                    getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(triangles.indexOf(tri))));
            }
        }
        return inertia;
    }

    public static float getMomentOfInertiaPolygon(List<List<Vector2>> triangles, float density) {
        float inertia = 0f;
        for (List<Vector2> tri : triangles) {
            if (tri.size() == 3) {
                //use parallel axis theorem to calculate the total moment of inertia:
                //I_total = I_triangle + m_triangle * d^2
                inertia += getMomentOfInertiaTriangle(tri.get(0), tri.get(1), tri.get(2),
                    getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), density));
            }
        }
        return inertia;
    }

    public static List<Vector2> getCircleVertices(int numSegments, float radius) {
        List<Vector2> vertices = new ArrayList<>();
        for (int i = 0; i < numSegments; i++) {
            float angle = (float) (2 * Math.PI * i / numSegments);
            vertices.add(new Vector2(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle)));
        }
        return vertices;
    }
}
