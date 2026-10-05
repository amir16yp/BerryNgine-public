package berryngine;

import javax.swing.SwingUtilities;
import java.util.Arrays;

/** The same basic rendering workload is included on both comparison branches. */
public final class TestScene implements Scene {
    private final PixelGraphics sprite = ShapeGenerator.filledCircle(12, 0x88ff8800);
    private float elapsed;
    private int frame;
    private int titleFrames;
    private float titleTimer;

    public static void main(String[] args) {
        if (args.length > 0 && "--benchmark".equals(args[0])) {
            int frames = args.length > 1 ? Integer.parseInt(args[1]) : 300;
            int threads = args.length > 2 ? Integer.parseInt(args[2]) :
                    Math.max(1, Runtime.getRuntime().availableProcessors());
            benchmark(frames, threads);
            return;
        }
        GameWindow.builder("BerryNgine test scene", 640, 360)
                .targetFps(0)
                .run(new TestScene());
    }

    private static void benchmark(int frames, int threads) {
        if (frames < 1 || threads < 1) throw new IllegalArgumentException("frames and threads must be positive");
        TestScene scene = new TestScene();
        int[] pixels = new int[640 * 360];
        try (ThreadedPixelGraphics pg = new ThreadedPixelGraphics(pixels, 640, 360, threads)) {
            for (int i = 0; i < 30; i++) {
                scene.frame = i;
                pg.beginFrame();
                scene.render(null, pg);
                pg.execute();
            }
            long start = System.nanoTime();
            for (int i = 0; i < frames; i++) {
                scene.frame = i;
                pg.beginFrame();
                scene.render(null, pg);
                pg.execute();
            }
            long elapsed = System.nanoTime() - start;
            System.out.printf("renderer=threaded, frames=%d, threads=%d, ms/frame=%.3f, checksum=%d%n",
                    frames, threads, elapsed / 1e6 / frames, Arrays.hashCode(pixels));
        }
    }

    @Override public void onSceneEnter(GameWindow gw) { }
    @Override public void onSceneExit(GameWindow gw) { }

    @Override public void update(GameWindow gw, float dt) {
        elapsed += dt;
        frame++;
        titleFrames++;
        titleTimer += dt;
        if (titleTimer >= 1f) {
            int fps = Math.round(titleFrames / titleTimer);
            SwingUtilities.invokeLater(() -> gw.frame.setTitle("BerryNgine test scene - " + fps + " FPS"));
            titleTimer = 0f;
            titleFrames = 0;
        }
    }

    @Override public void render(GameWindow gw, ThreadedPixelGraphics pg) {
        pg.clear(Color.DARK_BLUE);
        for (int i = 0; i < 400; i++) {
            int x = (i * 73 + frame * 3) % 700 - 30;
            int y = (i * 47 + frame * 2) % 410 - 30;
            pg.fillRect(x, y, 24, 16, Color.BLUE);
            pg.drawImageBlended(sprite, x + 5, y + 3);
        }
        pg.drawLine(0, 0, pg.width - 1, pg.height - 1, Color.WHITE);
    }
}
