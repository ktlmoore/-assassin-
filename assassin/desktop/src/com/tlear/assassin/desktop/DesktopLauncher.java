package com.tlear.assassin.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.tlear.assassin.Assassin;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Assassin";
		config.width = 1280;
		config.height = 800;
		new LwjglApplication(new Assassin(), config);
	}
}
