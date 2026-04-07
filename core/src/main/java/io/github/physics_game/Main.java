package io.github.physics_game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.object_types.*;

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
    public static float accumulator = 0f;
    final float GRAVITY = -9.8f;
    BitmapFont winFont;

    // throttle logging to once-per-second
    float logTimer = 0f;

    // Box2D body built from ear-clipped triangles of the same concave polygon.
    private ShapeRenderer shapeRenderer;

    private boolean showDebugOverlay = false;
    private boolean runPhysics = false;
    private static final float NORMAL_DEBUG_LENGTH = 0.6f;
    private static final float CONTACT_MARK_HALF_SIZE = 0.08f;
    private final int NUM_ITERATIONS = 5; // number of iterations for collision resolution
    private DynamicObject dynamicObject;
    private StaticObject floorObject;
    private Level currentLevel;
    private DrawTool drawTool;


    @Override
    public void create() {
        // Use a small world size (meters) so objects are visible with the debug renderer.
        camera = new OrthographicCamera();
        viewport = new FitViewport(20f, 15f, camera); // world units: 20 x 15
        viewport.apply(true);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        winFont = new BitmapFont();
        winFont.setColor(Color.WHITE);
        DynamicObject exampleObject = new DynamicObject(0, 0.5f, 0.1f, 0.5f,
            Arrays.asList(
                new Vector2(-0.7f, 0.7f),
                new Vector2(0.7f, 0.7f),
                new Vector2(0.7f, 0.2f),
                new Vector2(0.2f, 0.2f),
                new Vector2(0.2f, -0.7f),
                new Vector2(-0.7f, -0.7f)
            ),
            5, 5, 0);
        TutorialLevel tutorialLevel = new TutorialLevel();
        //exampleObject.setRotation((float)Math.PI);\

        drawTool = new DrawTool(camera, 0.4f);
        currentLevel = tutorialLevel;
        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());

        //CustomContactHandler.detect(squareBody1, squareBody2);



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

        PhysicsObject drawnObject = drawTool.update();
        if(drawnObject != null) {
            if(drawnObject instanceof StaticObject && drawnObject.getId() <1000) {
                //see what is going on
                float a = 1f;
            }
            currentLevel.getPhysicsObjects().removeIf(obj -> obj.getId() >= 1000);
            currentLevel.addPhysicsObject(drawnObject);
        }


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
                PhysicsResolver.step(currentLevel.getPhysicsObjects());
            }

            if(currentLevel.isComplete()) {
                Gdx.app.log("Main", "Level complete! Loading next level...");
                runPhysics = false;
            }
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setProjectionMatrix(camera.combined);
            int i = 1;
            for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if(obj instanceof DynamicTriggerObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.BLUE);
                } else if(obj instanceof DynamicObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.WHITE);
                } else if (obj instanceof StaticObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.GRAY);
                }
            }

            currentLevel.tick(delta);

            shapeRenderer.end();
        } else {
            //Debug view
            ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(currentLevel.getPhysicsObjects());

            if(currentLevel.isComplete()) {
                Gdx.app.log("Main", "Level complete! Loading next level...");
                runPhysics = false;
            }

            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if(obj instanceof DynamicTriggerObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.GOLD);
                } else if(obj instanceof DynamicObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.YELLOW);
                } else if (obj instanceof StaticObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.CYAN);
                } else if (obj instanceof UncollidableObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.RED);
                }
            }

            currentLevel.tick(delta);

            for(DebugForce f : forces) {
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
            }

            shapeRenderer.end();
        }

        //generate UI Here
        batch.begin();

        if(currentLevel.isComplete()) {
            GlyphLayout layout = new GlyphLayout(winFont, "Level Complete!", Color.GREEN, 0, 1, false);
            float x = camera.position.x + viewport.getScreenWidth()/2f - layout.width /2f;
            float y = camera.position.y + viewport.getScreenHeight() * 3f/4f + layout.height /2f;
            winFont.draw(batch, layout, x, y);

        }

        batch.end();
        camera.update();
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
                                  List<List<Vector2>> concaveLocalTriangles,
                                  Color color) {
        if (body == null || concaveLocalTriangles == null || concaveLocalTriangles.isEmpty()) {
            return;
        }
        shapeRenderer.setColor(color);
        Vector2 position = body.getPosition();
        float angle = body.getRotationRadians();

        for(List<Vector2> concaveLocalTriangle : concaveLocalTriangles) {
            float[] vertices = new float[concaveLocalTriangle.size() * 2];
            for (int i = 0; i < concaveLocalTriangle.size(); i++) {
                Vector2 worldVertex = toWorld(concaveLocalTriangle.get(i), position, angle);
                vertices[i * 2] = worldVertex.x;
                vertices[i * 2 + 1] = worldVertex.y;
            }

            shapeRenderer.triangle(
                vertices[0], vertices[1],
                vertices[2], vertices[3],
                vertices[4], vertices[5]
            );

        }
    }

    private void drawPolygons(CustomContactHandler.PolygonBody body,
                                  List<List<Vector2>> convexLocalBest,
                                  Color color) {
        if (body == null || convexLocalBest == null || convexLocalBest.isEmpty()) {
            return;
        }
        shapeRenderer.setColor(color);
        Vector2 position = body.getPosition();
        float angle = body.getRotationRadians();

        for (List<Vector2> poly : convexLocalBest) {
            //convert the List<Vector2> to a float[] of vertices for shapeRenderer.polygon()
            float[] vertices = new float[poly.size() * 2];
            for (int i = 0; i < poly.size(); i++) {
                Vector2 worldVertex = toWorld(poly.get(i), position, angle);
                vertices[i * 2] = worldVertex.x;
                vertices[i * 2 + 1] = worldVertex.y;
            }
            shapeRenderer.polygon(vertices);
        }
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
