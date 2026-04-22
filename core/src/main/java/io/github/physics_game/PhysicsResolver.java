package io.github.physics_game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.collision.ContactManifold;
import io.github.physics_game.collision.ContactPoint;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhysicsResolver {
    final static float fixedStep = 1f / 60f; // fixed time step
    final static int NUM_VEL_ITERATIONS = 15; //velocity constraints will iterate 5 times
    final static int NUM_POS_ITERATIONS = 15; // position correction (twice)
    final static Vector2 GRAVITY = new Vector2(0, -9.8f); // gravity vector

    // show force and collision (I think it's more like visualization)
    public static ArrayList<DebugForce> stepWithDebug(ArrayList<PhysicsObject> objects) {
        ArrayList<DebugForce> forces = new ArrayList<>(); // empty force arrayList

        for(PhysicsObject obj : objects) {
            if (obj instanceof DynamicObject) {
                DebugForce velForce = new DebugForce(((DynamicObject) obj).getCenter(),
                    new Vector2(((DynamicObject) obj).getLinearVelocity()));
                velForce.setColor(Color.GREEN);
                velForce.setType(DebugForce.Type.VELOCITY);
                forces.add(velForce);
            }
        }

        // create arrayList for collision
        ArrayList<ContactManifold> collisions = new ArrayList<>();
        // collision detection
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                PhysicsObject obj1 = objects.get(i);
                PhysicsObject obj2 = objects.get(j);
                if (!broadphaseOverlap(obj1, obj2)) continue; // AABB broadphase
                ContactManifold manifold = CustomContactHandler.detect(obj1, obj2); // check if they collide
                if(manifold.isColliding() && manifold.getPointCount() > 0) { // if yes and have valid contact point = add into collision list
                    collisions.add(manifold); // if time accumulated is not enought for physics step then go here
                }
            }
        }

        if(GameScreen.accumulator < fixedStep) {
            for (int i = 0; i < objects.size(); i++) {
                PhysicsObject obj = objects.get(i);
                if (obj instanceof DynamicObject) {
                    if(obj instanceof AntigravityObject){
                        DynamicObject dynObj = (DynamicObject) obj;
                        DebugForce gf = new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass()).scl(0.1f));
                        gf.setColor(new Color(1f, 0.55f, 0f, 1f));
                        gf.setType(DebugForce.Type.GRAVITY);
                        forces.add(gf);
                    } else {
                        DynamicObject dynObj = (DynamicObject) obj;
                        DebugForce gf = new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass()));
                        gf.setColor(new Color(1f, 0.55f, 0f, 1f));
                        gf.setType(DebugForce.Type.GRAVITY);
                        forces.add(gf);
                    }
                }

                for(int j = i + 1; j < objects.size(); j++) {
                    PhysicsObject other = objects.get(j);
                    // both charged + 1 is dynamic
                    if((obj instanceof Charged && other instanceof Charged) && (obj instanceof DynamicObject || other instanceof DynamicObject)) {
                        //This one only show the vector, DOES NOT change objects' state
                        List<DebugForce> debugCharges = ((Charged) obj).applyChargeForcePair(other, false);
                        forces.addAll(debugCharges);
                    }
                }
            }

            collisions = new ArrayList<>();

            for (int i = 0; i < objects.size(); i++) {
                for (int j = i + 1; j < objects.size(); j++) {
                    PhysicsObject obj1 = objects.get(i);
                    PhysicsObject obj2 = objects.get(j);
                    if (!broadphaseOverlap(obj1, obj2)) continue; // AABB broadphase
                    ContactManifold manifold = CustomContactHandler.detect(obj1, obj2);
                    if(manifold.isColliding() && manifold.getPointCount() > 0) {
                        collisions.add(manifold);
                    } // if collision = valid, then save the manifold in
                }
            }

            triggerAdditionalLogic(collisions);

            // velocity iteration
            for(int iteration = 0; iteration < NUM_VEL_ITERATIONS; iteration++) {
                for (ContactManifold coll : collisions) {
                    resolveCollision(coll, true, forces, 0, false);
                }
            }
        }

        // accumulated time enough = physic update
        while(GameScreen.accumulator >= fixedStep) {

            //RESOLVING ALL FORCES
            for (int i = 0; i < objects.size(); i++) {
                PhysicsObject obj = objects.get(i);
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    //apply gravity
                    if(obj instanceof AntigravityObject){
                        Vector2 currentVelocity = new Vector2(dynObj.getLinearVelocity());
                        Vector2 newVelocity = currentVelocity.add(new Vector2(GRAVITY).scl(0.1f).scl(fixedStep));
                        dynObj.setLinearVelocity(newVelocity);
                        DebugForce gf = new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(0.1f).scl(dynObj.getMass()));
                        gf.setColor(new Color(1f, 0.55f, 0f, 1f));
                        gf.setType(DebugForce.Type.GRAVITY);
                        forces.add(gf);
                    } else {
                        Vector2 currentVelocity = new Vector2(dynObj.getLinearVelocity());
                        Vector2 newVelocity = currentVelocity.add(new Vector2(GRAVITY).scl(fixedStep));
                        dynObj.setLinearVelocity(newVelocity);
                        DebugForce gf = new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass()));
                        gf.setColor(new Color(1f, 0.55f, 0f, 1f));
                        gf.setType(DebugForce.Type.GRAVITY);
                        forces.add(gf);
                    }
                }
                // apply charged force
                for(int j = i + 1; j < objects.size(); j++) {
                    PhysicsObject other = objects.get(j);
                    if((obj instanceof Charged && other instanceof Charged) && (obj instanceof DynamicObject || other instanceof DynamicObject)) {
                        // add charged force + return debug charge
                        List<DebugForce> debugCharges = ((Charged) obj).applyChargeForcePair(other, true);
                        forces.addAll(debugCharges);
                    }
                }
            }

            // recheck collision
            collisions = new ArrayList<>();

            for (int i = 0; i < objects.size(); i++) {
                for (int j = i + 1; j < objects.size(); j++) {
                    PhysicsObject obj1 = objects.get(i);
                    PhysicsObject obj2 = objects.get(j);
                    if (!broadphaseOverlap(obj1, obj2)) continue; // AABB broadphase
                    ContactManifold manifold = CustomContactHandler.detect(obj1, obj2);
                    if(manifold.isColliding() && manifold.getPointCount() > 0) {
                        collisions.add(manifold);
                    } // if collision = valid, then save the manifold in
                }
            }

            triggerAdditionalLogic(collisions);

            //RESOLVE COLLISIONS
            for(int i = 0; i < NUM_VEL_ITERATIONS; i++) {
                for (ContactManifold coll : collisions) {
                    resolveCollision(coll, true, forces, i, true);
                } // Really DO the resolve of collision
            }
            // update position
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    dynObj.updatePosition(fixedStep);
                }
                if(obj instanceof Following) {
                    ((Following) obj).updatePosition(); // if object realize following, call its own updatePosition
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
                            anyCorrection = true; // if it sinks, push them apart a bit
                        }
                    }
                }
                if(!anyCorrection) {
                    //nothing happened = done
                    break;
                }
            }
            // take off a fixed step time
            GameScreen.accumulator -= fixedStep;
        }
        return forces;
    }

    // how objects going to react after collision
    public static boolean resolveCollision(ContactManifold manifold, boolean isDebug, ArrayList<DebugForce> debugForces, int iteration, boolean isRun) {
        PhysicsObject obj1 = manifold.getA();
        PhysicsObject obj2 = manifold.getB();
        if ((!((obj1 instanceof StaticObject) && obj2 instanceof StaticObject)) && //if both = statics = no need for collision resover
            !(obj1 instanceof UncollidableObject || obj2 instanceof UncollidableObject)) { // if one of them is uncollidable = no collision resover
            Vector2 n = manifold.getNormal().nor(); // unit normal vector(?
            // Solve impulse for each contact point
            for (ContactPoint contact : manifold.getPoints()) {
                // resolve normal direction's collision
                Vector2 impulseN = resolveNormalMotion(obj1, obj2, contact, n, contact.penetration, debugForces, iteration, isRun, isDebug);
                if(impulseN == null) {
                    continue; // No impulse applied, skip friction
                }// because f<= u(the friction unit)*N, no N f is very small or none
                resolveFrictionMotion(obj1, obj2, contact, n, debugForces, iteration, isRun, isDebug, impulseN.len());
                // Recompute relative velocity after

            }
            return true;
        }
        return false;
    }

    // correct the position!
    public static boolean resolvePenetrationCorrection(PhysicsObject obj1, PhysicsObject obj2) {
        if((obj1 instanceof StaticObject && obj2 instanceof StaticObject) || (obj1 instanceof UncollidableObject || obj2 instanceof UncollidableObject)) {
            return false; // same logic as previous methods
        }
        // detect again
        ContactManifold contact = CustomContactHandler.detect(obj1, obj2);
        if (!contact.isColliding()) {
            return false;
        }

        if(contact.getPenetration() < 0.001f) {
            return false; // Ignore very small penetrations to prevent jitter
        }

        Vector2 n = contact.getNormal();
        float penetrationDepth = contact.getPenetration();
        float slop = 0.01f; //fault tolerante
        float percent = 0.2f;
        float correction = Math.max(penetrationDepth - slop, 0f) * percent;

        float invMassA = (obj1 instanceof DynamicObject) ? 1f / ((DynamicObject) obj1).getMass() : 0f;
        // if it's dynamicObject then find inverse mass = 1/m, else its 0
        float invMassB = (obj2 instanceof DynamicObject) ? 1f / ((DynamicObject) obj2).getMass() : 0f;
        float totalInvMass = invMassA + invMassB;

        if (totalInvMass <= 0f) { // <0 = no correction
            return false;
        }

        if (obj1 instanceof DynamicObject) { // if it's dynamic = change position
            DynamicObject dynObj1 = (DynamicObject) obj1;
            Vector2 newPositionA = new Vector2(dynObj1.getPosition()).sub(new Vector2(n).scl(correction * invMassA / totalInvMass));
            // shareA = correction*(invMassA/invMassA+invMassB)
            dynObj1.setPosition(newPositionA);
        }

        if (obj2 instanceof DynamicObject) {
            DynamicObject dynObj2 = (DynamicObject) obj2;
            // opposit from obj1, because A push to -n, B push to +n
            Vector2 newPositionB = new Vector2(dynObj2.getPosition()).add(new Vector2(n).scl(correction * invMassB / totalInvMass));
            dynObj2.setPosition(newPositionB);
        }

        return true;
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
        // velN = Vrel*n


        if (contact.penetration < 0.00001f) {
            return null;
        }


        float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
        float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
        float invInertiaA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getInertia();
        float invInertiaB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getInertia();

        // Effective normal mass: k_n = Σ(1/m) + Σ((r × n)² / I)
        // (r × n) is the scalar 2D cross product; squaring it keeps it non-negative.
        float rAxN = rA.x * n.y - rA.y * n.x;
        float rBxN = rB.x * n.y - rB.y * n.x;
        float kN = invMassA + invMassB + rAxN * rAxN * invInertiaA + rBxN * rBxN * invInertiaB;
        // jn = -Vn+bias)/kN
        if (kN <= 0f) return null;

        // 1st iteration set bias
        if(contact.firstIteration) {
            float slop = 0.001f;
            float beta = 0.3f;

            contact.firstIteration = false;
            contact.bias = (beta/fixedStep * Math.max(0, contact.penetration - slop));
            if(velN < -1f) {
                contact.bias += -restitution * velN;
            } // bias = beta/dt * max(0, pen - slop) - e*velN
        }
        //Calculate the magnitude of the normal impulse jn
        float jn = (-velN + contact.bias) / kN;

        //  accumulated impulse
        float temp = contact.accumulatedNormalImpulse;
        contact.accumulatedNormalImpulse = Math.max(temp + jn, 0f); // Jn>=0
        float slop = 0.01f;
        float beta = 0.1f;
        jn = contact.accumulatedNormalImpulse - temp;
        // jn = JnewTotal -JoldTotal

        // Distribute impulse over contact count for stability
        Vector2 impulseN = new Vector2(n).scl(jn);
        // Jn = jn*n

        if(obj1 instanceof DynamicObject) {
            DebugForce df = ((DynamicObject)obj1).applyForce(new Vector2(impulseN).scl(-1f), cp, new Color(1f, 0f, 0f, 1f / (iteration + 1)), isRun, isDebug);
            if(df != null) { df.setType(DebugForce.Type.IMPULSE_NORMAL); debugForces.add(df); }
        }
        if(obj2 instanceof DynamicObject) {
            DebugForce df = ((DynamicObject)obj2).applyForce(new Vector2(impulseN), cp, new Color(1f, 0f, 0f, 1f / (iteration + 1)), isRun, isDebug);
            if(df != null) { df.setType(DebugForce.Type.IMPULSE_NORMAL); debugForces.add(df); }
        }
        return impulseN;
    }

    // apply normal impulse
    //Resolve tangential (friction) impulse at the contact point
    public static void resolveFrictionMotion(PhysicsObject obj1, PhysicsObject obj2, ContactPoint contact, Vector2 n, List<DebugForce> debugForces, int iteration, boolean isRun, boolean isDebug, float jn) {
        Vector2 cp = contact.point; //get contact point
        Vector2 rA = new Vector2(cp).sub(obj1.getCenter());// Vector from center of A to contact point
        Vector2 rB = new Vector2(cp).sub(obj2.getCenter()); // same but for B

        // Recompute current velocities at contact point
        Vector2 vA = getContactVelocity(obj1, rA, obj1.getLinearVelocity(), obj1.getAngularVelocity());
        Vector2 vB = getContactVelocity(obj2, rB, obj2.getLinearVelocity(), obj2.getAngularVelocity());
        Vector2 relativeVel = new Vector2(vB).sub(vA); // Relative velocity at contact
        // calculate position vector of contact point relative to center of objects

        float invMassA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getMass();
        float invMassB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getMass();
        float invInertiaA = (obj1 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj1).getInertia();
        float invInertiaB = (obj2 instanceof StaticObject) ? 0f : 1f / ((DynamicObject) obj2).getInertia();


        // Compute tangent direction = remove normal component from relative velocity
        Vector2 tangent = new Vector2(relativeVel).sub(new Vector2(n).scl(relativeVel.dot(n)));

        // If there is significant tangential motion = normalize the tangent
        if (!tangent.epsilonEquals(Vector2.Zero, 1e-12f)) {
            tangent = tangent.nor();
            // Effective tangential mass: k_t = Σ(1/m) + Σ((r × t)² / I)
            float rAxT = rA.x * tangent.y - rA.y * tangent.x;
            float rBxT = rB.x * tangent.y - rB.y * tangent.x;
            float effectiveTangentialMass = invMassA + invMassB + rAxT * rAxT * invInertiaA + rBxT * rBxT * invInertiaB;
            //impulse which would bring the relative tangential velocity to zero
            float jt = -relativeVel.dot(tangent) / effectiveTangentialMass;

            float temp = contact.accumulatedFrictionImpulse;
            contact.accumulatedFrictionImpulse = contact.accumulatedFrictionImpulse + jt;

            //Clamping due to Coulomb's law of friction: |jt| <= μ * jn (geometric mean, Coulomb model)
            float mu = (float) Math.sqrt(obj1.getFriction() * obj2.getFriction());
            if (Math.abs(contact.accumulatedFrictionImpulse) > mu * contact.accumulatedNormalImpulse) {
                contact.accumulatedFrictionImpulse = mu * contact.accumulatedNormalImpulse * Math.signum(jt);
            }

            // Convert scalar friction impulse into vector form
            jt = contact.accumulatedFrictionImpulse - temp;
            //Gdx.app.log("Physics Resolver", "Applying friction impulse with magnitude " + jt);
            Vector2 frictionImpulse = new Vector2(tangent).scl(jt);

            // Apply friction impulse to object A (opposite direction)
            if(obj1 instanceof DynamicObject) {
                DebugForce df = ((DynamicObject)obj1).applyForce(new Vector2(frictionImpulse).scl(-1f), cp, new Color(0f, 0f, 1f, 1f / (iteration + 1)), isRun, isDebug);
                if(df != null) { df.setType(DebugForce.Type.IMPULSE_FRICTION); debugForces.add(df); }
            }
            if(obj2 instanceof DynamicObject) {
                DebugForce df = ((DynamicObject)obj2).applyForce(new Vector2(frictionImpulse), cp, new Color(0f, 0f, 1f, 1f / (iteration + 1)), isRun, isDebug);
                if(df != null) { df.setType(DebugForce.Type.IMPULSE_FRICTION); debugForces.add(df); }
            }

            vA = getContactVelocity(obj1, rA, obj1.getLinearVelocity(), obj1.getAngularVelocity());
            vB = getContactVelocity(obj2, rB, obj2.getLinearVelocity(), obj2.getAngularVelocity());
            relativeVel = new Vector2(vB).sub(vA);
            tangent = new Vector2(relativeVel).sub(new Vector2(n).scl(relativeVel.dot(n)));
        } // Recompute velocities after applying friction impulse (for next iteration)
    }

    // Handle non-physics game logic triggered by collisions
    public static void triggerAdditionalLogic(ArrayList<ContactManifold> collisions) {
        for(ContactManifold manifold : collisions) {
            PhysicsObject objA = manifold.getA();
            PhysicsObject objB = manifold.getB();

            // Ignore if both objects are uncollidable
            // If one object is triggerable, notify it of the collision with the other object's ID
            if(!(objA instanceof UncollidableObject && objB instanceof UncollidableObject)) {
                if(objA instanceof Triggerable) {
                    ((Triggerable) objA).addTriggerId(objB.getId());
                } else if(objB instanceof Triggerable) {
                    ((Triggerable) objB).addTriggerId(objA.getId());
                }
            }
        }
    }

    public static Vector2 getContactVelocity(PhysicsObject obj, Vector2 r, Vector2 linearVel, float angularVel) {
        // v + w x r
        Vector2 tangentialVel = new Vector2(-r.y, r.x).scl(angularVel);
        return new Vector2(linearVel).add(tangentialVel);
    } // Total velocity at contact point = linear velocity + angular contribution

    // Compute centroid (center of mass) of a triangle
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
        // Exact formula derived from the 2D area integral over barycentric coordinates:
        // I_z = (m/6) * (|ra|² + |rb|² + |rc|² + ra·rb + rb·rc + rc·ra)
        // where ra,rb,rc are vertex positions relative to the centroid.
        // The previous formula omitted the cross-dot terms, underestimating inertia.
        Vector2 com = getCenterOfMassTriangle(a, b, c);
        Vector2 ra = new Vector2(a).sub(com);
        Vector2 rb = new Vector2(b).sub(com);
        Vector2 rc = new Vector2(c).sub(com);
        return (mass / 6f) * (ra.dot(ra) + rb.dot(rb) + rc.dot(rc)
                            + ra.dot(rb) + rb.dot(rc) + rc.dot(ra));
    }

    // Compute center of mass of a polygon by decomposing into triangles
    public static Vector2 getCenterOfMassPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        if (triangles == null || densities == null || triangles.isEmpty()) {
            return new Vector2();
        } // Iterate through triangle decomposition of the polygon

        // Initialize weighted sum of triangle centers and total mass
        Vector2 weightedSum = new Vector2();
        float totalMass = 0f;
        // Use the smaller size to avoid index mismatch between triangles and densities
        int count = Math.min(triangles.size(), densities.size());

        for (int i = 0; i < count; i++) {
            List<Vector2> tri = triangles.get(i);
            if (tri == null || tri.size() != 3) {
                continue;
            }

            float mass = getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(i));
            if (mass <= 1e-8f) {
                continue;
            }
            // Compute center of mass of the current triangle
            Vector2 triCom = getCenterOfMassTriangle(tri.get(0), tri.get(1), tri.get(2));
            weightedSum.mulAdd(triCom, mass);
            totalMass += mass;
        }

        if (totalMass <= 1e-8f) {
            return new Vector2();
        }

        return weightedSum.scl(1f / totalMass);
    } // Final center of mass = weighted average

    // Compute the center of mass of a polygon assuming uniform density
    public static Vector2 getCenterOfMassPolygon(List<List<Vector2>> triangles) {
        if (triangles == null || triangles.isEmpty()) {
            return new Vector2();
        }

        Vector2 weightedSum = new Vector2();
        float totalMass = 0f;

        for (int i = 0; i < triangles.size(); i++) {
            List<Vector2> tri = triangles.get(i);
            if (tri == null || tri.size() != 3) {
                continue;
            }

            float mass = getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), 1f);
            if (mass <= 1e-8f) {
                continue;
            }

            Vector2 triCom = getCenterOfMassTriangle(tri.get(0), tri.get(1), tri.get(2));
            weightedSum.mulAdd(triCom, mass);
            totalMass += mass;
        }

        if (totalMass <= 1e-8f) {
            return new Vector2();
        }

        return weightedSum.scl(1f / totalMass);
    }

    // Compute total mass of a polygon from its triangle decomposition and per-triangle densities
    public static float getMassOfPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        if (triangles == null || densities == null || triangles.isEmpty()) {
            return 0f;
        }

        float mass = 0f;
        int count = Math.min(triangles.size(), densities.size());
        for (int i = 0; i < count; i++) {
            List<Vector2> tri = triangles.get(i);
            if (tri != null && tri.size() == 3) {
                mass += getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(i));
            }
        }
        return mass;
    }

    // Compute total mass of a polygon assuming uniform density
    public static float getMassOfPolygon(List<List<Vector2>> triangles, float density) {
        float area = 0f; // more like mass
        for (List<Vector2> tri : triangles) { // Sum the mass of each valid triangle
            if (tri.size() == 3) {
                area +=getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), density);
            }
        }
        return area;
    }

    // Compute total moment of inertia of a polygon using per-triangle densities
    public static float getMomentOfInertiaPolygon(List<List<Vector2>> triangles, List<Float> densities) {
        if (triangles == null || densities == null || triangles.isEmpty()) {
            return 0f;
        }

        float inertia = 0f;
        int count = Math.min(triangles.size(), densities.size());
        for (int i = 0; i < count; i++) { // Sum the moment of inertia of each valid triangle
            List<Vector2> tri = triangles.get(i);
            if (tri != null && tri.size() == 3) {
                float mass = getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), densities.get(i));
                inertia += getMomentOfInertiaTriangle(tri.get(0), tri.get(1), tri.get(2), mass);
            }
        }
        return inertia;
    }

    // Compute total moment of inertia of a polygon assuming uniform density
    public static float getMomentOfInertiaPolygon(List<List<Vector2>> triangles, float density) {
        float inertia = 0f;
        for (List<Vector2> tri : triangles) { // Sum the inertia contribution of each valid triangle
            if (tri.size() == 3) {
                inertia += getMomentOfInertiaTriangle(tri.get(0), tri.get(1), tri.get(2),
                    getMassOfTriangle(tri.get(0), tri.get(1), tri.get(2), density));
            }
        }
        return inertia;
    }

    // Returns the bounding radius of an object (max distance from center to any local vertex).
    // Used for conservative AABB broadphase: two objects can only collide if their bounding
    // circles overlap, i.e. dist(centers) <= r1 + r2.
    private static float getBoundingRadius(PhysicsObject obj) {
        float maxDistSq = 0f;
        for (Vector2 v : obj.getVertices()) {
            float dSq = v.x * v.x + v.y * v.y;
            if (dSq > maxDistSq) maxDistSq = dSq;
        }
        return (float) Math.sqrt(maxDistSq);
    }

    // Returns true if obj1 and obj2 are close enough to possibly collide (AABB broadphase).
    private static boolean broadphaseOverlap(PhysicsObject obj1, PhysicsObject obj2) {
        Vector2 c1 = obj1.getCenter();
        Vector2 c2 = obj2.getCenter();
        float combinedRadius = getBoundingRadius(obj1) + getBoundingRadius(obj2);
        float dx = c1.x - c2.x;
        float dy = c1.y - c2.y;
        return dx * dx + dy * dy <= combinedRadius * combinedRadius;
    }

    // Generate polygon vertices approximating a circle
    public static List<Vector2> getCircleVertices(int numSegments, float radius) {
        List<Vector2> vertices = new ArrayList<>();
        for (int i = 0; i < numSegments; i++) { // Create evenly spaced points around the circle
            float angle = (float) (2 * Math.PI * i / numSegments);
            vertices.add(new Vector2(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle)));
        }
        return vertices;
    }

    // Print an approximate grid representation of the shape for debugging
    public static void printShape(List<Vector2> vertices) {
        float res = 0.05f; // Grid resolution used for displaying points
        // Initialize bounding box limits
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE, minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for(Vector2 v : vertices) { // Find the bounding box of the shape
            if(v.x < minX) minX = v.x;
            if(v.x > maxX) maxX = v.x;
            if(v.y < minY) minY = v.y;
            if(v.y > maxY) maxY = v.y;
        }  // Expand bounds slightly for display padding
        minX -= 1f;
        maxX += 1f;
        minY -= 1f;
        maxY += 1f;
        // Compute approximate display width and height
        float width = Math.max(Math.abs(minX), Math.abs(maxX)) + 1f;
        float height = Math.max(Math.abs(minY), Math.abs(maxY)) + 1f;
        // create 2D gridd
        ArrayList<ArrayList<Integer>> pointGrid = new ArrayList<>();
        for(int y = (int)(maxY / res); y >= (int)(minY / res); y--) {
            ArrayList<Integer> row = new ArrayList<>(Collections.nCopies((int)((maxX - minX) / res) + 1, -1));
            pointGrid.add(row);
        }

        // mark vertex position
        for(int i = 0; i < vertices.size(); i++) {
            Vector2 v = vertices.get(i);
            int xIndex = (int)((v.x - minX) / res);
            int yIndex = (int)((v.y - minY) / res);
            // Only store points that fall inside the grid bound
            if(xIndex >= 0 && xIndex < pointGrid.get(0).size() && yIndex >= 0 && yIndex < pointGrid.size()) {
                pointGrid.get(yIndex).set(xIndex, i);
            }
        }

        // Print the grid row by row
        for(int y = pointGrid.size() - 1; y >= 0; y--) {
            StringBuilder sb = new StringBuilder();
            for(int x = 0; x < pointGrid.get(0).size(); x++) {
                if(pointGrid.get(y).get(x) >= 0) {
                    sb.append((pointGrid.get(y).get(x) % 1000));
                } else {
                    sb.append("   ");
                }
            }
            System.out.println(sb);
        }
    }

    // Print an approximate grid representation of multiple shapes for debugging
    public static void printListShape(List<List<Vector2>> verticesList) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE, minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        // Find the global bounding box containing all vertex lists
        for(List<Vector2> vertices : verticesList) {
            for(Vector2 v : vertices) {
                if(v.x < minX) minX = v.x;
                if(v.x > maxX) maxX = v.x;
                if(v.y < minY) minY = v.y;
                if(v.y > maxY) maxY = v.y;
            }
        }// Expand bounds slightly to add display padding
        minX -= 1f;
        maxX += 1f;
        minY -= 1f;
        maxY += 1f;
        float res = 0.05f;
        // Compute approximate display width and height
        float width = Math.max(Math.abs(minX), Math.abs(maxX)) + 1f;
        float height = Math.max(Math.abs(minY), Math.abs(maxY)) + 1f;
        ArrayList<ArrayList<Integer>> pointGrid = new ArrayList<>();
        for(int y = (int)(maxY / res); y >= (int)(minY / res); y--) {
            ArrayList<Integer> row = new ArrayList<>(Collections.nCopies((int)((maxX - minX) / res) + 1, -1));
            pointGrid.add(row);
        }
        // Mark each vertex position from every shape in the grid
        for(List<Vector2> vertices : verticesList) {
            for(int i = 0; i < vertices.size(); i++) {
                Vector2 v = vertices.get(i);
                int xIndex = (int)((v.x - minX) / res);
                int yIndex = (int)((v.y - minY) / res);
                if(xIndex >= 0 && xIndex < pointGrid.get(0).size() && yIndex >= 0 && yIndex < pointGrid.size()) {
                    pointGrid.get(yIndex).set(xIndex, i);
                }
            }
        }

        // print  grid row by row
        for(int y = pointGrid.size() - 1; y >= 0; y--) {
            StringBuilder sb = new StringBuilder();
            for(int x = 0; x < pointGrid.get(0).size(); x++) {
                if(pointGrid.get(y).get(x) >= 0) {
                    sb.append((pointGrid.get(y).get(x) % 1000));
                } else {
                    sb.append("   ");
                }
            }
            System.out.println(sb);
        }
    }
}
