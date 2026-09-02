package engine;

public interface Scene {

    void update(GameWindow gw, float dt);

    default void fixedUpdate(GameWindow gw, float fixedDt) {
    }

    void render(GameWindow gw, FramebufferPixelGraphics pg);

    void onSceneEnter(GameWindow gameWindow);

    void onSceneExit(GameWindow gameWindow);

    default void onWindowFocusChanged(GameWindow gameWindow, boolean focused) {
        if (!gameWindow.captureMouseByDefault) {
            return;
        }
        if (focused) {
            gameWindow.captureMouse();
        } else {
            gameWindow.releaseMouse();
        }
    }

}