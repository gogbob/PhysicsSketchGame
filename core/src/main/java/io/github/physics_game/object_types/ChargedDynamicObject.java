package io.github.physics_game.object_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.github.physics_game.PhysicsResolver;

import java.util.List;

public class ChargedDynamicObject extends DynamicObject implements Charged {
    private float chargeDensity;

    public ChargedDynamicObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, float chargeDensity) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
        this.chargeDensity = chargeDensity;
        setColor((chargeDensity >= 0) ? new Color(Math.min(chargeDensity/20f, 0.5f) + 0.5f, 0.5f, 0.5f, 1) :
            new Color(0.5f, 0.5f, Math.min(-chargeDensity/20f, 0.5f), 1));
    }

    @Override
    public float getChargeDensity() {
        return chargeDensity;
    }

    @Override
    public void setChargeDensity(float chargeDensity) {
        this.chargeDensity = chargeDensity;
        setColor((chargeDensity >= 0) ? new Color(Math.min(chargeDensity/20f, 0.5f) + 0.5f, 0.5f, 0.5f, 1) :
            new Color(0.5f, 0.5f, Math.min(-chargeDensity/20f, 0.5f), 1));
    }

    @Override
    public Vector2 findChargeForce(PhysicsObject charged) {
        //the force should go from A to B
        Vector2 totalForce = new Vector2();
        if(charged instanceof Charged) {
            //find all triangles and treat them as point charges, then sum the forces
            for(int pointA = 0; pointA < this.getMassSegments().size(); pointA++) {
                float massPA = this.getMassSegment(pointA);
                Vector2 pCenterA = this.getPointSegment(pointA);
                float chargeA = this.chargeDensity * massPA;
                for(int pointB = 0; pointB < charged.getMassSegments().size(); pointB++) {
                    float massPB = charged.getMassSegment(pointB);
                    Vector2 pCenterB = charged.getPointSegment(pointB);
                    float chargeB = this.chargeDensity * massPB;
                    Vector2 rNormal = new Vector2(pCenterB).sub(pCenterA).nor();
                    float r = new Vector2(pCenterB).sub(pCenterA).len();
                    Vector2 force = new Vector2(rNormal).scl(chargeA * chargeB/(r*r));
                    totalForce.add(force);
                }
            }
            return totalForce;
        } else {
            return new Vector2();
        }
    }
}
