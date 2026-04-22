package io.github.physics_game.object_types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class AntigravityObject extends DynamicObject {
    public AntigravityObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation);
        setColor(Color.CORAL);
    }

    public AntigravityObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, float mass, float inertia, Vector2 com, List<Vector2> pointSegments, List<Float> massSegments) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation, mass, inertia);
        setColor(Color.CORAL);
    }

    public AntigravityObject(int id, float friction, float restitution, float density, List<Vector2> vertices, float startX, float startY, float rotation, float mass, float inertia,
                             Vector2 com, List<Vector2> pointSegments, List<Float> massSegments, List<List<Vector2>> trianglesObj, List<List<Vector2>> concaveLocalBest) {
        super(id, friction, restitution, density, vertices, startX, startY, rotation, mass, inertia, com, pointSegments, massSegments, trianglesObj, concaveLocalBest);
        setColor(Color.CORAL);
    }
}
