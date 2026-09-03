package berryngine;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public final class BitmapFont {

    private final byte[][] glyphs;
    private final int glyphWidth;
    private final int glyphHeight;
    private final int glyphCount;

    private final Charset charset;
    private final CharsetEncoder encoder;

    public BitmapFont(byte[][] glyphs, int glyphWidth, int glyphHeight, Charset charset) {
        this.glyphs = glyphs;
        this.glyphWidth = glyphWidth;
        this.glyphHeight = glyphHeight;
        this.glyphCount = glyphs.length;

        this.charset = charset;
        this.encoder = charset.newEncoder();
    }

    public Charset getCharset() {
        return charset;
    }

    public int getGlyphWidth() {
        return glyphWidth;
    }

    public int getGlyphHeight() {
        return glyphHeight;
    }

    public int getGlyphCount() {
        return glyphCount;
    }

    public byte[] getGlyph(int index) {
        if (index < 0 || index >= glyphCount) {
            return glyphs[0];
        }
        return glyphs[index];
    }

    private boolean getPixel(int glyphIndex, int x, int y) {
        if (glyphIndex < 0 || glyphIndex >= glyphCount) return false;
        if (x < 0 || x >= glyphWidth || y < 0 || y >= glyphHeight) return false;

        byte row = glyphs[glyphIndex][y];
        return ((row >> (7 - x)) & 1) == 1;
    }

    /**
     * Converts a Java String into glyph indices according to this font's encoding.
     */
    public int[] getChars(String text) {
        if (text == null) return new int[0];

        try {
            ByteBuffer buffer = encoder.encode(CharBuffer.wrap(text));
            int[] result = new int[buffer.remaining()];

            for (int i = 0; i < result.length; i++) {
                result[i] = buffer.get() & 0xFF;
                if (result[i] >= glyphCount) {
                    result[i] = 0; // fallback glyph
                }
            }

            return result;

        } catch (CharacterCodingException e) {
            throw new RuntimeException("Encoding failed", e);
        }
    }

    /**
     * Converts raw glyph indices back to a Java String.
     */
    public String getString(byte[] glyphIndices) {
        if (glyphIndices == null) return "";

        try {
            ByteBuffer buffer = ByteBuffer.wrap(glyphIndices);
            CharBuffer chars = charset.decode(buffer);
            return chars.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public PixelGraphics createPreview() {
        int cols = 16;
        if (glyphCount > 0 && glyphCount < cols) {
            cols = glyphCount;
        }
        int rows = glyphCount == 0 ? 1 : (glyphCount + cols - 1) / cols;

        int w = Math.max(1, cols * glyphWidth);
        int h = Math.max(1, rows * glyphHeight);

        PixelGraphics img = new PixelGraphics(w, h);
        int on = 0xFFFFFFFF;
        int off = 0xFF000000;

        for (int i = 0; i < w * h; i++) {
            img.setPixel(i % w, i / w, off);
        }

        for (int glyphIndex = 0; glyphIndex < glyphCount; glyphIndex++) {
            int gx = (glyphIndex % cols) * glyphWidth;
            int gy = (glyphIndex / cols) * glyphHeight;

            for (int y = 0; y < glyphHeight; y++) {
                for (int x = 0; x < glyphWidth; x++) {
                    if (getPixel(glyphIndex, x, y)) {
                        img.setPixel(gx + x, gy + y, on);
                    }
                }
            }
        }

        return img;
    }

    public static final BitmapFont DEFAULT_8X9 = Utils.loadFontFromResources("/berryngine/default_assets/fonts/default8x9.psf");

    /*
    public static void main(String[] args)
    {
        // check that it works
        Utils.saveScreenshot(BitmapFont.DEFAULT_8X9.createPreview());
    }

     */
}