package engine;

public final class Cursor {

    private PixelGraphics sprite;
    private int hotX;
    private int hotY;
    private int x;
    private int y;
    private boolean visible = true;
    private boolean trackMouse = true;

    public Cursor(PixelGraphics sprite) {
            this(sprite, 0, 0);
    }

    public Cursor(PixelGraphics sprite, int hotX, int hotY) {
        this.sprite = sprite;
        this.hotX = hotX;
        this.hotY = hotY;
    }

    public void setSprite(PixelGraphics sprite) {
        this.sprite = sprite;
    }

    public PixelGraphics getSprite() {
        return sprite;
    }

    public void setHotspot(int x, int y) {
        this.hotX = x;
        this.hotY = y;
    }

    public int getHotX() {
        return hotX;
    }

    public int getHotY() {
        return hotY;
    }

    public void setPosition(int x, int y) {
        this.trackMouse = false;
        this.x = x;
        this.y = y;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setTrackMouse(boolean trackMouse) {
        this.trackMouse = trackMouse;
    }

    public boolean isTrackingMouse() {
        return trackMouse;
    }

    public void update() {
        if (trackMouse) {
            this.x = Input.getMouseScaledX();
            this.y = Input.getMouseScaledY();
        }
    }

    public void render(PixelGraphics target) {
        if (!visible || sprite == null) return;
        target.drawImage(sprite, x - hotX, y - hotY);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
