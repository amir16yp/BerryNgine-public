package berryngine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Ordered, tile-parallel framebuffer renderer. Calls record commands; execute()
 * is the frame barrier. Textures must not be modified until execute() returns.
 */
public final class ThreadedPixelGraphics implements AutoCloseable {
    private static final int MAX_CACHED_STRING_PIXELS = 1_000_000;
    private static final int MAX_CACHED_STRINGS = 256;

    private static final class StringKey {
        final Object font;
        final String text;
        final int color;

        StringKey(Object font, String text, int color) {
            this.font = font; this.text = text; this.color = color;
        }
        @Override public int hashCode() {
            int hash = System.identityHashCode(font);
            return 31 * (31 * hash + text.hashCode()) + color;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof StringKey)) return false;
            StringKey key = (StringKey) other;
            return font == key.font && color == key.color && text.equals(key.text);
        }
    }

    private interface Command { void draw(Tile tile); }

    private static final class Tile {
        final int[] pixels;
        final int width, height, y0, y1;
        Tile(int[] pixels, int width, int height, int y0, int y1) {
            this.pixels = pixels; this.width = width; this.height = height;
            this.y0 = y0; this.y1 = y1;
        }
        void put(int x, int y, int color) {
            if (x >= 0 && x < width && y >= y0 && y < y1) pixels[y * width + x] = color;
        }
        void blend(int x, int y, int color) {
            if (x < 0 || x >= width || y < y0 || y >= y1) return;
            int alpha = color >>> 24;
            if (alpha == 0) return;
            int index = y * width + x;
            if (alpha == 255) { pixels[index] = color; return; }
            int dst = pixels[index], inverse = 255 - alpha;
            int a = alpha + ((dst >>> 24) * inverse) / 255;
            int r = (((color >>> 16) & 255) * alpha + ((dst >>> 16) & 255) * inverse) / 255;
            int g = (((color >>> 8) & 255) * alpha + ((dst >>> 8) & 255) * inverse) / 255;
            int b = ((color & 255) * alpha + (dst & 255) * inverse) / 255;
            pixels[index] = a << 24 | r << 16 | g << 8 | b;
        }
    }

    private final int[] pixels;
    public final int width, height;
    private final int threads;
    private final ExecutorService workers;
    private final List<Command> commands = new ArrayList<>();
    private final LinkedHashMap<StringKey, PixelGraphics> stringCache = new LinkedHashMap<>(16, 0.75f, true);
    private int cachedStringPixels;
    private final Camera2D camera = new Camera2D();
    private Cursor cursor;
    private int clipX0, clipY0, clipX1, clipY1;
    private float alpha = 1f;

    public ThreadedPixelGraphics(int[] pixels, int width, int height, int threads) {
        if (width <= 0 || height <= 0 || pixels.length < (long) width * height || threads <= 0)
            throw new IllegalArgumentException("Invalid framebuffer or thread count");
        this.pixels = pixels; this.width = width; this.height = height;
        this.threads = threads;
        this.workers = threads == 1 ? null : Executors.newFixedThreadPool(threads - 1, task -> {
            Thread thread = new Thread(task, "BerryNgine-Render");
            thread.setDaemon(true);
            return thread;
        });
        camera.setViewport(width, height);
        clearClip();
    }

    public int getThreadCount() { return threads; }
    public Camera2D getCamera() { return camera; }
    public Cursor getCursor() { return cursor; }
    public void setCursor(Cursor cursor) { this.cursor = cursor; }
    public void update(float dt) {
        camera.update(dt);
        if (cursor != null) cursor.update();
    }
    public void beginFrame() { commands.clear(); clearClip(); alpha = 1f; }
    public void setGlobalAlpha(float value) { alpha = Math.max(0f, Math.min(1f, value)); }
    public float getGlobalAlpha() { return alpha; }
    public void setClip(int x, int y, int w, int h) {
        clipX0 = Math.max(0, x); clipY0 = Math.max(0, y);
        clipX1 = Math.min(width, x + w); clipY1 = Math.min(height, y + h);
    }
    public void clearClip() { clipX0 = 0; clipY0 = 0; clipX1 = width; clipY1 = height; }
    private boolean inClip(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    public void clear(int color) {
        commands.add(tile -> {
            for (int y = tile.y0; y < tile.y1; y++)
                java.util.Arrays.fill(tile.pixels, y * width, (y + 1) * width, color);
        });
    }
    public void setPixel(int x, int y, int color) {
        int x0 = clipX0, y0 = clipY0, x1 = clipX1, y1 = clipY1;
        commands.add(tile -> { if (inClip(x, y, x0, y0, x1, y1)) tile.put(x, y, color); });
    }
    public void blendPixel(int x, int y, int color) {
        int x0 = clipX0, y0 = clipY0, x1 = clipX1, y1 = clipY1;
        commands.add(tile -> { if (inClip(x, y, x0, y0, x1, y1)) tile.blend(x, y, color); });
    }
    public void fillRect(int x, int y, int w, int h, int color) { rect(x, y, w, h, color, false); }
    public void fillRectBlended(int x, int y, int w, int h, int color) { rect(x, y, w, h, color, true); }
    private void rect(int x, int y, int w, int h, int color, boolean blended) {
        int x0 = Math.max(clipX0, x), y0 = Math.max(clipY0, y);
        int x1 = Math.min(clipX1, x + w), y1 = Math.min(clipY1, y + h);
        commands.add(tile -> {
            for (int row = Math.max(y0, tile.y0); row < Math.min(y1, tile.y1); row++) {
                if (blended) {
                    for (int col = x0; col < x1; col++) tile.blend(col, row, color);
                } else if (x0 < x1) {
                    java.util.Arrays.fill(tile.pixels, row * width + x0, row * width + x1, color);
                }
            }
        });
    }
    public void drawLine(int x0, int y0, int x1, int y1, int color) {
        int cx0 = clipX0, cy0 = clipY0, cx1 = clipX1, cy1 = clipY1;
        commands.add(tile -> {
            int x = x0, y = y0, dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            while (true) {
                if (inClip(x, y, cx0, cy0, cx1, cy1)) tile.put(x, y, color);
                if (x == x1 && y == y1) break;
                int twice = 2 * err;
                if (twice >= dy) { err += dy; x += sx; }
                if (twice <= dx) { err += dx; y += sy; }
            }
        });
    }
    public void drawHorizontalLine(int x, int y, int length, int color) {
        fillRect(x, y, length, 1, color);
    }
    public void drawVLine(int x, int y0, int y1, int color) {
        fillRect(x, Math.min(y0, y1), 1, Math.abs(y1 - y0) + 1, color);
    }
    public void drawRect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        drawHorizontalLine(x, y, w, color);
        drawHorizontalLine(x, y + h - 1, w, color);
        drawVLine(x, y, y + h - 1, color);
        drawVLine(x + w - 1, y, y + h - 1, color);
    }
    public void drawImage(PixelGraphics image, int x, int y) { image(image, x, y, image == null ? 0 : image.width, image == null ? 0 : image.height, false); }
    public void drawImageBlended(PixelGraphics image, int x, int y) { image(image, x, y, image == null ? 0 : image.width, image == null ? 0 : image.height, true); }
    public void drawImageScaled(PixelGraphics image, int x, int y, int w, int h) { image(image, x, y, w, h, false); }
    public void drawImageScaledBlended(PixelGraphics image, int x, int y, int w, int h) { image(image, x, y, w, h, true); }
    public void drawImageScaled(int[] source, int sw, int sh, int x, int y, int w, int h) {
        image(new PixelGraphics(source, sw, sh), x, y, w, h, false);
    }
    public void drawImageScaledBlended(int[] source, int sw, int sh, int x, int y, int w, int h) {
        image(new PixelGraphics(source, sw, sh), x, y, w, h, true);
    }
    private void image(PixelGraphics image, int x, int y, int w, int h, boolean blended) {
        if (image == null || w <= 0 || h <= 0) return;
        int[] source = image.pixels;
        int sw = image.width, sh = image.height;
        int x0 = Math.max(x, clipX0), y0 = Math.max(y, clipY0);
        int x1 = Math.min(x + w, clipX1), y1 = Math.min(y + h, clipY1);
        float opacity = alpha;
        commands.add(tile -> {
            for (int row = Math.max(y0, tile.y0); row < Math.min(y1, tile.y1); row++) {
                int sy = (int) ((long) (row - y) * sh / h);
                for (int col = x0; col < x1; col++) {
                    int sx = (int) ((long) (col - x) * sw / w);
                    int color = source[sy * sw + sx];
                    int a = (int) ((color >>> 24) * opacity);
                    if (a == 0) continue;
                    color = (color & 0x00ffffff) | a << 24;
                    if (blended) tile.blend(col, row, color);
                    else tile.put(col, row, color);
                }
            }
        });
    }
    public void renderString(BitmapFont font, String text, int x, int y, int color) {
        drawImage(font.getStringImage(text, color), x, y);
    }
    public void renderString(SpriteSheetFont font, String text, int x, int y, int color) {
        drawImage(font.getStringImage(text, color), x, y);
    }
    /** Cache the generated bitmap for repeated text, font, and color combinations. */
    public void renderStringCached(BitmapFont font, String text, int x, int y, int color) {
        Objects.requireNonNull(font, "font");
        drawImage(cachedString(font, text, color), x, y);
    }
    /** Cache the generated bitmap for repeated text, font, and color combinations. */
    public void renderStringCached(SpriteSheetFont font, String text, int x, int y, int color) {
        Objects.requireNonNull(font, "font");
        drawImage(cachedString(font, text, color), x, y);
    }
    private PixelGraphics cachedString(Object font, String text, int color) {
        Objects.requireNonNull(text, "text");
        StringKey key = new StringKey(font, text, color);
        PixelGraphics image = stringCache.get(key);
        if (image != null) return image;
        image = font instanceof BitmapFont
                ? ((BitmapFont) font).getStringImage(text, color)
                : ((SpriteSheetFont) font).getStringImage(text, color);
        int area = image.pixels.length;
        if (area > MAX_CACHED_STRING_PIXELS) return image;
        while (!stringCache.isEmpty() &&
                (stringCache.size() >= MAX_CACHED_STRINGS || cachedStringPixels + area > MAX_CACHED_STRING_PIXELS)) {
            Iterator<Map.Entry<StringKey, PixelGraphics>> iterator = stringCache.entrySet().iterator();
            Map.Entry<StringKey, PixelGraphics> eldest = iterator.next();
            cachedStringPixels -= eldest.getValue().pixels.length;
            iterator.remove();
        }
        stringCache.put(key, image);
        cachedStringPixels += area;
        return image;
    }
    /** Clear cached text after changing a font's glyphs or atlas. */
    public void clearStringCache() { stringCache.clear(); cachedStringPixels = 0; }
    int cachedStringCount() { return stringCache.size(); }
    public boolean isVisibleWorld(float x, float y, float w, float h) { return camera.isVisible(x, y, w, h); }
    public void drawImageWorld(PixelGraphics image, float x, float y) {
        IVec2 screen = camera.worldToScreen(x, y);
        if (image != null) drawImageScaled(image, screen.x, screen.y,
                Math.max(1, Math.round(image.width * camera.zoom)), Math.max(1, Math.round(image.height * camera.zoom)));
    }
    public void drawImageBlendedWorld(PixelGraphics image, float x, float y) {
        IVec2 screen = camera.worldToScreen(x, y);
        if (image != null) drawImageScaledBlended(image, screen.x, screen.y,
                Math.max(1, Math.round(image.width * camera.zoom)), Math.max(1, Math.round(image.height * camera.zoom)));
    }
    public void fillRectWorld(float x, float y, float w, float h, int color) {
        IVec2 screen = camera.worldToScreen(x, y);
        fillRect(screen.x, screen.y, Math.max(1, Math.round(w * camera.zoom)), Math.max(1, Math.round(h * camera.zoom)), color);
    }
    public void drawRectWorld(float x, float y, float w, float h, int color) {
        IVec2 screen = camera.worldToScreen(x, y);
        drawRect(screen.x, screen.y, Math.max(1, Math.round(w * camera.zoom)),
                Math.max(1, Math.round(h * camera.zoom)), color);
    }
    public void drawLineWorld(float x0, float y0, float x1, float y1, int color) {
        IVec2 start = camera.worldToScreen(x0, y0);
        int sx = start.x, sy = start.y;
        IVec2 end = camera.worldToScreen(x1, y1);
        drawLine(sx, sy, end.x, end.y, color);
    }
    public void setPixelWorld(float x, float y, int color) {
        IVec2 screen = camera.worldToScreen(x, y);
        setPixel(screen.x, screen.y, color);
    }
    public void renderCursor() {
        if (cursor != null) {
            Cursor current = cursor;
            // Cursor's immediate renderer is a full-frame stage after queued drawing.
            effect(target -> current.render(target));
        }
    }

    /** Finish queued drawing, then apply an ordered full-frame effect. */
    public void effect(Consumer<PixelGraphics> effect) {
        Objects.requireNonNull(effect, "effect");
        execute();
        effect.accept(new PixelGraphics(pixels, width, height));
    }

    /** Execute all recorded commands before the buffer is presented. */
    public void execute() {
        if (commands.isEmpty()) return;
        // Recording is confined to the game loop thread. Reuse the command list
        // after the frame barrier instead of copying it on every frame.
        List<Command> batch = commands;
        int tileCount = Math.min(threads, height);
        if (tileCount == 1 || workers == null) {
            try { drawTile(batch, 0, height); }
            finally { commands.clear(); }
            return;
        }
        List<Future<?>> futures = new ArrayList<>(tileCount - 1);
        for (int i = 1; i < tileCount; i++) {
            final int start = i * height / tileCount, end = (i + 1) * height / tileCount;
            futures.add(workers.submit(() -> drawTile(batch, start, end)));
        }
        Throwable failure = null;
        boolean interrupted = false;
        try { drawTile(batch, 0, height / tileCount); }
        catch (Throwable error) { failure = error; }
        for (Future<?> future : futures) {
            boolean done = false;
            while (!done) {
                try { future.get(); done = true; }
                catch (InterruptedException e) {
                    interrupted = true;
                    if (failure == null) failure = e;
                }
                catch (ExecutionException e) {
                    if (failure == null) failure = e.getCause();
                    done = true;
                }
            }
        }
        commands.clear();
        if (interrupted) Thread.currentThread().interrupt();
        if (failure != null) throw new IllegalStateException("Render failed", failure);
    }
    private void drawTile(List<Command> batch, int y0, int y1) {
        Tile tile = new Tile(pixels, width, height, y0, y1);
        for (Command command : batch) command.draw(tile);
    }
    @Override public void close() { if (workers != null) workers.shutdownNow(); }
}
