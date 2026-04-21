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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.collision.CustomContactHandler;
import io.github.physics_game.levels.Level;
import io.github.physics_game.object_types.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GameScreen extends ScreenAdapter {
    private final MainGame game;

    // Initialize viewport and camera
    private DrawType drawType = DrawType.POSITIVE;
    private SpriteBatch batch;
    FitViewport viewport;
    OrthographicCamera camera;
    Box2DDebugRenderer debugRenderer;
    private OrthographicCamera uiCamera;
    private ScreenViewport uiViewport;
    public static float accumulator = 0f;
    BitmapFont winFont;

    // Score calculation
    private int finalScore = -1;
    private int finalStars = 0;
    private Integer selectedObject = null;
    private boolean scoreCalculated = false;

    // UI Sizing parameters
    public static final float viewPortWidth = 40f;
    public static final float viewPortHeight = 30f;
    public static int SIDE_BUFFER_PX = 250;
    public static int TOP_BUFFER_PX = 10;
    public static int BOTTOM_BUFFER_PX = 10;
    public static int BUTTON_WIDTH = 200;
    public static int BUTTON_HEIGHT = 64;
    public static final float selectInfoPeriod = 0.1f;

    public GameScreen(MainGame game, Level selectedLevel) {
        this.game = game;
        this.currentLevel = selectedLevel;
    }

    // throttle logging to once-per-second
    float logTimer = 0f;
    private float selectInfoTimer = selectInfoPeriod;
    private float physicsDataTimer = selectInfoPeriod;
    private String physicsDataString = "";
    private final Vector2 lastClickPos = new Vector2();

    // Texture loading
    private Texture fullRegionTexture;
    private Texture selectRegionTexture;
    private Texture whiteRegionTexture;

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
    private Level currentLevel;
    private DrawTool drawTool;
    private Texture panelBgTexture;

    // Help charts for physics explanation
    private final ArrayList<ChartHelpTarget> chartHelpTargets = new ArrayList<>();
    private ChartHelpTarget hoveredChartHelp = null;

    private static class ChartHelpTarget {
        String key;
        Rectangle iconBounds;
        Rectangle groupBounds;

        ChartHelpTarget(String key, Rectangle iconBounds, Rectangle groupBounds) {
            this.key = key;
            this.iconBounds = iconBounds;
            this.groupBounds = groupBounds;
        }
    }

    private void updateChartHelpHover() {
        hoveredChartHelp = null;

        float mx = Gdx.input.getX();
        float my = uiViewport.getScreenHeight() - Gdx.input.getY() - 1;

        for (ChartHelpTarget target : chartHelpTargets) {
            if (target.iconBounds.contains(mx, my)) {
                hoveredChartHelp = target;
                break;
            }
        }
    }

    // Render the chart tooltips on the right hand side of the game screen
    private void renderChartHelpTooltip() {
        if (hoveredChartHelp == null) return;

        String text = getChartHelpText(hoveredChartHelp.key);
        if (text == null || text.isEmpty()) return;

        GlyphLayout layout = new GlyphLayout(winFont, text);
        float pad = 10f;
        float boxW = Math.min(layout.width + pad * 2f, 280f);
        float boxH = layout.height + pad * 2f;

        float x = hoveredChartHelp.iconBounds.x - boxW - 8f;
        float y = hoveredChartHelp.iconBounds.y + boxH;

        if (x < 8f) {
            x = hoveredChartHelp.iconBounds.x + hoveredChartHelp.iconBounds.width + 8f;
        }
        if (y > uiViewport.getScreenHeight() - 8f) {
            y = uiViewport.getScreenHeight() - 8f;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.08f, 0.16f, 0.95f);
        shapeRenderer.rect(x, y - boxH, boxW, boxH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(0.45f, 0.75f, 1f, 1f));
        shapeRenderer.rect(x, y - boxH, boxW, boxH);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        winFont.setColor(Color.WHITE);
        winFont.draw(batch, text, x + pad, y - pad);
        batch.end();
    }

    // Formulas and Explanations of each chart displayed
    private String getChartHelpText(String key) {
        switch (key) {
            case "position":
                return "Position\nExplanation coming later.";
            case "velocity":
                return "Velocity\nExplanation coming later.";
            case "speed":
                return "Speed\nExplanation coming later.";
            case "acceleration":
                return "Acceleration\nExplanation coming later.";
            case "energy":
                return "Energy\nExplanation coming later.";
            default:
                return "";
        }
    }

    @Override
    public void show() {
        // Camera and viewport placement
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

        fullRegionTexture = new Texture("full_region.png");
        selectRegionTexture = new Texture("select_region.png");
        whiteRegionTexture = new Texture("white_region.png");

        drawType = currentLevel.getDrawTypes().get(currentLevel.getSelectedPaint());

        drawTool = new DrawTool(0.5f);

        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }

    @Override
    public void render(float delta) {
        boolean isSelecting = false;

        // Shared layout values
        int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
        int panelH = viewport.getScreenHeight();
        int panelY = (uiViewport.getScreenHeight() - panelH) / 2;

        int restartBtnH = 34;
        float restartBtnY = panelY + 12f;
        float shortcutsBaseY = restartBtnY + restartBtnH + 16f;

        int sidebarBtnW = panelW - 16;
        int paintGap = 6;

        // Input handling
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = uiViewport.getScreenHeight() - Gdx.input.getY() - 1;
            lastClickPos.set(mx, my);

            // Approximate paint button click region from bottom layout
            int numPaintBtns = currentLevel.getDrawTypes().size();
            float shortcutTitleY = shortcutsBaseY + 86f;
            float paintBtnsBaseYForInput = shortcutTitleY + 20f;
            int totalPaintH = numPaintBtns * BUTTON_HEIGHT + Math.max(0, numPaintBtns - 1) * paintGap;

            if (mx >= 0 && mx < panelW && my >= paintBtnsBaseYForInput && my < paintBtnsBaseYForInput + totalPaintH) {
                int relY = (int) (my - paintBtnsBaseYForInput);
                int slotH = BUTTON_HEIGHT + paintGap;
                int slot = relY / slotH;
                if (slot < numPaintBtns && relY % slotH < BUTTON_HEIGHT) {
                    currentLevel.setSelectedPaint(slot);
                    drawType = currentLevel.getDrawTypes().get(slot);
                }
            } else if (mx >= 8 && mx <= 8 + sidebarBtnW && my >= restartBtnY && my <= restartBtnY + restartBtnH) {
                resetLevel();
                isSelecting = true;
            } else if (mx < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2
                && mx > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2) {
                Integer i = isPointInsideObjects(viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
                if (i != null) {
                    selectedObject = i;
                    isSelecting = true;
                } else {
                    Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                    drawTool.update(drawType, currentLevel, 1, worldPos.x, worldPos.y);
                }
            }
        } else if (Gdx.input.getX() < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2
            && Gdx.input.getX() > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2) {
            Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            drawTool.update(drawType, currentLevel, Gdx.input.isButtonPressed(Input.Buttons.LEFT) ? 2 : 0, worldPos.x, worldPos.y);
        }

        if (currentLevel.getRunPhysics()) accumulator += Math.min(delta, 0.25f);
        else accumulator = 0.0f;

        logTimer += delta;
        if (currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
            currentLevel.setLevelTimer(currentLevel.getLevelTimer() + delta);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            showDebugOverlay = !showDebugOverlay;
            Gdx.app.log("Main", "Debug overlay " + (showDebugOverlay ? "ON" : "OFF"));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            PhysicsObject lastObj = currentLevel.getPhysicsObjects().get(currentLevel.getPhysicsObjects().size() - 1);
            drawTool.testAddPoint(true);
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
            resetLevel();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            showGraphs = !showGraphs;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }

        // Clear + background
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

        ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(currentLevel.getPhysicsObjects());

        if (currentLevel.isComplete()) {
            if (!scoreCalculated) {
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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (!showDebugOverlay) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (!(obj instanceof UncollidableObject) || obj instanceof NoDrawField) {
                    drawEarTriangles(obj.getLocalBody(), obj.getConcaveLocalTriangles(), obj.getColor());
                }
            }
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (selectedObject != null && obj.getId() == selectedObject) {
                    drawOutline(obj.getLocalBody(), Color.BLUE);
                }
            }
            shapeRenderer.end();
        } else {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            for (PhysicsObject obj : currentLevel.getPhysicsObjects()) {
                if (obj instanceof DynamicTriggerObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.GOLD);
                } else if (obj instanceof DynamicObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.YELLOW);
                } else if (obj instanceof StaticObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.CYAN);
                } else if (obj instanceof UncollidableObject) {
                    drawPolygons(obj.getLocalBody(), obj.getConcaveLocalBest(), Color.RED);
                }
                drawOutline(obj.getLocalBody(), Color.WHITE);
            }
            shapeRenderer.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (DebugForce f : forces) {
            if (f.getType() == DebugForce.Type.GRAVITY || f.getType() == DebugForce.Type.VELOCITY) {
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
            }
        }
        shapeRenderer.end();

        if (currentLevel.getRunPhysics()) {
            currentLevel.tick(delta);
        }

        // UI
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Panel backgrounds
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(panelBgTexture, uiViewport.getScreenWidth() - panelW, 0, panelW + 20f, uiViewport.getScreenHeight());
        batch.draw(panelBgTexture, 0, 0, panelW, uiViewport.getScreenHeight());
        batch.setColor(Color.WHITE);

        // ── LEFT PANEL ────────────────────────────────────────────────
        float lx = 14f;
        float topY = panelY + panelH - 10f;

        winFont.getData().setScale(0.9f);
        winFont.setColor(new Color(0.7f, 0.85f, 1f, 1f));
        winFont.setUseIntegerPositions(true);

        String timeStr = String.format("%d:%02d.%d",
            (int) (currentLevel.getLevelTimer() / 60f),
            (int) (currentLevel.getLevelTimer()) % 60,
            ((int) (currentLevel.getLevelTimer() * 10)) % 10);

        String levelHeaderStr;
        if (game.currentScores.get(currentLevel.getLevelId()) != null) {
            String bestTimeStr = String.format("%d:%02d.%d",
                (int) (game.currentScores.get(currentLevel.getLevelId()).getBestTime() / 60f),
                (int) (game.currentScores.get(currentLevel.getLevelId()).getBestTime()) % 60,
                ((int) (game.currentScores.get(currentLevel.getLevelId()).getBestTime() * 10)) % 10);

            levelHeaderStr = currentLevel.getLevelName() + "\nTime: " + timeStr
                + "\nBest Time: " + bestTimeStr
                + "\nBest Drawn proportion: " + String.format("%.0f%%", game.currentScores.get(currentLevel.getLevelId()).getBestShapeProportion() * 100f)
                + "\nScore: " + ScoreCalculator.calculateScore(
                game.currentScores.get(currentLevel.getLevelId()).getBestShapeProportion(),
                game.currentScores.get(currentLevel.getLevelId()).getBestTime(),
                currentLevel.getFreeProp()
            ) + "  [" + ScoreCalculator.calculateStars(
                ScoreCalculator.calculateScore(
                    game.currentScores.get(currentLevel.getLevelId()).getBestShapeProportion(),
                    game.currentScores.get(currentLevel.getLevelId()).getBestTime(),
                    currentLevel.getFreeProp()
                )
            ) + "★]"
                + "\nDescription:\n" + currentLevel.getDescription();
        } else {
            levelHeaderStr = currentLevel.getLevelName() + "\nTime: " + timeStr
                + "\nBest Time: --"
                + "\nBest Drawn proportion: --"
                + "\nDescription:\n" + currentLevel.getDescription();
        }

        GlyphLayout glHeader = new GlyphLayout(winFont, levelHeaderStr);
        winFont.draw(batch, glHeader, lx, topY);
        winFont.getData().setScale(1f);

        float sepY = topY - glHeader.height - 4f;
        batch.setColor(0.3f, 0.5f, 0.8f, 0.4f);
        batch.draw(panelBgTexture, lx, sepY, panelW - 16f, 1f);
        batch.setColor(Color.WHITE);

        // Physics data table
        winFont.setColor(Color.CYAN);
        winFont.setFixedWidthGlyphs("0123456789+-.,() ");
        GlyphLayout physicsLayout = new GlyphLayout();
        if (!physicsDataString.isEmpty()) {
            physicsLayout.setText(winFont, physicsDataString);
            winFont.draw(batch, physicsDataString, lx, sepY - 6f);
        }
        winFont.setFixedWidthGlyphs("");

        // Dynamic layout for lower sidebar
        float topContentBottomY = sepY - 6f - physicsLayout.height;

        float shortcutTitleY = shortcutsBaseY + 86f;
        float shortcutsTopY = shortcutTitleY;

        float paintBtnsBaseY = topContentBottomY - 100f;
        float minPaintBtnsBaseY = shortcutsTopY - 250f;
        if (paintBtnsBaseY < minPaintBtnsBaseY) {
            paintBtnsBaseY = minPaintBtnsBaseY;
        }

        // Paint selection buttons
        float trackY = 0f;
        for (int i = 0; i < MainGame.drawTypeButtons.size(); i++) {
            if (currentLevel.getDrawTypes().contains(MainGame.drawTypeButtons.get(i).drawType)) {
                int ind = currentLevel.getDrawTypes().indexOf(MainGame.drawTypeButtons.get(i).drawType);

                float btnBotY = paintBtnsBaseY + trackY;

                int rectStartX = 24;
                int rectStartY = 18;
                int rectTotalWidth = 145;
                int rectTotalHeight = 22;

                float amountNow = currentLevel.getCurrentDrawnAmounts().get(ind);
                float amountMax = currentLevel.getDrawAmounts().get(ind);
                float leftWidth = 0f;
                if (amountMax > 0.0001f) {
                    leftWidth = rectTotalWidth * amountNow / amountMax;
                }

                boolean full = (amountMax - amountNow) < 0.001f;

                Texture barTex;
                Texture btnTex;
                if (currentLevel.getSelectedPaint() == ind) {
                    barTex = full ? fullRegionTexture : selectRegionTexture;
                    btnTex = MainGame.drawTypeButtons.get(i).onTex;
                } else {
                    barTex = full ? fullRegionTexture : whiteRegionTexture;
                    btnTex = MainGame.drawTypeButtons.get(i).offTex;
                }

                batch.draw(barTex, lx + rectStartX, btnBotY + rectStartY - 60f, leftWidth, rectTotalHeight);
                batch.draw(btnTex, lx, btnBotY -60f, BUTTON_WIDTH, BUTTON_HEIGHT);

                trackY += BUTTON_HEIGHT + paintGap;
            }
        }

        // Shortcuts block
        winFont.setColor(new Color(0.78f, 0.78f, 0.78f, 1f));
        winFont.draw(batch, "Shortcuts", lx, shortcutTitleY);

        winFont.setColor(new Color(0.62f, 0.62f, 0.62f, 1f));
        winFont.draw(batch, "[Esc] back", lx, shortcutTitleY - 20f);
        winFont.draw(batch, "[R] restart", lx, shortcutTitleY - 38f);
        winFont.draw(batch, "[Space] play/pause", lx, shortcutTitleY - 56f);
        winFont.draw(batch, "[P] graphs", lx, shortcutTitleY - 74f);
        winFont.draw(batch, "[Click] object info", lx, shortcutTitleY - 92f);

        // Restart button
        batch.setColor(0.75f, 0.15f, 0.15f, 0.92f);
        batch.draw(panelBgTexture, 8f, restartBtnY, sidebarBtnW, restartBtnH);
        batch.setColor(Color.WHITE);

        winFont.setColor(Color.WHITE);
        GlyphLayout rl = new GlyphLayout(winFont, "RESTART  [R]");
        winFont.draw(batch, rl,
            8f + (sidebarBtnW - rl.width) / 2f,
            restartBtnY + (restartBtnH + rl.height) / 2f
        );

        batch.end();

        // Right panel graphs
        renderGraphOverlay();

        // Draw tooltip on top of everything
        renderChartHelpTooltip();

        // Physics data tracking
        physicsDataTimer += delta;
        if (physicsDataTimer >= selectInfoPeriod) {
            float temp = physicsDataTimer;
            while (physicsDataTimer >= selectInfoPeriod) physicsDataTimer -= selectInfoPeriod;

            DynamicObject mainObj = null;
            for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                if (o instanceof DynamicObject) {
                    mainObj = (DynamicObject) o;
                    break;
                }
            }

            if (mainObj != null && currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
                Vector2 pos = mainObj.getPosition();
                Vector2 vel = mainObj.getLinearVelocity();
                float mass = mainObj.getMass();
                float inertia = mainObj.getInertia();
                float omega = mainObj.getAngularVelocity();
                float ke = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
                float pe = mass * 9.8f * pos.y;
                Vector2 accel = mainObj.getCurrentLinearAcceleration(temp);
                mainObj.setCurrentLinearAcceleration(new Vector2());
                mainObj.setCurrentAngularAcceleration(0f);

                gTime.add(currentLevel.getLevelTimer());
                gPosX.add(pos.x);
                gPosY.add(pos.y);
                gVelX.add(vel.x);
                gVelY.add(vel.y);
                gSpeed.add(vel.len());
                gAccX.add(accel.x);
                gAccY.add(accel.y);
                gKE.add(ke);
                gPE.add(pe);
                gTE.add(ke + pe);

                while (gTime.size() > MAX_GRAPH_SAMPLES) {
                    gTime.remove(0);
                    gPosX.remove(0);
                    gPosY.remove(0);
                    gVelX.remove(0);
                    gVelY.remove(0);
                    gSpeed.remove(0);
                    gAccX.remove(0);
                    gAccY.remove(0);
                    gKE.remove(0);
                    gPE.remove(0);
                    gTE.remove(0);
                }
            }

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
                Vector2 pos = displayObj.getPosition();
                Vector2 vel = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getLinearVelocity() : new Vector2();
                float speed = vel.len();
                float mass = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getMass() : 0f;
                float inertia = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getInertia() : 0f;
                float omega = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getAngularVelocity() : 0f;
                float ke = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
                float pe = mass * 9.8f * pos.y;
                Vector2 accel = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getCurrentLinearAcceleration(temp) : new Vector2();

                StringBuilder sb = new StringBuilder();
                sb.append("  PHYSICS DATA  \n");
                sb.append("----------------\n");
                sb.append(String.format(" t   %8.2f s\n", currentLevel.getLevelTimer()));
                sb.append("----------------\n");
                sb.append(String.format(" x   %+8.3f m\n", pos.x));
                sb.append(String.format(" y   %+8.3f m\n", pos.y));
                sb.append("----------------\n");
                sb.append(String.format(" vx  %+8.3f\n", vel.x));
                sb.append(String.format(" vy  %+8.3f\n", vel.y));
                sb.append(String.format("|v|  %8.3f m/s\n", speed));
                sb.append("----------------\n");
                sb.append(String.format(" ax  %+8.3f\n", accel.x));
                sb.append(String.format(" ay  %+8.3f\n", accel.y));
                sb.append(String.format(" w   %+8.3f r/s\n", omega));
                sb.append("----------------\n");
                sb.append(String.format(" m   %8.3f kg\n", mass));
                sb.append(String.format(" u   %8.3f\n", displayObj.getFriction()));
                if (displayObj instanceof Charged) {
                    sb.append(String.format(" q   %8.3f C/kg\n", ((Charged) displayObj).getChargeDensity()));
                }
                sb.append("----------------\n");
                sb.append(String.format(" KE  %8.3f J\n", ke));
                sb.append(String.format(" PE  %8.3f J\n", pe));
                sb.append(String.format(" E   %8.3f J", ke + pe));
                physicsDataString = sb.toString();
            } else {
                physicsDataString = "";
            }
        }

        renderStaticObjectTooltip();
        renderCompletionOverlay();
    }

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

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        winFont.getData().setScale(1.55f);
        GlyphLayout titleL = new GlyphLayout(winFont, "LEVEL  COMPLETE!");
        float tx = pX + (pW - titleL.width) / 2f;
        float ty = pY + pH - 10f;
        winFont.setColor(0f, 0f, 0f, 0.55f);
        winFont.draw(batch, "LEVEL  COMPLETE!", tx + 2, ty - 2);
        winFont.setColor(Color.GOLD);
        winFont.draw(batch, "LEVEL  COMPLETE!", tx, ty);
        winFont.getData().setScale(1f);

        winFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        String starsText = finalStars == 3 ? "Perfect!" : finalStars == 2 ? "Good job!" : "Keep going!";
        GlyphLayout starsL = new GlyphLayout(winFont, starsText);
        winFont.draw(batch, starsL, pX + (pW - starsL.width) / 2f, pY + pH - 96f);

        Color scoreCol = finalScore >= 95 ? Color.GOLD : finalScore >= 60 ? Color.GREEN : Color.WHITE;
        winFont.setColor(scoreCol);
        String scoreText = "Score:  " + finalScore + " / 100";
        GlyphLayout scoreL = new GlyphLayout(winFont, scoreText);
        winFont.draw(batch, scoreL, pX + (pW - scoreL.width) / 2f, pY + pH - 120f);

        float drawnProp  = currentLevel.getCurrentDrawnProportion();
        float freeProp = Math.max(0, currentLevel.getFreeProp());
        String objText = drawnProp <= freeProp
            ? "Drawn Proportion: " + (int)(drawnProp * 100) + "%  (no penalty <= " + (int)(freeProp * 100) + "%)"
            : "Drawn Proportion: " + (int)(drawnProp * 100) + "%  (penalty >" + (int)(freeProp * 100)  + "%)";
        winFont.setColor(drawnProp <= freeProp ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.55f, 0.2f, 1f));
        GlyphLayout objL = new GlyphLayout(winFont, objText);
        winFont.draw(batch, objL, pX + (pW - objL.width) / 2f, pY + pH - 140f);

        winFont.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        String timeText = String.format("Time:  %.1f s", currentLevel.getLevelTimer());
        GlyphLayout timeL = new GlyphLayout(winFont, timeText);
        winFont.draw(batch, timeL, pX + (pW - timeL.width) / 2f, pY + pH - 158f);

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
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (fullRegionTexture != null) fullRegionTexture.dispose();
        if (selectRegionTexture != null) selectRegionTexture.dispose();
        if (whiteRegionTexture != null) whiteRegionTexture.dispose();
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

    // 9 mini-graphs stacked in the right panel
    private void renderGraphOverlay() {
        chartHelpTargets.clear();

        int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
        int rightX = uiViewport.getScreenWidth() - panelW;
        int panelH = viewport.getScreenHeight();
        int panelY = (uiViewport.getScreenHeight() - panelH) / 2;
        if (panelW < 20) return;

        final int N = 9;
        final float PAD = 3f, GAP = 2f;
        float cellW = panelW - PAD * 2;
        float cellH = (panelH - PAD * 2 - GAP * (N - 1)) / N;

        String[] labels = {
            "X Pos (m)", "Y Pos (m)", "Speed m/s",
            "Vel X", "Vel Y", "Acc Y m/s2",
            "KE (J)", "PE (J)", "E_tot (J)"
        };

        List<Float>[] sets = new List[]{
            gPosX, gPosY, gSpeed,
            gVelX, gVelY, gAccY,
            gKE, gPE, gTE
        };

        Color[] cols = {
            Color.CYAN, Color.GREEN, Color.YELLOW,
            Color.ORANGE, Color.CORAL, Color.RED,
            Color.GOLD, new Color(0.8f, 0.4f, 1f, 1f), Color.WHITE
        };

        // ── Build grouped help targets ──────────────────────────────────
        float groupX = rightX + PAD;
        float groupW = cellW;
        float iconSize = 14f;

        Rectangle posGroup = new Rectangle(
            groupX,
            panelY + panelH - PAD - (2 * cellH) - (1 * GAP),
            groupW,
            2 * cellH + GAP
        );

        Rectangle speedGroup = new Rectangle(
            groupX,
            panelY + panelH - PAD - (3 * cellH) - (2 * GAP),
            groupW,
            cellH
        );

        Rectangle velGroup = new Rectangle(
            groupX,
            panelY + panelH - PAD - (5 * cellH) - (4 * GAP),
            groupW,
            2 * cellH + GAP
        );

        Rectangle accGroup = new Rectangle(
            groupX,
            panelY + panelH - PAD - (6 * cellH) - (5 * GAP),
            groupW,
            cellH
        );

        Rectangle energyGroup = new Rectangle(
            groupX,
            panelY + panelH - PAD - (9 * cellH) - (8 * GAP),
            groupW,
            3 * cellH + 2 * GAP
        );

        chartHelpTargets.add(new ChartHelpTarget(
            "position",
            new Rectangle(
                posGroup.x + posGroup.width - iconSize - 6f,
                posGroup.y + posGroup.height - iconSize - 4f,
                iconSize,
                iconSize
            ),
            posGroup
        ));

        chartHelpTargets.add(new ChartHelpTarget(
            "speed",
            new Rectangle(
                speedGroup.x + speedGroup.width - iconSize - 6f,
                speedGroup.y + speedGroup.height - iconSize - 4f,
                iconSize,
                iconSize
            ),
            speedGroup
        ));

        chartHelpTargets.add(new ChartHelpTarget(
            "velocity",
            new Rectangle(
                velGroup.x + velGroup.width - iconSize - 6f,
                velGroup.y + velGroup.height - iconSize - 4f,
                iconSize,
                iconSize
            ),
            velGroup
        ));

        chartHelpTargets.add(new ChartHelpTarget(
            "acceleration",
            new Rectangle(
                accGroup.x + accGroup.width - iconSize - 6f,
                accGroup.y + accGroup.height - iconSize - 4f,
                iconSize,
                iconSize
            ),
            accGroup
        ));

        chartHelpTargets.add(new ChartHelpTarget(
            "energy",
            new Rectangle(
                energyGroup.x + energyGroup.width - iconSize - 6f,
                energyGroup.y + energyGroup.height - iconSize - 4f,
                iconSize,
                iconSize
            ),
            energyGroup
        ));

        // ── Backgrounds ────────────────────────────────────────────────
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

        // Draw tooltip icons
        for (ChartHelpTarget target : chartHelpTargets) {
            float cx = target.iconBounds.x + target.iconBounds.width / 2f;
            float cy = target.iconBounds.y + target.iconBounds.height / 2f;
            float radius = target.iconBounds.width / 2f;

            shapeRenderer.setColor(0.18f, 0.45f, 0.85f, 0.95f);
            shapeRenderer.circle(cx, cy, radius, 24);
        }
        shapeRenderer.end();

        // ── Graph lines ────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < N; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            drawGraphLines(rightX + PAD, cy, cellW, cellH, sets[i], cols[i]);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text and icon labels ───────────────────────────────────────
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        winFont.setUseIntegerPositions(true);

        for (int i = 0; i < N; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            List<Float> data = sets[i];
            float tx = rightX + PAD + 2;

            winFont.setColor(cols[i]);
            winFont.draw(batch, labels[i], tx, cy + cellH - 1);

            if (!data.isEmpty()) {
                float last = data.get(data.size() - 1);
                float vMin = last, vMax = last;
                for (float v : data) {
                    if (v < vMin) vMin = v;
                    if (v > vMax) vMax = v;
                }

                winFont.setColor(Color.WHITE);
                winFont.draw(batch, String.format("%+.2f", last), tx, cy + cellH - 12);

                winFont.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
                winFont.draw(batch, String.format("%.1f", vMax), tx, cy + cellH * 0.72f);
                winFont.draw(batch, String.format("%.1f", vMin), tx, cy + 10f);
            }
        }

        // Draw the "i" label inside each icon
        for (ChartHelpTarget target : chartHelpTargets) {
            GlyphLayout infoGlyph = new GlyphLayout(winFont, "i");
            float tx = target.iconBounds.x + (target.iconBounds.width - infoGlyph.width) / 2f;
            float ty = target.iconBounds.y + (target.iconBounds.height + infoGlyph.height) / 2f - 1f;

            winFont.setColor(Color.WHITE);
            winFont.draw(batch, "i", tx, ty);
        }

        batch.end();

        // Update hover state after bounds are built and icons drawn
        updateChartHelpHover();
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
