package berryngine;

public final class SpriteSheetFont {
    private final String characters;
    private final TextureAtlas atlas;
    private final int glyphW;
    private final int glyphH;

    public SpriteSheetFont(String characters, TextureAtlas atlas) {
        this.characters = characters;
        this.atlas = atlas;
        this.glyphW = atlas.getTextureWidth();
        this.glyphH = atlas.getTextureHeight();
    }

    public int getGlyphWidth() {
        return glyphW;
    }

    public int getGlyphHeight() {
        return glyphH;
    }

    public String getCharacters() {
        return characters;
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    public SpriteSheetFont scale(int scaleFactor) {
        if (scaleFactor <= 1) return this;
        int newW = glyphW * scaleFactor;
        int newH = glyphH * scaleFactor;
        int count = atlas.getTextureCount();
        PixelGraphics combined = new PixelGraphics(newW * count, newH);
        for (int i = 0; i < count; i++) {
            PixelGraphics glyph = atlas.getTexture(i).scale(scaleFactor);
            combined.drawImage(glyph, i * newW, 0);
        }
        return new SpriteSheetFont(characters, new TextureAtlas(combined, newW, newH));
    }

    public PixelGraphics getStringImage(String string, int color) {
        PixelGraphics image = new PixelGraphics(Math.max(1, string.length() * glyphW), glyphH);
        drawString(image, string, 0, 0, color);
        return image;
    }

    public void drawString(PixelGraphics g, String string, int x, int y, int color) {
        int cursorX = x;
        for (int i = 0; i < string.length(); i++) {
            int idx = characters.indexOf(string.charAt(i));
            if (idx >= 0 && idx < atlas.getTextureCount()) {
                PixelGraphics glyph = atlas.getTexture(idx);
                for (int row = 0; row < glyphH; row++) {
                    for (int col = 0; col < glyphW; col++) {
                        if ((glyph.pixels[row * glyphW + col] >>> 24) > 0) {
                            g.blendPixel(cursorX + col, y + row, color);
                        }
                    }
                }
            }
            cursorX += glyphW;
        }
    }
    private static String DEFAULT_FONT_CHARS = " !\\\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~";
    public static SpriteSheetFont START2P = new SpriteSheetFont(DEFAULT_FONT_CHARS, Utils.loadTextureAtlasFromResources("/berryngine/default_assets/fonts/start2p_8x8.qoi", 8, 8));
    public static SpriteSheetFont ABLE4 = new SpriteSheetFont(DEFAULT_FONT_CHARS, Utils.loadTextureAtlasFromResources("/berryngine/default_assets/fonts/able4_6x6.qoi", 6, 6));
}
