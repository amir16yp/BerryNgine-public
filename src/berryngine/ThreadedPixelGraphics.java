package berryngine;

import java.awt.Paint;
import java.awt.image.BufferedImage;
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
public final class ThreadedPixelGraphics extends PixelGraphics implements AutoCloseable {
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

    private static final class StringLookup {
        Object font;
        String text;
        int color;
        void set(Object font, String text, int color) {
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
    private static final class CachedString {
        final int[] pixels;
        final int width, height;
        CachedString(int[] pixels, int width, int height) {
            this.pixels = pixels; this.width = width; this.height = height;
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

    private final int threads;
    private final ExecutorService workers;
    private final List<Command> commands = new ArrayList<>();
    private final List<Future<?>> futures = new ArrayList<>();
    private final Tile[] tiles;
    private final Runnable[] tileTasks;
    private final LinkedHashMap<StringKey, CachedString> stringCache = new LinkedHashMap<>(16, 0.75f, true);
    private int cachedStringPixels;
    private final StringLookup stringLookup = new StringLookup();
    private final PixelGraphics effectView;
    private final Camera2D camera = new Camera2D();
    private Cursor cursor;
    private int clipX0, clipY0, clipX1, clipY1;
    private float alpha = 1f;

    public ThreadedPixelGraphics(int[] pixels, int width, int height, int threads) {
        super(pixels, width, height);
        if (width <= 0 || height <= 0 || pixels.length < (long) width * height || threads <= 0)
            throw new IllegalArgumentException("Invalid framebuffer or thread count");
        this.threads = threads;
        this.effectView = new PixelGraphics(pixels, width, height);
        this.workers = threads == 1 ? null : Executors.newFixedThreadPool(threads - 1, task -> {
            Thread thread = new Thread(task, "BerryNgine-Render");
            thread.setDaemon(true);
            return thread;
        });
        int tileCount = Math.min(threads, height);
        this.tiles = new Tile[tileCount];
        this.tileTasks = new Runnable[tileCount];
        for (int i = 0; i < tileCount; i++) {
            tiles[i] = new Tile(pixels, width, height, i * height / tileCount, (i + 1) * height / tileCount);
            final int index = i;
            tileTasks[i] = () -> drawTile(tiles[index]);
        }
        camera.setViewport(width, height);
        clearClip();
    }

    public int getThreadCount() { return threads; }
    public IVec2 getSize() { return super.getSize(); }
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
    public int getPixel(int x, int y) {
        execute();
        return x < 0 || y < 0 || x >= width || y >= height ? 0 : pixels[y * width + x];
    }
    public PixelGraphics getSubImage(int x, int y, int w, int h) {
        execute();
        return super.getSubImage(x, y, w, h);
    }
    public BufferedImage toBufferedImage() {
        execute();
        return super.toBufferedImage();
    }
    public void copyFrom(PixelGraphics source) {
        if (source == null) return;
        if (source.width != width || source.height != height)
            throw new IllegalArgumentException("Source dimensions must match framebuffer");
        effect(target -> System.arraycopy(source.pixels, 0, pixels, 0, width * height));
    }
    /** The window owns the framebuffer array; rebinding it would break presentation. */
    @Override public void setBuffer(int[] buffer) {
        throw new UnsupportedOperationException("The window owns the framebuffer buffer");
    }
    @Override public PixelGraphics scale(int factor) {
        execute();
        return super.scale(factor);
    }
    @Override public void scaleTo(PixelGraphics target, int factor) {
        execute();
        super.scaleTo(target, factor);
    }
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
    @Override public void blendPixelGlobal(int x, int y, int color) {
        int sourceAlpha = (int) ((color >>> 24) * alpha);
        blendPixel(x, y, (color & 0x00ffffff) | sourceAlpha << 24);
    }
    public void fillRect(int x, int y, int w, int h, int color) { rect(x, y, w, h, color, false); }
    public void fillRectBlended(int x, int y, int w, int h, int color) { rect(x, y, w, h, color, true); }
    public void fillGradientRect(int x, int y, int w, int h, int color1, int color2, boolean horizontal) {
        int x0 = Math.max(x, clipX0), y0 = Math.max(y, clipY0);
        int x1 = Math.min(x + w, clipX1), y1 = Math.min(y + h, clipY1);
        if (x0 >= x1 || y0 >= y1) return;
        commands.add(tile -> {
            for (int row = Math.max(y0, tile.y0); row < Math.min(y1, tile.y1); row++) {
                for (int col = x0; col < x1; col++) {
                    float t = horizontal ? (x1 - x0 <= 1 ? 0f : (float) (col - x0) / (x1 - x0 - 1))
                            : (y1 - y0 <= 1 ? 0f : (float) (row - y0) / (y1 - y0 - 1));
                    tile.put(col, row, gradientColor(color1, color2, t));
                }
            }
        });
    }
    private static int gradientColor(int first, int second, float t) {
        int a = (int) ((first >>> 24) + ((second >>> 24) - (first >>> 24)) * t);
        int r = (int) (((first >>> 16) & 255) + (((second >>> 16) & 255) - ((first >>> 16) & 255)) * t);
        int g = (int) (((first >>> 8) & 255) + (((second >>> 8) & 255) - ((first >>> 8) & 255)) * t);
        int b = (int) ((first & 255) + ((second & 255) - (first & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }
    public void drawVerticalGradient(int x, int y0, int y1, int topColor, int bottomColor) {
        int start = Math.max(clipY0, Math.min(y0, y1));
        int end = Math.min(clipY1 - 1, Math.max(y0, y1));
        if (x < clipX0 || x >= clipX1 || start >= end) return;
        commands.add(tile -> {
            for (int row = Math.max(start, tile.y0); row <= Math.min(end, tile.y1 - 1); row++)
                tile.put(x, row, gradientColor(topColor, bottomColor, (float) (row - start) / (end - start)));
        });
    }
    public void fillRect(int x, int y, int w, int h, Paint paint) {
        int cx = clipX0, cy = clipY0, cw = clipX1 - clipX0, ch = clipY1 - clipY0;
        effect(target -> {
            target.setClip(cx, cy, cw, ch);
            target.fillRect(x, y, w, h, paint);
        });
    }
    public void applyVignette(int color, float strength) {
        int cx = clipX0, cy = clipY0, cw = clipX1 - clipX0, ch = clipY1 - clipY0;
        effect(target -> {
            target.setClip(cx, cy, cw, ch);
            target.applyVignette(color, strength);
        });
    }
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
    @Override public void drawTaperedLine(float x0, float y0, float x1, float y1, float lineWidth, int color) {
        int cx = clipX0, cy = clipY0, cw = clipX1 - clipX0, ch = clipY1 - clipY0;
        effect(target -> {
            target.setClip(cx, cy, cw, ch);
            target.drawTaperedLine(x0, y0, x1, y1, lineWidth, color);
        });
    }
    @Override public void drawImage(int[] source, int sw, int sh, int x, int y) {
        image(source, sw, sh, x, y, sw, sh, false);
    }
    @Override public void drawImageBlended(int[] source, int sw, int sh, int x, int y) {
        image(source, sw, sh, x, y, sw, sh, true);
    }
    @Override public void drawImage(PixelGraphics source, int x, int y) {
        if (source != null) image(source.pixels, source.width, source.height, x, y, source.width, source.height, false);
    }
    @Override public void drawImageBlended(PixelGraphics source, int x, int y) {
        if (source != null) image(source.pixels, source.width, source.height, x, y, source.width, source.height, true);
    }
    public void drawImageScaled(PixelGraphics source, int x, int y, int w, int h) {
        if (source != null) image(source.pixels, source.width, source.height, x, y, w, h, true);
    }
    public void drawImageScaledBlended(PixelGraphics source, int x, int y, int w, int h) {
        if (source != null) image(source.pixels, source.width, source.height, x, y, w, h, true);
    }
    @Override public void drawImageTinted(int[] source, int sw, int sh, int x, int y, int tint) {
        variantImage(source, sw, sh, x, y, tint, false, false, true);
    }
    @Override public void drawImageTinted(PixelGraphics source, int x, int y, int tint) {
        if (source != null) variantImage(source.pixels, source.width, source.height, x, y, tint, false, false, true);
    }
    @Override public void drawImageFlipped(int[] source, int sw, int sh, int x, int y, boolean flipH, boolean flipV) {
        variantImage(source, sw, sh, x, y, 0, flipH, flipV, false);
    }
    @Override public void drawImageFlipped(PixelGraphics source, int x, int y, boolean flipH, boolean flipV) {
        if (source != null) variantImage(source.pixels, source.width, source.height, x, y, 0, flipH, flipV, false);
    }
    private void variantImage(int[] source, int sw, int sh, int x, int y, int tint,
                              boolean flipH, boolean flipV, boolean tinted) {
        int x0 = Math.max(x, clipX0), y0 = Math.max(y, clipY0);
        int x1 = Math.min(x + sw, clipX1), y1 = Math.min(y + sh, clipY1);
        if (x0 >= x1 || y0 >= y1) return;
        int tr = (tint >>> 16) & 255, tg = (tint >>> 8) & 255;
        int tb = tint & 255, ta = tint >>> 24;
        commands.add(tile -> {
            for (int row = Math.max(y0, tile.y0); row < Math.min(y1, tile.y1); row++) {
                int sy = flipV ? sh - 1 - (row - y) : row - y;
                for (int col = x0; col < x1; col++) {
                    int sx = flipH ? sw - 1 - (col - x) : col - x;
                    int color = source[sy * sw + sx];
                    if (tinted) {
                        int a = (color >>> 24) * ta / 255;
                        int r = ((color >>> 16) & 255) * tr / 255;
                        int g = ((color >>> 8) & 255) * tg / 255;
                        int b = (color & 255) * tb / 255;
                        color = a << 24 | r << 16 | g << 8 | b;
                    }
                    tile.blend(col, row, color);
                }
            }
        });
    }
    public void drawImageScaled(int[] source, int sw, int sh, int x, int y, int w, int h) {
        image(source, sw, sh, x, y, w, h, true);
    }
    public void drawImageScaledBlended(int[] source, int sw, int sh, int x, int y, int w, int h) {
        image(source, sw, sh, x, y, w, h, true);
    }
    private void image(int[] source, int sw, int sh, int x, int y, int w, int h, boolean blended) {
        if (source == null || sw <= 0 || sh <= 0 || w <= 0 || h <= 0) return;
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
        if (font == null || text == null) return;
        int[] glyphs = font.getChars(text);
        int glyphW = font.getGlyphWidth(), glyphH = font.getGlyphHeight();
        int cx0 = clipX0, cy0 = clipY0, cx1 = clipX1, cy1 = clipY1;
        commands.add(tile -> {
            for (int i = 0; i < glyphs.length; i++) {
                byte[] glyph = font.getGlyph(glyphs[i]);
                int glyphX = x + i * glyphW;
                for (int row = Math.max(y, Math.max(cy0, tile.y0)); row < Math.min(y + glyphH, Math.min(cy1, tile.y1)); row++) {
                    int bits = glyph[row - y] & 255;
                    for (int col = 0; col < glyphW; col++) {
                        int px = glyphX + col;
                        if (px >= cx0 && px < cx1 && (bits & (1 << (7 - col))) != 0)
                            tile.put(px, row, color);
                    }
                }
            }
        });
    }
    public void renderString(SpriteSheetFont font, String text, int x, int y, int color) {
        if (font == null || text == null) return;
        int glyphW = font.getGlyphWidth(), glyphH = font.getGlyphHeight();
        int cx0 = clipX0, cy0 = clipY0, cx1 = clipX1, cy1 = clipY1;
        TextureAtlas atlas = font.getAtlas();
        String characters = font.getCharacters();
        for (int i = 0; i < text.length(); i++) {
            int index = characters.indexOf(text.charAt(i));
            if (index >= 0 && index < atlas.getTextureCount()) atlas.getTexture(index);
        }
        commands.add(tile -> {
            for (int i = 0; i < text.length(); i++) {
                int index = characters.indexOf(text.charAt(i));
                if (index < 0 || index >= atlas.getTextureCount()) continue;
                PixelGraphics glyph = atlas.getTexture(index);
                int glyphX = x + i * glyphW;
                for (int row = Math.max(y, Math.max(cy0, tile.y0)); row < Math.min(y + glyphH, Math.min(cy1, tile.y1)); row++) {
                    int source = (row - y) * glyphW;
                    for (int col = 0; col < glyphW; col++) {
                        int px = glyphX + col;
                        if (px >= cx0 && px < cx1 && (glyph.pixels[source + col] >>> 24) != 0)
                            tile.blend(px, row, color);
                    }
                }
            }
        });
    }
    @Override public void renderChar(BitmapFont font, int ch, int x, int y, int color) {
        effect(target -> target.renderChar(font, ch, x, y, color));
    }
    @Override public void renderString(BitmapFont font, String text, int x, int y,
                                       int color, int backgroundColor) {
        effect(target -> target.renderString(font, text, x, y, color, backgroundColor));
    }
    @Override public void renderString(BitmapFont font, String text, int x, int y,
                                       int color, int backgroundColor, int scale) {
        effect(target -> target.renderString(font, text, x, y, color, backgroundColor, scale));
    }
    @Override public void renderString(SpriteSheetFont font, String text, int x, int y, int color, int scale) {
        effect(target -> target.renderString(font, text, x, y, color, scale));
    }
    /** Cache the generated bitmap for repeated text, font, and color combinations. */
    public void renderStringCached(BitmapFont font, String text, int x, int y, int color) {
        Objects.requireNonNull(font, "font");
        CachedString cached = cachedString(font, text, color);
        image(cached.pixels, cached.width, cached.height, x, y, cached.width, cached.height, false);
    }
    /** Cache the generated bitmap for repeated text, font, and color combinations. */
    public void renderStringCached(SpriteSheetFont font, String text, int x, int y, int color) {
        Objects.requireNonNull(font, "font");
        CachedString cached = cachedString(font, text, color);
        image(cached.pixels, cached.width, cached.height, x, y, cached.width, cached.height, true);
    }
    private CachedString cachedString(Object font, String text, int color) {
        Objects.requireNonNull(text, "text");
        stringLookup.set(font, text, color);
        CachedString image = stringCache.get(stringLookup);
        if (image != null) return image;
        StringKey key = new StringKey(font, text, color);
        image = font instanceof BitmapFont
                ? bitmapStringImage((BitmapFont) font, text, color)
                : spriteStringImage((SpriteSheetFont) font, text, color);
        int area = image.pixels.length;
        if (area > MAX_CACHED_STRING_PIXELS) return image;
        while (!stringCache.isEmpty() &&
                (stringCache.size() >= MAX_CACHED_STRINGS || cachedStringPixels + area > MAX_CACHED_STRING_PIXELS)) {
            Iterator<Map.Entry<StringKey, CachedString>> iterator = stringCache.entrySet().iterator();
            Map.Entry<StringKey, CachedString> eldest = iterator.next();
            cachedStringPixels -= eldest.getValue().pixels.length;
            iterator.remove();
        }
        stringCache.put(key, image);
        cachedStringPixels += area;
        return image;
    }
    private static CachedString bitmapStringImage(BitmapFont font, String text, int color) {
        int[] glyphs = font.getChars(text);
        int glyphW = font.getGlyphWidth(), glyphH = font.getGlyphHeight();
        int imageW = Math.max(1, glyphs.length * glyphW);
        int[] data = new int[imageW * glyphH];
        for (int i = 0; i < glyphs.length; i++) {
            byte[] glyph = font.getGlyph(glyphs[i]);
            for (int row = 0; row < glyphH; row++) {
                int bits = glyph[row] & 255;
                int dst = row * imageW + i * glyphW;
                for (int col = 0; col < glyphW; col++)
                    if ((bits & (1 << (7 - col))) != 0) data[dst + col] = color;
            }
        }
        return new CachedString(data, imageW, glyphH);
    }
    private static CachedString spriteStringImage(SpriteSheetFont font, String text, int color) {
        int glyphW = font.getGlyphWidth(), glyphH = font.getGlyphHeight();
        int imageW = Math.max(1, text.length() * glyphW);
        int[] data = new int[imageW * glyphH];
        for (int i = 0; i < text.length(); i++) {
            int glyphIndex = font.getCharacters().indexOf(text.charAt(i));
            if (glyphIndex < 0 || glyphIndex >= font.getAtlas().getTextureCount()) continue;
            PixelGraphics glyph = font.getAtlas().getTexture(glyphIndex);
            for (int row = 0; row < glyphH; row++) {
                int src = row * glyphW, dst = row * imageW + i * glyphW;
                for (int col = 0; col < glyphW; col++)
                    if ((glyph.pixels[src + col] >>> 24) != 0) data[dst + col] = color;
            }
        }
        return new CachedString(data, imageW, glyphH);
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
    // Vec2/Vec3 use x and y for the screen plane. Draw order, not z, controls layering.
    public void setPixel(Vec2 p, int color) { setPixel((int) p.x, (int) p.y, color); }
    public void setPixel(Vec3 p, int color) { setPixel((int) p.x, (int) p.y, color); }
    public int getPixel(Vec2 p) { return getPixel((int) p.x, (int) p.y); }
    public int getPixel(Vec3 p) { return getPixel((int) p.x, (int) p.y); }
    public void blendPixel(Vec2 p, int color) { blendPixel((int) p.x, (int) p.y, color); }
    public void blendPixel(Vec3 p, int color) { blendPixel((int) p.x, (int) p.y, color); }
    public void drawLine(Vec2 from, Vec2 to, int color) {
        drawLine((int) from.x, (int) from.y, (int) to.x, (int) to.y, color);
    }
    public void drawLine(Vec3 from, Vec3 to, int color) {
        drawLine((int) from.x, (int) from.y, (int) to.x, (int) to.y, color);
    }
    public void fillRect(Vec2 p, int w, int h, int color) { fillRect((int) p.x, (int) p.y, w, h, color); }
    public void fillRect(Vec3 p, int w, int h, int color) { fillRect((int) p.x, (int) p.y, w, h, color); }
    public void fillRect(Vec2 p, int w, int h, Paint paint) { fillRect((int) p.x, (int) p.y, w, h, paint); }
    public void fillRect(Vec3 p, int w, int h, Paint paint) { fillRect((int) p.x, (int) p.y, w, h, paint); }
    public void fillRectBlended(Vec2 p, int w, int h, int color) { fillRectBlended((int) p.x, (int) p.y, w, h, color); }
    public void fillRectBlended(Vec3 p, int w, int h, int color) { fillRectBlended((int) p.x, (int) p.y, w, h, color); }
    public void drawRect(Vec2 p, int w, int h, int color) { drawRect((int) p.x, (int) p.y, w, h, color); }
    public void drawRect(Vec3 p, int w, int h, int color) { drawRect((int) p.x, (int) p.y, w, h, color); }
    public void fillGradientRect(Vec2 p, int w, int h, int a, int b, boolean horizontal) {
        fillGradientRect((int) p.x, (int) p.y, w, h, a, b, horizontal);
    }
    public void fillGradientRect(Vec3 p, int w, int h, int a, int b, boolean horizontal) {
        fillGradientRect((int) p.x, (int) p.y, w, h, a, b, horizontal);
    }
    public void drawImage(PixelGraphics source, Vec2 p) { drawImage(source, (int) p.x, (int) p.y); }
    public void drawImage(PixelGraphics source, Vec3 p) { drawImage(source, (int) p.x, (int) p.y); }
    public void drawImageBlended(PixelGraphics source, Vec2 p) { drawImageBlended(source, (int) p.x, (int) p.y); }
    public void drawImageBlended(PixelGraphics source, Vec3 p) { drawImageBlended(source, (int) p.x, (int) p.y); }
    public void drawImageScaled(PixelGraphics source, Vec2 p, int w, int h) {
        drawImageScaled(source, (int) p.x, (int) p.y, w, h);
    }
    public void drawImageScaled(PixelGraphics source, Vec3 p, int w, int h) {
        drawImageScaled(source, (int) p.x, (int) p.y, w, h);
    }
    public void drawImageTinted(PixelGraphics source, Vec2 p, int tint) { drawImageTinted(source, (int) p.x, (int) p.y, tint); }
    public void drawImageTinted(PixelGraphics source, Vec3 p, int tint) { drawImageTinted(source, (int) p.x, (int) p.y, tint); }
    public void drawImageFlipped(PixelGraphics source, Vec2 p, boolean h, boolean v) {
        drawImageFlipped(source, (int) p.x, (int) p.y, h, v);
    }
    public void drawImageFlipped(PixelGraphics source, Vec3 p, boolean h, boolean v) {
        drawImageFlipped(source, (int) p.x, (int) p.y, h, v);
    }
    public void setClip(Vec2 p, int w, int h) { setClip((int) p.x, (int) p.y, w, h); }
    public void setClip(Vec3 p, int w, int h) { setClip((int) p.x, (int) p.y, w, h); }
    public void renderString(BitmapFont font, String text, Vec2 p, int color) {
        renderString(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderString(BitmapFont font, String text, Vec3 p, int color) {
        renderString(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderString(SpriteSheetFont font, String text, Vec2 p, int color) {
        renderString(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderString(SpriteSheetFont font, String text, Vec3 p, int color) {
        renderString(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderStringCached(BitmapFont font, String text, Vec2 p, int color) {
        renderStringCached(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderStringCached(BitmapFont font, String text, Vec3 p, int color) {
        renderStringCached(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderStringCached(SpriteSheetFont font, String text, Vec2 p, int color) {
        renderStringCached(font, text, (int) p.x, (int) p.y, color);
    }
    public void renderStringCached(SpriteSheetFont font, String text, Vec3 p, int color) {
        renderStringCached(font, text, (int) p.x, (int) p.y, color);
    }
    public void drawImageWorld(PixelGraphics source, Vec2 p) { drawImageWorld(source, p.x, p.y); }
    public void drawImageWorld(PixelGraphics source, Vec3 p) { drawImageWorld(source, p.x, p.y); }
    public void drawImageBlendedWorld(PixelGraphics source, Vec2 p) { drawImageBlendedWorld(source, p.x, p.y); }
    public void drawImageBlendedWorld(PixelGraphics source, Vec3 p) { drawImageBlendedWorld(source, p.x, p.y); }
    public void setPixelWorld(Vec2 p, int color) { setPixelWorld(p.x, p.y, color); }
    public void setPixelWorld(Vec3 p, int color) { setPixelWorld(p.x, p.y, color); }
    public void fillRectWorld(Vec2 p, float w, float h, int color) { fillRectWorld(p.x, p.y, w, h, color); }
    public void fillRectWorld(Vec3 p, float w, float h, int color) { fillRectWorld(p.x, p.y, w, h, color); }
    public void drawRectWorld(Vec2 p, float w, float h, int color) { drawRectWorld(p.x, p.y, w, h, color); }
    public void drawRectWorld(Vec3 p, float w, float h, int color) { drawRectWorld(p.x, p.y, w, h, color); }
    public void drawLineWorld(Vec2 from, Vec2 to, int color) {
        drawLineWorld(from.x, from.y, to.x, to.y, color);
    }
    public void drawLineWorld(Vec3 from, Vec3 to, int color) {
        drawLineWorld(from.x, from.y, to.x, to.y, color);
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
        effectView.clearClip();
        effect.accept(effectView);
    }

    /** Execute all recorded commands before the buffer is presented. */
    public void execute() {
        if (commands.isEmpty()) return;
        // Recording is confined to the game loop thread. Reuse the command list
        // after the frame barrier instead of copying it on every frame.
        int tileCount = tiles.length;
        if (tileCount == 1 || workers == null) {
            try { drawTile(tiles[0]); }
            finally { commands.clear(); }
            return;
        }
        futures.clear();
        for (int i = 1; i < tileCount; i++) {
            futures.add(workers.submit(tileTasks[i]));
        }
        Throwable failure = null;
        boolean interrupted = false;
        try { drawTile(tiles[0]); }
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
        futures.clear();
        commands.clear();
        if (interrupted) Thread.currentThread().interrupt();
        if (failure != null) throw new IllegalStateException("Render failed", failure);
    }
    private void drawTile(Tile tile) {
        for (Command command : commands) command.draw(tile);
    }
    @Override public void close() { if (workers != null) workers.shutdownNow(); }
}
