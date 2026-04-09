package io.github.physics_game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import jdk.tools.jmod.Main;

public class LevelScreen extends ScreenAdapter{
    private final MainGame game;
    private Stage stage;
    private Skin skin;

    public LevelScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Label title = new Label("Level Selection", skin);

        TextButton backButton = new TextButton("Go back", skin);

        TextButton tutorialLevelButton = new TextButton("Tutorial", skin);

        TextButton level1Button = new TextButton("Level 1", skin);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        tutorialLevelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });

        level1Button.addListener(new ClickListener() {
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

        root.add(content).expand().center();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
    }
}
