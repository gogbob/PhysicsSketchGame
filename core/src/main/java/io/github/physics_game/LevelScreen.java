package io.github.physics_game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.levels.Level1;
import io.github.physics_game.levels.TutorialLevel;

public class LevelScreen extends ScreenAdapter {
    private final MainGame game;

    private SpriteBatch    batch;
    private ShapeRenderer  sr;
    private BitmapFont     font;
    private Texture        bgTexture;
    private Texture        whitePx;
    private OrthographicCamera cam;
    private ScreenViewport vp;

    public static final float viewPortWidth  = 40f;
    public static final float viewPortHeight = 30f;

    // Back button [x,y,w,h]
    private final float[] btnBack = new float[4];

    // 3 level cards [x,y,w,h]
    private static final int NUM_CARDS = 3;
    private final float[][] cards = new float[NUM_CARDS][4];

    private static final String[] NAMES    = { "Tutorial", "Level 1", "Level 2" };
    private static final String[] DESCS    = { "Learn the basics", "Drop into the cup", "Coming soon..." };
    private static final int[]    STARS    = { 0, 0, -1 }; // -1 = locked
    private static final int[]    LEVEL_IDS = { 0, 2, -1 }; // levelId for each card

    public LevelScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        cam = new OrthographicCamera();
        vp  = new ScreenViewport(cam);
        vp.apply(true);

        batch     = new SpriteBatch();
        sr        = new ShapeRenderer();
        font      = new BitmapFont();
        bgTexture = new Texture(Gdx.files.internal("levels.png"));
        bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        whitePx = new Texture(pm);
        pm.dispose();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void layout(int sw, int sh) {
        btnBack[0] = 24; btnBack[1] = sh - 58; btnBack[2] = 140; btnBack[3] = 42;

        float cardW = 220f, cardH = 270f, gap = 36f;
        float totalW = NUM_CARDS * cardW + (NUM_CARDS - 1) * gap;
        float startX = sw / 2f - totalW / 2f;
        float cardY  = sh / 2f - cardH / 2f - 10f;
        for (int i = 0; i < NUM_CARDS; i++) {
            cards[i][0] = startX + i * (cardW + gap);
            cards[i][1] = cardY;
            cards[i][2] = cardW;
            cards[i][3] = cardH;
        }
    }

    @Override
    public void render(float delta) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        int mx = Gdx.input.getX();
        int my = sh - Gdx.input.getY();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        vp.apply();
        batch.setProjectionMatrix(cam.combined);

        // ── Background ───────────────────────────────────────────────
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(bgTexture, 0, 0, sw, sh);
        batch.setColor(0f, 0f, 0f, 0.58f);
        batch.draw(whitePx, 0, 0, sw, sh);
        batch.end();

        // ── Grid + physics deco ───────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(cam.combined);
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(0.25f, 0.42f, 0.70f, 0.09f);
        for (int x = 0; x < sw; x += 44) sr.line(x, 0, x, sh);
        for (int y = 0; y < sh; y += 44) sr.line(0, y, sw, y);
        sr.setColor(0.30f, 0.55f, 0.85f, 0.07f);
        sr.circle(sw * 0.08f, sh * 0.40f, 55, 32);
        sr.circle(sw * 0.92f, sh * 0.55f, 40, 32);
        sr.end();

