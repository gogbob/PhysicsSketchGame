package io.github.physics_game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlayRenderer {
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont winFont;
    private final OrthographicCamera uiCamera;
    private final ScreenViewport uiViewport;
    private final FitViewport worldViewport;

    private final ArrayList<ChartHelpTarget> chartHelpTargets = new ArrayList<>();
    private ChartHelpTarget hoveredChartHelp = null;

    private float maxObservedTotalEnergy = 1f;

    private void drawEnergyBars(
        float x,
        float y,
        float w,
        float h,
        List<Float> gKE,
        List<Float> gPE,
        List<Float> gTE
    ) {
        if (gKE.isEmpty() || gPE.isEmpty() || gTE.isEmpty()) return;

        float ke = gKE.get(gKE.size() - 1);
        float pe = gPE.get(gPE.size() - 1);
        float te = gTE.get(gTE.size() - 1);

        float maxVal = Math.max(maxObservedTotalEnergy, 1f);

        float padX = 18f;
        float padTop = 22f;
        float padBottom = 22f;

        float chartX = x + padX;
        float chartY = y + padBottom;
        float chartW = w - padX * 2f;
        float chartH = h - padTop - padBottom;

        float gap = 14f;
        float barW = (chartW - 2f * gap) / 3f;

        float keH = chartH * (ke / maxVal);
        float peH = chartH * (pe / maxVal);
        float teH = chartH * (te / maxVal);

        shapeRenderer.setColor(Color.GOLD);
        shapeRenderer.rect(chartX, chartY, barW, keH);

        shapeRenderer.setColor(new Color(0.8f, 0.4f, 1f, 1f));
        shapeRenderer.rect(chartX + barW + gap, chartY, barW, peH);

        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(chartX + 2f * (barW + gap), chartY, barW, teH);
    }

    public void resetEnergyScale() {
        maxObservedTotalEnergy = 1f;
    }

    public GraphicOverlayRenderer(
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

    public void render(
        List<Float> gTime,
        List<Float> gPosX,
        List<Float> gPosY,
        List<Float> gSpeed,
        List<Float> gVelX,
        List<Float> gVelY,
        List<Float> gAccY,
        List<Float> gKE,
        List<Float> gPE,
        List<Float> gTE
    ) {
        chartHelpTargets.clear();

        int panelW = (uiViewport.getScreenWidth() - worldViewport.getScreenWidth()) / 2;
        int rightX = uiViewport.getScreenWidth() - panelW;
        int panelH = worldViewport.getScreenHeight();
        int panelY = (uiViewport.getScreenHeight() - panelH) / 2;
        if (panelW < 20) return;

        final int N = 9;
        final float PAD = 3f;
        final float GAP = 2f;
        float cellW = panelW - PAD * 2;
        float cellH = (panelH - PAD * 2 - GAP * (N - 1)) / N;

        float energyX = rightX + PAD;
        float energyY = panelY + panelH - PAD - (9 * cellH) - (8 * GAP);
        float energyW = cellW;
        float energyH = 3 * cellH + 2 * GAP;

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

        buildChartHelpTargets(rightX, panelY, panelW, panelH, PAD, GAP, cellW, cellH);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0.08f, 0.95f);
        shapeRenderer.rect(rightX, panelY, panelW, panelH);

        for (int i = 0; i < 6; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            shapeRenderer.setColor(0.04f, 0.04f, 0.16f, 1f);
            shapeRenderer.rect(rightX + PAD, cy, cellW, cellH);
        }

        // merged energy panel
        shapeRenderer.setColor(0.04f, 0.04f, 0.16f, 1f);
        shapeRenderer.rect(energyX, energyY, energyW, energyH);

        for (ChartHelpTarget target : chartHelpTargets) {
            float cx = target.iconBounds.x + target.iconBounds.width / 2f;
            float cy = target.iconBounds.y + target.iconBounds.height / 2f;
            float radius = target.iconBounds.width / 2f;

            shapeRenderer.setColor(0.18f, 0.45f, 0.85f, 0.95f);
            shapeRenderer.circle(cx, cy, radius, 24);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < 6; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            drawGraphLines(rightX + PAD, cy, cellW, cellH, gTime, sets[i], cols[i]);
        }
        shapeRenderer.end();

        // draw energy bars in their own ShapeRenderer pass
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawEnergyBars(energyX, energyY, energyW, energyH, gKE, gPE, gTE);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        winFont.setUseIntegerPositions(true);

        for (int i = 0; i < 6; i++) {
            float cy = panelY + panelH - PAD - (i + 1) * cellH - i * GAP;
            List<Float> data = sets[i];
            float tx = rightX + PAD + 2;

            winFont.setColor(cols[i]);
            winFont.draw(batch, labels[i], tx, cy + cellH - 1);

            if (!data.isEmpty()) {
                float last = data.get(data.size() - 1);
                float vMin = last;
                float vMax = last;

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

        winFont.setColor(Color.WHITE);
        winFont.draw(batch, "Energy (J)", energyX + 2f, energyY + energyH - 2f);

        if (!gKE.isEmpty() && !gPE.isEmpty() && !gTE.isEmpty()) {
            maxObservedTotalEnergy = Math.max(maxObservedTotalEnergy, gTE.get(gTE.size() - 1));

            float ke = gKE.get(gKE.size() - 1);
            float pe = gPE.get(gPE.size() - 1);
            float te = gTE.get(gTE.size() - 1);

            float labelY = energyY + 14f;
            float padX = 18f;
            float chartW = energyW - padX * 2f;
            float gap = 14f;
            float barW = (chartW - 2f * gap) / 3f;
            float chartX = energyX + padX;

            winFont.setColor(Color.GOLD);
            winFont.draw(batch, "KE", chartX + barW * 0.25f, labelY);

            winFont.setColor(new Color(0.8f, 0.4f, 1f, 1f));
            winFont.draw(batch, "PE", chartX + barW + gap + barW * 0.25f, labelY);

            winFont.setColor(Color.WHITE);
            winFont.draw(batch, "E", chartX + 2f * (barW + gap) + barW * 0.35f, labelY);

            winFont.setColor(Color.LIGHT_GRAY);
            winFont.draw(batch, String.format("KE %.1f", ke), energyX + 2f, energyY + energyH - 16f);
            winFont.draw(batch, String.format("PE %.1f", pe), energyX + 2f, energyY + energyH - 28f);
            winFont.draw(batch, String.format("E %.1f", te), energyX + 2f, energyY + energyH - 40f);
        }

        for (ChartHelpTarget target : chartHelpTargets) {
            GlyphLayout infoGlyph = new GlyphLayout(winFont, "i");
            float tx = target.iconBounds.x + (target.iconBounds.width - infoGlyph.width) / 2f;
            float ty = target.iconBounds.y + (target.iconBounds.height + infoGlyph.height) / 2f - 1f;

            winFont.setColor(Color.WHITE);
            winFont.draw(batch, "i", tx, ty);
        }

        batch.end();

        updateChartHelpHover();
    }

    public void renderHelpTooltip() {
        if (hoveredChartHelp == null) return;

        String text = ChartHelpText.get(hoveredChartHelp.key);
        if (text == null || text.isEmpty()) return;

        float pad = 10f;
        float maxWidth = 260f; // tooltip max width (adjust as needed)

        GlyphLayout layout = new GlyphLayout();
        layout.setText(
            winFont,
            text,
            Color.WHITE,
            maxWidth,
            Align.left,
            true
        );

        float boxW = layout.width + pad * 2f;
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
        winFont.getData().markupEnabled = true;
        winFont.setColor(Color.WHITE);
        winFont.draw(batch, layout, x + pad, y - pad);
        batch.end();
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

    private void buildChartHelpTargets(
        int rightX,
        int panelY,
        int panelW,
        int panelH,
        float PAD,
        float GAP,
        float cellW,
        float cellH
    ) {
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
    }

    private void drawGraphLines(
        float cx,
        float cy,
        float gW,
        float gH,
        List<Float> gTime,
        List<Float> values,
        Color lineColor
    ) {
        if (gTime.size() < 2 || values.size() < 2) return;

        float PT = gH * 0.30f;
        float PB = gH * 0.14f;
        float px = cx + 3f;
        float py = cy + PB;
        float pw = gW - 6f;
        float ph = gH - PB - PT;
        if (ph < 4f) return;

        float tMin = gTime.get(0);
        float tMax = gTime.get(gTime.size() - 1);
        float vMin = Float.MAX_VALUE;
        float vMax = -Float.MAX_VALUE;

        for (float v : values) {
            if (v < vMin) vMin = v;
            if (v > vMax) vMax = v;
        }

        float tRange = Math.max(tMax - tMin, 0.001f);
        float vRange = vMax - vMin;
        if (vRange < 0.001f) {
            vMin -= 0.5f;
            vMax += 0.5f;
            vRange = 1f;
        }

        shapeRenderer.setColor(0.15f, 0.15f, 0.28f, 1f);
        for (int k = 1; k < 4; k++) {
            shapeRenderer.line(px, py + ph * k / 4f, px + pw, py + ph * k / 4f);
        }

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
            float x2 = px + pw * ((gTime.get(i) - tMin) / tRange);
            float y2 = py + ph * ((values.get(i) - vMin) / vRange);
            shapeRenderer.line(x1, y1, x2, y2);
        }
    }
}
