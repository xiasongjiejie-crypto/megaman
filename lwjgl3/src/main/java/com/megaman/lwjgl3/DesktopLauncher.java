package com.megaman.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.megaman.CoreGame;

/** 桌面端（LWJGL3）启动器。 */
public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Mega Platformer");
        config.setWindowedMode(960, 540);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new CoreGame(), config);
    }
}
