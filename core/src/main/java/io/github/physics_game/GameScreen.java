package io.github.physics_game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.levels.Level;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.List;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GameScreen extends ScreenAdapter {
    private final MainGame game;

    private GraphicOverlayRenderer graphicOverlayRenderer;
    private GameOverlayRenderer gameOverlayRenderer;
    private GameSidebarRenderer gameSidebarRenderer;
    private WorldRenderHelper worldRenderHelper;

    private DrawType drawType = DrawType.POSITIVE;
    private SpriteBatch batch;
    FitViewport viewport;

    OrthographicCamera camera;
    private OrthographicCamera uiCamera;
    private ScreenViewport uiViewport;
    public static float accumulator = 0f;
    BitmapFont winFont;

    private int finalScore = -1;
    private int finalStars = 0;
    private Integer selectedObject = null;
    private boolean scoreCalculated = false;

    public static final float viewPortWidth = 40f;
    public static final float viewPortHeight = 30f;
    public static int SIDE_BUFFER_PX = 250;
    public static int TOP_BUFFER_PX = 10;
    public static int BOTTOM_BUFFER_PX = 10;
    public static int BUTTON_WIDTH = 240;
    public static int BUTTON_HEIGHT = 120;
    public static final float selectInfoPeriod = 0.1f;

    public GameScreen(MainGame game, Level selectedLevel) {
        this.game = game;
        this.currentLevel = selectedLevel;
    }

    // throttle logging to once-per-second
    private float physicsDataTimer = selectInfoPeriod;
    private String physicsDataString = "";
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
    private final Level currentLevel;
    private DrawTool drawTool;
    private Texture panelBgTexture;
    private Texture texFull;
    private Texture texSelect;
    private Texture texWhite;

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
        winFont.getData().markupEnabled = true;
        winFont.setColor(Color.WHITE);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        panelBgTexture = new Texture(pm);
        pm.dispose();
        texFull   = new Texture(Gdx.files.internal("full_region.png"));
        texSelect = new Texture(Gdx.files.internal("select_region.png"));
        texWhite  = new Texture(Gdx.files.internal("white_region.png"));

        drawType = currentLevel.getDrawTypes().get(currentLevel.getSelectedPaint());

        drawTool = new DrawTool(0.5f);

        // Initialize graphical rendering including shapes, camera, viewport
        graphicOverlayRenderer = new GraphicOverlayRenderer(
            shapeRenderer,
            batch,
            winFont,
            uiCamera,
            uiViewport,
            viewport
        );

        gameOverlayRenderer = new GameOverlayRenderer(
            shapeRenderer,
            batch,
            winFont,
            uiCamera,
            uiViewport,
            viewport
        );

        gameSidebarRenderer = new GameSidebarRenderer(
            batch,
            winFont,
            uiViewport,
            viewport,
            panelBgTexture,
            texFull,
            texSelect,
            texWhite
        );

        worldRenderHelper = new WorldRenderHelper(shapeRenderer);

        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }

    private void handleInput() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = uiViewport.getScreenHeight() - Gdx.input.getY() - 1;

            Integer clickedPaint = gameSidebarRenderer.getClickedPaintIndex(currentLevel, mx, my);
            if (clickedPaint != null) {
                currentLevel.setSelectedPaint(clickedPaint);
                drawType = currentLevel.getDrawTypes().get(clickedPaint);
            } else {
                GameSidebarRenderer.SidebarAction action = gameSidebarRenderer.getSidebarAction(mx, my);
                if (action == GameSidebarRenderer.SidebarAction.GO_TO_LEVELS) {
                    game.setScreen(new LevelScreen(game));
                } else if (action == GameSidebarRenderer.SidebarAction.RESTART) {
                    resetLevel();
                    graphicOverlayRenderer.resetEnergyScale();
                } else if (mx < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2
                    && mx > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2 && !currentLevel.isComplete()) {
                    Integer i = isPointInsideObjects(viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
                    if (i != null) {
                        selectedObject = i;
                    } else {
                        Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                        drawTool.update(drawType, currentLevel, 1, worldPos.x, worldPos.y);
                    }
                }
            }
        } else if (Gdx.input.getX() < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2
            && Gdx.input.getX() > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2 && !currentLevel.isComplete()) {
            Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            drawTool.update(drawType, currentLevel, Gdx.input.isButtonPressed(Input.Buttons.LEFT) ? 2 : 0, worldPos.x, worldPos.y);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("Main", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            PhysicsObject lastObj = currentLevel.getPhysicsObjects().get(currentLevel.getPhysicsObjects().size() - 1);
            PhysicsResolver.printShape(lastObj.getVertices());
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentLevel.setRunPhysics(!currentLevel.getRunPhysics());
            Gdx.app.log("Main", "Physics " + (currentLevel.getRunPhysics() ? "RUNNING" : "PAUSED"));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Gdx.app.log("Main", "ticking 1 time step");
            accumulator = 2f / 60f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            drawType = (drawType == DrawType.POSITIVE) ? DrawType.NEGATIVE : DrawType.POSITIVE;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            currentLevel.reinitialize();
            drawTool.reset();
            resetLevel();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            showGraphs = !showGraphs;
        }
    }

    private void updateSimulation(float delta) {
        if (currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
            accumulator += Math.min(delta, 0.25f);
        } else {
            accumulator = 0.0f;
        }

        if (currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
            currentLevel.setLevelTimer(currentLevel.getLevelTimer() + delta);
        }

        if (currentLevel.isComplete() && !scoreCalculated) {
            finalScore = ScoreCalculator.calculateScore(
                currentLevel.getCurrentDrawnProportion(),
                currentLevel.getLevelTimer(),
                currentLevel.getFreeProp()
            );
            finalStars = ScoreCalculator.calculateStars(finalScore);
            scoreCalculated = true;

            int levelId = currentLevel.getLevelId();
            if (game.currentScores.get(levelId) == null) {
                game.currentScores.set(levelId, new ScoreLevel());
            }
            game.currentScores.get(levelId).setNewBestScore(
                currentLevel.getLevelTimer(),
                currentLevel.getCurrentDrawnProportion(),
                currentLevel.getFreeProp()
            );

            Gdx.app.log("Main", "Level complete!");
            Gdx.app.log("Main", "Score = " + finalScore + ", Stars = " + finalStars);
        }
    }

    private ArrayList<DebugForce> renderWorld() {
        ScreenUtils.clear(Color.GRAY);
        viewport.apply();
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Texture txtBackground = currentLevel.getBackground();
        if (txtBackground != null) {
            batch.setColor(new Color(0.8f, 0.8f, 0.8f, 1.0f));
            batch.draw(txtBackground, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        } else {
            Gdx.app.log("Main", "Background texture is null!");
        }
        batch.end();

        ArrayList<DebugForce> forces;
        forces = PhysicsResolver.stepWithDebug(currentLevel.getPhysicsObjects());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!showDebugOverlay) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (!(obj instanceof UncollidableObject) || obj instanceof NoDrawField) {
                    worldRenderHelper.drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), obj.getColor());
                }
            }
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (selectedObject != null && obj.getId() == selectedObject) {
                    worldRenderHelper.drawOutline(obj.getLocalBody(), Color.BLUE);
                }
            }
            shapeRenderer.end();
        } else {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (obj instanceof DynamicTriggerObject) {
                    worldRenderHelper.drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.GOLD);
                } else if (obj instanceof DynamicObject) {
                    worldRenderHelper.drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.YELLOW);
                } else if (obj instanceof StaticObject) {
                    worldRenderHelper.drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.CYAN);
                } else if (obj instanceof UncollidableObject) {
                    worldRenderHelper.drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.RED);
                }
                worldRenderHelper.drawOutline(obj.getLocalBody(), Color.WHITE);
            }
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (DebugForce f : forces) {
            if (f.getType() == DebugForce.Type.GRAVITY || f.getType() == DebugForce.Type.VELOCITY) {
                worldRenderHelper.drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
            }
        }
        shapeRenderer.end();

        if (currentLevel.getRunPhysics()) {
            currentLevel.tick(Gdx.graphics.getDeltaTime());
        }

        return forces;
    }

    private void renderUiPanels() {
        int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;

        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(panelBgTexture, uiViewport.getScreenWidth() - panelW, 0, panelW + 20f, uiViewport.getScreenHeight());
        batch.setColor(Color.WHITE);

        gameSidebarRenderer.render(
            currentLevel,
            game,
            physicsDataString
        );

        batch.end();

        graphicOverlayRenderer.render(
            gTime,
            gPosX,
            gPosY,
            gSpeed,
            gVelX,
            gVelY,
            gAccY,
            gKE,
            gPE,
            gTE
        );

        graphicOverlayRenderer.renderHelpTooltip();

        gameOverlayRenderer.renderStaticObjectTooltip(
            currentLevel,
            selectedObject,
            lastClickPos
        );

        gameOverlayRenderer.renderCompletionOverlay(
            currentLevel.isComplete(),
            finalScore,
            finalStars,
            currentLevel.getLevelTimer(),
            currentLevel.getCurrentDrawnProportion(),
            currentLevel.getFreeProp()
        );
    }

    private void updatePhysicsData(float delta) {
        physicsDataTimer += delta;

        if (physicsDataTimer < selectInfoPeriod || currentLevel.isComplete()) {
            return;
        }

        float temp = physicsDataTimer;
        while (physicsDataTimer >= selectInfoPeriod) {
            physicsDataTimer -= selectInfoPeriod;
        }

        // Find the primary object (first DynamicObject) for graph recording
        DynamicObject mainObj = null;
        for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
            if (o instanceof DynamicObject) {
                mainObj = (DynamicObject) o;
                break;
            }
        }

        // Save accel here so the left panel can reuse it after the reset below
        Vector2 mainAccel = new Vector2();

        // Record graph data from primary object whenever physics runs
        if (mainObj != null && currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
            Vector2 pos = mainObj.getPosition();
            Vector2 vel = mainObj.getLinearVelocity();
            float mass = mainObj.getMass();
            float inertia = mainObj.getInertia();
            float omega = mainObj.getAngularVelocity();
            float ke = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
            float pe = mass * 9.8f * pos.y;
            Vector2 accel = mainObj.getCurrentLinearAcceleration(temp);

            mainAccel.set(accel);
            mainObj.setCurrentLinearAcceleration(new Vector2());
            mainObj.setCurrentAngularAcceleration(0f);

            gTime.add(currentLevel.getLevelTimer());
            gPosX.add(pos.x); gPosY.add(pos.y);
            gVelX.add(vel.x); gVelY.add(vel.y);
            gSpeed.add(vel.len());
            gAccX.add(accel.x); gAccY.add(accel.y);
            gKE.add(ke); gPE.add(pe); gTE.add(ke + pe);

            while (gTime.size() > MAX_GRAPH_SAMPLES) {
                gTime.remove(0); gPosX.remove(0); gPosY.remove(0);
                gVelX.remove(0); gVelY.remove(0); gSpeed.remove(0);
                gAccX.remove(0); gAccY.remove(0);
                gKE.remove(0); gPE.remove(0); gTE.remove(0);
            }
        }

        // Build data-table string from primary object (or selected object if clicked)
        PhysicsObject displayObj = null;
        if (selectedObject != null) {
            for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                if (o.getId() == selectedObject) {
                    displayObj = o;
                    break;
                }
            }
        }
        if (displayObj == null) displayObj = mainObj;

        if (displayObj != null) {
            physicsDataString = buildPhysicsDataString(displayObj, mainObj, mainAccel, temp);
        } else {
            physicsDataString = "";
        }
    }

    private String buildPhysicsDataString(
        PhysicsObject displayObj,
        DynamicObject mainObj,
        Vector2 mainAccel,
        float temp
    ) {
        Vector2 pos = displayObj.getPosition();
        Vector2 vel = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getLinearVelocity() : new Vector2();
        float speed = vel.len();
        float mass = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getMass() : 0f;
        float inertia = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getInertia() : 0f;
        float omega = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getAngularVelocity() : 0f;
        float ke = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
        float pe = mass * 9.8f * pos.y;
        Vector2 accel = (displayObj == mainObj) ? mainAccel
            : (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getCurrentLinearAcceleration(temp)
            : new Vector2();

        StringBuilder sb = new StringBuilder();

        if (displayObj instanceof Charged) {
            Charged chargedObj = (Charged) displayObj;
            float density = chargedObj.getChargeDensity();
            float q = density * mass;
            float feX = mass * accel.x;
            float feY = mass * (accel.y + 9.8f);

            // COULOMB'S LAW
            sb.append("[GOLD]-- COULOMB'S LAW --[]\n");
            sb.append("[CYAN] F = k * q1*q2 / r^2[]\n");
            sb.append("[LIGHT_GRAY] k = 8.99x10^9 N*m^2/C^2[]\n\n");

            // CHARGE
            sb.append("[GOLD]-- CHARGE --[]\n");
            sb.append("[CYAN] q = density * mass[]\n");
            sb.append(String.format("[WHITE] density = %.2f C/kg[]\n", density));
            sb.append(String.format("[WHITE] q = %.2f C[]\n\n", q));

            // ELECTRIC FORCE
            sb.append("[GOLD]-- ELECTRIC FORCE --[]\n");
            sb.append("[CYAN] F_e = F_total - F_gravity[]\n");
            sb.append(String.format("[WHITE] F_ex = %.2f N[]\n", feX));
            sb.append(String.format("[WHITE] F_ey = %.2f N[]\n\n", feY));

            // KINEMATICS
            sb.append("[GOLD]-- KINEMATICS --[]\n");
            sb.append("[CYAN] v = sqrt(vx^2 + vy^2)[]\n");
            sb.append(String.format("[WHITE]   = %.2f m/s[]\n", speed));
            sb.append(String.format("[LIGHT_GRAY] vx=%+.2f  vy=%+.2f[]\n\n", vel.x, vel.y));

            // ENERGY
            sb.append("[GOLD]-- ENERGY --[]\n");
            sb.append("[CYAN] KE = (1/2) * m * v^2[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n", ke));
            sb.append("[CYAN] PE = m * g * h[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n", pe));
            sb.append("[CYAN] E  = KE + PE[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n", ke + pe));
        } else {
            // KINEMATICS
            sb.append("[GOLD]-- KINEMATICS --[]\n");
            sb.append("[CYAN] v = sqrt(vx^2 + vy^2)[]\n");
            sb.append(String.format("[WHITE]   = %.2f m/s[]\n", speed));
            sb.append(String.format("[LIGHT_GRAY] vx=%+.2f  vy=%+.2f[]\n", vel.x, vel.y));
            sb.append(String.format("[LIGHT_GRAY] ax=%+.2f  ay=%+.2f[]\n\n", accel.x, accel.y));

            // NEWTON'S 2ND LAW
            sb.append("[GOLD]-- NEWTON'S 2ND LAW --[]\n");
            sb.append("[CYAN] F = m * a[]\n");
            sb.append(String.format("[WHITE] m  = %.3f kg[]\n", mass));
            sb.append("[CYAN] Weight = m * g[]\n");
            sb.append(String.format("[WHITE] Fg = %.3f N[]\n", mass * 9.8f));
            sb.append(String.format("[LIGHT_GRAY] (g = 9.8 m/s^2)[]\n"));
            sb.append(String.format("[WHITE] Friction: u = %.2f[]\n\n", displayObj.getFriction()));

            // ENERGY
            sb.append("[GOLD]-- ENERGY --[]\n");
            sb.append("[CYAN] KE = (1/2) * m * v^2[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n", ke));
            sb.append("[CYAN] PE = m * g * h[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n", pe));
            sb.append("[CYAN] E  = KE + PE[]\n");
            sb.append(String.format("[WHITE]    = %.3f J[]\n\n", ke + pe));

            // ROTATION
            sb.append("[GOLD]-- ROTATION --[]\n");
            sb.append("[CYAN] KE_rot = (1/2)*I*w^2[]\n");
            sb.append(String.format("[WHITE] w = %.3f rad/s[]\n", omega));
            sb.append(String.format("[WHITE] I = %.3f kg*m^2[]\n", inertia));
            sb.append(String.format("[WHITE] KE_rot = %.3f J[]\n", 0.5f * inertia * omega * omega));
        }

        return sb.toString();
    }

    @Override
    public void render(float delta) {
        // Shared layout values which are computed once, used in both click handler and drawing
        int panelW  = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
        int panelH  = viewport.getScreenHeight();
        int panelY  = (uiViewport.getScreenHeight() - panelH) / 2;

        // Handle user inputs like clicks or shortcuts
        handleInput();

        // Update time/physics simulation
        updateSimulation(delta);

        // Render world and shapes
        renderWorld();

        // Render panels
        renderUiPanels();

        // Update physics calculations and metrics
        updatePhysicsData(delta);
    }

    @Override
    public void dispose() {
        if (panelBgTexture != null) panelBgTexture.dispose();
        if (texFull != null)        texFull.dispose();
        if (texSelect != null)      texSelect.dispose();
        if (texWhite != null)       texWhite.dispose();
        if (batch != null)          batch.dispose();
        if (shapeRenderer != null)  shapeRenderer.dispose();
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
            // place it in the center region of the window
            viewport.setScreenBounds(worldX, worldY, worldW, worldH);
        }

        if (uiViewport != null) {
            uiViewport.update(width, height, true);
        }
    }

    public Integer isPointInsideObjects(Vector2 point) {
        for(PhysicsObject obj : currentLevel.getPhysicsObjects()) {
            if(!(obj instanceof UncollidableObject) && obj.getId() < 1000) {
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
        graphicOverlayRenderer.resetEnergyScale();
        gTime.clear();  gPosX.clear();  gPosY.clear();
        gVelX.clear();  gVelY.clear();  gSpeed.clear();
        gAccX.clear();  gAccY.clear();
        gKE.clear();    gPE.clear();    gTE.clear();
        accumulator = 0f;
        finalScore = -1;
        finalStars = 0;
        scoreCalculated = false;
        selectedObject = null;
        physicsDataString = "";
        physicsDataTimer = selectInfoPeriod;
    }
}
