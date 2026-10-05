package berryngine;

import java.awt.GradientPaint;
import java.util.Arrays;

public final class RenderApiParityTest {
    public static void main(String[] args) {
        int[] expectedPixels = new int[96 * 64];
        int[] actualPixels = new int[96 * 64];
        PixelGraphics expected = new PixelGraphics(expectedPixels, 96, 64);
        try (ThreadedPixelGraphics actual = new ThreadedPixelGraphics(actualPixels, 96, 64, 4)) {
            PixelGraphics asPixelGraphics = actual;
            int[] sprite = { 0x00000000, 0x88ff0000, 0xff00ff00, 0x440000ff };
            GradientPaint paint = new GradientPaint(0, 0, java.awt.Color.RED, 30, 20, java.awt.Color.BLUE);
            actual.beginFrame();
            expected.clear(0xff202020); actual.clear(0xff202020);
            expected.fillGradientRect(3, 4, 30, 20, 0xff123456, 0x80123456, true);
            actual.fillGradientRect(3, 4, 30, 20, 0xff123456, 0x80123456, true);
            expected.fillGradientRect(new IVec2(35, 5), 25, 20, 0xff000000, 0xffffffff, false);
            asPixelGraphics.fillGradientRect(new IVec2(35, 5), 25, 20, 0xff000000, 0xffffffff, false);
            expected.drawVerticalGradient(70, 2, 25, Color.RED, Color.BLUE);
            actual.drawVerticalGradient(70, 2, 25, Color.RED, Color.BLUE);
            expected.fillRect(2, 28, 25, 10, paint);
            actual.fillRect(2, 28, 25, 10, paint);
            expected.drawImage(sprite, 2, 2, 30, 10);
            actual.drawImage(sprite, 2, 2, 30, 10);
            expected.drawImageTinted(sprite, 2, 2, 34, 10, 0x80ffffff);
            actual.drawImageTinted(sprite, 2, 2, 34, 10, 0x80ffffff);
            expected.drawImageFlipped(sprite, 2, 2, 40, 10, true, false);
            actual.drawImageFlipped(sprite, 2, 2, 40, 10, true, false);
            expected.drawImageScaled(sprite, 2, 2, 45, 10, 8, 8);
            actual.drawImageScaled(sprite, 2, 2, 45, 10, 8, 8);
            expected.fillRect(5, 42, 12, 9, Color.GREEN);
            actual.fillRect(new Vec2(5, 42), 12, 9, Color.GREEN);
            expected.drawLine(20, 40, 32, 52, Color.WHITE);
            actual.drawLine(new Vec3(20, 40, 99), new Vec3(32, 52, -1), Color.WHITE);
            expected.renderString(BitmapFont.DEFAULT_8X9, "OK", 35, 45, Color.WHITE);
            actual.renderString(BitmapFont.DEFAULT_8X9, "OK", 35, 45, Color.WHITE);
            expected.renderString(SpriteSheetFont.ABLE4, "GO", 60, 45, 0x88ff0000);
            actual.renderString(SpriteSheetFont.ABLE4, "GO", 60, 45, 0x88ff0000);
            actual.execute();
            if (!Arrays.equals(expectedPixels, actualPixels)) {
                for (int i = 0; i < expectedPixels.length; i++)
                    if (expectedPixels[i] != actualPixels[i])
                        throw new AssertionError("Pixel " + i + ": expected " + Integer.toHexString(expectedPixels[i])
                                + ", got " + Integer.toHexString(actualPixels[i]));
            }
            actual.fillRect(0, 0, 1, 1, Color.RED);
            if (actual.getPixel(0, 0) != Color.RED) throw new AssertionError("Readback did not execute queued work");
        }
        System.out.println("RenderApiParityTest passed");
    }
}
