package io.github.physics_game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Collections;

public class MainGame extends Game {
    public ArrayList<ScoreLevel> currentScores = new ArrayList<>(Collections.nCopies(5, null));
    public static ArrayList<DrawTypeButton> drawTypeButtons = new ArrayList<DrawTypeButton>();

    public static class DrawTypeButton {
        DrawType drawType;
        int paintIndex;
        Texture offTex;
        Texture onTex;
        Rectangle bounds;
    }

    @Override
    public void create() {
        drawTypeButtons.add(new DrawTypeButton() {{
            drawType = DrawType.NORMAL;
            paintIndex = 0;
            offTex = new Texture("normal_paint.png");
            onTex = new Texture("normal_paint_selected.png");
            bounds = new Rectangle(10, 10, 50, 50);
        }});
        drawTypeButtons.add(new DrawTypeButton() {{
            drawType = DrawType.ICY;
            paintIndex = 1;
            offTex = new Texture("ice_paint.png");
            onTex = new Texture("ice_paint_selected.png");
            bounds = new Rectangle(10, 10, 50, 50);
        }});
        drawTypeButtons.add(new DrawTypeButton() {{
            drawType = DrawType.POSITIVE;
            paintIndex = 2;
            offTex = new Texture("poscharged_paint.png");
            onTex = new Texture("poscharged_paint_selected.png");
            bounds = new Rectangle(10, 10, 50, 50);
        }});
        drawTypeButtons.add(new DrawTypeButton() {{
            drawType = DrawType.NEGATIVE;
            paintIndex = 3;
            offTex = new Texture("negcharged_paint.png");
            onTex = new Texture("negcharged_paint_selected.png");
            bounds = new Rectangle(10, 10, 50, 50);
        }});
        setScreen(new MainMenuScreen(this));
    }
}
