package engine;

public final class GameLoop implements Runnable {

    private final GameWindow window;

    private volatile boolean running = true;

    private long lastTime;

    private float deltaTime;
    private float timeScale = 1.0f;

    // Fixed-timestep
    private float fixedStep = 1f / 60f;
    private int   fixedHz   = 60;
    private float accumulator = 0f;

    // Frame cap (0 = uncapped)
    private int  targetFps      = 0;
    private long targetFrameNs  = 0L;

    // Max dt clamp — prevents spiral of death after debugger pause
    private float maxDeltaTime = 1f / 15f;

    // FPS tracking
    private long fps;
    private long frameCount;
    private long fpsTimer;

    public GameLoop(GameWindow window) {
        this.window = window;
    }

    public void start() {
        new Thread(this, "GameLoop").start();
    }

    @Override
    public void run() {

        lastTime = System.nanoTime();
        fpsTimer = System.nanoTime();

        while (running) {

            long now = System.nanoTime();

            // ---------------- delta time ----------------
            deltaTime = (now - lastTime) / 1_000_000_000f;
            if (deltaTime > maxDeltaTime) deltaTime = maxDeltaTime;
            lastTime = now;

            float scaledDt = deltaTime * timeScale;

            if (window.isMouseCaptured()) {
                java.awt.Point center = window.getScreenCenter();
                if (center != null) {
                    java.awt.Point cur = java.awt.MouseInfo.getPointerInfo().getLocation();
                    Input.setRawMouseDelta(cur.x - center.x, cur.y - center.y);
                    window.warpToScreenCenter();
                }
            }
            Input.poll();
            // ---------------- fixed update ----------------
            accumulator += scaledDt;
            while (accumulator >= fixedStep) {
                window.sceneManager.fixedUpdate(fixedStep);
                accumulator -= fixedStep;
            }
            // ---------------- update/render ----------------
            window.sceneManager.update(scaledDt);

            FramebufferPixelGraphics graphics = window.getGraphicsAPI();
            graphics.update(scaledDt);
            window.sceneManager.render(graphics);
            graphics.renderCursor();
            window.present();

            // ---------------- FPS tracking ----------------
            frameCount++;

            if (now - fpsTimer >= 1_000_000_000L) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer = now;
            }

            // ---------------- frame cap ----------------
            if (targetFrameNs > 0) {
                long elapsed = System.nanoTime() - now;
                long sleepNs = targetFrameNs - elapsed;
                if (sleepNs > 1_000_000L) {
                    try { Thread.sleep(sleepNs / 1_000_000L); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    // ---------------- API ----------------

    public float getDeltaTime() {
        return deltaTime * timeScale;
    }

    public float getRawDeltaTime() {
        return deltaTime;
    }

    public long getFps() {
        return fps;
    }

    public void setTimeScale(float scale) {
        this.timeScale = scale;
    }

    public float getTimeScale() {
        return timeScale;
    }

    public void setTargetFps(int fps) {
        this.targetFps     = fps;
        this.targetFrameNs = fps > 0 ? 1_000_000_000L / fps : 0L;
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setFixedHz(int hz) {
        this.fixedHz   = hz;
        this.fixedStep = hz > 0 ? 1f / hz : 1f / 60f;
    }

    public int getFixedHz() {
        return fixedHz;
    }

    public float getFixedStep() {
        return fixedStep;
    }

    public void setMaxDeltaTime(float max) {
        this.maxDeltaTime = max;
    }

    public float getMaxDeltaTime() {
        return maxDeltaTime;
    }

    public void stop() {
        running = false;
    }
}