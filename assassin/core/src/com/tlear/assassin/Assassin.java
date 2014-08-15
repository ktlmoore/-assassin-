package com.tlear.assassin;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Assassin extends Game {
	
	public SpriteBatch batch;
	public BitmapFont font;
	
	public int width;
	public int height;
	
	public void create() {
		width = 1280;
		height = 800;
		
		batch = new SpriteBatch();
		// Use LibGDX's default Arial font
		font = new BitmapFont();
		this.setScreen(new MainMenuScreen(this));
	}
	
	public void render() {
		super.render(); // important!
	}
	
	public void dispose() {
		batch.dispose();
		font.dispose();
	}
}