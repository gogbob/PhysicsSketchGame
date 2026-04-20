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
    OrthographicCamera camera;
    Box2DDebugRenderer debugRenderer;
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
    public static int SIDE_BUFFER_PX = 200;
    public static int TOP_BUFFER_PX = 10;
    public static int BOTTOM_BUFFER_PX = 10;
    public static int BUTTON_WIDTH = 240;
    public static int BUTTON_HEIGHT = 120;
    public static int BUTTON_STARTY = 400;
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

        drawType = currentLevel.getDrawTypes().get(currentLevel.getSelectedPaint());

        drawTool = new DrawTool(0.5f);

        Gdx.app.log("Main", "DrawTool created!");

        // Log startup info
        Gdx.app.log("Main", "create() - viewport world size = " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }

    @Override
    public void render(float delta) {
        boolean isSelecting = false;
        // Shared layout values — computed once, used in both click handler and drawing
        int panelW  = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
        int panelH  = viewport.getScreenHeight();
        int panelY  = (uiViewport.getScreenHeight() - panelH) / 2;
        int restartBtnY = panelY + 8;
        int restartBtnH = 28;
        int paintBtnsBaseY = restartBtnY + restartBtnH + 44; // bottom of paint button stack

        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = uiViewport.getScreenHeight() - Gdx.input.getY() - 1;
            int btnW = panelW - 16;

            // Paint button click detection (stacked from paintBtnsBaseY upward)
            int numPaintBtns = currentLevel.getDrawTypes().size();
            int totalPaintH  = numPaintBtns * BUTTON_HEIGHT + (numPaintBtns - 1) * 5;
            if (mx >= 0 && mx < panelW && my >= paintBtnsBaseY && my < paintBtnsBaseY + totalPaintH) {
                int relY = (int)(my - paintBtnsBaseY);
                int slotH = BUTTON_HEIGHT + 5;
                int slot  = relY / slotH;
                if (slot < numPaintBtns && relY % slotH < BUTTON_HEIGHT) {
                    currentLevel.setSelectedPaint(slot);
                    drawType = currentLevel.getDrawTypes().get(slot);
                }
            } else if (mx >= 8 && mx <= 8 + btnW && my >= restartBtnY && my <= restartBtnY + restartBtnH) {
                resetLevel();
                isSelecting = true; // consume the click
            } else if(mx < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2 && mx > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2) {
                Integer i = isPointInsideObjects(viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
                if(i != null) {
                    System.out.println("Selecting");
                    selectedObject = i;
                    isSelecting = true;
                } else {
                    System.out.println("Clicked on empty space, starting draw");
                    Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                    drawTool.update(drawType, currentLevel, 1, worldPos.x, worldPos.y);
                }
            }
        } else if(Gdx.input.getX() < viewport.getScreenWidth() + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2 && Gdx.input.getX() > 5 + (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2) {
            Vector3 worldPos = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            drawTool.update(drawType, currentLevel, Gdx.input.isButtonPressed(Input.Buttons.LEFT) ? 2 : 0, worldPos.x, worldPos.y);
        }


        if(currentLevel.getRunPhysics()) accumulator += Math.min(delta, 0.25f);
        else accumulator = 0.0f;

        logTimer += delta;
        if (currentLevel.getRunPhysics() && !currentLevel.isComplete()) currentLevel.setLevelTimer(currentLevel.getLevelTimer() + delta);

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

        if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            currentLevel.reinitialize();
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentLevel.setRunPhysics(!currentLevel.getRunPhysics());
            Gdx.app.log("Main", "Physics " + (currentLevel.getRunPhysics() ? "RUNNING" : "PAUSED"));
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
        ArrayList<DebugForce> forces = PhysicsResolver.stepWithDebug(currentLevel.getPhysicsObjects());

        if(currentLevel.isComplete()) {
            if (!scoreCalculated) {
                finalScore = ScoreCalculator.calculateScore(
                    currentLevel.getNumDrawnObjects(),
                    currentLevel.getLevelTimer(),
                    currentLevel.getFreeObjects()
                );
                finalStars = ScoreCalculator.calculateStars(finalScore);
                scoreCalculated = true;

                Gdx.app.log("Main", "Level complete!");
                Gdx.app.log("Main", "Score = " + finalScore + ", Stars = " + finalStars);
            }
        }

        if(!showDebugOverlay) {
            //Normal view
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
        } else {
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
            shapeRenderer.end();
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        for(DebugForce f : forces) {
            if(f.getType() == DebugForce.Type.GRAVITY ||  f.getType() == DebugForce.Type.VELOCITY)
                drawArrow(f.getPosition(), f.getForce().nor(), f.getForce().len(), f.getColor());
        }

        shapeRenderer.end();

        if(currentLevel.getRunPhysics()) currentLevel.tick(delta);

        //camera.update();
        //batch.setProjectionMatrix(camera.combined);

        uiViewport.apply();

        //PANELS

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(panelBgTexture, uiViewport.getScreenWidth() - panelW, 0, panelW + 20f, uiViewport.getScreenHeight());
        batch.draw(panelBgTexture, 0, 0, panelW, uiViewport.getScreenHeight());
        batch.setColor(Color.WHITE);

        // ── LEFT PANEL ────────────────────────────────────────────────
        float lx = 8f;
        float topY = panelY + panelH - 20f;

        // Level name + time
        winFont.getData().setScale(1.0f);
        winFont.setColor(new Color(0.7f, 0.85f, 1f, 1f));
        winFont.setUseIntegerPositions(true);
        String timeStr = String.format("%d:%02d.%d",
            (int)(currentLevel.getLevelTimer() / 60f),
            (int)(currentLevel.getLevelTimer()) % 60,
            ((int)(currentLevel.getLevelTimer() * 10)) % 10);
        String levelHeaderStr = currentLevel.getLevelName() + "\nTime: " + timeStr;
        winFont.draw(batch, levelHeaderStr, lx, topY);

        // Separator
        float sepY = topY - 36f;
        batch.setColor(0.3f, 0.5f, 0.8f, 0.4f);
        batch.draw(panelBgTexture, lx, sepY, panelW - 16f, 1f);
        batch.setColor(Color.WHITE);

        // Physics data table
        winFont.setColor(Color.CYAN);
        winFont.setFixedWidthGlyphs("0123456789+-.,() ");
        if (!physicsDataString.isEmpty()) {
            winFont.draw(batch, physicsDataString, lx, sepY - 6f);
        }
        winFont.setFixedWidthGlyphs("");

        // ── BOTTOM: hints → paint buttons → RESTART ──────────────────
        // RESTART button
        int btnW = panelW - 16;
        batch.setColor(0.75f, 0.15f, 0.15f, 0.92f);
        batch.draw(panelBgTexture, 8, restartBtnY, btnW, restartBtnH);
        batch.setColor(Color.WHITE);
        winFont.setColor(Color.WHITE);
        GlyphLayout rl = new GlyphLayout(winFont, "RESTART  [R]");
        winFont.draw(batch, rl, 8 + (btnW - rl.width) / 2f, restartBtnY + (restartBtnH + rl.height) / 2f);

        // Hint line above restart
        winFont.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
        winFont.draw(batch, "[P] graphs   [click] obj info", lx, restartBtnY + restartBtnH + 18f);

        // Paint selection buttons stacked from paintBtnsBaseY upward
        float trackY = 0f;
        for (int i = 0; i < MainGame.drawTypeButtons.size(); i++) {
            if (currentLevel.getDrawTypes().contains(MainGame.drawTypeButtons.get(i).drawType)) {
                int ind = currentLevel.getDrawTypes().indexOf(MainGame.drawTypeButtons.get(i).drawType);

                float btnBotY = paintBtnsBaseY + trackY;
                int rectStartX = 48, rectStartY = 35, rectTotalWidth = 162, rectTotalHeight = 30;
                float leftWidth = rectTotalWidth * currentLevel.getCurrentDrawnAmounts().get(ind)
                                  / currentLevel.getDrawAmounts().get(ind);
                boolean full = currentLevel.getDrawAmounts().get(ind) - currentLevel.getCurrentDrawnAmounts().get(ind) < 0.001f;

                Texture barTex;
                Texture btnTex;
                if (currentLevel.getSelectedPaint() == ind) {
                    barTex = full ? new Texture("full_region.png") : new Texture("select_region.png");
                    btnTex = MainGame.drawTypeButtons.get(i).onTex;
                } else {
                    barTex = full ? new Texture("full_region.png") : new Texture("white_region.png");
                    btnTex = MainGame.drawTypeButtons.get(i).offTex;
                }
                batch.draw(barTex, lx + rectStartX, btnBotY + rectStartY, leftWidth, rectTotalHeight);
                batch.draw(btnTex, lx, btnBotY, BUTTON_WIDTH, BUTTON_HEIGHT);
                trackY += BUTTON_HEIGHT + 5;
            }
        }

        batch.end();

        // ── RIGHT PANEL — always graphs ───────────────────────────────
        renderGraphOverlay();

        // Physics data tracking (graph recording + data-table string update)
        physicsDataTimer += delta;

        if (physicsDataTimer >= selectInfoPeriod) {
            float temp = physicsDataTimer;
            while (physicsDataTimer >= selectInfoPeriod) physicsDataTimer -= selectInfoPeriod;

            // Find the primary object (first DynamicObject) for graph recording
            DynamicObject mainObj = null;
            for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                if (o instanceof DynamicObject) { mainObj = (DynamicObject) o; break; }
            }

            // Record graph data from primary object whenever physics runs
            if (mainObj != null && currentLevel.getRunPhysics() && !currentLevel.isComplete()) {
                Vector2 pos   = mainObj.getPosition();
                Vector2 vel   = mainObj.getLinearVelocity();
                float mass    = mainObj.getMass();
                float inertia = mainObj.getInertia();
                float omega   = mainObj.getAngularVelocity();
                float ke      = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
                float pe      = mass * 9.8f * pos.y;
                Vector2 accel = mainObj.getCurrentLinearAcceleration(temp);
                mainObj.setCurrentLinearAcceleration(new Vector2());
                mainObj.setCurrentAngularAcceleration(0f);

                gTime.add(currentLevel.getLevelTimer());
                gPosX.add(pos.x);   gPosY.add(pos.y);
                gVelX.add(vel.x);   gVelY.add(vel.y);
                gSpeed.add(vel.len());
                gAccX.add(accel.x); gAccY.add(accel.y);
                gKE.add(ke);        gPE.add(pe);        gTE.add(ke + pe);
                while (gTime.size() > MAX_GRAPH_SAMPLES) {
                    gTime.remove(0);  gPosX.remove(0);  gPosY.remove(0);
                    gVelX.remove(0);  gVelY.remove(0);  gSpeed.remove(0);
                    gAccX.remove(0);  gAccY.remove(0);
                    gKE.remove(0);    gPE.remove(0);    gTE.remove(0);
                }
            }

            // Build data-table string from primary object (or selected object if clicked)
            PhysicsObject displayObj = null;
            if (selectedObject != null) {
                for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                    if (o.getId() == selectedObject) { displayObj = o; break; }
                }
            }
            if (displayObj == null) displayObj = mainObj;

            if (displayObj != null) {
                Vector2 pos    = displayObj.getPosition();
                Vector2 vel    = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getLinearVelocity() : new Vector2();
                float speed    = vel.len();
                float mass     = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getMass() : 0f;
                float inertia  = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getInertia() : 0f;
                float omega    = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getAngularVelocity() : 0f;
                float ke       = 0.5f * mass * vel.len2() + 0.5f * inertia * omega * omega;
                float pe       = mass * 9.8f * pos.y;
                Vector2 accel  = (displayObj instanceof DynamicObject) ? ((DynamicObject) displayObj).getCurrentLinearAcceleration(temp) : new Vector2();

                StringBuilder sb = new StringBuilder();
                sb.append("  PHYSICS DATA  \n");
                sb.append("----------------\n");
                sb.append(String.format(" t   %8.2f s\n", currentLevel.getLevelTimer()));
                sb.append("----------------\n");
                sb.append(String.format(" x   %+8.3f m\n", pos.x));
                sb.append(String.format(" y   %+8.3f m\n", pos.y));
                sb.append("----------------\n");
                sb.append(String.format(" vx  %+8.3f\n",   vel.x));
                sb.append(String.format(" vy  %+8.3f\n",   vel.y));
                sb.append(String.format("|v|  %8.3f m/s\n", speed));
                sb.append("----------------\n");
                sb.append(String.format(" ax  %+8.3f\n",   accel.x));
                sb.append(String.format(" ay  %+8.3f\n",   accel.y));
                sb.append(String.format(" w   %+8.3f r/s\n", omega));
                sb.append("----------------\n");
                sb.append(String.format(" m   %8.3f kg\n", mass));
                sb.append(String.format(" u   %8.3f\n",    displayObj.getFriction()));
                if (displayObj instanceof Charged) {
                    sb.append(String.format(" q   %8.3f C/kg\n", ((Charged) displayObj).getChargeDensity()));
                }
                sb.append("----------------\n");
                sb.append(String.format(" KE  %8.3f J\n",  ke));
                sb.append(String.format(" PE  %8.3f J\n",  pe));
                sb.append(String.format(" E   %8.3f J",    ke + pe));
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

        int freeObjs  = currentLevel.getFreeObjects();
        int extraObjs = Math.max(0, currentLevel.getNumDrawnObjects() - freeObjs);
        String objText = extraObjs == 0
            ? "Objects: " + currentLevel.getNumDrawnObjects() + "  (no penalty)"
            : "Objects: " + currentLevel.getNumDrawnObjects() + "  (+" + extraObjs + " over limit)";
        winFont.setColor(extraObjs == 0 ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.55f, 0.2f, 1f));
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

    // 9 mini-graphs stacked in the right panel — game world stays fully visible.
    @SuppressWarnings("unchecked")
    private void renderGraphOverlay() {
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
            float tx = rightX + PAD + 2;
            winFont.setColor(cols[i]);
            winFont.draw(batch, labels[i], tx, cy + cellH - 1);
            if (!data.isEmpty()) {
                float last = data.get(data.size() - 1);
                float vMin = last, vMax = last;
                for (float v : data) { if (v < vMin) vMin = v; if (v > vMax) vMax = v; }
                winFont.setColor(Color.WHITE);
                winFont.draw(batch, String.format("%+.2f", last), tx, cy + cellH - 12);
                winFont.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
                winFont.draw(batch, String.format("%.1f", vMax), tx, cy + cellH * 0.72f);
                winFont.draw(batch, String.format("%.1f", vMin), tx, cy + 10f);
            }
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
