package io.github.physics_game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

import java.util.ArrayList;
import java.util.Collections;

public class MainGame extends Game {
    public ArrayList<ScoreLevel> currentScores = new ArrayList<>(Collections.nCopies(5, null));
    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }
}
