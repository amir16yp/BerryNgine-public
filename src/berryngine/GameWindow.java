package berryngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Method;

public class GameWindow extends Canvas {

    // ---------------- BUILDER ----------------

    public static Builder builder(String title, int width, int height) {
        return new Builder(title, new IVec2(width, height));
    }

    public static Builder builder(String title, IVec2 internalResolution) {
        return new Builder(title, internalResolution);
    }

    public static final class Builder {
        private final String title;
        private final IVec2 internalResolution;
        private int windowScale = 1;
        private boolean resizable = true;
        private boolean fullscreen = false;
        private int targetFps = 0;
        private int fixedHz = 60;
        private boolean defaultMouseCapture = false;
        private PixelGraphics icon = null;

        private Builder(String title, IVec2 internalResolution) {
            this.title = title;
            this.internalResolution = internalResolution;
        }

        public Builder setCaptureMouseByDefault(boolean captureMouseByDefault) {
            this.defaultMouseCapture = captureMouseByDefault;
            return this;
        }

        /**
         * Initial window size = internal resolution * scale.
         */
        public Builder scale(int windowScale) {
            this.windowScale = Math.max(1, windowScale);
            return this;
        }

        public Builder resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        public Builder fullscreen(boolean fullscreen) {
            this.fullscreen = fullscreen;
            return this;
        }

        /**
         * 0 = uncapped.
         */
        public Builder targetFps(int fps) {
            this.targetFps = fps;
            return this;
        }

        public Builder fixedHz(int hz) {
            this.fixedHz = hz;
            return this;
        }

        public Builder icon(PixelGraphics icon) {
            this.icon = icon;
            return this;
        }

        public GameWindow build() {
            GameWindow window = new GameWindow(title, internalResolution, windowScale, resizable);
            window.gameLoop.setTargetFps(targetFps);
            window.gameLoop.setFixedHz(fixedHz);
            if (fullscreen) window.setBorderlessFullscreen(true);
            window.captureMouseByDefault = this.defaultMouseCapture;
            if (icon != null)
            {
                setIcon(window.frame, icon.toBufferedImage());
            }
            return window;
        }

        private static void setIcon(Frame frame, BufferedImage icon)
        {
            frame.setIconImage(icon);

            try
            {
                Class<?> taskbar = Class.forName("java.awt.Taskbar");

                Method supported = taskbar.getMethod("isTaskbarSupported");
                if (!(Boolean) supported.invoke(null))
                    return;

                Object instance = taskbar.getMethod("getTaskbar").invoke(null);

                Class<?> feature = Class.forName("java.awt.Taskbar$Feature");
                Object iconFeature = Enum.valueOf(
                        (Class<Enum>) feature,
                        "ICON_IMAGE"
                );

                if (!(Boolean) taskbar.getMethod(
                        "isSupported", feature
                ).invoke(instance, iconFeature))
                    return;

                taskbar.getMethod(
                        "setIconImage", Image.class
                ).invoke(instance, icon);
            }
            catch (Throwable ignored)
            {
            }
        }

        /**
         * Builds the window, sets the initial scene and starts the game loop.
         */
        public GameWindow run(Scene initialScene) {
            GameWindow window = build();
            window.run(initialScene);
            return window;
        }
    }

    public final JFrame frame;
    private final IVec2 internalResolution;
    public final SceneManager sceneManager;
    public final GameLoop gameLoop;
    public final SoundSystem soundSystem;
    public final AudioMixer audioMixer;
    private Robot robot;
    private boolean mouseCaptured = false;
    private boolean borderlessFullscreen = false;
    private Rectangle windowedBounds = null;
    private static final java.awt.Cursor BLANK_CURSOR;

    static {
        BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BLANK_CURSOR = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank");
    }

    public IVec2 getInternalResolution() {
        return internalResolution;
    }

    private final FramebufferPixelGraphics graphics;
    private final BufferedImage backBuffer;
    public boolean captureMouseByDefault = false;

    public GameWindow(String title, int width, int height) {
        this(title, new IVec2(width, height));
    }

    public GameWindow(String title, IVec2 internalResolution) {
        this(title, internalResolution, 1, true);
    }

