package engine;

public final class Input {

    private static int screenWidth = 1;
    private static int screenHeight = 1;
    private static int windowWidth = 1;
    private static int windowHeight = 1;
    private static final int NUM_KEYS = 256;

    private static final boolean[] rawKeys = new boolean[NUM_KEYS];
    private static final boolean[] keys = new boolean[NUM_KEYS];
    private static final boolean[] keysDown = new boolean[NUM_KEYS];
    private static final boolean[] keysUp = new boolean[NUM_KEYS];
    private static final boolean[] prevKeys = new boolean[NUM_KEYS];

    private static int mouseX, mouseY;
    private static int prevMouseX, prevMouseY;
    private static int mouseDeltaX, mouseDeltaY;
    private static int mouseWheel;
    private static int mouseWheelThisFrame;

    private static boolean hasRawDelta = false;
    private static int rawDeltaX, rawDeltaY;

    private static final boolean[] mouseButtons = new boolean[8];
    private static final boolean[] mouseButtonsDown = new boolean[8];
    private static final boolean[] mouseButtonsUp = new boolean[8];
    private static final boolean[] prevMouseButtons = new boolean[8];

    // Typed text (keyTyped events accumulated between polls)
    private static final StringBuilder rawTyped = new StringBuilder();
    private static String typedText = "";

    // =========================
    // INTERNAL SETTERS (ONLY engine uses these)
    // =========================

    static void setKey(int keyCode, boolean pressed) {
        if (keyCode < 0 || keyCode >= NUM_KEYS) return;
        rawKeys[keyCode] = pressed;
    }

    static void setMouseButton(int button, boolean pressed) {
        if (button <= 0 || button >= mouseButtons.length) return;
        mouseButtons[button] = pressed;
    }

    static void setMousePosition(int x, int y) {
        mouseX = x;
        mouseY = y;
    }

    static void setRawMouseDelta(int dx, int dy) {
        rawDeltaX = dx;
        rawDeltaY = dy;
        hasRawDelta = true;
    }

    static void addMouseWheel(int delta) {
        mouseWheel += delta;
    }

    static void addTypedChar(char c) {
        synchronized (rawTyped) {
            rawTyped.append(c);
        }
    }

    // =========================
    // FRAME UPDATE
    // =========================

    public static void poll() {

        // keyboard
        for (int i = 0; i < NUM_KEYS; i++) {
            boolean current = rawKeys[i];
            keysDown[i] = current && !prevKeys[i];
            keysUp[i] = !current && prevKeys[i];
            keys[i] = current;
            prevKeys[i] = current;
        }

        // mouse delta
        if (hasRawDelta) {
            mouseDeltaX = rawDeltaX;
            mouseDeltaY = rawDeltaY;
            hasRawDelta = false;
            rawDeltaX = 0;
            rawDeltaY = 0;
        } else {
            mouseDeltaX = mouseX - prevMouseX;
            mouseDeltaY = mouseY - prevMouseY;
        }
        prevMouseX = mouseX;
        prevMouseY = mouseY;

        // mouse wheel snapshot & reset
        mouseWheelThisFrame = mouseWheel;
        mouseWheel = 0;

        // typed text snapshot
        synchronized (rawTyped) {
            typedText = rawTyped.toString();
            rawTyped.setLength(0);
        }

        // mouse buttons
        for (int i = 1; i < mouseButtons.length; i++) {
            boolean current = mouseButtons[i];
            mouseButtonsDown[i] = current && !prevMouseButtons[i];
            mouseButtonsUp[i] = !current && prevMouseButtons[i];
            prevMouseButtons[i] = current;
        }
    }

    // =========================
    // PUBLIC API (UNCHANGED)
    // =========================

    public static int getMouseX() {
        return mouseX;
    }

    public static int getMouseY() {
        return mouseY;
    }

    public static int getMouseDeltaX() {
        return mouseDeltaX;
    }

    public static int getMouseDeltaY() {
        return mouseDeltaY;
    }

    public static int getMouseWheel() {
        return mouseWheelThisFrame;
    }

    public static boolean isMouseButton(int button) {
        return button > 0 && button < mouseButtons.length && mouseButtons[button];
    }

    public static boolean isMouseButtonDown(int button) {
        return button > 0 && button < mouseButtons.length && mouseButtonsDown[button];
    }

    public static boolean isMouseButtonUp(int button) {
        return button > 0 && button < mouseButtons.length && mouseButtonsUp[button];
    }

    public static boolean isKey(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS && keys[keyCode];
    }

    public static boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS && keysDown[keyCode];
    }

    public static boolean isKeyUp(int keyCode) {
        return keyCode >= 0 && keyCode < NUM_KEYS && keysUp[keyCode];
    }

    public static String getTypedText() {
        return typedText;
    }

    public static boolean hasTypedInput() {
        return !typedText.isEmpty();
    }

    static void setScreenSize(int w, int h) {
        screenWidth = w;
        screenHeight = h;
    }

    static void setWindowSize(int w, int h) {
        windowWidth = w;
        windowHeight = h;
    }

    public static int getMouseScaledX() {
        if (windowWidth == 0) return mouseX;
        return (int) ((mouseX / (double) windowWidth) * screenWidth);
    }

    public static int getMouseScaledY() {
        if (windowHeight == 0) return mouseY;
        return (int) ((mouseY / (double) windowHeight) * screenHeight);
    }

    // =========================
    // AXIS HELPERS
    // =========================

    public static float getAxisHorizontal() {
        float v = 0;
        if (keys[java.awt.event.KeyEvent.VK_A] || keys[java.awt.event.KeyEvent.VK_LEFT]) v -= 1f;
        if (keys[java.awt.event.KeyEvent.VK_D] || keys[java.awt.event.KeyEvent.VK_RIGHT]) v += 1f;
        return v;
    }

    public static float getAxisVertical() {
        float v = 0;
        if (keys[java.awt.event.KeyEvent.VK_W] || keys[java.awt.event.KeyEvent.VK_UP]) v -= 1f;
        if (keys[java.awt.event.KeyEvent.VK_S] || keys[java.awt.event.KeyEvent.VK_DOWN]) v += 1f;
        return v;
    }

    public static float getAxis(int negativeKey, int positiveKey) {
        float v = 0;
        if (keys[negativeKey]) v -= 1f;
        if (keys[positiveKey]) v += 1f;
        return v;
    }
}