package io.github.physics_game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.levels.Level;

/**
 * Renders left sidebar for physics calculations and player actions
 */
public class GameSidebarRenderer {

    public enum SidebarAction {
        NONE,
        GO_TO_LEVELS,
        RESTART
    }

    private final SpriteBatch batch;
    private final BitmapFont winFont;
    private final ScreenViewport uiViewport;
    private final FitViewport worldViewport;

    private final Texture panelBgTexture;
    private final Texture texFull;
    private final Texture texSelect;
    private final Texture texWhite;

    public GameSidebarRenderer(
        SpriteBatch batch,
        BitmapFont winFont,
        ScreenViewport uiViewport,
        FitViewport worldViewport,
        Texture panelBgTexture,
        Texture texFull,
        Texture texSelect,
        Texture texWhite
    ) {
        this.batch = batch;
        this.winFont = winFont;
        this.uiViewport = uiViewport;
        this.worldViewport = worldViewport;
        this.panelBgTexture = panelBgTexture;
        this.texFull = texFull;
        this.texSelect = texSelect;
        this.texWhite = texWhite;
    }

    public SidebarAction getSidebarAction(float mx, float my) {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();
        int panelY = getPanelY();

        int restartBtnY = panelY + 8;
        int restartBtnH = 28;
        int btnW = panelW - 16;
        int halfW = (btnW - 4) / 2;

        if (my >= restartBtnY && my <= restartBtnY + restartBtnH) {
            if (mx >= 8 && mx <= 8 + halfW) {
                return SidebarAction.GO_TO_LEVELS;
            } else if (mx >= 8 + halfW + 4 && mx <= 8 + btnW) {
                return SidebarAction.RESTART;
            }
        }

        return SidebarAction.NONE;
    }

    public Integer getClickedPaintIndex(Level currentLevel, float mx, float my) {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();
        int panelY = getPanelY();

        int restartBtnY = panelY + 8;
        int restartBtnH = 28;
        int paintBtnsBaseY = restartBtnY + restartBtnH + 32;

        int numPaintBtns = currentLevel.getDrawTypes().size();
        int totalPaintH = numPaintBtns * GameScreen.BUTTON_HEIGHT + Math.max(0, numPaintBtns - 1) * 5;

        if (!(mx >= 0 && mx < panelW && my >= paintBtnsBaseY && my < paintBtnsBaseY + totalPaintH)) {
            return null;
        }

        int relY = (int)(my - paintBtnsBaseY);
        int slotH = GameScreen.BUTTON_HEIGHT + 5;
        int slot = relY / slotH;

        if (slot < numPaintBtns && relY % slotH < GameScreen.BUTTON_HEIGHT) {
            return slot;
        }

        return null;
    }

