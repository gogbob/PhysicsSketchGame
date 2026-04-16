package io.github.physics_game;

import com.badlogic.gdx.*;
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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.levels.Level;
import io.github.physics_game.levels.Level1;
import io.github.physics_game.levels.TutorialLevel;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.badlogic.gdx.Input.Keys.B;
import static com.badlogic.gdx.utils.JsonValue.ValueType.object;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GameScreen extends ScreenAdapter {
    private final MainGame game;

    private DrawType drawType = DrawType.POSITIVE;
    private SpriteBatch batch;
    FitViewport viewport;
    private Texture image;
    private Texture ballImage;
    OrthographicCamera camera;
    Box2DDebugRenderer debugRenderer;
    private OrthographicCamera uiCamera;
    private ScreenViewport uiViewport;
    public static float accumulator = 0f;
    final float GRAVITY = -9.8f;
    BitmapFont winFont;
    private float levelTimer = 0f;
    private int finalScore = -1;
    private int finalStars = 0;
    private Integer selectedObject = null;
    private boolean scoreCalculated = false;
    public static final float viewPortWidth = 40f;
    public static final float viewPortHeight = 30f;
    public static int SIDE_BUFFER_PX = 200;
    public static int TOP_BUFFER_PX = 10;
    public static int BOTTOM_BUFFER_PX = 10;
    public static final float selectInfoPeriod = 0.1f;

    public GameScreen(MainGame game, Level selectedLevel) {
        this.game = game;
        this.currentLevel = selectedLevel;
    }

    // throttle logging to once-per-second
    float logTimer = 0f;
    private float selectInfoTimer = selectInfoPeriod;
    private String stringInfo = "";

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
    public void show() {
        // Use a small world size (meters) so objects are visible with the debug renderer.
        camera = new OrthographicCamera();
        viewport = new FitViewport(viewPortWidth, viewPortHeight, camera); // world units: 20 x 15
        viewport.apply(true);
        uiCamera = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCamera);
        uiViewport.apply(true);
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

        drawTool = new DrawTool(camera, viewport, 0.5f);

        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }

    @Override
    public void render(float delta) {
        boolean isSelecting = false;
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Integer i = isPointInsideObjects(viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
            if(i != null) {
                selectedObject = i;
                isSelecting = true;
            } else {
                isSelecting = false;
            }
        }
        if(!isSelecting) {
            PhysicsObject drawnObject = drawTool.update(drawType);
            if(drawnObject != null) {
                runPhysics = true;
                if (drawnObject instanceof DynamicObject) {
                    currentLevel.getPhysicsObjects().removeIf(obj -> obj.getId() >= 1000);
                    currentLevel.addPhysicsObject(drawnObject);
                    currentLevel.setNumDrawnObjects(currentLevel.getNumDrawnObjects() + 1);
                } else {
                    currentLevel.getPhysicsObjects().removeIf(obj -> obj.getId() >= 1000);
                    currentLevel.addPhysicsObject(drawnObject);
                }
            }
        }
        if (!currentLevel.isComplete()) {
            levelTimer += delta;
        }

        if(runPhysics) accumulator += Math.min(delta, 0.25f);
        else accumulator = 0.0f;
        logTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("Main", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            //run debug
            float maxIndex = -1;
            PhysicsObject lastObj = currentLevel.getPhysicsObjects().get(currentLevel.getPhysicsObjects().size() - 1);
            drawTool.testAddPoint(true);
            PhysicsResolver.printShape(lastObj.getVertices());
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

        if(Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            drawType = (drawType == DrawType.POSITIVE)? DrawType.NEGATIVE : DrawType.POSITIVE;
        }

        // clear the screen
        ScreenUtils.clear(Color.GRAY);
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // create the background
        Texture txtBackground = currentLevel.getBackground();
        if(txtBackground != null) {
            batch.setColor(new Color(0.8f, 0.8f, 0.8f, 1.0f));
            batch.draw(txtBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        } else {
            Gdx.app.log("Main", "Background texture is null!");
        }
        batch.end();


        if(!showDebugOverlay) {
            //Normal view
            if(runPhysics){
                PhysicsResolver.step(currentLevel.getPhysicsObjects());
            }

            if(currentLevel.isComplete()) {
                if (!scoreCalculated) {
                    finalScore = ScoreCalculator.calculateScore(
                        currentLevel.getNumDrawnObjects(),
                        levelTimer
                    );
                    finalStars = ScoreCalculator.calculateStars(finalScore);
                    scoreCalculated = true;

                    Gdx.app.log("Main", "Level complete!");
                    Gdx.app.log("Main", "Score = " + finalScore + ", Stars = " + finalStars);
                }
            }

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setProjectionMatrix(camera.combined);
            int i = 1;
            for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if(!(obj instanceof UncollidableObject)) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), obj.getColor());
                }
            }
            shapeRenderer.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if(selectedObject != null) {
                    if(obj.getId() == selectedObject) {
                        drawOutline(obj.getLocalBody(), Color.BLUE);
                    }
                }
            }
            shapeRenderer.end();

            currentLevel.tick(delta);


        } else {
            //Debug view
            ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(currentLevel.getPhysicsObjects());

            if(currentLevel.isComplete()) {
                Gdx.app.log("Main", "Level complete! Loading next level...");
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

                //draw the outline
                drawOutline(obj.getLocalBody(), Color.WHITE);
            }

            currentLevel.tick(delta);

            for(DebugForce f : forces) {
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
            }

            shapeRenderer.end();
        }

        //camera.update();
        //batch.setProjectionMatrix(camera.combined);
        //generate UI Here
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        if(currentLevel.isComplete()) {
            GlyphLayout layout1 = new GlyphLayout(winFont, "Level Complete!");
            winFont.setColor(Color.GREEN);

            float x1 = (Gdx.graphics.getWidth() - layout1.width) / 2f;
            float y1 = Gdx.graphics.getHeight() * 0.75f;

            winFont.draw(batch, layout1, x1, y1);

            GlyphLayout layout2 = new GlyphLayout(winFont, "Score: " + finalScore);
            winFont.setColor(Color.WHITE);
            winFont.draw(batch, layout2, (Gdx.graphics.getWidth() - layout2.width)/2f, y1 - 30);

            GlyphLayout layout3 = new GlyphLayout(winFont, "Stars: " + finalStars);
            winFont.setColor(Color.YELLOW);
            winFont.draw(batch, layout3, (Gdx.graphics.getWidth() - layout3.width)/2f, y1 - 60);
        }

        selectInfoTimer += delta;
        System.out.println(selectInfoTimer);
        if(selectedObject != null) {
            if (selectInfoTimer >= selectInfoPeriod) {

                while (selectInfoTimer >= selectInfoPeriod) selectInfoTimer -= selectInfoPeriod;
                PhysicsObject obj = null;
                for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                    if (selectedObject != null) {
                        if (o.getId() == selectedObject) {
                            obj = o;
                        }
                    }
                }
                if (obj == null) {
                    selectedObject = null;
                    Gdx.app.log("Main", "Selected object not found!");
                    return;
                }

    // actual side buffers (these can differ from SIDE_BUFFER_PX depending on aspect/stretching)

                String suffix = ((obj instanceof Charged) ? "Charged " : (obj instanceof AntigravityObject) ? "Antigravity " : "");
                String type = ((obj instanceof DynamicObject) ? "Dynamic " : (obj instanceof StaticObject) ? "Static " : "");

                StringBuilder builder = new StringBuilder();
                // add buffer to stabilize
                builder.append("                                                            \n");
                builder.append(suffix + type + "Object: " + selectedObject);
                builder.append(String.format("\nFriction: %+6.3f", obj.getFriction()) +
                    String.format("\n Restitution: %+6.3f", obj.getRestitution()));
                if (obj instanceof DynamicObject) {
                    builder.append(String.format("\nLinear Velocity: (%+7.3f, %+7.3f)", obj.getLinearVelocity().x, obj.getLinearVelocity().y) +
                        String.format("\nAngular Velocity: %+7.3f", obj.getAngularVelocity()) +
                        String.format("\nLinear Acceleration: (%+7.3f, %+7.3f)", ((DynamicObject) obj).getCurrentLinearAcceleration(delta).x, ((DynamicObject) obj).getCurrentLinearAcceleration(delta).y) +
                        String.format("\nAngular Acceleration: %+7.3f", ((DynamicObject) obj).getCurrentAngularAcceleration(delta)) +
                        String.format("\nDensity: %+6.3f", obj.getDensity()) +
                        String.format("\nMass: %+6.3f", ((DynamicObject) obj).getMass()) +
                        String.format("\nInertia: %+6.3f", ((DynamicObject) obj).getInertia()));
                }
                if (obj instanceof Charged) {
                    builder.append(String.format("\nCharge Density: %+6.3f", ((Charged) obj).getChargeDensity()));
                }

                stringInfo = builder.toString();

                for(PhysicsObject o : currentLevel.getPhysicsObjects()) {
                    if(o instanceof DynamicObject) {
                        ((DynamicObject) o).setCurrentAngularAcceleration(0f);
                        ((DynamicObject) o).setCurrentLinearAcceleration(new Vector2(0f, 0f));
                    }
                }
            }
            int panelW  = (uiViewport.getScreenWidth() - viewport.getScreenWidth())/2;
            int rightPanelX = uiViewport.getScreenWidth() - panelW;
            int panelH = viewport.getScreenHeight();
            int panelY = (uiViewport.getScreenHeight() - panelH)/2;

            GlyphLayout layout1 = new GlyphLayout(winFont,   stringInfo);
            float x = rightPanelX + 5f;           // fixed left padding
            float y = panelY + panelH - 20f;       // fixed top anchor
            winFont.setUseIntegerPositions(true);  // reduce subpixel shimmer
            winFont.setFixedWidthGlyphs("0123456789+-.,() ");
            winFont.draw(batch, layout1, Math.round(x), Math.round(y));
        }


        batch.end();

    }

    @Override
    public void dispose() {
        debugRenderer.dispose();
        if (batch != null) batch.dispose();
        if (image != null) image.dispose();
        if (ballImage != null) ballImage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {

            int worldW = Math.min((int)((height - TOP_BUFFER_PX - BOTTOM_BUFFER_PX) * viewPortWidth/viewPortHeight), width - 2 * SIDE_BUFFER_PX);
            int worldH = (int)(worldW * viewPortHeight/viewPortWidth);
            int worldX = (width - worldW)/2;
            int worldY = (height - worldH)/2;

            // Update with the size the world should live in
            viewport.update(worldW, worldH, true);
            // Then place it in the center region of the window
            viewport.setScreenBounds(worldX, worldY, worldW, worldH);
        }

        if (uiViewport != null) {
            uiViewport.update(width, height, true);
        }
    }

    public Integer isPointInsideObjects(Vector2 point) {
        for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
            if(!(obj instanceof UncollidableObject || obj instanceof StaticObject)) {
                if(obj.isPointInsideObject(point)) {
                    return obj.getId();
                }
            }
        }

        return null;
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

    private void drawOutline(CustomContactHandler.PolygonBody body,
                             Color color) {
        if (body == null) {
            return;
        }
        shapeRenderer.setColor(color);
        Vector2 position = body.getPosition();
        float angle = body.getRotationRadians();

        for(int i = 0; i < body.getLocalVertices().size(); i++) {
            Vector2 worldVertex1 = toWorld(body.getLocalVertices().get(i), position, angle);
            Vector2 worldVertex2 = toWorld(body.getLocalVertices().get((i+1) % body.getLocalVertices().size()), position, angle);
            shapeRenderer.line(worldVertex1.x, worldVertex1.y, worldVertex2.x, worldVertex2.y);

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
