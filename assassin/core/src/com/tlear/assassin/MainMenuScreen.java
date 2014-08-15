package com.tlear.assassin;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class MainMenuScreen implements Screen {
	
	final Assassin game;
	
	OrthographicCamera camera;
	
	public MainMenuScreen(final Assassin game) {
		this.game = game;
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, game.width, game.height);
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.07f, 0.12f, 0.12f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		camera.update();
		game.batch.setProjectionMatrix(camera.combined);
		
		game.batch.begin();
		game.font.draw(game.batch, "Welcome to Assassin!", 100, 500);
		game.font.draw(game.batch, "WASD to move.  Mouse to shoot.", 100, 450);
		game.font.draw(game.batch, "Press any key to begin", 100, 400);
		game.batch.end();
		
		if(Gdx.input.isTouched() || Gdx.input.isKeyPressed(Keys.ANY_KEY)) {
			game.setScreen(new GameScreen(game));
			dispose();
		}
	}
	
	@Override
	public void pause() {
	}
	
	@Override
	public void resume() {
	}
	
	@Override
	public void dispose() {
	}
	
	@Override
	public void resize(int width, int height) {
		game.width = width;
		game.height = height;
	}
	
	@Override
	public void hide() {
	}
	
	@Override
	public void show() {
	}
}