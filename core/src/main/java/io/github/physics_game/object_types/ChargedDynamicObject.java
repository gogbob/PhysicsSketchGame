package io.github.physics_game.object_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.DebugForce;
import io.github.physics_game.PhysicsResolver;
import io.github.physics_game.collision.CustomContactHandler;

import java.util.ArrayList;
import java.util.List;

public class ChargedDynamicObject extends DynamicObject implements Charged {
    private float chargeDensity;

    public ChargedDynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, float chargeDensity) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
        this.chargeDensity = chargeDensity;
        setColor((chargeDensity * getDensity() >= 0) ? new Color(1f, 1f - Math.min(chargeDensity * getDensity()/5f, 0.7f), 1f - Math.min(chargeDensity * getDensity()/5f, 0.7f), 1) :
            new Color(1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f, 1));
    }

    public ChargedDynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation,
                               float mass, float inertia, Vector2 com, List<Vector2> pointSegments, List<Float> massSegments,  float chargeDensity) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation, mass, inertia,com, pointSegments, massSegments);
        this.chargeDensity = chargeDensity;
        setColor((chargeDensity * getDensity() >= 0) ? new Color(1f, 1f - Math.min(chargeDensity * getDensity()/5f, 0.7f), 1f - Math.min(chargeDensity * getDensity()/5f, 0.7f), 1) :
            new Color(1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f, 1));
    }

    @Override
    public float getChargeDensity() {
        return chargeDensity;
    }

    @Override
    public void setChargeDensity(float chargeDensity) {
        this.chargeDensity = chargeDensity;
        setColor((chargeDensity * getDensity() >= 0) ? new Color(1f, 1f - Math.min(chargeDensity * getDensity()/3f, 0.7f), 1f - Math.min(chargeDensity * getDensity()/3f, 0.7f), 1) :
            new Color(1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f - Math.min(-chargeDensity * getDensity()/3f, 0.7f), 1f, 1));
    }

    @Override
    public List<DebugForce> applyChargeForcePair(PhysicsObject charged, boolean isRun) {
        ArrayList<DebugForce> list = new ArrayList<>();
        //the force should go from A to B
        if(charged instanceof Charged) {
            //find all triangles and treat them as point charges, then sum the forces
            for(int pointA = 0; pointA < this.getMassSegments().size(); pointA++) {
                float massPA = this.getMassSegments().get(pointA);
                Vector2 pCenterA = this.getWorldPointSegment(pointA);
                float chargeA = this.chargeDensity * massPA;
                for(int pointB = 0; pointB < charged.getMassSegments().size(); pointB++) {
                    float massPB = charged.getMassSegments().get(pointB);
                    Vector2 pCenterB = charged.getWorldPointSegment(pointB);
                    float chargeB = ((Charged) charged).getChargeDensity() * massPB;
                    Vector2 rNormal = new Vector2(pCenterB).sub(pCenterA).nor();
                    float r = new Vector2(pCenterB).sub(pCenterA).len();
                    Vector2 force = new Vector2(rNormal).scl(chargeA * chargeB/(r*r));
                    if(isRun) {
                        list.add(this.applyForce(new Vector2(force).scl(-1f), pCenterA, Color.CYAN, true, true));
                        if(charged instanceof DynamicObject) list.add(((DynamicObject)charged).applyForce(force, pCenterB, Color.CYAN, true, true));
                    } else {
                        list.add(new DebugForce(pCenterA, new Vector2(force).scl(-1f), Color.CYAN));
                        if(charged instanceof DynamicObject) list.add(new DebugForce(pCenterB, force, Color.CYAN));
                    }

                }
            }
        }
        return list;
    }
}
