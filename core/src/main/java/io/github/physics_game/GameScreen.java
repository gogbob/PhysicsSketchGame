package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.physics_game.collision.CustomContactHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameScreen extends ScreenAdapter {
    private final MainGame game;

    private SpriteBatch batch;
    private FitViewport viewport;
    private Texture image;
    private Texture ballImage;
    private OrthographicCamera camera;
    public static float accumulator = 0f;

    public GameScreen(MainGame game) {
        this.game = game;
    }

    float logTimer = 0f;

    private ShapeRenderer shapeRenderer;

    private boolean showDebugOverlay = false;
    private boolean runPhysics = false;
    private DynamicObject dynamicObject;
    private StaticObject floorObject;
    private Level exampleLevel;
    private DrawTool drawTool;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(20f, 15f, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        DynamicObject exampleObject = new DynamicObject(
            0, 0.5f, 0.1f, 0.5f,
            Arrays.asList(
                new Vector2(-0.7f, 0.7f),
                new Vector2(0.7f, 0.7f),
                new Vector2(0.7f, 0.2f),
                new Vector2(0.2f, 0.2f),
                new Vector2(0.2f, -0.7f),
                new Vector2(-0.7f, -0.7f)
            ),
            5, 5, 0
        );

        TutorialLevel tutorialLevel = new TutorialLevel();
        drawTool = new DrawTool(camera, tutorialLevel);
        exampleLevel = tutorialLevel;

        Gdx.app.log("GameScreen", "DrawTool created!");
        Gdx.app.log("GameScreen", "show() - viewport world size = "
            + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }

    @Override
    public void render(float delta) {
        drawTool.update();

        if (runPhysics) accumulator += Math.min(delta, 0.25f);
        else accumulator = 0.0f;

        logTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("GameScreen", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            runPhysics = !runPhysics;
            Gdx.app.log("GameScreen", "Physics " + (runPhysics ? "RUNNING" : "PAUSED"));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Gdx.app.log("GameScreen", "ticking 1 time step");
            accumulator = 2f / 60f;
        }

        ScreenUtils.clear(Color.BLACK);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        if (!showDebugOverlay) {
            if (runPhysics) {
                PhysicsResolver.step(accumulator, exampleLevel.getPhysicsObjects());
            }

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            for (PhysicsObject obj : exampleLevel.getPhysicsObjects()) {
                if (obj instanceof DynamicTriggerObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.BLUE);
                } else if (obj instanceof DynamicObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.WHITE);
                } else if (obj instanceof StaticObject) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.GRAY);
                }
            }

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
        } else {
            ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(accumulator, exampleLevel.getPhysicsObjects());

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            for (PhysicsObject obj : exampleLevel.getPhysicsObjects()) {
                if (obj instanceof DynamicTriggerObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.GOLD);
                } else if (obj instanceof DynamicObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.YELLOW);
                } else if (obj instanceof StaticObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.CYAN);
                } else if (obj instanceof TriggerObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalTriangles(), Color.RED);
                }
            }

            for (DebugForce f : forces) {
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
            }

            shapeRenderer.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (image != null) image.dispose();
        if (ballImage != null) ballImage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
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

        for (List<Vector2> concaveLocalTriangle : concaveLocalTriangles) {
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

        Vector2 end = new Vector2(start).mulAdd(direction.nor(), scale);
        shapeRenderer.line(start.x, start.y, end.x, end.y);

        Vector2 side = new Vector2(-direction.y, direction.x).scl(0.06f);
        Vector2 back = new Vector2(direction).scl(0.12f);
        Vector2 left = new Vector2(end).sub(back).add(side);
        Vector2 right = new Vector2(end).sub(back).sub(side);

        shapeRenderer.line(end.x, end.y, left.x, left.y);
        shapeRenderer.line(end.x, end.y, right.x, right.y);
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
