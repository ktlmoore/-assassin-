package com.tlear.assassin;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class GameScreen implements Screen {
	final Assassin game;
	
	private Intersector intersector;
	
	// Textures
	private Texture circleImage;
	private Texture squareImage;
	private Texture bloodImage;
	
	// Colours
	private Color bgColor;
	
	// Sounds
	private Sound fireSound;
	private Sound squareDieSound;
	private Sound raveCubeDieSound;
	private Sound endOfGameSound;
	private Sound slowSound;
	private Sound specialSound;
	private Sound rofSound;
	private Sound genocideSound;
	private Sound mrcSpawnSound;
	private Sound mrcDieSound;
	
	private BitmapFont font;
	
	// Camera
	private OrthographicCamera camera;
	
	// Sprite Batches
	private SpriteBatch batch;
	
	// Renderers
	private ShapeRenderer shapeRenderer;
	
	// Logic
	private Circle circle;
	private Array<Square> squares;
	private Array<Bullet> bullets;
	private Array<PowerUp> powerups;
	private Array<Point> blood;
	private int nextSide;
	private int score;
	private Cursor cursor;
	
	private float RoF; // Rate of Fire
	private boolean AoEon; // AoE shot on?
	

	// Time
	private int time;
	private long lastSpawnTime;
	private long lastFireTime;
	private long lastPowerUpSpawnTime;
	private long AoEonSince;
	private long RoFonSince;
	
	// Godmode
	private boolean godmode;
	private boolean bloodmode;
	
	private int windowWidth;
	private int windowHeight;
	
	// Game state
	
	static final int GAME_RUNNING = 0;
	static final int GAME_PAUSED = 1;
	static final int GAME_OVER = 2;
	
	private int state;
	
	// Init
	public GameScreen(final Assassin game) {
		this.game = game;
		
		bgColor = new Color(0.07f, 0.12f, 0.12f, 1);
		
		Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		font = new BitmapFont();
		font.setColor(1, 1, 1, 1);
		
		batch = new SpriteBatch();
		
		batch.begin();
		font.draw(batch, "Loading...", game.width / 2 - 50, game.height / 2);
		batch.end();
		
		windowWidth = game.width;
		windowHeight = game.height;
		
		// load the textures
		circleImage = new Texture("circle.png");
		squareImage = new Texture("square.png");
		bloodImage = new Texture("blood.png");
		
		// load the sounds
		fireSound = Gdx.audio.newSound(Gdx.files.internal("pew.mp3"));
		squareDieSound = Gdx.audio.newSound(Gdx.files.internal("squaredie.mp3"));
		raveCubeDieSound = Gdx.audio.newSound(Gdx.files.internal("ravecubedie.mp3"));
		endOfGameSound = Gdx.audio.newSound(Gdx.files.internal("youlose.mp3"));
		slowSound = Gdx.audio.newSound(Gdx.files.internal("slow.mp3"));
		specialSound = Gdx.audio.newSound(Gdx.files.internal("special.mp3"));
		genocideSound = Gdx.audio.newSound(Gdx.files.internal("death.mp3"));
		rofSound = Gdx.audio.newSound(Gdx.files.internal("pewpewpew.mp3"));
		mrcSpawnSound = Gdx.audio.newSound(Gdx.files.internal("mrcspawn.mp3"));
		mrcDieSound = Gdx.audio.newSound(Gdx.files.internal("mrcdie.mp3"));
		
		// set up camera
		camera = new OrthographicCamera();
		camera.setToOrtho(false, windowWidth, windowHeight);
		
		// set up rendering
		
		shapeRenderer = new ShapeRenderer();
		
		Gdx.input.setCursorCatched(true);
		
		// initialise logic
		circle = new Circle(windowWidth / 2 - 15 / 2, windowHeight / 2 - 15 / 2);
		cursor = new Cursor(0, 0);
		
		RoF = 1.0f;
		
		// Logic
		squares = new Array<Square>();
		powerups = new Array<PowerUp>();
		bullets = new Array<Bullet>();
		blood = new Array<Point>();
		
		nextSide = 0;
		
		score = 0;
		time = 0;
		
		// Game params
		godmode = false;
		bloodmode = false;
		
		// Time
		lastSpawnTime = 500000000;
		lastPowerUpSpawnTime = 6000000000L;
		lastFireTime = 0;
		AoEonSince = 0;
		RoFonSince = 0;
		
		// start game
		spawnSquare(null, 0.0f);
		state = 0;
	}
	
	private void spawnSquare(Vector3 location, float speed) {
		Square square = new Square(-1, -1);
		boolean placed = false;
		int tries = 0;
		while (!placed && tries < 5) {
			if (location == null) {
				switch (nextSide) {		
				// Come in from a side
				case 0:
					square = new Square(-30, MathUtils.random(0, windowHeight+30));
					break;
				case 1:
					square = new Square(MathUtils.random(0, windowWidth+30), windowHeight+30);
					break;
				case 2:
					square = new Square(windowWidth+30, MathUtils.random(-30, windowHeight+30));
					break;
				case 3:
					square = new Square(MathUtils.random(-30, windowWidth+30), -30);
					break;
				default:
					throw new Error("nextSide is undefined");
				}
			} else {
				square = new Square(location.x + (tries * 2), location.y + (tries*2), speed);
			}
			placed = true;
			for (int i = 0; i < squares.size && placed; i++) {
				if (square.hitBox.intersects(squares.get(i).hitBox) && !squares.get(i).mommaRaveCube) {placed = false; tries++;}
			}
			if (square.mommaRaveCube) placed = true;
		}
		if (tries < 5) squares.add(square);
		if (square.mommaRaveCube) mrcSpawnSound.play();
		if (location == null) {lastSpawnTime = TimeUtils.nanoTime(); nextSide = (nextSide + 1) % 4;}
	}
	
	private void spawnPowerUp() {
		PowerUp powerup = new PowerUp();
		lastPowerUpSpawnTime = TimeUtils.nanoTime();
		powerups.add(powerup);
	}
	
	void checkKill() {
		if (!godmode) {
			for(int i = 0; i < squares.size; i++) {
				Square square = squares.get(i);
				if (square.hitBox.intersects(circle.hitBox)) {
					endGame();
				}
			}
		}
	}
	
	private void fireBullet(float mouseX, float mouseY) {
		int variance = (int)(((Math.random()*2) - 1.0) * 10);
		if (TimeUtils.nanoTime() - lastFireTime >= 200000000 * RoF) {
			fireSound.play();
			if (AoEon) {
				for (int i = -5; i < 5; i++) {
					Bullet bullet = new Bullet(mouseX, mouseY, 0.0f);
					bullet.v.rotate(new Vector3(0, 0, 10), 5.0f * i);
					bullets.add(bullet);
				}
			} else {
				Bullet bullet = new Bullet(mouseX, mouseY, variance);
				bullets.add(bullet);
			}
			lastFireTime = TimeUtils.nanoTime();
		}
	}

	private void update(float delta) {
		switch (state) {
		case GAME_RUNNING:
			update_running(delta);
			 break;
		case GAME_PAUSED:
			update_paused();
			break;
		case GAME_OVER:
			update_over();
			break;
		default:
			throw new Error("ILLEGAL GAME STATE " + state);
		}
	}
	
	// Update_running - called while game is running
	private void update_running(float delta) {
		Vector3 mousePos = new Vector3();
		
		/** Control **/
		// Mouse position
		mousePos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(mousePos);
		cursor.move(mousePos.x, mousePos.y);
		
		// Keyboard input
		if(Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) circle.accel(3);
		if(Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) circle.accel(2);
		if(Gdx.input.isKeyPressed(Keys.DOWN) || Gdx.input.isKeyPressed(Keys.S)) circle.accel(1); 
		if(Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W)) circle.accel(0);
		if(Gdx.input.isKeyPressed(Keys.G)) godmode = !godmode;
		if(Gdx.input.isKeyPressed(Keys.B)) bloodmode = !bloodmode;
		if(Gdx.input.isKeyPressed(Keys.P)) pause();
		
		// Mouse input
		if(Gdx.input.isTouched()) {
			fireBullet(mousePos.x, mousePos.y);
		}
		
		/** Entity spawning **/
		// Square spawning
		if (TimeUtils.nanoTime() - lastSpawnTime > 550000000 * (1 - (1 / (50000 - score)))) spawnSquare(null, 0.0f);
		// Power up spawning
		if (TimeUtils.nanoTime() - lastPowerUpSpawnTime > 6000000000L) spawnPowerUp();
		
		
		/** Movement **/
		// Move circle
		circle.move();
				
		// Move squares
		for(int j = 0; j < squares.size; j++) {
			Square square = squares.get(j);
			square.move();
		}
		
		// Move bullets
		for (int j = 0; j < bullets.size; j++) {
			Bullet bullet = bullets.get(j);
			bullet.move();
		}
					
		/** Timeouts **/
		if (TimeUtils.nanoTime() - AoEonSince > 2000000000L) AoEon = false;
		if (TimeUtils.nanoTime() - RoFonSince > 4000000000L) RoF = 1.0f;
					
		time++;
		if (time % 50 == 0) score++;
		
		// One check for dead
		checkKill();
		
		for (int i = 0; i < squares.size; i++) {
			if (squares.get(i).hp <= 0) squares.removeIndex(i);
		}
	}
	
	// Update_over - called while game is over
	private void update_over() {
		// Check for restart
		if (Gdx.input.isKeyPressed(Keys.R)) restart();
	}
	
	// Update_paused - called while game is paused
	private void update_paused() {
		// NO UPDATING - wait for mouse click
		if (Gdx.input.isTouched()) resume();
	}
	
	private void draw() {
		// Set bgColor to dark gray
		Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		// Setting up drawing
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		
		if (game.getScreen() == this) {
			
			/** Order:
			 *  Background fill
			 *  Powerups
			 *  Bullet fill
			 *  Player line
			 *  Player fill
			 *  Squares line
			 *  Squares fill
			 *  Crosshair lines
			 *  Bloodmode
			 *  Pause screen
			 **/
			
			// Background
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
			shapeRenderer.rect(50, 50, windowWidth - 100, windowHeight - 100);
			shapeRenderer.end();
			
			// Powerups
			for (PowerUp powerup : powerups) {
				powerup.render();
			}
			
			// Bullets
			for(Bullet bullet: bullets) {
				bullet.render();
			}
						
			// Player
			circle.render();
						
			// Squares
			for(Square square: squares) {
				square.render();
			}
			shapeRenderer.end();
			
			// Cursor
			cursor.render();
			
			// Game over information
			if (state == GAME_OVER) {
				batch.begin();
				font.draw(batch, "YOU HAVE DIED.  YOUR SCORE WAS: " + score, windowWidth / 2 - 50, windowHeight / 2);
				font.draw(batch, "PRESS 'R' TO RETURN TO MENU", windowWidth / 2 - 50, windowHeight / 2 - 50);
				batch.end();
			}
			
			// Score
			batch.begin();
			if (!bloodmode && !godmode) font.draw(batch, "Score: " + score, 10, windowHeight - 10);
			else font.draw(batch, "Score: " + score + " " + godmode + " " + bloodmode, 10, windowHeight - 10);
			batch.end();
			
			// Blood
			if (bloodmode) {
				batch.begin();
				for (Point p : blood) {
					batch.draw(bloodImage, p.x, p.y);
				}
				batch.end();
			}
			
			if (state == GAME_PAUSED) {
				// PAUSED
				// Draw translucent layer (THIS DOESN'T WORK I DO NOT KNOW WHY)
				/*
			    shapeRenderer.begin(ShapeType.Filled);
				shapeRenderer.setColor(bgColor.r, bgColor.g, bgColor.b, 0.0f);
				shapeRenderer.rect(0,  0,  windowWidth,  windowHeight);
				shapeRenderer.end();
				*/
				// Draw text
				batch.begin();
				font.draw(batch, "PAUSED.  Click to continue.", windowWidth / 2 - 100, windowHeight / 2);
				batch.end();
			}
		}
	}
	
	@Override
	public void render (float delta) {
		update(delta);
		draw();
	}
	
	private void endGame() {
		// Called when a square hits the circle
		if (state != GAME_OVER) {
			endOfGameSound.play();
			state = GAME_OVER;
			Gdx.input.setCursorCatched(false);
		}
	}
	
	private void restart() {
		dispose();
		game.setScreen(new MainMenuScreen(game));
	}
	
	@Override
	public void pause() {
		if (state != GAME_OVER) {
			Gdx.input.setCursorCatched(false);
			state = GAME_PAUSED;
		}
	}
	
	@Override
	public void resume() {
		if (state != GAME_OVER) {
			Gdx.input.setCursorCatched(true);
			state = GAME_RUNNING;
		}
	}
	
	@Override
	public void dispose() {
		shapeRenderer.dispose();
		circleImage.dispose();
		squareImage.dispose();
		font.dispose();
		batch.dispose();
	}
	
	@Override
	public void resize(int width, int height) {
		game.width = width;
		game.height = height;
		windowWidth = width;
		windowHeight = height;
	}
	
	private class Bullet extends Rectangle {
		Vector3 v; // Velocity
		float speed;
		public Bullet(float mouseX, float mouseY, float variance) {
			speed = 3000;
			width = 5;
			height = 5;
			Vector3 c = circle.hitBox.getCenter();
			x = c.x;
			y = c.y;
			double length = Math.sqrt((mouseX - x + variance)*(mouseX - x + variance) + (mouseY - y + variance)*(mouseY - y + variance));
			v = new Vector3((mouseX - x + variance) / (float)length * speed * Gdx.graphics.getDeltaTime(), (mouseY - y + variance) / (float)length * speed * Gdx.graphics.getDeltaTime(), 0);
		}
		public void move() {
			Ray ray1 = new Ray(new Vector3(x, y, 0), v);
			
			x += v.x;
			y += v.y;
			
			Ray ray2 = new Ray(new Vector3(x, y, 0), v);
			
			// Check for hit
			boolean hit = false;
			for(int i = 0; i < squares.size; i++) {
				Square square = squares.get(i);
				if ((intersector.intersectRayBounds(ray1, square.hitBox, null) && !intersector.intersectRayBounds(ray2, square.hitBox, null)) || square.hitBox.intersects(new BoundingBox(new Vector3(x, y, 0), new Vector3(x+5,y+5, 0)))) {
					square.hit(false);
					if(bullets.indexOf(this, false) < bullets.size && bullets.indexOf(this, false) >= 0) bullets.removeIndex(bullets.indexOf(this, false));
					hit = true;
				}
			}
		
			// Check for out of bounds
			if (!hit && (x > windowWidth || x < 0 || y > windowHeight || y < 0)) bullets.removeIndex(bullets.indexOf(this, false));
		}
		public void render() {
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.setColor(1, 0, 0, 1);
			shapeRenderer.rect(x, y, width, height);
			shapeRenderer.end();
		}
	}
	
	private class Circle {
		BoundingBox hitBox;
		Vector3 v;
		float speed;
		int size;
		Color colour;
		float accelAmount;
		
		Circle(float x, float y) {
			size = 30;
			hitBox = new BoundingBox(new Vector3(x+10, y+10, 0), new Vector3(x+20, y+20, 5));
			v = new Vector3(0.0f,0.0f,0.0f);
			speed = 200 * Gdx.graphics.getDeltaTime();
			colour = new Color(1, 1, 1, 1);
			accelAmount = 0.1f;
		}
		
		void move() {
			if(speed == 0.0) {speed = 200 * Gdx.graphics.getDeltaTime();}
			Vector3 min = hitBox.getMin().add(v.cpy().scl(speed));
			if (min.x <= 50 || min.x >= windowWidth-50-size) {
				v.x *= -0.25;
				min.x = Math.max(50, Math.min(windowWidth-50-size, min.x));
			}
			if (min.y <= 50 || min.y >= windowHeight-50-size) {
				v.y *= -0.25;
				min.y = Math.max(50, Math.min(windowHeight-50-size, min.y));
			}
			Vector3 max = min.cpy().add(size, size, size);
			hitBox.set(min, max);
			
			Iterator<PowerUp> iter = powerups.iterator();
			while(iter.hasNext()) {
				PowerUp powerup = iter.next();
				if (powerup.hitBox.intersects(hitBox)) {
					powerup.pickUp();
					iter.remove();
				}
			}
		}
		
		void accel(int dir) {
			// Accelerate in given direction, capped by 1.5x
			switch (dir) {
				case 0:
					v.y = Math.min(v.y + accelAmount, 1.5f);
					break;
				case 1:
					v.y = Math.max(v.y - accelAmount, -1.5f);
					break;
				case 2:
					v.x = Math.min(v.x + accelAmount, 1.5f);
					break;
				case 3:
					v.x = Math.max(v.x - accelAmount, -1.5f);
					break;
				default:
					break;
			}
		}
		
		Vector3 centre() {
			// Centre of hitbox
			return hitBox.getCenter().cpy();
		}
		
		void render() {
			// Render the circle
			shapeRenderer.begin(ShapeType.Filled);
			Vector3 c = hitBox.getCenter();
			// Line
			shapeRenderer.setColor(0, 0, 0, 1);
			shapeRenderer.circle(c.x, c.y, circle.size / 2 + 2);
			// Fill
			shapeRenderer.setColor(colour);
			shapeRenderer.circle(c.x, c.y, (circle.size / 2) - 2);
			shapeRenderer.end();
		}
	}
	
	private class Square {
		BoundingBox hitBox;
		float speed = Math.max((float)Math.random() + 0.5f, 0.7f) * 150.0f * Gdx.graphics.getDeltaTime();
		float maxSpeed;
		int size = 30;
		boolean raveCube;
		boolean mommaRaveCube;
		int hp;
		int maxhp;
		float maxv = 0.9f;
		long lastSpawnCube;
		
		// Vectors
		Vector3 dir;
		Vector3 v;
		
		// Constructor
		Square(float x, float y) {
			raveCube = Math.random() < 0.05;
			mommaRaveCube = !raveCube && Math.random() < 0.02;
			hp = 1;
			if (raveCube) {size *= 2; hp = 5; speed *= 0.8;}
			if (mommaRaveCube) {size *= 4; hp = 30; speed *= 0.6;}
			maxhp = hp;
			hitBox = new BoundingBox(new Vector3(x, y, 0), new Vector3(x + size, y + size, size));
			v = new Vector3(0, 0, 0);
			dir = circle.centre().cpy().sub(hitBox.getCenter()).nor().scl(speed);
			maxSpeed = speed;
			lastSpawnCube = 0;
		}
		
		Square(float x, float y, float s) {
			raveCube = Math.random() < 0.05;
			hp = 1;
			if (raveCube) {size *= 2; hp = 5;}
			maxhp = hp;
			hitBox = new BoundingBox(new Vector3(x, y, 0), new Vector3(x + size, y + size, size));
			v = new Vector3(0, 0, 0);
			dir = circle.centre().cpy().sub(hitBox.getCenter()).nor().scl(speed);
			maxSpeed = speed;
		}
		
		// Move the square toward the circle, slowing to avoid other squares
		void move() {
			accel();
			moveBy(1);
			if (mommaRaveCube && TimeUtils.nanoTime() - lastSpawnCube > 1000000000) {
				spawnSquare(hitBox.getCenter(), speed);
				lastSpawnCube = TimeUtils.nanoTime();
			}
		}
		
		void moveBy(int scl) {
			// Amount to scale by to avoid other squares
			float scale = scl;
			
			// Direction to circle, normalised and scaled by the speed
			dir = circle.centre().sub(hitBox.getCenter()).nor().scl(speed * v.len());
			
			Vector3 min = hitBox.getMin();
			Vector3 max = hitBox.getMax();
					
			Iterator<Square> iter = squares.iterator();
			
			// Check if we hit any other squares with the given trajectory
			while(iter.hasNext()) {
				Square square = iter.next();
				if (!square.equals(this) && ((mommaRaveCube && square.mommaRaveCube) || (!mommaRaveCube && !square.mommaRaveCube))) {
					BoundingBox target = new BoundingBox(min.cpy().add(dir.cpy().scl(scale)), max.cpy().add(dir.cpy().scl(scale)));
					while (square.hitBox.intersects(target) && scale > 0) {
						// Slow down to avoid other squares
						scale = Math.max(0, scale - 0.125f);
						target.set(min.cpy().add(dir.cpy().scl(scale)), max.cpy().add(dir.cpy().scl(scale)));
					}
				}
			}
			
			// Scale by found value then move
			v.scl(scale);
			dir.scl(scale);
			hitBox.set(min.add(dir), max.add(dir));
		}
		
		// Accelerate in the right direction
		void accel() {
			if (dir.x > 0) v.x = Math.min(v.x + 0.025f, maxv);
			else if (dir.x < 0) v.x = Math.max(v.x - 0.025f,  -maxv);
			if (dir.y > 0) v.y = Math.min(v.y + 0.025f, maxv);
			else if (dir.y < 0) v.y = Math.max(v.x - 0.025f, -maxv);
		}
		
		// Nudge the square back a little
		void nudge() {
			v.x *= 0.25;
			v.y *= 0.25;
			moveBy(-10);
		}
		
		void hit(boolean genocide) {
			hp--;
			if (hp <= 0) {
				if (!genocide) { 
					if (raveCube) raveCubeDieSound.play(0.75f);
					else if (mommaRaveCube) mrcDieSound.play(0.75f);
					else squareDieSound.play(0.75f);
				}
				blood.add(new Point(hitBox.getMin().x, hitBox.getMin().y));
			}
			else nudge();
			score += 5;
		}
		
		void render() {
			shapeRenderer.begin(ShapeType.Filled);
			Vector3 s = hitBox.getMin();
			// Line
			shapeRenderer.setColor(0, 0, 0, 1);
			shapeRenderer.rect(s.x - 2, s.y - 2, size + 4, size+4);
			// Fill
			if(raveCube || mommaRaveCube){
				// Random colour
				shapeRenderer.setColor((float)Math.random(), (float)Math.random(), (float)Math.random(), 1);
				shapeRenderer.rect(s.x+2, s.y+2, size-4, size-4);
				// Draw health bar
				shapeRenderer.setColor(0, 0, 0, 1);
				shapeRenderer.rect(s.x, s.y + size + 5, size, 5);
				shapeRenderer.setColor(1, 0, 0, 1);
				shapeRenderer.rect(s.x, s.y + size + 5, (size * hp)/maxhp, 5);
			} else {
				shapeRenderer.setColor(0, 1, 1, 1);
				shapeRenderer.rect(s.x+2, s.y+2, size-4, size-4);
			}
			shapeRenderer.end();
		}
	}
	
	public enum PowerUpType {
		RateOfFire, Slow, Genocide, AoE
	}
	
	public class PowerUp {
		PowerUpType type;
		Color colour;
		Vector3 pos;
		BoundingBox hitBox;
		int size;
		
		public PowerUp() {
			size = 28;

			// Type
			int rand = (int) (Math.random() * 10);
			switch (rand) {
			case 0:
				type = PowerUpType.Slow;
				colour = Color.BLUE;
				break;
			case 1:
				type = PowerUpType.AoE;
				colour = Color.YELLOW;
				break;
			case 2:
				type = PowerUpType.RateOfFire;
				colour = Color.RED;
				break;
			case 3:
				type = PowerUpType.RateOfFire;
				colour = Color.RED;
				break;
			case 4:
				type = PowerUpType.Genocide;
				colour = Color.BLACK;
				break;
			case 5:
				type = PowerUpType.AoE;
				colour = Color.YELLOW;
				break;
			case 6:
				type = PowerUpType.AoE;
				colour = Color.YELLOW;
				break;
			case 7:
				type = PowerUpType.Slow;
				colour = Color.BLUE;
				break;
			case 8:
				type = PowerUpType.Genocide;
				colour = Color.BLACK;
				break;
			case 9:
				type = PowerUpType.RateOfFire;
				colour = Color.RED;
				break;
			case 10:
				type = PowerUpType.Slow;
				colour = Color.BLUE;
				break;
			default:
				throw new Error("Undefined powerup type");
			}
			
			// Position
			boolean placed = false;
			while (!placed) {
				placed = true;
				
				pos = new Vector3();
				rand = (int) (Math.random() * (windowWidth - 200));
				pos.x = 100 + rand;
				rand = (int) (Math.random() * (windowHeight - 200));
				pos.y = 100 + rand;
				pos.z = 0;
				// Set up hitbox
				hitBox = new BoundingBox(pos, pos.cpy().add(size, size, size));
				
				for (PowerUp p2 : powerups) {
					if (p2.hitBox.intersects(hitBox)) placed = false;
				}
			}
		}
		
		void pickUp() {
			switch (type) {
			case Slow:
				slowSound.play();
				// Halve speed of all squares
				for (Square square : squares) {
					square.speed -= 0.5 * square.maxSpeed;
				}
				break;
			case RateOfFire:
				rofSound.play();
				// Increase RoF by 25%
				RoF *= 0.75;
				RoFonSince = TimeUtils.nanoTime();
				break;
			case Genocide:
				genocideSound.play();
				// Harm all squares
				Iterator<Square> iter = squares.iterator();
				for (int i = 0; i < squares.size; i++) {
					Square square = squares.get(i);
					square.hit(true);
				}
				break;
			case AoE:
				specialSound.play(0.5f);
				AoEon = true;
				AoEonSince = TimeUtils.nanoTime();
				break;
			default:
				throw new Error("Undefined powerup type");
			}
			circle.colour = colour;
		}
		
		
		void render() {
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.setColor(Color.WHITE);
			shapeRenderer.circle(pos.x + size / 2, pos.y + size / 2, size / 2 + 2);
			shapeRenderer.setColor(colour);
			shapeRenderer.circle(pos.x + size / 2, pos.y + size / 2, (size / 2) - 2);
			shapeRenderer.end();
		}
	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {
		if (state != GAME_OVER) state = GAME_PAUSED;
	}
	
	public class Point {
		float x;
		float y;
		Point(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	public class Cursor {
		Vector3 pos;
		Vector3 offx;
		Vector3 offy;
		Cursor (float x, float y) {
			pos = new Vector3(x, y, 0);
			offx = new Vector3(10, 0, 0);
			offy = new Vector3(0, 10, 0);
		}
		public void move(float x, float y) {
			pos.x = x;
			pos.y = y;
		}
		public void render() {
			shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.setColor(1, 1, 1, 1);
			shapeRenderer.line(pos.cpy().sub(offx), pos.cpy().add(offx));
			shapeRenderer.line(pos.cpy().sub(offy), pos.cpy().add(offy));
			shapeRenderer.end();
		}
	}
}
