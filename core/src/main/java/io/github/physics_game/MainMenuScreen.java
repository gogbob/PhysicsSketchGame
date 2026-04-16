package io.github.physics_game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class MainMenuScreen extends ScreenAdapter{
    private final MainGame game;
    private Stage stage;
    private Skin skin;

    private Texture backgroundTexture;
    private SpriteBatch batch;

    public MainMenuScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("menu.png"));

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Label title = new Label("An Educational Physics Game", skin);
        TextButton playButton = new TextButton("Play", skin);
        TextButton quitButton = new TextButton("Quit", skin);
        TextButton helpButton = new TextButton("How to Play", skin);

        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LevelScreen(game));
            }
        });

        helpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // game.setScreen(new HelpScreen(game));
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(title).padBottom(40).row();
        table.add(playButton).width(220).height(60).padBottom(20).row();
        table.add(helpButton).width(220).height(60).padBottom(20).row();
        table.add(quitButton).width(220).height(60);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        // Draw texture starting at bottom-left (0,0) scaled to the screen size
        batch.draw(backgroundTexture, 0, 0, stage.getWidth(), stage.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
