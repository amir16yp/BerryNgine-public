package berryngine;

public final class TextureAtlas {
    private PixelGraphics source;
    private final int textureWidth;
    private final int textureHeight;
    private int columns;
    private int rows;
    private PixelGraphics[] cache;

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

    public TextureAtlas scale(int scaleFactor) {
        if (scaleFactor <= 1) {
            return this;
        }
        return new TextureAtlas(source.scale(scaleFactor), textureWidth * scaleFactor, textureHeight * scaleFactor);
    }

    public void addTexture(PixelGraphics tex) {
        if (tex == null) {
            throw new NullPointerException("Texture cannot be null");
        }
        if (tex.width != textureWidth || tex.height != textureHeight) {
            throw new IllegalArgumentException("Texture size (" + tex.width + "x" + tex.height + ") must match atlas cell size (" + textureWidth + "x" + textureHeight + ")");
        }

        PixelGraphics newSource;
        PixelGraphics[] newCache;
        int newIndex;

        if (rows == 1) {
            int newColumns = columns + 1;
            newSource = new PixelGraphics(newColumns * textureWidth, textureHeight);
            newSource.drawImage(source, 0, 0);
            newSource.drawImage(tex, columns * textureWidth, 0);
            newCache = new PixelGraphics[newColumns];
            newIndex = columns;
            columns = newColumns;
        } else {
            int newRows = rows + 1;
            newSource = new PixelGraphics(columns * textureWidth, newRows * textureHeight);
            newSource.drawImage(source, 0, 0);
            newSource.drawImage(tex, 0, rows * textureHeight);
            newCache = new PixelGraphics[columns * newRows];
            newIndex = rows * columns;
            rows = newRows;
        }

        System.arraycopy(cache, 0, newCache, 0, cache.length);
        newCache[newIndex] = tex;
        source = newSource;
        cache = newCache;
    }

    public static TextureAtlas fromTextures(PixelGraphics[] texArray) {
        if (texArray == null || texArray.length == 0) {
            throw new IllegalArgumentException("Texture array must not be null or empty");
        }

        int textureWidth = texArray[0].width;
        int textureHeight = texArray[0].height;
        for (int i = 0; i < texArray.length; i++) {
            PixelGraphics tex = texArray[i];
            if (tex == null) {
                throw new NullPointerException("Texture at index " + i + " cannot be null");
            }
            if (tex.width != textureWidth || tex.height != textureHeight) {
                throw new IllegalArgumentException("All textures must have the same dimensions");
            }
        }

        PixelGraphics combined = new PixelGraphics(textureWidth * texArray.length, textureHeight);
        for (int i = 0; i < texArray.length; i++) {
            combined.drawImage(texArray[i], i * textureWidth, 0);
        }
        return new TextureAtlas(combined, textureWidth, textureHeight);
    }
}
