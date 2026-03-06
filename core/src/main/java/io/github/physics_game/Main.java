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

    // throttle logging to once-per-second
    float logTimer = 0f;

    // Custom collision demo bodies (independent from Box2D contact callbacks).
    private CustomContactHandler.PolygonBody customDynamicBody;
    private CustomContactHandler.PolygonBody customFloorBody;

    // Box2D body built from ear-clipped triangles of the same concave polygon.
    private Body concaveBox2dBody;
    private ShapeRenderer shapeRenderer;

    private List<List<Vector2>> concaveLocalTriangles;
    private List<List<Vector2>> floorLocalTriangles;

    private boolean showDebugOverlay = true;
    private ContactResult lastCustomContact = ContactResult.NO_CONTACT;
    private static final float NORMAL_DEBUG_LENGTH = 0.6f;
    private static final float CONTACT_MARK_HALF_SIZE = 0.08f;

    @Override
    public void create() {
        Box2D.init();
        world = new World(new Vector2(0,-1.0f), true);
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
        BodyDef groundBd = new BodyDef();
        groundBd.type = BodyDef.BodyType.StaticBody;
        groundBd.position.set(0f, 0f);
        Body groundBody = world.createBody(groundBd);

        EdgeShape floor = new EdgeShape();
        float leftX = 0.5f; // small margin from left edge
        float rightX = viewport.getWorldWidth() - 0.5f; // small margin from right edge
        float leftY = 0.5f; // near bottom
        float rightY = 1.5f; // slanted upward to the right
        floor.set(new Vector2(leftX, leftY), new Vector2(rightX, rightY));

        FixtureDef floorFd = new FixtureDef();
        floorFd.shape = floor;
        floorFd.friction = 0.0f;
        floorFd.restitution = 0f;
        groundBody.createFixture(floorFd);
        floor.dispose();

        // Shared concave local polygon definition used by both custom math and Box2D fixtures.
        List<Vector2> concaveLocalVertices = Arrays.asList(
            new Vector2(-0.7f, 0.7f),
            new Vector2(0.7f, 0.7f),
            new Vector2(0.7f, 0.2f),
            new Vector2(0.2f, 0.2f),
            new Vector2(0.2f, -0.7f),
            new Vector2(-0.7f, -0.7f)
        );

        concaveLocalTriangles = EarClippingDecomposer.decomposeToTriangles(concaveLocalVertices);

        // Concave "L" shape to demonstrate ear clipping decomposition in custom detection.
        customDynamicBody = new CustomContactHandler.PolygonBody(concaveLocalVertices)
            .setPosition(startX, startY)
            .setRotationRadians(0f);

        List<Vector2> floorLocalVertices = Arrays.asList(
            new Vector2(leftX, leftY - 0.08f),
            new Vector2(rightX, rightY - 0.08f),
            new Vector2(rightX, rightY + 0.08f),
            new Vector2(leftX, leftY + 0.08f)
        );

        customFloorBody = new CustomContactHandler.PolygonBody(floorLocalVertices)
            .setPosition(0f, 0f)
            .setRotationRadians(0f);
        floorLocalTriangles = EarClippingDecomposer.decomposeToTriangles(floorLocalVertices);

        // Example: build a Box2D body from the same concave polygon by adding one fixture per ear-clipped triangle.
        concaveBox2dBody = createBodyFromEarClippedTriangles(concaveLocalVertices, startX, startY, BodyDef.BodyType.DynamicBody);

        setupContactListener();
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

            // Move Box2D concave polygon down so it collides with the slanted floor in debug view.
            if (concaveBox2dBody != null) {
                Vector2 p = concaveBox2dBody.getPosition();
                concaveBox2dBody.setTransform(p.x, p.y - 0.01f, concaveBox2dBody.getAngle() - 0.01f);

                // Keep custom-math body synchronized with Box2D transform for apples-to-apples contact logs.
                Vector2 bp = concaveBox2dBody.getPosition();
                customDynamicBody.setPosition(bp.x, bp.y).setRotationRadians(concaveBox2dBody.getAngle());
            }

            accumulator -= fixedStep;
        }

        ContactResult customContact = CustomContactHandler.detect(customDynamicBody, customFloorBody);
        lastCustomContact = customContact;
        if (customContact.isColliding()) {
            Vector2 n = customContact.getNormal();
            Vector2 cp = customContact.getContactPoint();
            Gdx.app.log("CustomContact", String.format("contact p=(%.3f, %.3f) depth=%.4f normal=(%.3f, %.3f)",
                cp.x, cp.y, customContact.getPenetrationDepth(), n.x, n.y));
        }

        // clear the screen so debug renderer is visible
        ScreenUtils.clear(Color.BLACK);

        camera.update();

        // render physics debug shapes
        debugRenderer.render(world, camera.combined);

        if (showDebugOverlay) {
            // Draw ear-clipped triangles on top of Box2D debug view.
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            drawEarTriangles(customFloorBody, floorLocalTriangles, Color.CYAN);
            drawEarTriangles(customDynamicBody, concaveLocalTriangles, Color.YELLOW);
            drawContactNormalOverlay(lastCustomContact, Color.RED);
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

    private void setupContactListener() {
        world.setContactListener(new ContactListener() {

            @Override
            public void beginContact(Contact contact) {
                Body a = contact.getFixtureA().getBody();
                Body b = contact.getFixtureB().getBody();
                Gdx.app.log("Main", "beginContact between bodies: " + a + " and " + b);
                // Trigger game logic here
            }

            @Override
            public void endContact(Contact contact) {}

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
                // Optional: disable or modify collisions
                // contact.setEnabled(false);
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    private Body createBodyFromEarClippedTriangles(List<Vector2> localVertices,
                                                   float worldX,
                                                   float worldY,
                                                   BodyDef.BodyType type) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = type;
        bodyDef.position.set(worldX, worldY);
        Body body = world.createBody(bodyDef);

        List<List<Vector2>> triangles = EarClippingDecomposer.decomposeToTriangles(localVertices);
        for (List<Vector2> triangle : triangles) {
            PolygonShape polygon = new PolygonShape();
            polygon.set(new Vector2[]{triangle.get(0), triangle.get(1), triangle.get(2)});

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = polygon;
            fixtureDef.density = type == BodyDef.BodyType.DynamicBody ? 1f : 0f;
            fixtureDef.friction = 0.3f;
            fixtureDef.restitution = 0.1f;
            body.createFixture(fixtureDef);
            polygon.dispose();
        }

        return body;
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

    private void drawContactNormalOverlay(ContactResult contact, Color color) {
        if (contact == null || !contact.isColliding()) {
            return;
        }

        Vector2 p = contact.getContactPoint();
        Vector2 n = contact.getNormal().nor();
        Vector2 tip = new Vector2(p).mulAdd(n, NORMAL_DEBUG_LENGTH);

        shapeRenderer.setColor(color);

        // Contact marker (small X) and normal direction ray from the estimated contact point.
        shapeRenderer.line(p.x - CONTACT_MARK_HALF_SIZE, p.y - CONTACT_MARK_HALF_SIZE,
            p.x + CONTACT_MARK_HALF_SIZE, p.y + CONTACT_MARK_HALF_SIZE);
        shapeRenderer.line(p.x - CONTACT_MARK_HALF_SIZE, p.y + CONTACT_MARK_HALF_SIZE,
            p.x + CONTACT_MARK_HALF_SIZE, p.y - CONTACT_MARK_HALF_SIZE);
        shapeRenderer.line(p.x, p.y, tip.x, tip.y);

        // Tiny arrow head at the ray tip for direction clarity.
        Vector2 side = new Vector2(-n.y, n.x).scl(0.06f);
        Vector2 back = new Vector2(n).scl(0.12f);
        Vector2 left = new Vector2(tip).sub(back).add(side);
        Vector2 right = new Vector2(tip).sub(back).sub(side);
        shapeRenderer.line(tip.x, tip.y, left.x, left.y);
        shapeRenderer.line(tip.x, tip.y, right.x, right.y);
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
