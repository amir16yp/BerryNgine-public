package berryngine;

import java.util.Arrays;

public final class StringCacheTest {
    public static void main(String[] args) {
        int[] uncachedPixels = new int[160 * 40];
        int[] cachedPixels = new int[160 * 40];
        try (ThreadedPixelGraphics uncached = new ThreadedPixelGraphics(uncachedPixels, 160, 40, 1);
             ThreadedPixelGraphics cached = new ThreadedPixelGraphics(cachedPixels, 160, 40, 4)) {
            for (int frame = 0; frame < 3; frame++) {
                uncached.beginFrame();
                cached.beginFrame();
                uncached.clear(Color.DARK_BLUE);
                cached.clear(Color.DARK_BLUE);
                uncached.renderString(BitmapFont.DEFAULT_8X9, "Score 42", 2, 2, Color.WHITE);
                cached.renderStringCached(BitmapFont.DEFAULT_8X9, new String("Score 42"), 2, 2, Color.WHITE);
                uncached.renderString(SpriteSheetFont.ABLE4, "HELLO", 2, 20, Color.RED);
                cached.renderStringCached(SpriteSheetFont.ABLE4, "HELLO", 2, 20, Color.RED);
                uncached.renderString(SpriteSheetFont.ABLE4, "HELLO", 40, 20, Color.BLUE);
                cached.renderStringCached(SpriteSheetFont.ABLE4, "HELLO", 40, 20, Color.BLUE);
                uncached.execute();
                cached.execute();
                if (!Arrays.equals(uncachedPixels, cachedPixels))
                    throw new AssertionError("Cached text differs from uncached text on frame " + frame);
                if (cached.cachedStringCount() != 3)
                    throw new AssertionError("Repeated text generated an extra cache entry");
            }
            for (int i = 0; i < 300; i++)
                cached.renderStringCached(SpriteSheetFont.ABLE4, "item " + i, 0, 0, Color.WHITE);
            if (cached.cachedStringCount() > 256)
                throw new AssertionError("String cache did not evict old entries");
            cached.clearStringCache();
            if (cached.cachedStringCount() != 0)
                throw new AssertionError("String cache did not clear");
        }
        System.out.println("StringCacheTest passed");
    }
}
