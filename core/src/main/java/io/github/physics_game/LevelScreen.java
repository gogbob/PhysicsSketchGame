package io.github.physics_game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.physics_game.levels.Level1;
import io.github.physics_game.levels.TutorialLevel;
import jdk.tools.jmod.Main;

public class LevelScreen extends ScreenAdapter{
    private final MainGame game;
    private Stage stage;
    private Skin skin;

    public static final float viewPortWidth = 40f;
    public static final float viewPortHeight = 30f;

    private Texture backgroundTexture;
    private SpriteBatch batch;

    public LevelScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        batch = new SpriteBatch();
        backgroundTexture = new Texture(Gdx.files.internal("levels.png"));

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Label title = new Label("Level Selection", skin);

        TextButton backButton = new TextButton("Go back", skin);

        TextButton tutorialLevelButton = new TextButton("Tutorial", skin);

        TextButton level1Button = new TextButton("Level 1", skin);
        TextButton level2Button = new TextButton("Level 2", skin);

        TutorialLevel tutorialLevel = new TutorialLevel(viewPortWidth, viewPortHeight);
        Level1 level1 = new Level1(viewPortWidth, viewPortHeight);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        tutorialLevelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game, tutorialLevel));
            }
        });

        level1Button.addListener(new ClickListener() {
           @Override
           public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game, level1));
           }
        });

        level2Button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

            }
        });

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // top bar row
        root.top().left();
        root.add(backButton)
            .width(180)
            .height(50)
            .pad(20)
            .left()
            .top();
        root.row();

        // main content row
        Table content = new Table();
        content.center();

        content.add(title).padBottom(30).row();
        content.add(tutorialLevelButton)
            .width(100)
            .height(100)
            .pad(20)
            .padBottom(30);

        content.add(level1Button)
            .width(100)
            .height(100)
            .pad(20)
            .padBottom(30);

        content.add(level2Button)
            .width(100)
            .height(100)
            .pad(20)
            .padBottom(30);

        root.add(content).expand().center();
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
