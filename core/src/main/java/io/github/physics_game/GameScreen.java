package io.github.physics_game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
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
    private float physicsDataTimer = selectInfoPeriod;
    private String physicsDataString = "";
    private float physicsElapsedTime = 0f;
    private final Vector2 lastClickPos = new Vector2();

    // Graph recording
    private boolean showGraphs = false;
    private static final int MAX_GRAPH_SAMPLES = 300; // 30 s at 10 Hz
    private final List<Float> gTime  = new ArrayList<>();
    private final List<Float> gPosX  = new ArrayList<>();
    private final List<Float> gPosY  = new ArrayList<>();
    private final List<Float> gVelX  = new ArrayList<>();
    private final List<Float> gVelY  = new ArrayList<>();
    private final List<Float> gSpeed = new ArrayList<>();
    private final List<Float> gAccX  = new ArrayList<>();
    private final List<Float> gAccY  = new ArrayList<>();
    private final List<Float> gKE    = new ArrayList<>();
    private final List<Float> gPE    = new ArrayList<>();
    private final List<Float> gTE    = new ArrayList<>();

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
    private Texture panelBgTexture;


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
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        panelBgTexture = new Texture(pm);
        pm.dispose();
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
            int mx = Gdx.input.getX();
            int my = Gdx.graphics.getHeight() - Gdx.input.getY(); // flip to bottom-origin
            lastClickPos.set(mx, my);
            int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
            int btnY   = (uiViewport.getScreenHeight() - viewport.getScreenHeight()) / 2 + 8;
            int btnW   = panelW - 16;
            int btnH   = 28;
            if (mx >= 8 && mx <= 8 + btnW && my >= btnY && my <= btnY + btnH) {
                resetLevel();
                isSelecting = true; // consume the click
            } else {
                Integer i = isPointInsideObjects(viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
                if(i != null) {
                    selectedObject = i;
                    isSelecting = true;
                }
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
        if (runPhysics) physicsElapsedTime += delta;

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

        if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetLevel();
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            showGraphs = !showGraphs;
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

            if(currentLevel.isComplete() && !scoreCalculated) {
                finalScore = ScoreCalculator.calculateScore(
                    currentLevel.getNumDrawnObjects(),
                    levelTimer,
                    currentLevel.getFreeObjects()
                );
                finalStars = ScoreCalculator.calculateStars(finalScore);
                scoreCalculated = true;
                Gdx.app.log("Main", "Score = " + finalScore + ", Stars = " + finalStars);
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

        // Physics Data Panel (left side)
        physicsDataTimer += delta;
        if (physicsDataTimer >= selectInfoPeriod) {
            while (physicsDataTimer >= selectInfoPeriod) physicsDataTimer -= selectInfoPeriod;
            DynamicObject mainObj = null;
            for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                if (o instanceof DynamicObject) { mainObj = (DynamicObject) o; break; }
            }
            if (mainObj != null) {
                Vector2 pos    = mainObj.getPosition();
                Vector2 vel    = mainObj.getLinearVelocity();
                float speed    = vel.len();
                float mass     = mainObj.getMass();
                float inertia  = mainObj.getInertia();
                float omega    = mainObj.getAngularVelocity();
                float ke       = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
                float pe       = mass * 9.8f * pos.y;
                // use selectInfoPeriod as divisor → average a over this update interval
                Vector2 accel  = mainObj.getCurrentLinearAcceleration(selectInfoPeriod);
                float angAccel = mainObj.getCurrentAngularAcceleration(selectInfoPeriod);
                // reset accumulator so each interval is independent (fixes drift bug)
                mainObj.setCurrentLinearAcceleration(new Vector2());
                mainObj.setCurrentAngularAcceleration(0f);
                // record snapshot for graphs — freeze data once level is complete
                if (runPhysics && !currentLevel.isComplete()) {
                    gTime.add(physicsElapsedTime);
                    gPosX.add(pos.x);    gPosY.add(pos.y);
                    gVelX.add(vel.x);    gVelY.add(vel.y);
                    gSpeed.add(speed);
                    gAccX.add(accel.x);  gAccY.add(accel.y);
                    gKE.add(ke);         gPE.add(pe);         gTE.add(ke + pe);
                    while (gTime.size() > MAX_GRAPH_SAMPLES) {
                        gTime.remove(0);  gPosX.remove(0);  gPosY.remove(0);
                        gVelX.remove(0);  gVelY.remove(0);  gSpeed.remove(0);
                        gAccX.remove(0);  gAccY.remove(0);
                        gKE.remove(0);    gPE.remove(0);    gTE.remove(0);
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("  PHYSICS DATA  \n");
                sb.append("----------------\n");
                sb.append(String.format(" t   %8.2f s\n",    physicsElapsedTime));
                sb.append("----------------\n");
                sb.append(String.format(" x   %+8.3f m\n",   pos.x));
                sb.append(String.format(" y   %+8.3f m\n",   pos.y));
                sb.append("----------------\n");
                sb.append(String.format(" vx  %+8.3f\n",     vel.x));
                sb.append(String.format(" vy  %+8.3f\n",     vel.y));
                sb.append(String.format("|v|  %8.3f m/s\n",  speed));
                sb.append("----------------\n");
                sb.append(String.format(" ax  %+8.3f\n",     accel.x));
                sb.append(String.format(" ay  %+8.3f\n",     accel.y));
                sb.append(String.format(" w   %+8.3f r/s\n", omega));
                sb.append("----------------\n");
                sb.append(String.format(" m   %8.3f kg\n",   mass));
                sb.append(String.format(" u   %8.3f\n",      mainObj.getFriction()));
                sb.append("----------------\n");
                sb.append(String.format(" KE  %8.3f J\n",    ke));
                sb.append(String.format(" PE  %8.3f J\n",    pe));
                sb.append(String.format(" E   %8.3f J",      ke + pe));
                physicsDataString = sb.toString();
            } else {
                physicsDataString = "";
            }
        }
        if (!physicsDataString.isEmpty()) {
            int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
            int panelH = viewport.getScreenHeight();
            int panelY = (uiViewport.getScreenHeight() - panelH) / 2;
            batch.setColor(0f, 0f, 0f, 0.6f);
            batch.draw(panelBgTexture, 0, panelY, panelW, panelH);
            batch.setColor(Color.WHITE);
            winFont.setColor(Color.CYAN);
            winFont.setUseIntegerPositions(true);
            winFont.setFixedWidthGlyphs("0123456789+-.,() ");
            winFont.draw(batch, physicsDataString, 8f, panelY + panelH - 20f);
        }

        // Key hints — above restart button
        {
            int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
            int btnY   = (uiViewport.getScreenHeight() - viewport.getScreenHeight()) / 2 + 8;
            winFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
            winFont.draw(batch, "[P]  show / hide graphs", 8f, btnY + 28 + 46);
            winFont.draw(batch, "[click]  object info",    8f, btnY + 28 + 30);
        }

        // Restart button — bottom of left panel
        {
            int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
            int btnY   = (uiViewport.getScreenHeight() - viewport.getScreenHeight()) / 2 + 8;
            int btnW   = panelW - 16;
            int btnH   = 28;
            batch.setColor(0.75f, 0.15f, 0.15f, 0.92f);
            batch.draw(panelBgTexture, 8, btnY, btnW, btnH);
            batch.setColor(Color.WHITE);
            winFont.setColor(Color.WHITE);
            GlyphLayout rl = new GlyphLayout(winFont, "RESTART  [R]");
            winFont.draw(batch, rl, 8 + (btnW - rl.width) / 2f, btnY + (btnH + rl.height) / 2f);
        }

        selectInfoTimer += delta;
        if(!showGraphs && selectedObject != null) {
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
                    builder.append(String.format("\nLinear Velocity: \n(%+7.3f, %+7.3f)", obj.getLinearVelocity().x, obj.getLinearVelocity().y) +
                        String.format("\nAngular Velocity: %+7.3f", obj.getAngularVelocity()) +
                        String.format("\nLinear Acceleration: \n(%+7.3f, %+7.3f)", ((DynamicObject) obj).getCurrentLinearAcceleration(delta).x, ((DynamicObject) obj).getCurrentLinearAcceleration(delta).y) +
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

        if (showGraphs) renderGraphOverlay();
        renderStaticObjectTooltip();
        renderCompletionOverlay();
    }

    // Draws a filled 5-pointed star centred at (cx, cy). Must be inside ShapeType.Filled.
    private void drawStar(float cx, float cy, float outerR, Color color) {
        shapeRenderer.setColor(color);
        float innerR = outerR * 0.42f;
        for (int i = 0; i < 5; i++) {
            float a1 = (float)(Math.PI / 2 + 2 * Math.PI * i       / 5);
            float ai = (float)(Math.PI / 2 + 2 * Math.PI * i       / 5 + Math.PI / 5);
            float a2 = (float)(Math.PI / 2 + 2 * Math.PI * (i + 1) / 5);
            shapeRenderer.triangle(
                cx + outerR * (float)Math.cos(a1), cy + outerR * (float)Math.sin(a1),
                cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                cx, cy);
            shapeRenderer.triangle(
                cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                cx + outerR * (float)Math.cos(a2), cy + outerR * (float)Math.sin(a2),
                cx, cy);
        }
    }

    private void renderCompletionOverlay() {
        if (!currentLevel.isComplete()) return;

        int sw = uiViewport.getScreenWidth();
        int sh = uiViewport.getScreenHeight();
        int vx = viewport.getScreenX();
        int vy = viewport.getScreenY();
        int vw = viewport.getScreenWidth();
        int vh = viewport.getScreenHeight();

        int pW = Math.min(vw - 40, 330);
        int pH = 230;
        int pX = vx + (vw - pW) / 2;
        int pY = vy + (vh - pH) / 2;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        // Filled pass: panel bg + stars
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.06f, 0.20f, 0.97f);
        shapeRenderer.rect(pX, pY, pW, pH);

        float starCY = pY + pH - 72f;
        for (int i = 0; i < 3; i++) {
            float sx = pX + pW / 2f + (i - 1) * 48f;
            Color sc = (i < finalStars) ? Color.GOLD : new Color(0.22f, 0.22f, 0.28f, 1f);
            drawStar(sx, starCY, 17f, sc);
        }
        shapeRenderer.end();

        // Line pass: border + dividers
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GOLD);
        shapeRenderer.rect(pX, pY, pW, pH);
        shapeRenderer.setColor(new Color(1f, 0.85f, 0f, 0.35f));
        shapeRenderer.rect(pX + 3, pY + 3, pW - 6, pH - 6);
        shapeRenderer.setColor(new Color(1f, 0.85f, 0f, 0.3f));
        shapeRenderer.line(pX + 14, pY + pH - 32, pX + pW - 14, pY + pH - 32);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Text pass
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Title with drop shadow
        winFont.getData().setScale(1.55f);
        GlyphLayout titleL = new GlyphLayout(winFont, "LEVEL  COMPLETE!");
        float tx = pX + (pW - titleL.width) / 2f;
        float ty = pY + pH - 10f;
        winFont.setColor(0f, 0f, 0f, 0.55f);
        winFont.draw(batch, "LEVEL  COMPLETE!", tx + 2, ty - 2);
        winFont.setColor(Color.GOLD);
        winFont.draw(batch, "LEVEL  COMPLETE!", tx, ty);
        winFont.getData().setScale(1f);

        // Stars label
        winFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        String starsText = finalStars == 3 ? "Perfect!" : finalStars == 2 ? "Good job!" : "Keep going!";
        GlyphLayout starsL = new GlyphLayout(winFont, starsText);
        winFont.draw(batch, starsL, pX + (pW - starsL.width) / 2f, pY + pH - 96f);

        // Score
        Color scoreCol = finalScore >= 95 ? Color.GOLD : finalScore >= 60 ? Color.GREEN : Color.WHITE;
        winFont.setColor(scoreCol);
        String scoreText = "Score:  " + finalScore + " / 100";
        GlyphLayout scoreL = new GlyphLayout(winFont, scoreText);
        winFont.draw(batch, scoreL, pX + (pW - scoreL.width) / 2f, pY + pH - 120f);

        // Objects used
        int freeObjs  = currentLevel.getFreeObjects();
        int extraObjs = Math.max(0, currentLevel.getNumDrawnObjects() - freeObjs);
        String objText = extraObjs == 0
            ? "Objects: " + currentLevel.getNumDrawnObjects() + "  (no penalty)"
            : "Objects: " + currentLevel.getNumDrawnObjects() + "  (+" + extraObjs + " over limit)";
        winFont.setColor(extraObjs == 0 ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.55f, 0.2f, 1f));
        GlyphLayout objL = new GlyphLayout(winFont, objText);
        winFont.draw(batch, objL, pX + (pW - objL.width) / 2f, pY + pH - 140f);

        // Time
        winFont.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        String timeText = String.format("Time:  %.1f s", levelTimer);
        GlyphLayout timeL = new GlyphLayout(winFont, timeText);
        winFont.draw(batch, timeL, pX + (pW - timeL.width) / 2f, pY + pH - 158f);

        // Restart hint
        winFont.setColor(new Color(0.48f, 0.48f, 0.48f, 1f));
        GlyphLayout hintL = new GlyphLayout(winFont, "[R]  Play Again");
        winFont.draw(batch, hintL, pX + (pW - hintL.width) / 2f, pY + 22f);

        winFont.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void dispose() {
        if (debugRenderer != null) debugRenderer.dispose();
        if (panelBgTexture != null) panelBgTexture.dispose();
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
            if(!(obj instanceof UncollidableObject)) {
                if(obj.isPointInsideObject(point)) {
                    return obj.getId();
                }
            }
        }

        return null;
    }

    private void resetLevel() {
        currentLevel.reset();
        drawTool.reset();
        gTime.clear();  gPosX.clear();  gPosY.clear();
        gVelX.clear();  gVelY.clear();  gSpeed.clear();
        gAccX.clear();  gAccY.clear();
        gKE.clear();    gPE.clear();    gTE.clear();
        levelTimer = 0f;
        physicsElapsedTime = 0f;
        accumulator = 0f;
        runPhysics = false;
        finalScore = -1;
        finalStars = 0;
        scoreCalculated = false;
        selectedObject = null;
        stringInfo = "";
        physicsDataString = "";
        physicsDataTimer = selectInfoPeriod;
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

    // 9 mini-graphs stacked in the right panel — game world stays fully visible.
    @SuppressWarnings("unchecked")
    private void renderGraphOverlay() {
        if (gTime.size() < 2) return;
        int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
        int rightX  = uiViewport.getScreenWidth() - panelW;
        int panelH  = viewport.getScreenHeight();
        int panelY  = (uiViewport.getScreenHeight() - panelH) / 2;
        if (panelW < 20) return;

        final int N = 9;
        final float PAD = 3f, GAP = 2f;
        float cellW = panelW - PAD * 2;
        float cellH = (panelH - PAD * 2 - GAP * (N - 1)) / N;

        String[] labels = { "X Pos (m)", "Y Pos (m)", "Speed m/s",
                            "Vel X",     "Vel Y",     "Acc Y m/s2",
                            "KE (J)",    "PE (J)",    "E_tot (J)" };
        List<Float>[] sets = new List[]{ gPosX, gPosY, gSpeed,
                                         gVelX, gVelY, gAccY,
                                         gKE,   gPE,   gTE };
        Color[] cols = {
            Color.CYAN,   Color.GREEN,  Color.YELLOW,
            Color.ORANGE, Color.CORAL,  Color.RED,
            Color.GOLD,   new Color(0.8f, 0.4f, 1f, 1f), Color.WHITE
        };

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0.08f, 0.95f);
        shapeRenderer.rect(rightX, panelY, panelW, panelH);
        for (int i = 0; i < N; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            shapeRenderer.setColor(0.04f, 0.04f, 0.16f, 1f);
            shapeRenderer.rect(rightX + PAD, cy, cellW, cellH);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < N; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            drawGraphLines(rightX + PAD, cy, cellW, cellH, sets[i], cols[i]);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        winFont.setUseIntegerPositions(true);
        for (int i = 0; i < N; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            List<Float> data = sets[i];
            if (data.isEmpty()) continue;
            float last = data.get(data.size() - 1);
            float vMin = last, vMax = last;
            for (float v : data) { if (v < vMin) vMin = v; if (v > vMax) vMax = v; }
            float tx = rightX + PAD + 2;
            winFont.setColor(cols[i]);
            winFont.draw(batch, labels[i], tx, cy + cellH - 1);
            winFont.setColor(Color.WHITE);
            winFont.draw(batch, String.format("%+.2f", last), tx, cy + cellH - 12);
            winFont.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
            winFont.draw(batch, String.format("%.1f", vMax), tx, cy + cellH * 0.72f);
            winFont.draw(batch, String.format("%.1f", vMin), tx, cy + 10f);
        }
        batch.end();
    }

    private void drawGraphLines(float cx, float cy, float gW, float gH,
                                List<Float> values, Color lineColor) {
        if (gTime.size() < 2 || values.size() < 2) return;
        float PT = gH * 0.30f, PB = gH * 0.14f;
        float px = cx + 3f, py = cy + PB;
        float pw = gW - 6f,  ph = gH - PB - PT;
        if (ph < 4f) return;

        float tMin = gTime.get(0), tMax = gTime.get(gTime.size() - 1);
        float vMin = Float.MAX_VALUE, vMax = -Float.MAX_VALUE;
        for (float v : values) { if (v < vMin) vMin = v; if (v > vMax) vMax = v; }
        float tRange = Math.max(tMax - tMin, 0.001f);
        float vRange = vMax - vMin;
        if (vRange < 0.001f) { vMin -= 0.5f; vMax += 0.5f; vRange = 1f; }

        shapeRenderer.setColor(0.15f, 0.15f, 0.28f, 1f);
        for (int k = 1; k < 4; k++) shapeRenderer.line(px, py + ph * k / 4f, px + pw, py + ph * k / 4f);

        if (vMin < 0 && vMax > 0) {
            shapeRenderer.setColor(0.45f, 0.45f, 0.55f, 1f);
            float zy = py + ph * (-vMin / vRange);
            shapeRenderer.line(px, zy, px + pw, zy);
        }

        shapeRenderer.setColor(0.4f, 0.4f, 0.52f, 1f);
        shapeRenderer.line(px, py, px + pw, py);
        shapeRenderer.line(px, py, px, py + ph);

        shapeRenderer.setColor(lineColor);
        int n = Math.min(gTime.size(), values.size());
        for (int i = 1; i < n; i++) {
            float x1 = px + pw * ((gTime.get(i - 1) - tMin) / tRange);
            float y1 = py + ph * ((values.get(i - 1) - vMin) / vRange);
            float x2 = px + pw * ((gTime.get(i)     - tMin) / tRange);
            float y2 = py + ph * ((values.get(i)     - vMin) / vRange);
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }

    private void renderStaticObjectTooltip() {
        if (selectedObject == null) return;
        PhysicsObject obj = null;
        for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
            if (o.getId() == selectedObject) { obj = o; break; }
        }
        if (!(obj instanceof StaticObject)) return;

        String tip = String.format("Static Object  #%d\nFriction:    %.3f\nRestitution: %.3f\nDensity:     %.3f",
            obj.getId(), obj.getFriction(), obj.getRestitution(), obj.getDensity());
        GlyphLayout gl = new GlyphLayout(winFont, tip);
        float bw = gl.width + 14f, bh = gl.height + 12f;
        float tx = Math.min(lastClickPos.x + 14, uiViewport.getScreenWidth() - bw - 6);
        float ty = Math.min(lastClickPos.y + bh + 20, uiViewport.getScreenHeight() - 6f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.08f, 0f, 0.88f);
        shapeRenderer.rect(tx - 4, ty - bh - 2, bw, bh);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(tx - 4, ty - bh - 2, bw, bh);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        winFont.setColor(Color.YELLOW);
        winFont.draw(batch, tip, tx, ty - 4);
        batch.end();
    }
}
