package engine;

public final class TextureAtlas {
    private final PixelGraphics source;
    private final int textureWidth;
    private final int textureHeight;
    private final int columns;
    private final int rows;
    private final PixelGraphics[] cache;

    public TextureAtlas(PixelGraphics source, int textureWidth, int textureHeight) {
        this.source = source;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.columns = source.width / textureWidth;
        this.rows = source.height / textureHeight;
        this.cache = new PixelGraphics[columns * rows];
    }

    public PixelGraphics getTexture(int index) {
        if (index < 0 || index >= cache.length) {
            throw new IndexOutOfBoundsException("Texture index " + index + " out of range [0, " + cache.length + ")");
        }
        if (cache[index] == null) {
            int col = index % columns;
            int row = index / columns;
            cache[index] = source.getSubImage(col * textureWidth, row * textureHeight, textureWidth, textureHeight);
        }
        return cache[index];
    }

    public PixelGraphics getTexture(int x, int y) {
        if (x < 0 || x >= columns || y < 0 || y >= rows) {
            throw new IndexOutOfBoundsException("Texture coordinate (" + x + ", " + y + ") out of range [0-" + columns + ", 0-" + rows + ")");
        }
        return getTexture(y * columns + x);
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getTextureCount() {
        return columns * rows;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
}
