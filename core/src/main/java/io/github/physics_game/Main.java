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

import java.util.ArrayList;
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
    private boolean runPhysics = false;
    private ArrayList<ContactResult> customContacts;
    private static final float NORMAL_DEBUG_LENGTH = 0.6f;
    private static final float CONTACT_MARK_HALF_SIZE = 0.08f;
    private final int NUM_ITERATIONS = 5; // number of iterations for collision resolution
    private DynamicObject dynamicObject;
    private StaticObject floorObject;
    private Level exampleLevel;
    private DrawTool drawTool;


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
        DynamicObject exampleObject = new DynamicObject(0, 0.5f, 0.5f, 1f,
            Arrays.asList(
                new Vector2(-0.7f, 0.7f),
                new Vector2(0.7f, 0.7f),
                new Vector2(0.7f, 0.2f),
                new Vector2(0.2f, 0.2f),
                new Vector2(0.2f, -0.7f),
                new Vector2(-0.7f, -0.7f)
            ),
            5, 5, 0, world);
        exampleLevel = new Level(0, "Example Level", new ArrayList<>());
        //exampleObject.setRotation((float)Math.PI);
        exampleLevel.addPhysicsObject(exampleObject);

        drawTool = new DrawTool(camera, world, exampleLevel);
        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());

        // Place the body near the center of the viewport so it's visible
//        float startX = viewport.getWorldWidth() / 2f;
//        float startY = viewport.getWorldHeight() / 2f;

        // Create a slanted static floor along the bottom of the viewport so the circle can collide with it.
        // We use an EdgeShape from a left point near the left edge up to a slightly higher right point to make it slanted.

        // Shared concave local polygon definition used by both custom math and Box2D fixtures.
//        List<Vector2> concaveLocalVertices = Arrays.asList(
//            new Vector2(-0.7f, 0.7f),
//            new Vector2(0.7f, 0.7f),
//            new Vector2(0.7f, 0.2f),
//            new Vector2(0.2f, 0.2f),
//            new Vector2(0.2f, -0.7f),
//            new Vector2(-0.7f, -0.7f)
//        );
//
//        dynamicObject = new DynamicObject(0, 0.3f, 0.1f, concaveLocalVertices, startX, startY, 0f, world);
    }

    @Override
    public void render() {

        float delta = Gdx.graphics.getDeltaTime();

        drawTool.update();


        if(runPhysics) accumulator += Math.min(delta, 0.25f);
        else accumulator = 0.0f;
        logTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("Main", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            runPhysics = !runPhysics;
            Gdx.app.log("Main", "Physics " + (runPhysics ? "RUNNING" : "PAUSED"));
        }
        //make stepping key
        if(Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Gdx.app.log("Main", "ticking 1 time step");
            accumulator = 2f / 60f;
        }

        // clear the screen
        ScreenUtils.clear(Color.BLACK);

        if(!showDebugOverlay) {
            //Normal view
            if(runPhysics){
                PhysicsResolver.step(accumulator, exampleLevel.getPhysicsObjects());
            }
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setProjectionMatrix(camera.combined);
            int i = 1;
            for(PhysicsObject obj : exampleLevel.getPhysicsObjects()) {
                drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), (obj instanceof StaticObject)? Color.GRAY : Color.WHITE);
            }

            // draw the drawing line
            if (drawTool.isDrawing()) {
                ArrayList<Vector2> pts = drawTool.getPoints();
                if (pts.size() > 1) {
                    shapeRenderer.setColor(Color.GREEN);
                    for (int j = 0; j < pts.size() - 1; j++) {
                        Vector2 p1 = pts.get(j);
                        Vector2 p2 = pts.get(j + 1);
                        shapeRenderer.rectLine(p1.x, p1.y, p2.x, p2.y, 0.05f);
                    }
                }
            }

            shapeRenderer.end();

            camera.update();
        } else {
            //Debug view
            debugRenderer.render(world, camera.combined);
            ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(accumulator, exampleLevel.getPhysicsObjects());
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for(PhysicsObject obj : exampleLevel.getPhysicsObjects()) {
                if(obj instanceof DynamicObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.YELLOW);
                } else if (obj instanceof StaticObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.CYAN);
                } else {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.WHITE);
                }
            }

            for(DebugForce f : forces) {
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len() * 10f, f.getColor());
            }

            shapeRenderer.end();

            camera.update();
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

    private void drawContactNormalOverlay(ArrayList<ContactResult> contact, Color color) {
        for(ContactResult c : contact) {
            drawSingleContactNormal(c, color);
        }
    }

    private void drawSingleContactNormal(ContactResult contact, Color color) {
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
