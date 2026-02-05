package io.github.physics_game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

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

    // expose the physics body so we can inspect/draw it in render()
    Body circleBody;

    // radius in world units (meters)
    float circleRadius = 0.6f;

    // throttle logging to once-per-second
    float logTimer = 0f;

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


        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());

        // Place the body near the center of the viewport so it's visible
        float startX = viewport.getWorldWidth() / 2f;
        float startY = viewport.getWorldHeight() / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(startX, startY);

        circleBody = world.createBody(bd);

        CircleShape shape = new CircleShape();
        // give the circle a radius so the fixture is valid and visible
        shape.setRadius(circleRadius);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0.3f;
        fd.restitution = 0.1f;

        circleBody.createFixture(fd);
        shape.dispose();
        setupContactListener();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        accumulator += Math.min(delta, 0.25f);
        logTimer += delta;

        while(accumulator >= 1/60f) {
            world.step(1/60f, 6, 2);
            accumulator -= 1/60f;
        }

        // clear the screen so debug renderer is visible
        ScreenUtils.clear(Color.BLACK);

        camera.update();

        // render physics debug shapes
        debugRenderer.render(world, camera.combined);

        // Draw a visible sprite for the circle body (uses world coordinates because we set the batch projection)
        if (ballImage != null && circleBody != null) {
            Vector2 p = circleBody.getPosition();
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            float size = circleRadius * 2f; // world units
            batch.draw(ballImage, p.x - circleRadius, p.y - circleRadius, size, size);
            batch.end();

            // Throttle logging to once per second to avoid spamming the console
            if (logTimer >= 1f) {
                Gdx.app.log("Main", String.format("render() dt=%.4f bodyPos=(%.2f, %.2f)", delta, p.x, p.y));
                logTimer = 0f;
            }
        }

    }

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        if (batch != null) batch.dispose();
        if (image != null) image.dispose();
        if (ballImage != null) ballImage.dispose();
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
}