        // ── Click handling ───────────────────────────────────────────
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (hit(btnBack, mx, my)) { game.setScreen(new MainMenuScreen(game)); return; }
            if (hit(cards[0], mx, my) && STARS[0] >= 0) {
                game.setScreen(new GameScreen(game, new TutorialLevel(viewPortWidth, viewPortHeight)));
                return;
            }
            if (hit(cards[1], mx, my) && STARS[1] >= 0) {
                game.setScreen(new GameScreen(game, new Level1(viewPortWidth, viewPortHeight)));
                return;
            }
        }

        // ── Filled shapes ─────────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Back button
        boolean backHov = hit(btnBack, mx, my);
        sr.setColor(backHov ? new Color(0.22f, 0.38f, 0.65f, 0.95f)
                            : new Color(0.10f, 0.16f, 0.32f, 0.88f));
        sr.rect(btnBack[0], btnBack[1], btnBack[2], btnBack[3]);
        sr.setColor(1f, 1f, 1f, backHov ? 0.18f : 0.08f);
        sr.rect(btnBack[0], btnBack[1] + btnBack[3] - 3, btnBack[2], 3);

        // Level cards
        for (int i = 0; i < NUM_CARDS; i++) {
            boolean locked = STARS[i] < 0;
            boolean hov    = !locked && hit(cards[i], mx, my);
            float[] c = cards[i];

            if (locked)    sr.setColor(0.09f, 0.09f, 0.13f, 0.82f);
            else if (hov)  sr.setColor(0.12f, 0.26f, 0.52f, 0.96f);
            else           sr.setColor(0.08f, 0.13f, 0.28f, 0.90f);
            sr.rect(c[0], c[1], c[2], c[3]);

            // top highlight strip
            sr.setColor(1f, 1f, 1f, locked ? 0.04f : (hov ? 0.18f : 0.10f));
            sr.rect(c[0], c[1] + c[3] - 3, c[2], 3f);

            // colored top accent bar (only non-locked)
            if (!locked) {
                Color accent = (i == 0) ? new Color(0.28f, 0.72f, 1.00f, 1f)
                                        : new Color(0.85f, 0.72f, 0.08f, 1f);
                sr.setColor(accent);
                sr.rect(c[0], c[1] + c[3] - 3, c[2], 3f);
            }

            // Stars (filled gold)
            if (!locked) {
                float starR = 14f;
                float starY = c[1] + c[3] / 2f - 22f;
                float starSpacing = starR * 2.6f;
                float starStartX = c[0] + c[2] / 2f - starSpacing;
                int levelId = LEVEL_IDS[i];
                ScoreLevel scoreLevel = (levelId >= 0 && levelId < game.currentScores.size()) ? game.currentScores.get(levelId) : null;
                int earnedStars = (scoreLevel != null) ? scoreLevel.getNumStars() : 0;
                for (int s = 0; s < 3; s++) {
                    Color sc = s < earnedStars ? Color.GOLD
                                               : new Color(0.20f, 0.22f, 0.30f, 1f);
                    drawStar(starStartX + s * starSpacing, starY, starR, sc);
                }
            }
        }
        sr.end();

        // ── Outlines / borders ────────────────────────────────────────
        sr.begin(ShapeRenderer.ShapeType.Line);

        sr.setColor(backHov ? new Color(0.50f, 0.78f, 1f, 0.90f)
                            : new Color(0.28f, 0.50f, 0.82f, 0.55f));
        sr.rect(btnBack[0], btnBack[1], btnBack[2], btnBack[3]);

        for (int i = 0; i < NUM_CARDS; i++) {
            boolean locked = STARS[i] < 0;
            boolean hov    = !locked && hit(cards[i], mx, my);
            float[] c = cards[i];
            if (locked)   sr.setColor(0.22f, 0.22f, 0.28f, 0.40f);
            else if (hov) sr.setColor(0.48f, 0.78f, 1.00f, 0.90f);
            else          sr.setColor(0.28f, 0.52f, 0.86f, 0.58f);
            sr.rect(c[0], c[1], c[2], c[3]);
        }
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Text ──────────────────────────────────────────────────────
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Page title
        font.getData().setScale(2.0f);
        String title = "SELECT  LEVEL";
        GlyphLayout tgl = new GlyphLayout(font, title);
        font.setColor(0f, 0f, 0f, 0.50f);
        font.draw(batch, title, sw / 2f - tgl.width / 2f + 2, sh * 0.88f - 2);
        font.setColor(new Color(0.28f, 0.72f, 1.00f, 1f));
        font.draw(batch, title, sw / 2f - tgl.width / 2f, sh * 0.88f);
        font.getData().setScale(1f);

        // Back button label
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        GlyphLayout bgl = new GlyphLayout(font, "< BACK");
        font.draw(batch, "< BACK",
            btnBack[0] + (btnBack[2] - bgl.width) / 2f,
            btnBack[1] + (btnBack[3] + bgl.height) / 2f);
        font.getData().setScale(1f);

        // Card text
        for (int i = 0; i < NUM_CARDS; i++) {
            boolean locked = STARS[i] < 0;
            float[] c = cards[i];
            float cx = c[0], cy = c[1], cw = c[2], ch = c[3];

            // Level number (large watermark)
            font.getData().setScale(2.4f);
            font.setColor(locked ? new Color(0.28f, 0.28f, 0.34f, 1f)
                                 : new Color(0.28f, 0.60f, 0.95f, 0.28f));
            String num = i == 0 ? "00" : String.format("%02d", i);
            GlyphLayout numGl = new GlyphLayout(font, num);
            font.draw(batch, num, cx + (cw - numGl.width) / 2f, cy + ch - 10f);
            font.getData().setScale(1f);

            // Level name
            font.getData().setScale(1.4f);
            font.setColor(locked ? new Color(0.35f, 0.35f, 0.40f, 1f) : Color.WHITE);
            GlyphLayout nameGl = new GlyphLayout(font, NAMES[i]);
            font.draw(batch, NAMES[i], cx + (cw - nameGl.width) / 2f, cy + ch - 58f);
            font.getData().setScale(1f);

            // Description
            font.getData().setScale(1.0f);
            font.setColor(locked ? new Color(0.28f, 0.28f, 0.32f, 1f)
                                 : new Color(0.58f, 0.68f, 0.82f, 1f));
            GlyphLayout descGl = new GlyphLayout(font, DESCS[i]);
            font.draw(batch, DESCS[i], cx + (cw - descGl.width) / 2f, cy + ch - 84f);
            font.getData().setScale(1f);

            // Locked label
            if (locked) {
                font.getData().setScale(1.3f);
                font.setColor(new Color(0.35f, 0.35f, 0.40f, 1f));
                GlyphLayout lkGl = new GlyphLayout(font, "LOCKED");
                font.draw(batch, "LOCKED", cx + (cw - lkGl.width) / 2f, cy + ch / 2f + 10f);
                font.getData().setScale(1f);
            }
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawStar(float cx, float cy, float outerR, Color color) {
        sr.setColor(color);
        float innerR = outerR * 0.42f;
        for (int i = 0; i < 5; i++) {
            float a1 = (float)(Math.PI / 2 + 2 * Math.PI * i / 5);
            float ai = (float)(Math.PI / 2 + 2 * Math.PI * i / 5 + Math.PI / 5);
            float a2 = (float)(Math.PI / 2 + 2 * Math.PI * (i + 1) / 5);
            sr.triangle(cx + outerR * (float)Math.cos(a1), cy + outerR * (float)Math.sin(a1),
                        cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                        cx, cy);
            sr.triangle(cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                        cx + outerR * (float)Math.cos(a2), cy + outerR * (float)Math.sin(a2),
                        cx, cy);
        }
    }

    private boolean hit(float[] r, int mx, int my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    @Override
    public void resize(int width, int height) {
        vp.update(width, height, true);
        layout(width, height);
    }

    @Override
    public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (batch != null)     batch.dispose();
        if (sr != null)        sr.dispose();
        if (font != null)      font.dispose();
        if (bgTexture != null) bgTexture.dispose();
        if (whitePx != null)   whitePx.dispose();
    }
}
