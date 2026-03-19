package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.collision.ContactResult;
import io.github.physics_game.collision.CustomContactHandler;

import java.util.ArrayList;
import java.util.List;

public class PhysicsResolver {
    final static float fixedStep = 1f / 60f;
    final static int NUM_VEL_ITERATIONS = 6;
    final static int NUM_POS_ITERATIONS = 3;
    final static Vector2 GRAVITY = new Vector2(0, -0.03f);
    public static void step(float accumulator, ArrayList<PhysicsObject> objects) {
        while(accumulator >= fixedStep) {
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
            boolean anyCollision = false;
            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {

                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(resolveCollision(obj1, obj2, false, null, iteration)) {
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
            }

            //correct the positions to prevent sinking due to numerical errors
            boolean anyCorrection = false;
            for(int iteration = 0; iteration < NUM_POS_ITERATIONS; iteration++) {
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

            accumulator -= fixedStep;
        }
    }

    public static ArrayList<DebugForce> stepWithDebug(float accumulator, ArrayList<PhysicsObject> objects) {

        ArrayList<DebugForce> forces = new ArrayList<>();

        for(PhysicsObject obj : objects) {
            if (obj instanceof DynamicObject) {
                DebugForce velForce = new DebugForce(((DynamicObject) obj).getCenter(), new Vector2(((DynamicObject) obj).getLinearVelocity()).scl(2f));
                velForce.setColor(Color.GREEN);
                forces.add(velForce);
            }
        }
        if(accumulator < fixedStep) {
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    forces.add(new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass())));
                }
            }
            return forces;
        }
        while(accumulator >= fixedStep) {
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
                }
            }


            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {
                boolean anyCollision = false;
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if(resolveCollision(obj1, obj2, true, forces, iteration)) {
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

            accumulator -= fixedStep;
        }
        return forces;
    }

    public static boolean resolveCollision(PhysicsObject obj1, PhysicsObject obj2, boolean isDebug, ArrayList<DebugForce> debugForces, int iteration) {
        if (!(obj1 instanceof StaticObject && obj2 instanceof StaticObject)) {
            ContactResult customContact = CustomContactHandler.detect(obj1.getLocalBody(), obj2.getLocalBody());
            if (customContact.isColliding()) {

                Vector2 n = customContact.getNormal();
                Vector2 cp = customContact.getContactPoint();
                float penetrationDepth = customContact.getPenetrationDepth();
                float restitution = Math.min(obj1.getRestitution(), obj2.getRestitution());
                //Gdx.app.log("Physics Resolver", "Collision detected at (" + cp.x + ", " + cp.y + ") with normal (" + n.x + ", " + n.y + ") and penetration depth " + penetrationDepth);
                Vector2 rA = new Vector2(cp).sub(obj1.getCenter());
                Vector2 rB = new Vector2(cp).sub(obj2.getCenter());

                Vector2 linearVelA = obj1.getLinearVelocity();
                float angularVelA = obj1.getAngularVelocity();

                Vector2 velAtContactA = getContactVelocity(obj1, rA, linearVelA, angularVelA);


                Vector2 linearVelB = obj2.getLinearVelocity();
                float angularVelB = obj2.getAngularVelocity();

                Vector2 velAtContactB = getContactVelocity(obj2, rB, linearVelB, angularVelB);

                Vector2 relativeVel = new Vector2(velAtContactB).sub(velAtContactA);
                if(relativeVel.dot(n) >= 0f) {
                    //Gdx.app.log("Physics Resolver", "Objects are moving apart, no collision resolution needed");
                    return false; // Objects are moving apart, no need to resolve
                }
                //according to the equation (v'rel)*n = -e(vrel * n) the equation for the impulse scalar (j) is:
                // j = -(1 + e) * (vrel * n) / (mA^-1 + mB^-1 + (rA x n)^2 * IA^-1 + (rB x n)^2 * IB^-1)
                // The inverse mass and inertia will euqual to zero for static objects, so they have "infinite" mass:
                float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
                float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
                float invInertiaA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getInertia();
                float invInertiaB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getInertia();

                float effectiveMass = invMassA + invMassB + (rA.crs(n) * rA.crs(n)) * invInertiaA + (rB.crs(n) * rB.crs(n)) * invInertiaB;
                if (effectiveMass <= 0f) {
                    //Gdx.app.log("Physics Resolver", "Effective mass is zero or negative, cannot resolve collision");
                    return false; // No collision resolution possible
                }

                float j = -(1f + restitution) * relativeVel.dot(n) / effectiveMass; //should be positive
                //Gdx.app.log("Physics Resolver", "Applying impulse with magnitude " + j);
                Vector2 impulse = new Vector2(n).scl(j);
                if(isDebug) {
                    DebugForce impulseForceA = new DebugForce(new Vector2(cp).add(new Vector2(n).scl(0.1f)), new Vector2(impulse).scl(0.1f));
                    impulseForceA.setColor(new Color(1f, 0f, 0f, 1f / (iteration + 1)));
                    debugForces.add(impulseForceA);
                    DebugForce impulseForceB = new DebugForce(new Vector2(cp).sub(new Vector2(n).scl(0.1f)), new Vector2(impulse).scl(-0.1f));
                    impulseForceB.setColor(new Color(1f, 0f, 0f, 1f / (iteration + 1)));
                    debugForces.add(impulseForceB);
                }

                // Apply impulse to obj1
                if (obj1 instanceof DynamicObject) {
                    DynamicObject dynObj1 = (DynamicObject) obj1;
                    //substract since impulse is in the same direction as the normal, which points from obj1 to obj2
                    Vector2 newLinearVelA = new Vector2(dynObj1.getLinearVelocity()).sub(new Vector2(impulse).scl(invMassA));
                    float newAngularVelA = dynObj1.getAngularVelocity() - rA.crs(impulse) * invInertiaA;
                    dynObj1.setLinearVelocity(newLinearVelA);
                    dynObj1.setAngularVelocity(newAngularVelA);
                }
                // Apply impulse to obj2
                if (obj2 instanceof DynamicObject) {
                    DynamicObject dynObj2 = (DynamicObject) obj2;
                    Vector2 newLinearVelB = new Vector2(dynObj2.getLinearVelocity()).add(new Vector2(impulse).scl(invMassB));
                    float newAngularVelB = dynObj2.getAngularVelocity() + rB.crs(impulse) * invInertiaB;
                    dynObj2.setLinearVelocity(newLinearVelB);
                    dynObj2.setAngularVelocity(newAngularVelB);
                }

                // resolve friction
                Vector2 tangent = new Vector2(relativeVel).sub(new Vector2(n).scl(relativeVel.dot(n))).nor();
                if(tangent.len2() > 1e-8f) tangent = tangent.nor();
                else tangent.setZero();

                if(!tangent.isZero(1e-6f)) {
                    float effectiveTangentialMass = invMassA + invMassB + (rA.crs(tangent) * rA.crs(tangent)) * invInertiaA + (rB.crs(tangent) * rB.crs(tangent)) * invInertiaB;
                    //impulse which would bring the relative tangential velocity to zero
                    float jt = -relativeVel.dot(tangent) / effectiveTangentialMass;
                    //Clamping due to Coulomb's law of friction: |jt| <= μ * j
                    float mu = (obj1.getFriction() + obj2.getFriction()) / 2f;
                    if (Math.abs(jt) > mu * j) {
                        jt = mu * j * Math.signum(jt);
                    }
                    //Gdx.app.log("Physics Resolver", "Applying friction impulse with magnitude " + jt);
                    Vector2 frictionImpulse = new Vector2(tangent).scl(jt);
                    if(isDebug) {
                        DebugForce frictionImpulseForceA = new DebugForce(new Vector2(cp).add(new Vector2(tangent).scl(0.1f)), new Vector2(frictionImpulse).scl(0.1f));
                        frictionImpulseForceA.setColor(new Color(0f, 0f, 1f, 1f / (iteration + 1)));
                        debugForces.add(frictionImpulseForceA);
                        DebugForce frictionImpulseForceB = new DebugForce(new Vector2(cp).sub(new Vector2(tangent).scl(0.1f)), new Vector2(frictionImpulse).scl(-0.1f));
                        frictionImpulseForceB.setColor(new Color(0f, 0f, 1f, 1f / (iteration + 1)));
                        debugForces.add(frictionImpulseForceB);
                    }
                    // Apply friction impulse to obj1
                    if (obj1 instanceof DynamicObject) {
                        DynamicObject dynObj1 = (DynamicObject) obj1;
                        Vector2 newLinearVelA = new Vector2(dynObj1.getLinearVelocity()).sub(new Vector2(frictionImpulse).scl(invMassA));
                        float newAngularVelA = dynObj1.getAngularVelocity() - rA.crs(frictionImpulse) * invInertiaA;
                        dynObj1.setLinearVelocity(newLinearVelA);
                        dynObj1.setAngularVelocity(newAngularVelA);
                    }
                    // Apply friction impulse to obj2
                    if (obj2 instanceof DynamicObject) {
                        DynamicObject dynObj2 = (DynamicObject) obj2;
                        Vector2 newLinearVelB = new Vector2(dynObj2.getLinearVelocity()).add(new Vector2(frictionImpulse).scl(invMassB));
                        float newAngularVelB = dynObj2.getAngularVelocity() + rB.crs(frictionImpulse) * invInertiaB;
                        dynObj2.setLinearVelocity(newLinearVelB);
                        dynObj2.setAngularVelocity(newAngularVelB);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean resolvePenetrationCorrection(PhysicsObject obj1, PhysicsObject obj2) {
        if(obj1 instanceof StaticObject && obj2 instanceof StaticObject) {
            return false; // No correction needed for two static objects
        }

        ContactResult contact = CustomContactHandler.detect(obj1.getLocalBody(), obj2.getLocalBody());
        if (!contact.isColliding()) {
            return false; // No penetration to correct
        }

        //Gdx.app.log("Physics Resolver", "Resolving penetration");
        Vector2 n = contact.getNormal();
        Vector2 cp = contact.getContactPoint();
        float penetrationDepth = contact.getPenetrationDepth();
        float slop = 0.01f; // Small slop to prevent jitter
        float percent = 0.8f; // Percentage of penetration to correct
        float correction = Math.max(penetrationDepth - slop, 0f) * percent;

        float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
        float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
        float totalInvMass = invMassA + invMassB;
        if (totalInvMass <= 0f) {
            return false; // No correction possible
        }

        if (obj1 instanceof DynamicObject) {
            DynamicObject dynObj1 = (DynamicObject) obj1;
            Vector2 newPositionA = new Vector2(dynObj1.getPosition()).sub(new Vector2(n).scl(correction*invMassA/totalInvMass));
            dynObj1.setPosition(newPositionA);
        }

        if (obj2 instanceof DynamicObject) {
            DynamicObject dynObj2 = (DynamicObject) obj2;
            Vector2 newPositionB = new Vector2(dynObj2.getPosition()).add(new Vector2(n).scl(correction*invMassB/totalInvMass));
            dynObj2.setPosition(newPositionB);
        }

        return true;
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
}
