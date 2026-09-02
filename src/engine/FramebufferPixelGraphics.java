package engine;

/**
 * A PixelGraphics that represents the main framebuffer.
 * Owns a Camera2D and an optional software Cursor, and provides
 * world-space drawing helpers that route through the camera.
 */
public class FramebufferPixelGraphics extends PixelGraphics {

    private final Camera2D camera;
    private Cursor cursor;

    public FramebufferPixelGraphics(int[] pixels, int width, int height) {
        super(pixels, width, height);
        this.camera = new Camera2D();
        this.camera.setViewport(width, height);
    }

    public FramebufferPixelGraphics(int width, int height) {
        super(width, height);
        this.camera = new Camera2D();
        this.camera.setViewport(width, height);
    }

    public Camera2D getCamera() {
        return camera;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    /**
     * Called once per frame by the game loop before the scene renders.
     */
    public void update(float dt) {
        camera.update(dt);
        if (cursor != null) {
            cursor.update();
        }
    }

    /**
     * Called once per frame by the game loop after the scene renders, so the cursor is always on top.
     */
    public void renderCursor() {
        if (cursor != null) {
            cursor.render(this);
        }
    }

    // ---------------- WORLD-SPACE DRAWING ----------------

    /**
     * True if a world-space rectangle intersects the camera view.
     */
    public boolean isVisibleWorld(float worldX, float worldY, float w, float h) {
        return camera.isVisible(worldX, worldY, w, h);
    }

    public void drawImageWorld(PixelGraphics src, float worldX, float worldY) {
        if (src == null) return;
        IVec2 s = camera.worldToScreen(worldX, worldY);
        if (camera.zoom == 1.0f) {
            drawImage(src, s.x, s.y);
        } else {
            int dw = Math.max(1, Math.round(src.width * camera.zoom));
            int dh = Math.max(1, Math.round(src.height * camera.zoom));
            drawImageScaled(src.pixels, src.width, src.height, s.x, s.y, dw, dh);
        }
    }

    public void drawImageBlendedWorld(PixelGraphics src, float worldX, float worldY) {
        if (src == null) return;
        IVec2 s = camera.worldToScreen(worldX, worldY);
        if (camera.zoom == 1.0f) {
            drawImageBlended(src, s.x, s.y);
        } else {
            int dw = Math.max(1, Math.round(src.width * camera.zoom));
            int dh = Math.max(1, Math.round(src.height * camera.zoom));
            drawImageScaledBlended(src.pixels, src.width, src.height, s.x, s.y, dw, dh);
        }
    }

    public void fillRectWorld(float worldX, float worldY, float w, float h, int color) {
        IVec2 s = camera.worldToScreen(worldX, worldY);
        int dw = Math.max(1, Math.round(w * camera.zoom));
        int dh = Math.max(1, Math.round(h * camera.zoom));
        fillRect(s.x, s.y, dw, dh, color);
    }

    public void drawRectWorld(float worldX, float worldY, float w, float h, int color) {
        IVec2 s = camera.worldToScreen(worldX, worldY);
        int dw = Math.max(1, Math.round(w * camera.zoom));
        int dh = Math.max(1, Math.round(h * camera.zoom));
        drawRect(s.x, s.y, dw, dh, color);
    }

    public void drawLineWorld(float worldX1, float worldY1, float worldX2, float worldY2, int color) {
        // worldToScreen returns a shared temp vector, so copy the first result
        IVec2 s1 = camera.worldToScreen(worldX1, worldY1);
        int x1 = s1.x, y1 = s1.y;
        IVec2 s2 = camera.worldToScreen(worldX2, worldY2);
        drawLine(x1, y1, s2.x, s2.y, color);
    }

    public void setPixelWorld(float worldX, float worldY, int color) {
        IVec2 s = camera.worldToScreen(worldX, worldY);
        setPixel(s.x, s.y, color);
    }
}