    private GameWindow(String title, IVec2 internalResolution, int windowScale, boolean resizable) {
        this.internalResolution = internalResolution;
        Dimension windowDimension = new Dimension(internalResolution.x * windowScale, internalResolution.y * windowScale);
        setPreferredSize(windowDimension);
        setMinimumSize(new Dimension(internalResolution.x, internalResolution.y));

        setIgnoreRepaint(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        // ---------------- Frame ----------------
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(resizable);
        frame.setLayout(new BorderLayout());

        frame.add(this, BorderLayout.CENTER);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Input.setKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                Input.setKey(e.getKeyCode(), false);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                Input.addTypedChar(e.getKeyChar());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Input.setMouseButton(e.getButton(), true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Input.setMouseButton(e.getButton(), false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (mouseCaptured) setCursor(BLANK_CURSOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!mouseCaptured) setCursor(java.awt.Cursor.getDefaultCursor());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Input.setMousePosition(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Input.setMousePosition(e.getX(), e.getY());
            }
        });

        addMouseWheelListener(e -> Input.addMouseWheel(e.getWheelRotation()));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Input.setWindowSize(getWidth(), getHeight());
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);

        Input.setScreenSize(internalResolution.x, internalResolution.y);
        Input.setWindowSize(windowDimension.width, windowDimension.height);

        backBuffer = new BufferedImage(internalResolution.x, internalResolution.y, BufferedImage.TYPE_INT_ARGB);
        int[] sharedPixels = ((DataBufferInt) backBuffer.getRaster().getDataBuffer()).getData();
        graphics = new FramebufferPixelGraphics(sharedPixels, internalResolution.x, internalResolution.y);
        // ---------------- INPUT ----------------
//        addKeyListener(input);

        // Make sure canvas gets focus AFTER it exists in a visible frame
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
        });

        try {
            robot = new Robot();
        } catch (AWTException e) {
            robot = null;
        }
        this.sceneManager = new SceneManager(this);
        this.gameLoop = new GameLoop(this);
        this.soundSystem = new SoundSystem();
        this.audioMixer = new AudioMixer(soundSystem);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameLoop.stop();
                audioMixer.close();
                soundSystem.close();
            }
        });
    }

    // ---------------- LIFECYCLE ----------------

    /**
     * Shows the window (if hidden), sets the initial scene and starts the game loop.
     */
    public void run(Scene initialScene) {
        setScene(initialScene);
        start();
    }

    /**
     * Shows the window (if hidden) and starts the game loop.
     */
    public void start() {
        showWindow();
        gameLoop.start();
    }

    /**
     * Stops the game loop.
     */
    public void stop() {
        gameLoop.stop();
    }

    /**
     * Makes the window visible. Called automatically by start().
     */
    public void showWindow() {
        if (!frame.isVisible()) {
            frame.setVisible(true);
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }
    }

    // ---------------- CONVENIENCE DELEGATES ----------------

    public void setScene(Scene scene) {
        sceneManager.setScene(scene);
    }

    public Scene getScene() {
        return sceneManager.getScene();
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public float getDeltaTime() {
        return gameLoop.getDeltaTime();
    }

    public long getFps() {
        return gameLoop.getFps();
    }

    public void setTargetFps(int fps) {
        gameLoop.setTargetFps(fps);
    }

    public int getInternalWidth() {
        return internalResolution.x;
    }

    public int getInternalHeight() {
        return internalResolution.y;
    }

    public void captureMouse() {
        mouseCaptured = true;
        setCursor(BLANK_CURSOR);
        warpToScreenCenter();
    }

    public void releaseMouse() {
        mouseCaptured = false;
        setCursor(java.awt.Cursor.getDefaultCursor());
    }

    public boolean isMouseCaptured() {
        return mouseCaptured;
    }

    public boolean isBorderlessFullscreen() {
        return borderlessFullscreen;
    }

    public void setCursorVisible(boolean visible) {
        if (visible) {
            if (!mouseCaptured) setCursor(java.awt.Cursor.getDefaultCursor());
        } else {
            setCursor(BLANK_CURSOR);
        }
    }

    public void setBorderlessFullscreen(boolean fullscreen) {
        if (fullscreen == borderlessFullscreen) return;

        borderlessFullscreen = fullscreen;
        if (fullscreen) {
            windowedBounds = frame.getBounds();
            GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
            Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setBounds(screenBounds);
            frame.setVisible(true);
        } else {
            frame.dispose();
            frame.setUndecorated(false);
            if (windowedBounds != null) {
                frame.setBounds(windowedBounds);
            } else {
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
        }
        if (mouseCaptured) setCursor(BLANK_CURSOR);
        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }

    public Point getScreenCenter() {
        if (!isShowing()) return null;
        Point loc = getLocationOnScreen();
        return new Point(loc.x + getWidth() / 2, loc.y + getHeight() / 2);
    }

    public void warpToScreenCenter() {
        if (robot == null) return;
        Point c = getScreenCenter();
        if (c != null) robot.mouseMove(c.x, c.y);
    }

    // ---------------- API ----------------

    public FramebufferPixelGraphics getGraphicsAPI() {
        return graphics;
    }

    public Camera2D getCamera() {
        return graphics.getCamera();
    }

    public Cursor getSoftwareCursor() {
        return graphics.getCursor();
    }

    public void setSoftwareCursor(Cursor cursor) {
        graphics.setCursor(cursor);
    }

    public void clear(int color) {
        graphics.clear(color);
    }

    // ---------------- PRESENT ----------------

    public void present() {
        if (!isDisplayable()) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (getBufferStrategy() == null) {
            createBufferStrategy(2);
        }
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) return;

        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(backBuffer, 0, 0, w, h, null);
                g.dispose();
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
    }
}