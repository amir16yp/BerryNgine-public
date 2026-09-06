package berryngine;

public final class Cursor {

    private PixelGraphics sprite;
    private IVec2 hotspot = new IVec2();
    private IVec2 position = new IVec2();
    private boolean visible = true;
    private boolean trackMouse = true;

    public Cursor(PixelGraphics sprite) {
        this(sprite, 0, 0);
    }

    public Cursor(PixelGraphics sprite, int hotX, int hotY) {
        this(sprite, new IVec2(hotX, hotY));
    }

    public Cursor(PixelGraphics sprite, IVec2 hotspot) {
        this.sprite = sprite;
        this.hotspot.set(hotspot);
    }

    public void setSprite(PixelGraphics sprite) {
        this.sprite = sprite;
    }

    public PixelGraphics getSprite() {
        return sprite;
    }

    public void setHotspot(IVec2 hotspot) {
        this.hotspot.set(hotspot);
    }

    public void setHotspot(int x, int y) {
        this.hotspot.set(x, y);
    }

    public IVec2 getHotspot() {
        return hotspot.copy();
    }

    public int getHotX() {
        return hotspot.x;
    }

    public int getHotY() {
        return hotspot.y;
    }

    public void setPosition(IVec2 position) {
        this.trackMouse = false;
        this.position.set(position);
    }

    public void setPosition(int x, int y) {
        this.trackMouse = false;
        this.position.set(x, y);
    }

    public IVec2 getPosition() {
        return position.copy();
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
            this.position.set(Input.getMouseScaledX(), Input.getMouseScaledY());
        }
    }

    public void render(PixelGraphics target) {
        if (!visible || sprite == null) return;
        target.drawImage(sprite, position.x - hotspot.x, position.y - hotspot.y);
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }
}
