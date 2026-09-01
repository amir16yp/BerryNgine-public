package engine;

import java.nio.charset.Charset;

public final class PSF1Parser {

    private static final byte PSF1_MAGIC0 = 0x36;
    private static final byte PSF1_MAGIC1 = 0x04;
    private static final int PSF1_MODE512 = 0x01;
    private static final int PSF1_HEADER_SIZE = 4;

    private PSF1Parser() {
    }

    public static BitmapFont parse(byte[] data) {
        return parse(data, Charset.forName("CP437"));
    }

    public static BitmapFont parse(byte[] data, String charsetName) {
        Charset cs;
        try {
            cs = Charset.forName(charsetName == null || charsetName.trim().isEmpty() ? "CP437" : charsetName.trim());
        } catch (RuntimeException ex) {
            cs = Charset.forName("CP437");
        }
        return parse(data, cs);
    }

    public static BitmapFont parse(byte[] data, Charset charset) {
        if (data == null || data.length < PSF1_HEADER_SIZE) {
            throw new IllegalArgumentException("Invalid PSF1 data: too short");
        }

        if (data[0] != PSF1_MAGIC0 || data[1] != PSF1_MAGIC1) {
            throw new IllegalArgumentException("Invalid PSF1 magic number");
        }

        int mode = data[2] & 0xFF;
        int charsize = data[3] & 0xFF;

        int glyphCount = (mode & PSF1_MODE512) != 0 ? 512 : 256;
        int glyphWidth = 8;
        int glyphHeight = charsize;

        int expectedDataSize = PSF1_HEADER_SIZE + (glyphCount * charsize);
        if (data.length < expectedDataSize) {
            throw new IllegalArgumentException(
                    "Invalid PSF1 data: expected at least " + expectedDataSize +
                            " bytes, got " + data.length);
        }

        byte[][] glyphs = new byte[glyphCount][glyphHeight];

        int offset = PSF1_HEADER_SIZE;
        for (int i = 0; i < glyphCount; i++) {
            for (int row = 0; row < glyphHeight; row++) {
                glyphs[i][row] = data[offset++];
            }
        }

        return new BitmapFont(glyphs, glyphWidth, glyphHeight,
                charset == null ? Charset.forName("CP437") : charset);
    }
}