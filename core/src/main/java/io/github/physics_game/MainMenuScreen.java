package io.github.physics_game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen extends ScreenAdapter {
    private final MainGame game;

    private SpriteBatch     batch;
    private Texture         bgTexture;
    private TextureAtlas    atlas;
    private TextureRegion   btnStartRegion;
    private TextureRegion   btnExitRegion;
    private TextureRegion   btnCreditsRegion;
    private OrthographicCamera cam;
    private ScreenViewport  vp;

    // Button hit-rects [x, y, w, h]  (bottom-left origin, screen pixels)
    private final float[] rPlay   = new float[4];
    private final float[] rHelp   = new float[4];
    private final float[] rQuit   = new float[4];

    public MainMenuScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        cam = new OrthographicCamera();
        vp  = new ScreenViewport(cam);
        vp.apply(true);

        batch     = new SpriteBatch();
        bgTexture = new Texture(Gdx.files.internal("menu.png"));
        bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        atlas            = new TextureAtlas(Gdx.files.internal("menuButtons.atlas"));
        btnStartRegion   = atlas.findRegion("startButton");
        btnExitRegion    = atlas.findRegion("exitButton");
        btnCreditsRegion = atlas.findRegion("creditsButton");

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void layout(int sw, int sh) {
        // startButton original is 385×147 → draw at 320×112
        float bw = 320f, bh = 112f;
        float cx = sw / 2f;
        float midY = sh * 0.46f;
        rPlay[0] = cx - bw / 2f;   rPlay[1] = midY;        rPlay[2] = bw; rPlay[3] = bh;

        // creditsButton → "How To Play"  364×103 → 240×68
        float hw = 240f, hh = 68f;
        rHelp[0] = cx - hw / 2f;   rHelp[1] = midY - hh - 18f; rHelp[2] = hw; rHelp[3] = hh;

        // exitButton 168×116 → 180×65
        float qw = 180f, qh = 65f;
        rQuit[0] = cx - qw / 2f;   rQuit[1] = rHelp[1] - qh - 14f; rQuit[2] = qw; rQuit[3] = qh;
    }

    @Override
    public void render(float delta) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        // mouse in bottom-left origin
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
        batch.end();


        // ── Click handling ───────────────────────────────────────────
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (hit(rPlay, mx, my)) { game.setScreen(new LevelScreen(game)); return; }
            if (hit(rHelp, mx, my)) { /* TODO: HelpScreen */ }
            if (hit(rQuit, mx, my)) { Gdx.app.exit(); }
        }

        // ── Button sprites ───────────────────────────────────────────
        batch.begin();

        // PLAY — startButton texture, tinted on hover
        batch.setColor(hit(rPlay, mx, my) ? new Color(1f, 1f, 1f, 1f)
                                          : new Color(0.85f, 0.85f, 0.85f, 0.92f));
        batch.draw(btnStartRegion, rPlay[0], rPlay[1], rPlay[2], rPlay[3]);

        // HOW TO PLAY — creditsButton texture re-purposed
        batch.setColor(hit(rHelp, mx, my) ? new Color(1f, 1f, 1f, 1f)
                                          : new Color(0.80f, 0.80f, 0.80f, 0.88f));
        batch.draw(btnCreditsRegion, rHelp[0], rHelp[1], rHelp[2], rHelp[3]);

        // QUIT — exitButton texture
        batch.setColor(hit(rQuit, mx, my) ? new Color(1f, 1f, 1f, 1f)
                                          : new Color(0.80f, 0.80f, 0.80f, 0.88f));
        batch.draw(btnExitRegion, rQuit[0], rQuit[1], rQuit[2], rQuit[3]);

        batch.end();
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
        if (bgTexture != null) bgTexture.dispose();
        if (atlas != null)     atlas.dispose();
    }
}
