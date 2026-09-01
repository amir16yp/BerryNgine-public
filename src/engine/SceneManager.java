package engine;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SceneManager {

    private volatile Scene currentScene;

    private final GameWindow window;

    public SceneManager(GameWindow window) {
        this.window = window;
        window.frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                notifyFocusChanged(true);
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                notifyFocusChanged(false);
            }
        });
    }

    private void notifyFocusChanged(boolean focused) {
        if (currentScene != null) {
            currentScene.onWindowFocusChanged(window, focused);
        }
    }

    public void setScene(Scene scene) {
        if (this.currentScene != null)
        {
            this.currentScene.onSceneExit(window);
        }
        this.currentScene = scene;
        this.currentScene.onSceneEnter(window);
        // 🔥 force focus back to the game canvas
        EventQueue.invokeLater(() -> {
            window.requestFocusInWindow();
        });
    }

    public Scene getScene() {
        return currentScene;
    }

    public void fixedUpdate(float fixedDt) {
        if (currentScene != null) {
            currentScene.fixedUpdate(window, fixedDt);
        }
    }

    public void update(float dt) {
        if (currentScene != null) {
            currentScene.update(window, dt);
        }
    }

    public void render(FramebufferPixelGraphics pg) {
        if (currentScene != null) {

            currentScene.render(window, pg);
        }
    }
}