package io.github.physics_game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.physics_game.collision.ContactResult;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.collision.EarClippingDecomposer;

import java.util.Arrays;
import java.util.List;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter implements ApplicationListener {
    private SpriteBatch batch;
    FitViewport viewport;
    private Texture image;
    private Texture ballImage;
    World world;
    OrthographicCamera camera;
    Box2DDebugRenderer debugRenderer;
    float accumulator = 0f;
    final float GRAVITY = -1.8f;

    // throttle logging to once-per-second
    float logTimer = 0f;


    // Box2D body built from ear-clipped triangles of the same concave polygon.
    private ShapeRenderer shapeRenderer;


    private boolean showDebugOverlay = false;
    private ContactResult lastCustomContact = ContactResult.NO_CONTACT;
    private static final float NORMAL_DEBUG_LENGTH = 0.6f;
    private static final float CONTACT_MARK_HALF_SIZE = 0.08f;
    private DynamicObject dynamicObject;
    private StaticObject floorObject;
    @Override
    public void create() {
        Box2D.init();
        world = new World(new Vector2(0,0), true);
        debugRenderer = new Box2DDebugRenderer();

        // Use a small world size (meters) so Box2D objects are visible with the debug renderer.
        camera = new OrthographicCamera();
        viewport = new FitViewport(10f, 7.5f, camera); // world units: 10 x 7.5
        viewport.apply(true);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();


        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());

        // Place the body near the center of the viewport so it's visible
        float startX = viewport.getWorldWidth() / 2f;
        float startY = viewport.getWorldHeight() / 2f;

        // Create a slanted static floor along the bottom of the viewport so the circle can collide with it.
        // We use an EdgeShape from a left point near the left edge up to a slightly higher right point to make it slanted.

        // Shared concave local polygon definition used by both custom math and Box2D fixtures.
        List<Vector2> concaveLocalVertices = Arrays.asList(
            new Vector2(-0.7f, 0.7f),
            new Vector2(0.7f, 0.7f),
            new Vector2(0.7f, 0.2f),
            new Vector2(0.2f, 0.2f),
            new Vector2(0.2f, -0.7f),
            new Vector2(-0.7f, -0.7f)
        );

        dynamicObject = new DynamicObject(0, 0.3f, 0.1f, concaveLocalVertices, startX, startY, 0f, world);

        float leftX = 0.5f; // small margin from left edge
        float rightX = viewport.getWorldWidth() - 0.5f; // small margin from right edge
        float leftY = 0.5f; // near bottom
        float rightY = 1.5f; // slanted upward to the right
        List<Vector2> floorLocalVertices = Arrays.asList(
            new Vector2(leftX, leftY - 0.08f),
            new Vector2(rightX, rightY - 0.08f),
            new Vector2(rightX, rightY + 0.08f),
            new Vector2(leftX, leftY + 0.08f)
        );



        floorObject = new StaticObject(1, 0.3f, 0.1f, floorLocalVertices, 0f, 0f, 0f, world);
        // Example: build a Box2D body from the same concave polygon by adding one fixture per ear-clipped triangle.
        Gdx.app.log("Object data", "Inertia of example body: " + String.valueOf(dynamicObject.getBody().getInertia()));
        Gdx.app.log("Object data", "Mass of example body: " + String.valueOf(dynamicObject.getBody().getMass()));
    }

    @Override
    public void render() {

        float delta = Gdx.graphics.getDeltaTime();
        accumulator += Math.min(delta, 0.25f);
        logTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("Main", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }



        final float fixedStep = 1f / 60f;
        while(accumulator >= fixedStep) {
            // Step the physics simulation with a fixed time step. This ensures consistent behavior regardless of frame rate.

            //1. Integrate forces → update velocity
            //2. Detect collisions
            //3. Build contact constraints
            //4. Solve velocity constraints (impulses)
            //5. Solve position constraints
            //6. Update positions

            //RESOLVING ALL FORCES

            // Move Box2D concave polygon down so it collides with the slanted floor in debug view.
            if (dynamicObject.getBody()  != null) {

                dynamicObject.setLinearVelocity(new Vector2(dynamicObject.getLinearVelocity()).add(new Vector2(0f, GRAVITY).scl(fixedStep)));
            }

            //RESOLVE COLLISIONS

            lastCustomContact = resolveCollisions(dynamicObject, floorObject);

            if (dynamicObject.getBody()  != null) {
                dynamicObject.updatePosition(fixedStep);
            }

            accumulator -= fixedStep;
        }



        // clear the screen so debug renderer is visible
        ScreenUtils.clear(Color.BLACK);

        camera.update();

        // render physics debug shapes
        debugRenderer.render(world, camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setProjectionMatrix(camera.combined);
        drawEarTriangles(floorObject.getLocalBody(), floorObject.getConcaveLocalTriangles(), Color.WHITE);
        drawEarTriangles(dynamicObject.getLocalBody(), dynamicObject.getConcaveLocalTriangles(), Color.WHITE);
        shapeRenderer.end();
        camera.update();
        if (showDebugOverlay) {
            // Draw ear-clipped triangles on top of Box2D debug view.
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            drawEarTriangles(floorObject.getLocalBody(), floorObject.getConcaveLocalTriangles(), Color.CYAN);
            drawEarTriangles(dynamicObject.getLocalBody(), dynamicObject.getConcaveLocalTriangles(), Color.YELLOW);
            drawContactNormalOverlay(lastCustomContact, Color.RED);

            //draw a marker at the center of mass of the Box2D body
            //and also draw where the gravity is being applied to the body
            if (dynamicObject.getBody() != null) {
                Vector2 com = dynamicObject.getBody() .getWorldCenter();
                drawMarker(com, CONTACT_MARK_HALF_SIZE, Color.MAGENTA);
                drawArrow(com, new Vector2(0f, -GRAVITY).nor(), GRAVITY * 0.1f * dynamicObject.getMass(), Color.MAGENTA);
            }

            if(dynamicObject.getLocalBody() != null) {
                Vector2 com = dynamicObject.getLocalBody().getPosition();
                drawMarker(com, CONTACT_MARK_HALF_SIZE, Color.GREEN);
            }
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        if (batch != null) batch.dispose();
        if (image != null) image.dispose();
        if (ballImage != null) ballImage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override
    public void resize(int width, int height) {
        // keep viewport in sync with window size
        if (viewport != null) viewport.update(width, height, true);
    }

    private void input() {

    }

    private void logic() {

    }

    private void draw() {
    }

    private void drawEarTriangles(CustomContactHandler.PolygonBody body,
                                  List<List<Vector2>> localTriangles,
                                  Color color) {
        if (body == null || localTriangles == null || localTriangles.isEmpty()) {
            return;
        }

        shapeRenderer.setColor(color);
        Vector2 position = body.getPosition();
        float angle = body.getRotationRadians();

        for (List<Vector2> tri : localTriangles) {
            Vector2 a = toWorld(tri.get(0), position, angle);
            Vector2 b = toWorld(tri.get(1), position, angle);
            Vector2 c = toWorld(tri.get(2), position, angle);
            shapeRenderer.triangle(a.x, a.y, b.x, b.y, c.x, c.y);
        }
    }

    private ContactResult resolveCollisions(PhysicsObject physicsObject1, PhysicsObject physicsObject2) {
        if(physicsObject1 instanceof StaticObject && physicsObject2 instanceof StaticObject) {
            return null; // skip collision when concerning two static objects
        }
        ContactResult customContact = CustomContactHandler.detect(physicsObject1.getLocalBody(), physicsObject2.getLocalBody());
        lastCustomContact = customContact;
        if (customContact.isColliding()) {

            Vector2 n = customContact.getNormal();
            Vector2 cp = customContact.getContactPoint();
            float penetrationDepth = customContact.getPenetrationDepth();
            float restitution = Math.min(physicsObject1.getRestitution(), physicsObject2.getRestitution());
            Gdx.app.log("CustomContact", String.format("contact p=(%.3f, %.3f) depth=%.4f normal=(%.3f, %.3f)",
                cp.x, cp.y, customContact.getPenetrationDepth(), n.x, n.y));
        }
        return lastCustomContact;
    }

    private void drawContactNormalOverlay(ContactResult contact, Color color) {
        if (contact == null || !contact.isColliding()) {
            return;
        }

        Vector2 cp = contact.getContactPoint();
        Vector2 n = contact.getNormal();
        drawArrow(cp, n, NORMAL_DEBUG_LENGTH, color);
        drawMarker(cp, CONTACT_MARK_HALF_SIZE, color);
    }

    private void drawArrow(Vector2 start, Vector2 direction, float scale, Color color) {
        shapeRenderer.setColor(color);

        // Arrow
        Vector2 end = new Vector2(start).mulAdd(direction.nor(), scale);
        shapeRenderer.line(start.x, start.y, end.x, end.y);
        // Arrowhead
        Vector2 side = new Vector2(-direction.y, direction.x).scl(0.06f);
        Vector2 back = new Vector2(direction).scl(0.12f);
        Vector2 left = new Vector2(end).sub(back).add(side);
        Vector2 right = new Vector2(end).sub(back).sub(side);
        shapeRenderer.line(end.x, end.y, left.x, left.y);
        shapeRenderer.line(end.x, end.y, right.x, right.y);
    }

    private void drawMarker(Vector2 position, float size, Color color) {
        shapeRenderer.setColor(color);
        shapeRenderer.line(position.x - size, position.y - size, position.x + size, position.y + size);
        shapeRenderer.line(position.x - size, position.y + size, position.x + size, position.y - size);
    }

    private Vector2 toWorld(Vector2 local, Vector2 position, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector2(
            local.x * cos - local.y * sin + position.x,
            local.x * sin + local.y * cos + position.y
        );
    }
}
