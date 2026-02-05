package io.github.physics_game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    FitViewport viewport;
    private Texture image;
    private Texture ballImage;
    World world;

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(8, 5);
        image = new Texture("libgdx.png");
        ballImage = new Texture("simple_ball.png");
        world = new World(new Vector2(0,0), true);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        input();
        logic();
        draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    private void input() {

    }

    private void logic() {

    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.end();
    }
}