    public void render(
        Level currentLevel,
        MainGame game,
        String physicsDataString
    ) {
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();
        int panelY = getPanelY();
        int restartBtnY = panelY + 8;
        int restartBtnH = 28;
        int paintBtnsBaseY = restartBtnY + restartBtnH + 32;

        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(panelBgTexture, 0, 0, panelW, uiViewport.getScreenHeight());
        batch.setColor(Color.WHITE);

        float lx = 8f;
        float topY = panelY + panelH - 10f;

        winFont.getData().setScale(1.0f);
        winFont.setColor(new Color(0.7f, 0.85f, 1f, 1f));
        winFont.setUseIntegerPositions(true);

        String timeStr = String.format("%d:%02d.%d",
            (int)(currentLevel.getLevelTimer() / 60f),
            (int)(currentLevel.getLevelTimer()) % 60,
            ((int)(currentLevel.getLevelTimer() * 10)) % 10
        );

        String levelHeaderStr;
        if (game.currentScores.get(currentLevel.getLevelId()) != null) {
            String bestTimeStr = String.format("%d:%02d.%d",
                (int)(game.currentScores.get(currentLevel.getLevelId()).getBestTime() / 60f),
                (int)(game.currentScores.get(currentLevel.getLevelId()).getBestTime()) % 60,
                ((int)(game.currentScores.get(currentLevel.getLevelId()).getBestTime() * 10)) % 10
            );
            levelHeaderStr = currentLevel.getLevelName()
                + "\nTime:  " + timeStr
                + "\nBest:  " + bestTimeStr;
        } else {
            levelHeaderStr = currentLevel.getLevelName()
                + "\nTime:  " + timeStr;
        }

        GlyphLayout glHeader = new GlyphLayout(winFont, levelHeaderStr);
        winFont.draw(batch, glHeader, lx, topY);

        float sepY = topY - glHeader.height - 4f;
        batch.setColor(0.3f, 0.5f, 0.8f, 0.4f);
        batch.draw(panelBgTexture, lx, sepY, panelW - 16f, 1f);
        batch.setColor(Color.WHITE);

        winFont.getData().setScale(1.3f);
        winFont.setColor(Color.WHITE);
        winFont.setFixedWidthGlyphs("0123456789+-.,() ");
        if (!physicsDataString.isEmpty()) {
            winFont.draw(batch, physicsDataString, lx, sepY - 6f);
            winFont.draw(batch, physicsDataString, lx + 0.8f, sepY - 6f);
        }
        winFont.setFixedWidthGlyphs("");
        winFont.getData().setScale(1f);

        int btnW = panelW - 16;
        int halfW = (btnW - 4) / 2;

        batch.setColor(0.15f, 0.35f, 0.75f, 0.92f);
        batch.draw(panelBgTexture, 8, restartBtnY, halfW, restartBtnH);
        batch.setColor(Color.WHITE);
        winFont.setColor(Color.WHITE);
        GlyphLayout ll = new GlyphLayout(winFont, "LEVELS");
        winFont.draw(batch, ll, 8 + (halfW - ll.width) / 2f, restartBtnY + (restartBtnH + ll.height) / 2f);

        batch.setColor(0.75f, 0.15f, 0.15f, 0.92f);
        batch.draw(panelBgTexture, 8 + halfW + 4, restartBtnY, halfW, restartBtnH);
        batch.setColor(Color.WHITE);
        winFont.setColor(Color.WHITE);
        GlyphLayout rl = new GlyphLayout(winFont, "RESTART [R]");
        winFont.draw(batch, rl, 8 + halfW + 4 + (halfW - rl.width) / 2f, restartBtnY + (restartBtnH + rl.height) / 2f);

        winFont.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
        winFont.draw(batch, "[P] graphs   [click] obj info", lx, restartBtnY + restartBtnH + 18f);

        float trackY = 0f;
        for (int i = 0; i < MainGame.drawTypeButtons.size(); i++) {
            if (currentLevel.getDrawTypes().contains(MainGame.drawTypeButtons.get(i).drawType)) {
                int ind = currentLevel.getDrawTypes().indexOf(MainGame.drawTypeButtons.get(i).drawType);

                float btnBotY = paintBtnsBaseY + trackY;

                float rectStartX = GameScreen.BUTTON_WIDTH * 0.30f;
                float rectStartY = GameScreen.BUTTON_HEIGHT * 0.29f;
                float rectTotalWidth = GameScreen.BUTTON_WIDTH * 0.68f;
                float rectTotalHeight = GameScreen.BUTTON_HEIGHT * 0.25f;

                float amountNow = currentLevel.getCurrentDrawnAmounts().get(ind);
                float amountMax = currentLevel.getDrawAmounts().get(ind);
                float leftWidth = 0f;
                if (amountMax > 0.0001f) {
                    leftWidth = rectTotalWidth * amountNow / amountMax;
                }

                boolean full = amountMax - amountNow < 0.001f;

                Texture barTex;
                Texture btnTex;
                if (currentLevel.getSelectedPaint() == ind) {
                    barTex = full ? texFull : texSelect;
                    btnTex = MainGame.drawTypeButtons.get(i).onTex;
                } else {
                    barTex = full ? texFull : texWhite;
                    btnTex = MainGame.drawTypeButtons.get(i).offTex;
                }

                batch.draw(barTex, lx + rectStartX, btnBotY + rectStartY, leftWidth, rectTotalHeight);
                batch.draw(btnTex, lx, btnBotY, GameScreen.BUTTON_WIDTH, GameScreen.BUTTON_HEIGHT);

                trackY += GameScreen.BUTTON_HEIGHT + 5;
            }
        }
    }

    private int getPanelWidth() {
        return (uiViewport.getScreenWidth() - worldViewport.getScreenWidth()) / 2;
    }

    private int getPanelHeight() {
        return worldViewport.getScreenHeight();
    }

    private int getPanelY() {
        return (uiViewport.getScreenHeight() - getPanelHeight()) / 2;
    }
}
