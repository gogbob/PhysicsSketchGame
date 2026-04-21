package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.levels.Level;
import io.github.physics_game.object_types.PhysicsObject;
import io.github.physics_game.object_types.StaticObject;

/**
 * Renders game overlay components including completion status, static object tooltip and drawing stars
 */
public class GameOverlayRenderer {
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont winFont;
    private final OrthographicCamera uiCamera;
    private final ScreenViewport uiViewport;
    private final FitViewport worldViewport;

    public GameOverlayRenderer(
        ShapeRenderer shapeRenderer,
        SpriteBatch batch,
        BitmapFont winFont,
        OrthographicCamera uiCamera,
        ScreenViewport uiViewport,
        FitViewport worldViewport
    ) {
        this.shapeRenderer = shapeRenderer;
        this.batch = batch;
        this.winFont = winFont;
        this.uiCamera = uiCamera;
        this.uiViewport = uiViewport;
        this.worldViewport = worldViewport;
    }

    public void renderStaticObjectTooltip(
        Level currentLevel,
        Integer selectedObject,
        Vector2 lastClickPos
    ) {
        if (selectedObject == null) return;

        PhysicsObject obj = null;
        for (PhysicsObject o : currentLevel.getPhysicsObjects()) {
            if (o.getId() == selectedObject) {
                obj = o;
                break;
            }
        }

        if (!(obj instanceof StaticObject)) return;

        String tip = String.format(
            "Static Object  #%d\nFriction:    %.3f\nRestitution: %.3f\nDensity:     %.3f",
            obj.getId(), obj.getFriction(), obj.getRestitution(), obj.getDensity()
        );

        GlyphLayout gl = new GlyphLayout(winFont, tip);
        float bw = gl.width + 14f;
        float bh = gl.height + 12f;
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

    public void renderCompletionOverlay(
        boolean levelComplete,
        int finalScore,
        int finalStars,
        float levelTimer,
        float currentDrawnProportion,
        float freeProp
    ) {
        if (!levelComplete) return;

        int vx = worldViewport.getScreenX();
        int vy = worldViewport.getScreenY();
        int vw = worldViewport.getScreenWidth();
        int vh = worldViewport.getScreenHeight();

        int pW = Math.min(vw - 40, 420);
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

        String objText = currentDrawnProportion <= freeProp
            ? "Drawn Proportion: " + (int)(currentDrawnProportion * 100) + "%  (no penalty <= " + (int)(freeProp * 100) + "%)"
            : "Drawn Proportion: " + (int)(currentDrawnProportion * 100) + "%  (penalty >" + (int)(freeProp * 100) + "%)";
        winFont.setColor(currentDrawnProportion <= freeProp
            ? new Color(0.4f, 1f, 0.5f, 1f)
            : new Color(1f, 0.55f, 0.2f, 1f));
        GlyphLayout objL = new GlyphLayout(winFont, objText);
        winFont.draw(batch, objL, pX + (pW - objL.width) / 2f, pY + pH - 140f);

        winFont.setColor(new Color(0.65f, 0.65f, 0.65f, 1f));
        String timeText = String.format("Time:  %.1f s", levelTimer);
        GlyphLayout timeL = new GlyphLayout(winFont, timeText);
        winFont.draw(batch, timeL, pX + (pW - timeL.width) / 2f, pY + pH - 158f);

        winFont.setColor(new Color(0.48f, 0.48f, 0.48f, 1f));
        GlyphLayout hintL = new GlyphLayout(winFont, "[R]  Play Again");
        winFont.draw(batch, hintL, pX + (pW - hintL.width) / 2f, pY + 22f);

        winFont.setColor(Color.WHITE);
        batch.end();
    }

    private void drawStar(float cx, float cy, float outerR, Color color) {
        shapeRenderer.setColor(color);
        float innerR = outerR * 0.42f;

        for (int i = 0; i < 5; i++) {
            float a1 = (float)(Math.PI / 2 + 2 * Math.PI * i / 5);
            float ai = (float)(Math.PI / 2 + 2 * Math.PI * i / 5 + Math.PI / 5);
            float a2 = (float)(Math.PI / 2 + 2 * Math.PI * (i + 1) / 5);

            shapeRenderer.triangle(
                cx + outerR * (float)Math.cos(a1), cy + outerR * (float)Math.sin(a1),
                cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                cx, cy
            );
            shapeRenderer.triangle(
                cx + innerR * (float)Math.cos(ai), cy + innerR * (float)Math.sin(ai),
                cx + outerR * (float)Math.cos(a2), cy + outerR * (float)Math.sin(a2),
                cx, cy
            );
        }
    }
}
