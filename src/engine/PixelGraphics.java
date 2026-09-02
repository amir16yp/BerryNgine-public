package engine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.util.Arrays;

/**
 * PixelGraphics: A high-performance software renderer.
 * Operates on ARGB format: 0xAARRGGBB
 */
public class PixelGraphics {
    public int[] pixels;
    public int width;
    public int height;
    private float globalAlpha = 1.0f;

    // Scissor / clip rectangle (defaults to full buffer)
    private int clipX1, clipY1, clipX2, clipY2;

    public PixelGraphics(int[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        clipX2 = width;
        clipY2 = height;
    }

    public PixelGraphics(int width, int height) {
        this.pixels = new int[width * height];
        this.width = width;
        this.height = height;
        clipX2 = width;
        clipY2 = height;
    }

    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] dst = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(this.pixels, 0, dst, 0, Math.min(this.pixels.length, dst.length));
        return image;
    }

    /**
     * Deep copies pixel data from another PixelGraphics instance.
     */
    public void copyFrom(PixelGraphics src) {
        if (src == null) return;

        // Resize if needed (optional but useful)
        if (this.pixels.length != src.pixels.length) {
            this.pixels = new int[src.pixels.length];
        }

        System.arraycopy(src.pixels, 0, this.pixels, 0, src.pixels.length);
    }

    public void setBuffer(int[] buffer) {
        this.pixels = buffer;
    }

    public void fillGradientRect(int x, int y, int w, int h,
                                 int color1, int color2,
                                 boolean horizontal) {

        if (w <= 0 || h <= 0) return;

        int xStart = Math.max(0, x);
        int yStart = Math.max(0, y);
        int xEnd = Math.min(this.width, x + w);
        int yEnd = Math.min(this.height, y + h);

        int renderW = xEnd - xStart;
        int renderH = yEnd - yStart;

        if (renderW <= 0 || renderH <= 0) return;

        // Extract ARGB
        int a1 = (color1 >>> 24) & 0xFF;
        int r1 = (color1 >>> 16) & 0xFF;
        int g1 = (color1 >>> 8) & 0xFF;
        int b1 = (color1) & 0xFF;

        int a2 = (color2 >>> 24) & 0xFF;
        int r2 = (color2 >>> 16) & 0xFF;
        int g2 = (color2 >>> 8) & 0xFF;
        int b2 = (color2) & 0xFF;

        if (horizontal) {
            // LEFT → RIGHT gradient
            for (int i = 0; i < renderW; i++) {

                float t = (renderW <= 1) ? 0f : (float) i / (renderW - 1);

                int a = (int) (a1 + (a2 - a1) * t);
                int r = (int) (r1 + (r2 - r1) * t);
                int g = (int) (g1 + (g2 - g1) * t);
                int b = (int) (b1 + (b2 - b1) * t);

                int color = (a << 24) | (r << 16) | (g << 8) | b;

                int rowOffset = (yStart * this.width) + (xStart + i);

                for (int j = 0; j < renderH; j++) {
                    pixels[rowOffset + j * this.width] = color;
                }
            }

        } else {
            // TOP → BOTTOM gradient
            for (int j = 0; j < renderH; j++) {

                float t = (renderH <= 1) ? 0f : (float) j / (renderH - 1);

                int a = (int) (a1 + (a2 - a1) * t);
                int r = (int) (r1 + (r2 - r1) * t);
                int g = (int) (g1 + (g2 - g1) * t);
                int b = (int) (b1 + (b2 - b1) * t);

                int color = (a << 24) | (r << 16) | (g << 8) | b;

                int offset = (yStart + j) * this.width + xStart;
                int end = offset + renderW;

                while (offset < end) {
                    pixels[offset++] = color;
                }
            }
        }
    }

    /**
     * Draws a horizontal line from (x, y) to (x + width, y)
     */
    public void drawHorizontalLine(int x, int y, int width, int color) {
        // Basic screen clipping
        if (y < 0 || y >= height || x + width <= 0 || x >= width) return;

        // Clamp start and end points
        int xStart = Math.max(0, x);
        int xEnd = Math.min(this.width - 1, x + width - 1);

        int rowOffset = y * this.width;
        for (int i = xStart; i <= xEnd; i++) {
            // If your engine supports alpha, use a blend function here
            // Otherwise, simply overwrite the pixel
            pixels[rowOffset + i] = color;
        }
    }

    /**
     * Helper to interpolate between two ARGB sets and pack them into an int.
     */
    private int interpolate(int a1, int r1, int g1, int b1, int a2, int r2, int g2, int b2, float ratio) {
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // --- 1. Pixel Operations ---

    public void setPixel(int x, int y, int color) {
        if (x < clipX1 || x >= clipX2 || y < clipY1 || y >= clipY2) return;
        pixels[y * width + x] = color;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0;
        return pixels[y * width + x];
    }

    // --- 2. Lines (Bresenham's Algorithm) ---

    public void drawLine(int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            setPixel(x1, y1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x1 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    public void fillRect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;

        int xStart = Math.max(0, x);
        int yStart = Math.max(0, y);
        int xEnd = Math.min(width, x + w);
        int yEnd = Math.min(height, y + h);

        if (xStart >= xEnd || yStart >= yEnd) return;

        int rowWidth = xEnd - xStart;

        for (int row = yStart; row < yEnd; row++) {
            int offset = row * width + xStart;
            int end = offset + rowWidth;

            while (offset < end) {
                pixels[offset++] = color;
            }
        }
    }

    public void fillRectBlended(int x, int y, int w, int h, int color) {
        int xStart = Math.max(0, x);
        int yStart = Math.max(0, y);
        int xEnd = Math.min(width, x + w);
        int yEnd = Math.min(height, y + h);

        for (int row = yStart; row < yEnd; row++) {
            for (int col = xStart; col < xEnd; col++) {
                blendPixel(col, row, color);
            }
        }
    }

    public void blendPixel(int x, int y, int srcColor) {
        if (x < clipX1 || x >= clipX2 || y < clipY1 || y >= clipY2) return;

        int idx = y * width + x;
        int dstColor = pixels[idx];

        int srcA = srcColor >>> 24;
        if (srcA == 0) return;

        // Fast path: fully opaque
        if (srcA == 255) {
            pixels[idx] = srcColor;
            return;
        }

        int dstA = dstColor >>> 24;

        int srcR = (srcColor >> 16) & 0xFF;
        int srcG = (srcColor >> 8) & 0xFF;
        int srcB = srcColor & 0xFF;

        int dstR = (dstColor >> 16) & 0xFF;
        int dstG = (dstColor >> 8) & 0xFF;
        int dstB = dstColor & 0xFF;

        int invA = 255 - srcA;

        int outA = srcA + (dstA * invA) / 255;
        int outR = (srcR * srcA + dstR * invA) / 255;
        int outG = (srcG * srcA + dstG * invA) / 255;
        int outB = (srcB * srcA + dstB * invA) / 255;

        pixels[idx] =
                (outA << 24) |
                        (outR << 16) |
                        (outG << 8) |
                        outB;
    }

    public void blendPixelGlobal(int x, int y, int srcColor) {
        if (x < clipX1 || x >= clipX2 || y < clipY1 || y >= clipY2) return;

        int idx = y * width + x;
        int dstColor = pixels[idx];

        int srcA = (int) ((srcColor >>> 24) * globalAlpha);
        if (srcA <= 0) return;

        // Fast path: fully opaque after global alpha
        if (srcA >= 255) {
            pixels[idx] = (srcColor & 0x00FFFFFF) | (255 << 24);
            return;
        }

        int dstA = dstColor >>> 24;

        int srcR = (srcColor >> 16) & 0xFF;
        int srcG = (srcColor >> 8) & 0xFF;
        int srcB = srcColor & 0xFF;

        int dstR = (dstColor >> 16) & 0xFF;
        int dstG = (dstColor >> 8) & 0xFF;
        int dstB = dstColor & 0xFF;

        int invA = 255 - srcA;

        int outA = srcA + (dstA * invA) / 255;
        int outR = (srcR * srcA + dstR * invA) / 255;
        int outG = (srcG * srcA + dstG * invA) / 255;
        int outB = (srcB * srcA + dstB * invA) / 255;

        pixels[idx] =
                (outA << 24) |
                        (outR << 16) |
                        (outG << 8) |
                        outB;
    }

    /**
     * Fills a rectangle using a Java2D Paint object (Gradients).
     */
    public void fillRect(int x, int y, int w, int h, Paint paint) {
        // Bounds clipping
        int xStart = Math.max(0, x);
        int yStart = Math.max(0, y);
        int xEnd = Math.min(width, x + w);
        int yEnd = Math.min(height, y + h);
        int renderW = xEnd - xStart;
        int renderH = yEnd - yStart;

        if (renderW <= 0 || renderH <= 0) return;

        // Create a context from the paint
        Rectangle bounds = new Rectangle(x, y, w, h);
        PaintContext context = paint.createContext(null, bounds, bounds, new AffineTransform(), new RenderingHints(null));
        Raster raster = context.getRaster(xStart, yStart, renderW, renderH);

        // Pull the integer ARGB data from the raster
        int[] paintPixels = new int[renderW * renderH];
        if (raster.getDataBuffer() instanceof DataBufferInt) {
            paintPixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        } else {
            // Fallback for different buffer types
            Object data = raster.getDataElements(xStart, yStart, renderW, renderH, null);
            if (data instanceof int[]) paintPixels = (int[]) data;
        }

        // Apply to our buffer
        for (int row = 0; row < renderH; row++) {
            int srcOffset = row * renderW;
            int destOffset = (yStart + row) * width + xStart;
            for (int col = 0; col < renderW; col++) {
                blendPixel(xStart + col, yStart + row, paintPixels[srcOffset + col]);
            }
        }
        context.dispose();
    }

    public void drawRect(int x, int y, int w, int h, int color) {
        int x2 = x + w - 1;
        int y2 = y + h - 1;
        // Horizontal lines
        drawLine(x, y, x2, y, color);
        drawLine(x, y2, x2, y2, color);
        // Vertical lines
        drawVLine(x, y, y2, color);
        drawVLine(x2, y, y2, color);
    }

    // --- 4. Vertical Line Optimization (Raycasting Hero) ---

    public void drawVLine(int x, int y1, int y2, int color) {
        if (x < 0 || x >= width) return;

        int startY = Math.max(0, Math.min(y1, y2));
        int endY = Math.min(height - 1, Math.max(y1, y2));

        int offset = startY * width + x;
        for (int y = startY; y <= endY; y++) {
            pixels[offset] = color;
            offset += width; // Jump exactly one row in memory
        }
    }

    // --- 5. Screen Utilities ---

    public void clear(int color) {
        // Highly optimized system call for filling arrays
        Arrays.fill(pixels, color);
    }

    public void drawVerticalGradient(int x, int y1, int y2, int topColor, int bottomColor) {
        if (x < 0 || x >= width) return;

        int startY = Math.max(0, Math.min(y1, y2));
        int endY = Math.min(height - 1, Math.max(y1, y2));
        int totalSteps = endY - startY;
        if (totalSteps <= 0) return;

        // Extract ARGB components
        int a1 = (topColor >> 24) & 0xFF, r1 = (topColor >> 16) & 0xFF, g1 = (topColor >> 8) & 0xFF, b1 = topColor & 0xFF;
        int a2 = (bottomColor >> 24) & 0xFF, r2 = (bottomColor >> 16) & 0xFF, g2 = (bottomColor >> 8) & 0xFF, b2 = bottomColor & 0xFF;

        int offset = startY * width + x;
        for (int i = 0; i <= totalSteps; i++) {
            // Linear interpolation (Fixed-point math for speed)
            float ratio = (float) i / totalSteps;
            int a = (int) (a1 + (a2 - a1) * ratio);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            pixels[offset] = (a << 24) | (r << 16) | (g << 8) | b;
            offset += width;
        }
    }


    public void applyVignette(int color, float strength) {
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        float maxDist = (float) Math.sqrt(centerX * centerX + centerY * centerY) * strength;

        int vr = (color >> 16) & 0xFF;
        int vg = (color >> 8) & 0xFF;
        int vb = color & 0xFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float dx = x - centerX;
                float dy = y - centerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Calculate vignette intensity based on distance from center
                float t = Math.min(1.0f, dist / maxDist);
                int alpha = (int) (t * 180); // Max vignette opacity

                if (alpha > 0) {
                    blendPixel(x, y, (alpha << 24) | (vr << 16) | (vg << 8) | vb);
                }
            }
        }
    }

    /**
     * The core pixel-copying logic with clipping
     */
    public void drawImage(int[] srcPixels, int srcW, int srcH, int x, int y) {
        int xStart = Math.max(0, -x);
        int yStart = Math.max(0, -y);
        int xEnd = Math.min(srcW, width - x);
        int yEnd = Math.min(srcH, height - y);

        if (xStart >= xEnd || yStart >= yEnd) return;

        for (int row = yStart; row < yEnd; row++) {
            int srcOffset = row * srcW;
            int destOffset = (row + y) * width + (xStart + x);

            for (int col = xStart; col < xEnd; col++) {
                int color = srcPixels[srcOffset + col];
                // Only draw if not fully transparent
                if ((color >>> 24) > 0) {
                    pixels[destOffset] = color;
                }
                destOffset++;
            }
        }
    }

    public void drawImageBlended(int[] srcPixels, int srcW, int srcH, int x, int y) {
        int xStart = Math.max(0, -x);
        int yStart = Math.max(0, -y);
        int xEnd = Math.min(srcW, width - x);
        int yEnd = Math.min(srcH, height - y);

        if (xStart >= xEnd || yStart >= yEnd) return;

        for (int row = yStart; row < yEnd; row++) {
            int srcOffset = row * srcW;
            int dstY = row + y;
            int dstXStart = xStart + x;

            for (int col = xStart; col < xEnd; col++) {
                int color = srcPixels[srcOffset + col];
                if ((color >>> 24) != 0) {
                    blendPixel(dstXStart + (col - xStart), dstY, color);
                }
            }
        }
    }

    /**
     * Draws another PixelGraphics instance onto this one at (x, y).
     */
    public void drawImage(PixelGraphics src, int x, int y) {
        if (src == null) return;
        drawImage(src.pixels, src.width, src.height, x, y);
    }

    public void drawImageBlended(PixelGraphics src, int x, int y) {
        if (src == null) return;
        drawImageBlended(src.pixels, src.width, src.height, x, y);
    }

    public void drawImageTinted(int[] srcPixels, int srcW, int srcH, int x, int y, int tint) {
        int tR = (tint >> 16) & 0xFF;
        int tG = (tint >> 8) & 0xFF;
        int tB = tint & 0xFF;
        int tA = (tint >>> 24) & 0xFF;

        int xStart = Math.max(0, -x);
        int yStart = Math.max(0, -y);
        int xEnd = Math.min(srcW, width - x);
        int yEnd = Math.min(srcH, height - y);
        if (xStart >= xEnd || yStart >= yEnd) return;

        for (int row = yStart; row < yEnd; row++) {
            int srcOff = row * srcW;
            int dstY = row + y;
            int dstX0 = xStart + x;
            for (int col = xStart; col < xEnd; col++) {
                int c = srcPixels[srcOff + col];
                int a = (c >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = ((c >> 16) & 0xFF) * tR / 255;
                int g = ((c >> 8) & 0xFF) * tG / 255;
                int b = (c & 0xFF) * tB / 255;
                int ra = a * tA / 255;
                blendPixel(dstX0 + (col - xStart), dstY, (ra << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    public void drawImageTinted(PixelGraphics src, int x, int y, int tint) {
        if (src == null) return;
        drawImageTinted(src.pixels, src.width, src.height, x, y, tint);
    }

    public void drawImageFlipped(int[] srcPixels, int srcW, int srcH, int x, int y, boolean flipH, boolean flipV) {
        int xStart = Math.max(0, -x);
        int yStart = Math.max(0, -y);
        int xEnd = Math.min(srcW, width - x);
        int yEnd = Math.min(srcH, height - y);
        if (xStart >= xEnd || yStart >= yEnd) return;

        for (int row = yStart; row < yEnd; row++) {
            int srcRow = flipV ? (srcH - 1 - row) : row;
            int dstY = row + y;
            for (int col = xStart; col < xEnd; col++) {
                int srcCol = flipH ? (srcW - 1 - col) : col;
                int color = srcPixels[srcRow * srcW + srcCol];
                if ((color >>> 24) > 0) blendPixel(col + x, dstY, color);
            }
        }
    }

    public void drawImageFlipped(PixelGraphics src, int x, int y, boolean flipH, boolean flipV) {
        if (src == null) return;
        drawImageFlipped(src.pixels, src.width, src.height, x, y, flipH, flipV);
    }

    /**
     * Draws a scaled version of an image array.
     * Uses Nearest Neighbor interpolation for that "pixel art" look.
     */
    public void drawImageScaled(int[] srcPixels, int srcW, int srcH, int x, int y, int dstW, int dstH) {
        for (int i = 0; i < dstH; i++) {
            int sy = i * srcH / dstH;
            int destY = y + i;
            if (destY < 0 || destY >= height) continue;

            for (int j = 0; j < dstW; j++) {
                int sx = j * srcW / dstW;
                int destX = x + j;
                if (destX < 0 || destX >= width) continue;

                int color = srcPixels[sy * srcW + sx];
                if ((color >>> 24) > 0) {
                    blendPixel(destX, destY, color);
                }
            }
        }
    }

    public void drawImageScaledBlended(int[] srcPixels, int srcW, int srcH, int x, int y, int dstW, int dstH) {
        for (int i = 0; i < dstH; i++) {
            int sy = i * srcH / dstH;
            int destY = y + i;
            if (destY < 0 || destY >= height) continue;

            for (int j = 0; j < dstW; j++) {
                int sx = j * srcW / dstW;
                int destX = x + j;
                if (destX < 0 || destX >= width) continue;

                int color = srcPixels[sy * srcW + sx];
                if ((color >>> 24) != 0) {
                    blendPixel(destX, destY, color);
                }
            }
        }
    }

    public float getGlobalAlpha() {
        return globalAlpha;
    }

    public void setGlobalAlpha(float alpha) {
        this.globalAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    public void setClip(int x, int y, int w, int h) {
        clipX1 = Math.max(0, x);
        clipY1 = Math.max(0, y);
        clipX2 = Math.min(width, x + w);
        clipY2 = Math.min(height, y + h);
    }

    public void clearClip() {
        clipX1 = 0;
        clipY1 = 0;
        clipX2 = width;
        clipY2 = height;
    }

    public PixelGraphics getSubImage(int x, int y, int w, int h) {
        int xStart = Math.max(0, x);
        int yStart = Math.max(0, y);
        int xEnd = Math.min(this.width, x + w);
        int yEnd = Math.min(this.height, y + h);

        int newW = xEnd - xStart;
        int newH = yEnd - yStart;

        if (newW <= 0 || newH <= 0) {
            return new PixelGraphics(1, 1);
        }

        int[] out = new int[newW * newH];

        for (int row = 0; row < newH; row++) {
            int srcOffset = (yStart + row) * this.width + xStart;
            int dstOffset = row * newW;

            System.arraycopy(this.pixels, srcOffset, out, dstOffset, newW);
        }

        return new PixelGraphics(out, newW, newH);
    }

    public void drawTaperedLine(
            float x1, float y1,
            float x2, float y2,
            float width,
            int color) {

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;

        float stepX = dx / length;
        float stepY = dy / length;

        for (float i = 0; i <= length; i++) {

            float t = i / length; // 0 → 1 (taper factor)

            float wx = width * (1.0f - t);

            int radius = Math.max(1, (int) wx);

            int cx = (int) (x1 + stepX * i);
            int cy = (int) (y1 + stepY * i);

            // stamp small circles along path
            for (int y = -radius; y <= radius; y++) {
                for (int x = -radius; x <= radius; x++) {
                    if (x * x + y * y <= radius * radius) {
                        this.blendPixel(cx + x, cy + y, color);
                    }
                }
            }
        }
    }

    /**
     * Creates a new PixelGraphics instance scaled up by an integer factor.
     * Perfect for retro-style software rendering.
     * * @param scaleFactor The multiplier (e.g., 2 for 2x scaling)
     *
     * @return A new scaled PixelGraphics instance
     */
    public PixelGraphics scale(int scaleFactor) {
        if (scaleFactor <= 1) return this; // Or return a copy if you prefer consistency

        int newWidth = this.width * scaleFactor;
        int newHeight = this.height * scaleFactor;
        PixelGraphics scaled = new PixelGraphics(newWidth, newHeight);

        for (int y = 0; y < this.height; y++) {
            int srcRowOffset = y * this.width;

            // Calculate the starting row in the destination
            int destRowStart = y * scaleFactor;

            for (int x = 0; x < this.width; x++) {
                int color = this.pixels[srcRowOffset + x];

                // Scale the pixel horizontally and vertically
                for (int rowOffset = 0; rowOffset < scaleFactor; rowOffset++) {
                    int destOffset = (destRowStart + rowOffset) * newWidth + (x * scaleFactor);

                    // Fill the horizontal 'block' of pixels for this scale factor
                    for (int colOffset = 0; colOffset < scaleFactor; colOffset++) {
                        scaled.pixels[destOffset + colOffset] = color;
                    }
                }
            }
        }
        return scaled;
    }

    public void scaleTo(PixelGraphics target, int scaleFactor) {
        if (scaleFactor <= 1) {
            System.arraycopy(this.pixels, 0, target.pixels, 0,
                    Math.min(this.pixels.length, target.pixels.length));
            return;
        }

        int srcW = this.width;
        int srcH = this.height;
        int dstW = target.width;

        if (target.width < this.width * scaleFactor ||
                target.height < this.height * scaleFactor) {
            throw new IllegalArgumentException("Target too small");
        }

        for (int y = 0; y < srcH; y++) {
            int srcRow = y * srcW;
            int dstRowBase = (y * scaleFactor) * dstW;

            for (int x = 0; x < srcW; x++) {
                int color = this.pixels[srcRow + x];
                int dstX = x * scaleFactor;

                for (int dy = 0; dy < scaleFactor; dy++) {
                    int dstIndex = dstRowBase + (dy * dstW) + dstX;

                    Arrays.fill(
                            target.pixels,
                            dstIndex,
                            dstIndex + scaleFactor,
                            color
                    );
                }
            }
        }
    }
    // --- Bitmap Font Rendering ---

    public void renderChar(BitmapFont font, int ch, int x, int y, int color) {
        if (font == null) return;

        int glyphW = font.getGlyphWidth();
        int glyphH = font.getGlyphHeight();

        int[] glyphs = font.getChars(Character.toString((char) ch));
        if (glyphs.length == 0) return;

        int glyphIndex = glyphs[0];
        byte[] glyph = font.getGlyph(glyphIndex);

        for (int row = 0; row < glyphH; row++) {
            if (y + row < 0 || y + row >= height) continue;

            byte bits = glyph[row];

            for (int col = 0; col < glyphW; col++) {
                if (x + col < 0 || x + col >= width) continue;

                int mask = 1 << (7 - col);
                if ((bits & mask) != 0) {
                    pixels[(y + row) * width + (x + col)] = color;
                }
            }
        }
    }

    public void renderString(BitmapFont font, String text, int x, int y, int color) {
        if (font == null || text == null) return;

        int[] glyphIndices = font.getChars(text);
        int cursorX = x;
        int glyphW = font.getGlyphWidth();
        int glyphH = font.getGlyphHeight();

        for (int i = 0; i < glyphIndices.length; i++) {
            byte[] glyph = font.getGlyph(glyphIndices[i]);

            for (int row = 0; row < glyphH; row++) {
                if (y + row < 0 || y + row >= height) continue;

                byte bits = glyph[row];
                int base = (y + row) * width + cursorX;

                for (int col = 0; col < glyphW; col++) {
                    if (x + col < 0 || x + col >= width) continue;

                    if ((bits & (1 << (7 - col))) != 0) {
                        pixels[base + col] = color;
                    }
                }
            }

            cursorX += glyphW;
        }
    }

    public void renderString(BitmapFont font, String text,
                             int x, int y,
                             int textColor,
                             int backgroundColor) {

        if (font == null || text == null) return;

        int glyphW = font.getGlyphWidth();
        int glyphH = font.getGlyphHeight();

        int[] glyphIndices = font.getChars(text);

        int cursorX = x;

        for (int i = 0; i < glyphIndices.length; i++) {

            byte[] glyph = font.getGlyph(glyphIndices[i]);

            for (int row = 0; row < glyphH; row++) {

                int py = y + row;
                if (py < 0 || py >= height) continue;

                byte bits = glyph[row];

                int base = py * width + cursorX;

                for (int col = 0; col < glyphW; col++) {

                    int px = cursorX + col;
                    if (px < 0 || px >= width) continue;

                    int mask = 1 << (7 - col);

                    if ((bits & mask) != 0) {
                        pixels[base + col] = textColor;
                    } else {
                        pixels[base + col] = backgroundColor;
                    }
                }
            }

            cursorX += glyphW;
        }
    }

    public void renderString(SpriteSheetFont font, String text, int x, int y, int color) {
        if (font == null || text == null) return;
        font.drawString(this, text, x, y, color);
    }

    public void renderString(BitmapFont font, String text, int x, int y, int color, int backgroundColor, int scale) {
        if (font == null || text == null || scale <= 0) return;
        if (scale == 1) {
            renderString(font, text, x, y, color, backgroundColor);
            return;
        }

        int glyphW = font.getGlyphWidth();
        int glyphH = font.getGlyphHeight();
        int[] glyphIndices = font.getChars(text);
        int cursorX = x;

        for (int i = 0; i < glyphIndices.length; i++) {
            byte[] glyph = font.getGlyph(glyphIndices[i]);

            for (int row = 0; row < glyphH; row++) {
                byte bits = glyph[row];
                for (int col = 0; col < glyphW; col++) {
                    int px = cursorX + col * scale;
                    int py = y + row * scale;
                    fillRect(px, py, scale, scale, (bits & (1 << (7 - col))) != 0 ? color : backgroundColor);
                }
            }

            cursorX += glyphW * scale;
        }
    }

    public void renderString(SpriteSheetFont font, String text, int x, int y, int color, int scale) {
        if (font == null || text == null || scale <= 0) return;
        if (scale == 1) {
            renderString(font, text, x, y, color);
            return;
        }

        int glyphW = font.getGlyphWidth();
        int glyphH = font.getGlyphHeight();
        int cursorX = x;

        for (int i = 0; i < text.length(); i++) {
            int idx = font.getCharacters().indexOf(text.charAt(i));
            if (idx >= 0) {
                PixelGraphics glyph = font.getAtlas().getTexture(idx);
                for (int row = 0; row < glyphH; row++) {
                    for (int col = 0; col < glyphW; col++) {
                        if ((glyph.pixels[row * glyphW + col] >>> 24) > 0) {
                            fillRect(cursorX + col * scale, y + row * scale, scale, scale, color);
                        }
                    }
                }
            }
            cursorX += glyphW * scale;
        }
    }

    // --- IVec2 Overloads ---

    public void setPixel(IVec2 p, int color) {
        setPixel(p.x, p.y, color);
    }

    public int getPixel(IVec2 p) {
        return getPixel(p.x, p.y);
    }

    public void blendPixel(IVec2 p, int srcColor) {
        blendPixel(p.x, p.y, srcColor);
    }

    public void blendPixelGlobal(IVec2 p, int srcColor) {
        blendPixelGlobal(p.x, p.y, srcColor);
    }

    public void drawLine(IVec2 p1, IVec2 p2, int color) {
        drawLine(p1.x, p1.y, p2.x, p2.y, color);
    }

    public void drawTaperedLine(IVec2 p1, IVec2 p2, float width, int color) {
        drawTaperedLine(p1.x, p1.y, p2.x, p2.y, width, color);
    }

    public void drawHorizontalLine(IVec2 p, int width, int color) {
        drawHorizontalLine(p.x, p.y, width, color);
    }

    public void drawVLine(IVec2 p, int y2, int color) {
        drawVLine(p.x, p.y, y2, color);
    }

    public void drawRect(IVec2 p, int w, int h, int color) {
        drawRect(p.x, p.y, w, h, color);
    }

    public void fillRect(IVec2 p, int w, int h, int color) {
        fillRect(p.x, p.y, w, h, color);
    }

    public void fillRectBlended(IVec2 p, int w, int h, int color) {
        fillRectBlended(p.x, p.y, w, h, color);
    }

    public void fillRect(IVec2 p, int w, int h, Paint paint) {
        fillRect(p.x, p.y, w, h, paint);
    }

    public void fillGradientRect(IVec2 p, int w, int h, int color1, int color2, boolean horizontal) {
        fillGradientRect(p.x, p.y, w, h, color1, color2, horizontal);
    }

    public void drawImage(int[] srcPixels, int srcW, int srcH, IVec2 p) {
        drawImage(srcPixels, srcW, srcH, p.x, p.y);
    }

    public void drawImage(PixelGraphics src, IVec2 p) {
        drawImage(src, p.x, p.y);
    }

    public void drawImageBlended(int[] srcPixels, int srcW, int srcH, IVec2 p) {
        drawImageBlended(srcPixels, srcW, srcH, p.x, p.y);
    }

    public void drawImageBlended(PixelGraphics src, IVec2 p) {
        drawImageBlended(src, p.x, p.y);
    }

    public void drawImageTinted(int[] srcPixels, int srcW, int srcH, IVec2 p, int tint) {
        drawImageTinted(srcPixels, srcW, srcH, p.x, p.y, tint);
    }

    public void drawImageTinted(PixelGraphics src, IVec2 p, int tint) {
        drawImageTinted(src, p.x, p.y, tint);
    }

    public void drawImageFlipped(int[] srcPixels, int srcW, int srcH, IVec2 p, boolean flipH, boolean flipV) {
        drawImageFlipped(srcPixels, srcW, srcH, p.x, p.y, flipH, flipV);
    }

    public void drawImageFlipped(PixelGraphics src, IVec2 p, boolean flipH, boolean flipV) {
        drawImageFlipped(src, p.x, p.y, flipH, flipV);
    }

    public void drawImageScaled(int[] srcPixels, int srcW, int srcH, IVec2 p, int dstW, int dstH) {
        drawImageScaled(srcPixels, srcW, srcH, p.x, p.y, dstW, dstH);
    }

    public void drawImageScaledBlended(int[] srcPixels, int srcW, int srcH, IVec2 p, int dstW, int dstH) {
        drawImageScaledBlended(srcPixels, srcW, srcH, p.x, p.y, dstW, dstH);
    }

    public void setClip(IVec2 p, int w, int h) {
        setClip(p.x, p.y, w, h);
    }

    public PixelGraphics getSubImage(IVec2 p, int w, int h) {
        return getSubImage(p.x, p.y, w, h);
    }

    public void renderChar(BitmapFont font, int ch, IVec2 p, int color) {
        renderChar(font, ch, p.x, p.y, color);
    }

    public void renderString(BitmapFont font, String text, IVec2 p, int color) {
        renderString(font, text, p.x, p.y, color);
    }

    public void renderString(BitmapFont font, String text, IVec2 p, int textColor, int backgroundColor) {
        renderString(font, text, p.x, p.y, textColor, backgroundColor);
    }

    public void renderString(SpriteSheetFont font, String text, IVec2 p, int color) {
        renderString(font, text, p.x, p.y, color);
    }

    public void renderString(BitmapFont font, String text, IVec2 p, int color, int backgroundColor, int scale) {
        renderString(font, text, p.x, p.y, color, backgroundColor, scale);
    }

    public void renderString(SpriteSheetFont font, String text, IVec2 p, int color, int scale) {
        renderString(font, text, p.x, p.y, color, scale);
    }
}