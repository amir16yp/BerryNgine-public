package engine;

public final class Camera2D {

    public final Vec2 position = new Vec2();
    public float zoom = 1.0f;
    public float rotation = 0.0f;

    private final IVec2 viewport = new IVec2(1, 1);

    private final Vec2 shake = new Vec2();
    private final IVec2 tempScreen = new IVec2();
    private final Vec2 tempWorld = new Vec2();
    private float shakeTrauma;
    private float shakeDecay = 5.0f;
    private float shakeMaxOffset = 10.0f;

    public Camera2D() {
    }

    public Camera2D(float x, float y) {
        position.set(x, y);
    }

    public Camera2D(int viewportWidth, int viewportHeight, float x, float y) {
        viewport.set(viewportWidth, viewportHeight);
        position.set(x, y);
    }

    public void setViewport(int width, int height) {
        viewport.set(width, height);
    }

    public IVec2 getViewport() {
        return viewport;
    }

    public int getViewportWidth() {
        return viewport.x;
    }

    public int getViewportHeight() {
        return viewport.y;
    }

    public void centerOn(float worldX, float worldY) {
        position.set(worldX, worldY);
    }

    public void centerOn(Vec2 target) {
        centerOn(target.x, target.y);
    }

    public void follow(float targetX, float targetY, float dt, float smoothSpeed) {
        float t = Mathf.clamp01(smoothSpeed * dt);
        position.x += (targetX - position.x) * t;
        position.y += (targetY - position.y) * t;
    }

    public void follow(Vec2 target, float dt, float smoothSpeed) {
        follow(target.x, target.y, dt, smoothSpeed);
    }

    public IVec2 worldToScreen(float worldX, float worldY) {
        float dx = worldX - position.x;
        float dy = worldY - position.y;

        if (rotation != 0.0f) {
            float cos = Mathf.cos(rotation);
            float sin = Mathf.sin(rotation);
            float rx = dx * cos - dy * sin;
            float ry = dx * sin + dy * cos;
            dx = rx;
            dy = ry;
        }

        float sx = dx * zoom - shake.x + viewport.x / 2.0f;
        float sy = dy * zoom - shake.y + viewport.y / 2.0f;

        return tempScreen.set((int) sx, (int) sy);
    }

    public IVec2 worldToScreen(Vec2 world) {
        return worldToScreen(world.x, world.y);
    }

    public Vec2 screenToWorld(float screenX, float screenY) {
        float dx = (screenX - viewport.x / 2.0f + shake.x) / zoom;
        float dy = (screenY - viewport.y / 2.0f + shake.y) / zoom;

        if (rotation != 0.0f) {
            float cos = Mathf.cos(-rotation);
            float sin = Mathf.sin(-rotation);
            float rx = dx * cos - dy * sin;
            float ry = dx * sin + dy * cos;
            dx = rx;
            dy = ry;
        }

        return tempWorld.set(dx + position.x, dy + position.y);
    }

    public Vec2 screenToWorld(Vec2 screen) {
        return screenToWorld(screen.x, screen.y);
    }

    public Vec2 screenToWorld(IVec2 screen) {
        return screenToWorld(screen.x, screen.y);
    }

    public Vec2 getWorldMousePosition() {
        return screenToWorld(Input.getMouseScaledX(), Input.getMouseScaledY());
    }

    public float getWorldMouseX() {
        return getWorldMousePosition().x;
    }

    public float getWorldMouseY() {
        return getWorldMousePosition().y;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void setRotation(float radians) {
        this.rotation = radians;
    }

    public void setRotationDegrees(float degrees) {
        this.rotation = Mathf.toRadians(degrees);
    }

    public float getRotationDegrees() {
        return Mathf.toDegrees(rotation);
    }

    public void addTrauma(float amount) {
        this.shakeTrauma = Mathf.clamp01(this.shakeTrauma + amount);
    }

    public void setShakeDecay(float decay) {
        this.shakeDecay = decay;
    }

    public void setShakeMaxOffset(float maxOffset) {
        this.shakeMaxOffset = maxOffset;
    }

    public void update(float dt) {
        if (shakeTrauma <= 0.0f) {
            shake.zero();
            return;
        }

        float traumaSq = shakeTrauma * shakeTrauma;
        float angle = (float) Math.random() * Mathf.TWO_PI;
        float offset = (float) Math.random() * shakeMaxOffset * traumaSq;

        shake.set(Mathf.cos(angle) * offset, Mathf.sin(angle) * offset);
        shakeTrauma = Mathf.clamp01(shakeTrauma - shakeDecay * dt);
    }

    public boolean isVisible(float worldX, float worldY, float width, float height) {
        worldToScreen(worldX, worldY);
        int tlx = tempScreen.x, tly = tempScreen.y;
        worldToScreen(worldX + width, worldY + height);
        int brx = tempScreen.x, bry = tempScreen.y;

        int minX = Math.min(tlx, brx);
        int maxX = Math.max(tlx, brx);
        int minY = Math.min(tly, bry);
        int maxY = Math.max(tly, bry);

        return maxX >= 0 && minX <= viewport.x && maxY >= 0 && minY <= viewport.y;
    }

    public float getLeft() {
        return screenToWorld(0, 0).x;
    }

    public float getRight() {
        return screenToWorld(viewport.x, 0).x;
    }

    public float getTop() {
        return screenToWorld(0, 0).y;
    }

    public float getBottom() {
        return screenToWorld(0, viewport.y).y;
    }
}
