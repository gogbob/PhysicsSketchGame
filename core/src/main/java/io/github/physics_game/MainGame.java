package io.github.physics_game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class MainGame extends Game {
    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }
}
