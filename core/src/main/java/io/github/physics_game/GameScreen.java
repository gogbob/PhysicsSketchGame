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
    private static final int MAX_GRAPH_SAMPLES = 50; // 5 s at 10 Hz
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
        if(Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = uiViewport.getScreenHeight() - Gdx.input.getY() - 1;
            int panelW = (uiViewport.getScreenWidth() - viewport.getScreenWidth()) / 2;
            int btnY   = (uiViewport.getScreenHeight() - viewport.getScreenHeight()) / 2 + 8;
            int btnW   = panelW - 16;
            int btnH   = 28;
            if((mx < 5 + BUTTON_WIDTH) && my >= BUTTON_STARTY && my < BUTTON_STARTY + (BUTTON_HEIGHT + 5) * currentLevel.getDrawTypes().size()) {
                if((((int) my) - BUTTON_STARTY) % (BUTTON_HEIGHT + 5) < BUTTON_HEIGHT) {
                    System.out.println("Selecting button: " + ((currentLevel.getDrawTypes().size() - 1) - (((int) my) - BUTTON_STARTY) / (BUTTON_HEIGHT + 5)) % currentLevel.getDrawTypes().size());
                    currentLevel.setSelectedPaint(((currentLevel.getDrawTypes().size() - 1) - (((int) my) - BUTTON_STARTY) / (BUTTON_HEIGHT + 5)) % currentLevel.getDrawTypes().size());

                    drawType = currentLevel.getDrawTypes().get(currentLevel.getSelectedPaint());
                }
            } else if (mx >= 8 && mx <= 8 + btnW && my >= btnY && my <= btnY + btnH) {
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
        System.out.println(currentLevel.getRunPhysics());
        System.out.println(accumulator);

        logTimer += delta;
        if (currentLevel.getRunPhysics()) currentLevel.setLevelTimer(currentLevel.getLevelTimer() + delta);

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
                    currentLevel.getLevelTimer()
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

        int panelW  = (uiViewport.getScreenWidth() - viewport.getScreenWidth())/2;
        int panelH = viewport.getScreenHeight();
        int panelY = (uiViewport.getScreenHeight() - panelH)/2;

        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(panelBgTexture, uiViewport.getScreenWidth() - panelW, 0, panelW + 20f, uiViewport.getScreenHeight());
        batch.draw(panelBgTexture, 0, 0, panelW, uiViewport.getScreenHeight());
        batch.setColor(Color.WHITE);

        batch.end();

        //generate UI Here
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

            if(game.currentScores.get(currentLevel.getLevelId()) == null) {
                game.currentScores.add(currentLevel.getLevelId(), new ScoreLevel(currentLevel.getLevelTimer(), currentLevel.getCurrentDrawnProportion()));
            }

            game.currentScores.get(currentLevel.getLevelId()).setNewBestScore(currentLevel.getLevelTimer(), currentLevel.getCurrentDrawnProportion());
        }

        // LEFT PANEL


        StringBuilder levelInfoBuilder = new StringBuilder();
        levelInfoBuilder.append("Level: " + currentLevel.getLevelName());
        levelInfoBuilder.append("\n\nTime: " + (int)(currentLevel.getLevelTimer() / 60f) +
            ":" + (int)(currentLevel.getLevelTimer()) +
            "." + ((int)(currentLevel.getLevelTimer() * 1000)%1000));
        levelInfoBuilder.append("\nPercent paint used: " + ((int)(currentLevel.getCurrentDrawnProportion() * 100f) % 100) + "%");

        levelInfoBuilder.append("\n Level Best: ");
        if(game.currentScores.get(currentLevel.getLevelId()) == null) {
            levelInfoBuilder.append("\n ---- ");
        } else {
            levelInfoBuilder.append("\n Best Time: " + game.currentScores.get(currentLevel.getLevelId()).getBestTime());
            levelInfoBuilder.append("\n Best Paint: " + (int)(game.currentScores.get(currentLevel.getLevelId()).getBestShapeProportion() * 100f) + "%");
            levelInfoBuilder.append("\n Stars: " + game.currentScores.get(currentLevel.getLevelId()).getNumStars());
        }

        GlyphLayout layoutLevelInfo = new GlyphLayout(winFont, levelInfoBuilder.toString());
        float leftPanelX = 5f;           // fixed left padding
        float leftPanelY = panelY + panelH - 20f;       // fixed top anchor
        winFont.setUseIntegerPositions(true);  // reduce subpixel shimmer
        winFont.setFixedWidthGlyphs("0123456789+-.,() ");
        winFont.draw(batch, layoutLevelInfo, Math.round(leftPanelX), Math.round(leftPanelY));


        float trackY = 0f;

        //show the paint selection buttons
        for(int i = 0; i < MainGame.drawTypeButtons.size(); i++) {
            if(currentLevel.getDrawTypes().contains(MainGame.drawTypeButtons.get(i).drawType)) {
                int ind = currentLevel.getDrawTypes().indexOf(MainGame.drawTypeButtons.get(i).drawType);

                int rectStartX = 48;
                int rectStartY = 35;
                int rectTotalWidth = 162;
                int rectTotalHeight = 30;


                float leftWidth = rectTotalWidth * currentLevel.getCurrentDrawnAmounts().get(ind) / currentLevel.getDrawAmounts().get(ind);

                if(currentLevel.getSelectedPaint() == ind) {
                    Texture whiteRegion = new Texture("select_region.png");
                    if(currentLevel.getDrawAmounts().get(ind) - currentLevel.getCurrentDrawnAmounts().get(ind) < 0.001f) {
                        whiteRegion = new Texture("full_region.png");
                    }

                    batch.draw(whiteRegion, 5 + rectStartX, leftPanelY - BUTTON_HEIGHT - trackY - BUTTON_STARTY + rectStartY, leftWidth, rectTotalHeight);
                    batch.draw(MainGame.drawTypeButtons.get(i).onTex, leftPanelX, leftPanelY - BUTTON_HEIGHT - trackY - BUTTON_STARTY, BUTTON_WIDTH, BUTTON_HEIGHT);
                    trackY += BUTTON_HEIGHT + 5;
                } else {
                    Texture whiteRegion = new Texture("white_region.png");
                    if(currentLevel.getDrawAmounts().get(ind) - currentLevel.getCurrentDrawnAmounts().get(ind) < 0.001f) {
                        whiteRegion = new Texture("full_region.png");
                    }
                    batch.draw(whiteRegion, 5 + rectStartX, leftPanelY - BUTTON_HEIGHT - trackY - BUTTON_STARTY + rectStartY, leftWidth, rectTotalHeight);
                    batch.draw(MainGame.drawTypeButtons.get(i).offTex, leftPanelX, leftPanelY - BUTTON_HEIGHT - trackY - BUTTON_STARTY, BUTTON_WIDTH, BUTTON_HEIGHT);
                    trackY += BUTTON_HEIGHT + 5;
                }
            }
        }

        batch.end();

        // RIGHT PANEL

        // Physics Data Panel (left side)
        physicsDataTimer += delta;

        if (physicsDataTimer >= selectInfoPeriod) {
            float temp = physicsDataTimer;
            while (physicsDataTimer >= selectInfoPeriod) physicsDataTimer -= selectInfoPeriod;
            PhysicsObject selectedObj = null;
            for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
                if (selectedObject != null) {
                    if (o.getId() == selectedObject) {
                        selectedObj = o;
                    }
                }
            }
            if (selectedObj != null) {
                Vector2 pos = selectedObj.getPosition();
                Vector2 vel = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getLinearVelocity() : new Vector2();
                float speed = vel.len();
                float mass = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getMass() : 0f;
                float inertia = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getInertia() : 0f;
                float omega = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getAngularVelocity() : 0f;
                float ke = 0.5f * mass * vel.dot(vel) + 0.5f * inertia * omega * omega;
                float pe = mass * 9.8f * pos.y;
                // use selectInfoPeriod as divisor → average a over this update interval
                Vector2 accel = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getCurrentLinearAcceleration(temp) : new Vector2();
                float angAccel = (selectedObj instanceof DynamicObject)? ((DynamicObject) selectedObj).getCurrentAngularAcceleration(temp) : 0f;
                // reset accumulator so each interval is independent (fixes drift bug)
                if(selectedObj instanceof DynamicObject) {
                    ((DynamicObject)selectedObj).setCurrentLinearAcceleration(new Vector2());
                    ((DynamicObject)selectedObj).setCurrentAngularAcceleration(0f);
                }

                // record snapshot for graphs — freeze data once level is complete
                if (currentLevel.getRunPhysics()) {
                    gTime.add(currentLevel.getLevelTimer());
                    gPosX.add(pos.x);
                    gPosY.add(pos.y);
                    gVelX.add(vel.x);
                    gVelY.add(vel.y);
                    gSpeed.add(speed);
                    gAccX.add(accel.x);
                    gAccY.add(accel.y);
                    gKE.add(ke);
                    gPE.add(pe);
                    gTE.add(ke + pe);
                    while (gTime.size() > MAX_GRAPH_SAMPLES) {
                        System.out.println("Update graph");
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

                if (!showGraphs && selectedObject != null) {
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
                    sb.append(String.format(" u   %8.3f\n", selectedObj.getFriction()));
                    if(selectedObj instanceof Charged) {
                        sb.append(String.format(" q   %8.3f C/kg\n", ((Charged) selectedObj).getChargeDensity()));
                    }
                    sb.append("----------------\n");
                    sb.append(String.format(" KE  %8.3f J\n", ke));
                    sb.append(String.format(" PE  %8.3f J\n", pe));
                    sb.append(String.format(" E   %8.3f J", ke + pe));
                    physicsDataString = sb.toString();

                }
            } else {
                physicsDataString = "";
            }
        }

        if(!showGraphs) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            batch.setColor(Color.WHITE);
            winFont.setColor(Color.CYAN);
            winFont.setUseIntegerPositions(true);
            winFont.setFixedWidthGlyphs("0123456789+-.,() ");
            winFont.draw(batch, physicsDataString, uiViewport.getScreenWidth() - panelW + 5f, panelY + panelH - 20f);
            batch.end();
        } else {
            renderGraphOverlay();
        }

        batch.begin();
        // Key hints — above restart button
        {
            int btnY   = (uiViewport.getScreenHeight() - viewport.getScreenHeight()) / 2 + 8;
            winFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
            winFont.draw(batch, "[P]  show / hide graphs", 8f, btnY + 28 + 46);
            winFont.draw(batch, "[click]  object info",    8f, btnY + 28 + 30);
        }

        // Restart button — bottom of left panel
        {
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
        currentLevel.reinitialize();
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
        shapeRenderer.rect(rightX, panelY - 400, panelW, panelH - 400);
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
