package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.collision.ContactResult;
import io.github.physics_game.collision.CustomContactHandler;

import java.util.ArrayList;

public class PhysicsResolver {
    final static float fixedStep = 1f / 60f;
    final static int NUM_ITERATIONS = 6;
    final static Vector2 GRAVITY = new Vector2(0, -0.01f);
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

            ArrayList<ContactResult> customContacts = new ArrayList<>();

            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {
                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if (!(obj1 instanceof StaticObject && obj2 instanceof StaticObject)) {
                            ContactResult customContact = CustomContactHandler.detect(obj1.getLocalBody(), obj2.getLocalBody());
                            customContacts.add(customContact);
                            if (customContact.isColliding()) {
                                Vector2 n = customContact.getNormal();
                                Vector2 cp = customContact.getContactPoint();
                                float penetrationDepth = customContact.getPenetrationDepth();
                                float restitution = Math.min(obj1.getRestitution(), obj2.getRestitution());
                                //Gdx.app.log("CustomContact", String.format("contact p=(%.3f, %.3f) depth=%.4f normal=(%.3f, %.3f)",
                                //   cp.x, cp.y, customContact.getPenetrationDepth(), n.x, n.y));
                            }
                        }
                    }
                }
            }

            // move the objects
            for (PhysicsObject obj : objects) {
                if (obj instanceof DynamicObject) {
                    DynamicObject dynObj = (DynamicObject) obj;
                    dynObj.updatePosition(fixedStep);
                }
            }

            accumulator -= fixedStep;
        }
    }

    public static ArrayList<DebugForce> stepWithDebug(float accumulator, ArrayList<PhysicsObject> objects, boolean runPhysics) {

        ArrayList<DebugForce> forces = new ArrayList<>();

        for(PhysicsObject obj : objects) {
            if (obj instanceof DynamicObject) {
                DebugForce velForce = new DebugForce(((DynamicObject) obj).getCenter(), new Vector2(((DynamicObject) obj).getLinearVelocity()).scl(2f));
                velForce.setColor(Color.GREEN);
                forces.add(velForce);
            }
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
                    //apply gravity
                    if (runPhysics) {
                        Vector2 currentVelocity = dynObj.getLinearVelocity();
                        Vector2 newVelocity = currentVelocity.add(new Vector2(GRAVITY).scl(fixedStep));
                        dynObj.setLinearVelocity(newVelocity);
                    }
                    if(accumulator - fixedStep <= fixedStep) {
                        forces.add(new DebugForce(dynObj.getCenter(), new Vector2(GRAVITY).scl(dynObj.getMass())));
                    }
                }
            }

            ArrayList<ContactResult> customContacts = new ArrayList<>();

            //RESOLVE COLLISIONS
            for(int iteration = 0; iteration < NUM_ITERATIONS; iteration++) {

                for (int i = 0; i < objects.size(); i++) {
                    for (int j = i + 1; j < objects.size(); j++) {
                        PhysicsObject obj1 = objects.get(i);
                        PhysicsObject obj2 = objects.get(j);
                        if (!(obj1 instanceof StaticObject && obj2 instanceof StaticObject)) {
                            ContactResult customContact = CustomContactHandler.detect(obj1.getLocalBody(), obj2.getLocalBody());
                            customContacts.add(customContact);
                            if (customContact.isColliding()) {

                                Vector2 n = customContact.getNormal();
                                Vector2 cp = customContact.getContactPoint();
                                float penetrationDepth = customContact.getPenetrationDepth();
                                float restitution = Math.min(obj1.getRestitution(), obj2.getRestitution());
                                //Gdx.app.log("CustomContact", String.format("contact p=(%.3f, %.3f) depth=%.4f normal=(%.3f, %.3f)",
                                //    cp.x, cp.y, customContact.getPenetrationDepth(), n.x, n.y));
                            }
                        }
                    }
                }
            }
            if(runPhysics) {
                for (PhysicsObject obj : objects) {
                    if (obj instanceof DynamicObject) {
                        DynamicObject dynObj = (DynamicObject) obj;
                        dynObj.updatePosition(fixedStep);
                    }
                }
            }

            accumulator -= fixedStep;
        }
        return forces;
    }


}
