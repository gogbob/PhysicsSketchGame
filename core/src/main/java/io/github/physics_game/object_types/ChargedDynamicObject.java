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
        setColor((chargeDensity >= 0) ? new Color(Math.min(chargeDensity/20f, 0.5f) + 0.5f, 0.5f, 0.5f, 1) : new Color(0.5f, 0.5f, Math.min(-chargeDensity/20f, 0.5f), 1));
    }

    @Override
    public float getChargeDensity() {
        return chargeDensity;
    }

    @Override
    public void setChargeDensity(float chargeDensity) {
        this.chargeDensity = chargeDensity;
    }

    @Override
    public Vector2 findChargeForce(PhysicsObject charged) {
        //the force should go from A to B
        Vector2 totalForce = new Vector2();
        if(charged instanceof Charged) {
            //find all triangles and treat them as point charges, then sum the forces
            for(List<Vector2> triA : this.getConcaveLocalTriangles()) {
                float chargeA = this.chargeDensity * PhysicsResolver.getMassOfTriangle(triA.get(0), triA.get(1), triA.get(2), this.getDensity());
                for(List<Vector2> triB : charged.getConcaveLocalTriangles()) {
                    float chargeB = (charged instanceof DynamicObject)
                        ?((Charged) charged).getChargeDensity() * PhysicsResolver.getMassOfTriangle(triB.get(0), triB.get(1), triB.get(2), ((DynamicObject) charged).getDensity())
                    : PhysicsResolver.getMassOfTriangle(triB.get(0), triB.get(1), triB.get(2), 1f);
                    Vector2 centerA = new Vector2(PhysicsResolver.getCenterOfMassTriangle(triA.get(0), triA.get(1), triA.get(2))).add(getPosition());
                    Vector2 centerB = new Vector2(PhysicsResolver.getCenterOfMassTriangle(triB.get(0), triB.get(1), triB.get(2))).add(charged.getPosition());
                    Vector2 rNormal = new Vector2(centerB).sub(centerA).nor();
                    float r = new Vector2(centerB).sub(centerA).len();
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
