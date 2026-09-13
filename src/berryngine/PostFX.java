package berryngine;

import java.util.Random;

public final class PostFX {

    private PostFX() {
    }

    // -------------------------------------------------------------------------
    // COLOR
    // -------------------------------------------------------------------------

    public static void grayscale(PixelGraphics g) {
        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int gray = (r * 77 + gr * 150 + b * 29) >> 8;

            pixels[i] = Color.fromRGBA(
                    gray,
                    gray,
                    gray,
                    Color.getAlpha(c)
            );
        }
    }

    public static void invert(PixelGraphics g) {
        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = 255 - Color.getRed(c);
            int gr = 255 - Color.getGreen(c);
            int b = 255 - Color.getBlue(c);

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    public static void tint(PixelGraphics g, int color, float amount) {
        amount = Mathf.clamp01(amount);

        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = Color.lerp(pixels[i], color, amount);
        }
    }

    public static void brightness(PixelGraphics g, float amount) {
        amount = Math.max(0.0f, amount);

        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = Color.multiply(pixels[i], amount);
        }
    }

    public static void contrast(PixelGraphics g, float amount) {
        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            r = clamp255((int) ((r - 128) * amount + 128));
            gr = clamp255((int) ((gr - 128) * amount + 128));
            b = clamp255((int) ((b - 128) * amount + 128));

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    /**
     * amount:
     * 0   = grayscale
     * 1   = unchanged
     * > 1 = increased saturation
     */
    public static void saturation(PixelGraphics g, float amount) {
        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int gray = (r * 77 + gr * 150 + b * 29) >> 8;

            r = clamp255((int) (gray + (r - gray) * amount));
            gr = clamp255((int) (gray + (gr - gray) * amount));
            b = clamp255((int) (gray + (b - gray) * amount));

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    /**
     * Maps blackPoint -> 0 and whitePoint -> 255.
     */
    public static void levels(
            PixelGraphics g,
            int blackPoint,
            int whitePoint) {

        blackPoint = clamp255(blackPoint);
        whitePoint = clamp255(whitePoint);

        if (whitePoint <= blackPoint) {
            return;
        }

        int[] pixels = g.pixels;
        float scale = 255.0f / (whitePoint - blackPoint);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = clamp255((int) ((Color.getRed(c) - blackPoint) * scale));
            int gr = clamp255((int) ((Color.getGreen(c) - blackPoint) * scale));
            int b = clamp255((int) ((Color.getBlue(c) - blackPoint) * scale));

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    /**
     * Reduces each RGB channel to a fixed number of levels.
     */
    public static void posterize(PixelGraphics g, int levels) {
        if (levels <= 1) {
            grayscale(g);
            return;
        }

        int[] pixels = g.pixels;
        int max = levels - 1;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = quantizeChannel(Color.getRed(c), levels);
            int gr = quantizeChannel(Color.getGreen(c), levels);
            int b = quantizeChannel(Color.getBlue(c), levels);

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    public static void paletteQuantize(PixelGraphics g, int[] palette) {
        if (palette == null || palette.length == 0) {
            return;
        }

        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int bestColor = palette[0];
            int bestDistance = Integer.MAX_VALUE;

            for (int j = 0; j < palette.length; j++) {
                int p = palette[j];

                int pr = Color.getRed(p);
                int pg = Color.getGreen(p);
                int pb = Color.getBlue(p);

                int dr = r - pr;
                int dg = gr - pg;
                int db = b - pb;

                int distance = dr * dr + dg * dg + db * db;

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestColor = p;

                    if (distance == 0) {
                        break;
                    }
                }
            }

            pixels[i] = Color.fromRGBA(
                    Color.getRed(bestColor),
                    Color.getGreen(bestColor),
                    Color.getBlue(bestColor),
                    Color.getAlpha(c)
            );
        }
    }

    public static void paletteQuantizeDither(PixelGraphics g, int[] palette, float strength) {
        if (palette == null || palette.length == 0) {
            return;
        }

        strength = Mathf.clamp01(strength);

        final int[][] bayer = {
                { 0,  8,  2, 10 },
                {12,  4, 14,  6 },
                { 3, 11,  1,  9 },
                {15,  7, 13,  5 }
        };

        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                int c = pixels[index];

                int r = Color.getRed(c);
                int gr = Color.getGreen(c);
                int b = Color.getBlue(c);

                float threshold =
                        ((bayer[y & 3][x & 3] + 0.5f) / 16.0f - 0.5f)
                                * 255.0f
                                * strength;

                r = clamp255((int) (r + threshold));
                gr = clamp255((int) (gr + threshold));
                b = clamp255((int) (b + threshold));

                int bestColor = palette[0];
                int bestDistance = Integer.MAX_VALUE;

                for (int j = 0; j < palette.length; j++) {
                    int p = palette[j];

                    int pr = Color.getRed(p);
                    int pg = Color.getGreen(p);
                    int pb = Color.getBlue(p);

                    int dr = r - pr;
                    int dg = gr - pg;
                    int db = b - pb;

                    int distance = dr * dr + dg * dg + db * db;

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestColor = p;

                        if (distance == 0) {
                            break;
                        }
                    }
                }

                pixels[index] = Color.fromRGBA(
                        Color.getRed(bestColor),
                        Color.getGreen(bestColor),
                        Color.getBlue(bestColor),
                        Color.getAlpha(c)
                );
            }
        }
    }

    public static void colorReplace(
            PixelGraphics g,
            int from,
            int to,
            int tolerance) {

        int[] pixels = g.pixels;

        int fr = Color.getRed(from);
        int fg = Color.getGreen(from);
        int fb = Color.getBlue(from);

        tolerance = Math.max(0, tolerance);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            if (Math.abs(r - fr) <= tolerance &&
                    Math.abs(gr - fg) <= tolerance &&
                    Math.abs(b - fb) <= tolerance) {

                pixels[i] = Color.fromRGBA(
                        Color.getRed(to),
                        Color.getGreen(to),
                        Color.getBlue(to),
                        Color.getAlpha(c)
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // DITHERING
    // -------------------------------------------------------------------------

    /**
     * 4x4 Bayer ordered dithering.
     *
     * levels = number of colors per RGB channel.
     */
    public static void dither(PixelGraphics g, int levels) {
        if (levels <= 1) {
            grayscale(g);
            return;
        }

        final int[] matrix = {
                0,  8,  2, 10,
                12, 4, 14,  6,
                3, 11,  1,  9,
                15, 7, 13,  5
        };

        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;

        float step = 255.0f / (levels - 1);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int index = y * w + x;
                int c = pixels[index];

                int threshold = matrix[(y & 3) * 4 + (x & 3)];

                float offset =
                        (threshold / 16.0f - 0.5f) * step;

                int r = clamp255((int) (Color.getRed(c) + offset));
                int gr = clamp255((int) (Color.getGreen(c) + offset));
                int b = clamp255((int) (Color.getBlue(c) + offset));

                r = quantizeChannel(r, levels);
                gr = quantizeChannel(gr, levels);
                b = quantizeChannel(b, levels);

                pixels[index] = Color.fromRGBA(
                        r,
                        gr,
                        b,
                        Color.getAlpha(c)
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // CRT / RETRO
    // -------------------------------------------------------------------------

    public static void scanlines(
            PixelGraphics g,
            int spacing,
            float strength) {

        if (spacing <= 0) {
            return;
        }

        strength = Mathf.clamp01(strength);

        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y += spacing) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                pixels[index] =
                        Color.multiply(pixels[index], 1.0f - strength);
            }
        }
    }

    /**
     * Adds random monochrome noise.
     */
    public static void noise(
            PixelGraphics g,
            float strength,
            long seed) {

        strength = Mathf.clamp01(strength);

        if (strength <= 0.0f) {
            return;
        }

        Random random = new Random(seed);

        int[] pixels = g.pixels;

        int amount = (int) (255.0f * strength);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int noise = random.nextInt(amount * 2 + 1) - amount;

            int r = clamp255(Color.getRed(c) + noise);
            int gr = clamp255(Color.getGreen(c) + noise);
            int b = clamp255(Color.getBlue(c) + noise);

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(c)
            );
        }
    }

    /**
     * Convenience overload using a non-deterministic seed.
     */
    public static void noise(
            PixelGraphics g,
            float strength) {

        noise(g, strength, System.nanoTime());
    }

    /**
     * Separates the red and blue channels horizontally.
     */
    public static void chromaticAberration(
            PixelGraphics g,
            int offset) {

        if (offset <= 0) {
            return;
        }

        int[] pixels = g.pixels;
        int[] source = pixels.clone();

        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y++) {
            int row = y * w;

            for (int x = 0; x < w; x++) {
                int redX = clamp(x + offset, 0, w - 1);
                int blueX = clamp(x - offset, 0, w - 1);

                int center = source[row + x];
                int red = source[row + redX];
                int blue = source[row + blueX];

                pixels[row + x] = Color.fromRGBA(
                        Color.getRed(red),
                        Color.getGreen(center),
                        Color.getBlue(blue),
                        Color.getAlpha(center)
                );
            }
        }
    }

    /**
     * Barrel-like CRT curvature.
     *
     * This is implemented with nearest-neighbor sampling so it
     * remains appropriate for a software framebuffer.
     */
    public static void screenCurve(
            PixelGraphics g,
            float strength) {

        if (strength == 0.0f) {
            return;
        }

        int[] pixels = g.pixels;
        int[] source = pixels.clone();

        int w = g.width;
        int h = g.height;

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        for (int y = 0; y < h; y++) {
            float ny = (y - cy) / cy;

            for (int x = 0; x < w; x++) {
                float nx = (x - cx) / cx;

                float r2 = nx * nx + ny * ny;

                float factor = 1.0f + strength * r2;

                float sx = nx * factor;
                float sy = ny * factor;

                int sourceX = (int) (sx * cx + cx);
                int sourceY = (int) (sy * cy + cy);

                int index = y * w + x;

                if (sourceX < 0 ||
                        sourceX >= w ||
                        sourceY < 0 ||
                        sourceY >= h) {

                    pixels[index] = Color.fromRGBA(
                            0,
                            0,
                            0,
                            Color.getAlpha(source[index])
                    );
                } else {
                    pixels[index] =
                            source[sourceY * w + sourceX];
                }
            }
        }
    }

    /**
     * Simple RGB channel separation.
     */
    public static void colorFringing(
            PixelGraphics g,
            int offset) {

        chromaticAberration(g, offset);
    }

    /**
     * Horizontal sine-wave distortion.
     */
    public static void horizontalDistortion(
            PixelGraphics g,
            float strength,
            float frequency) {

        if (strength == 0.0f) {
            return;
        }

        int[] pixels = g.pixels;
        int[] source = pixels.clone();

        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y++) {
            int displacement =
                    (int) (
                            Mathf.sin(y * frequency)
                                    * strength
                    );

            for (int x = 0; x < w; x++) {
                int sourceX = x + displacement;

                if (sourceX < 0) {
                    sourceX = 0;
                } else if (sourceX >= w) {
                    sourceX = w - 1;
                }

                pixels[y * w + x] =
                        source[y * w + sourceX];
            }
        }
    }

    // -------------------------------------------------------------------------
    // VIGNETTE / LIGHTING
    // -------------------------------------------------------------------------

    public static void vignette(
            PixelGraphics g,
            float strength) {

        strength = Mathf.clamp01(strength);

        int[] pixels = g.pixels;

        int w = g.width;
        int h = g.height;

        float cx = w * 0.5f;
        float cy = h * 0.5f;

        float maxDist =
                Mathf.fastSqrt(cx * cx + cy * cy);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                float dx = x - cx;
                float dy = y - cy;

                float dist =
                        Mathf.fastSqrt(dx * dx + dy * dy)
                                / maxDist;

                float factor =
                        1.0f - (dist * dist * strength);

                int index = y * w + x;

                pixels[index] =
                        Color.multiply(pixels[index], factor);
            }
        }
    }

    /**
     * Blends the entire screen toward a color.
     */
    public static void fade(
            PixelGraphics g,
            int color,
            float amount) {

        tint(g, color, amount);
    }

    /**
     * Same basic operation as fade, useful semantically
     * for flashes such as explosions or damage.
     */
    public static void flash(
            PixelGraphics g,
            int color,
            float amount) {

        tint(g, color, amount);
    }

    /**
     * Simple distance-based fog.
     *
     * This operates on screen coordinates rather than world depth,
     * so it is mainly useful for stylized effects.
     */
    public static void fog(
            PixelGraphics g,
            int color,
            float amount) {

        tint(g, color, Mathf.clamp01(amount));
    }

    /**
     * Green monochrome night-vision effect.
     */
    public static void nightVision(
            PixelGraphics g,
            float strength) {

        strength = Mathf.clamp01(strength);

        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int gray =
                    (r * 77 + gr * 150 + b * 29) >> 8;

            int green =
                    clamp255((int) (gray * 1.35f));

            int target =
                    Color.fromRGBA(
                            0,
                            green,
                            0,
                            Color.getAlpha(c)
                    );

            pixels[i] =
                    Color.lerp(c, target, strength);
        }
    }

    /**
     * Stylized thermal / heat-vision effect.
     */
    public static void heatVision(
            PixelGraphics g,
            float strength) {

        strength = Mathf.clamp01(strength);

        int[] pixels = g.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int gray =
                    (r * 77 + gr * 150 + b * 29) >> 8;

            int rr;
            int gg;
            int bb;

            float t = gray / 255.0f;

            if (t < 0.25f) {
                float p = t / 0.25f;

                rr = 0;
                gg = 0;
                bb = (int) (p * 255.0f);

            } else if (t < 0.5f) {
                float p = (t - 0.25f) / 0.25f;

                rr = 0;
                gg = (int) (p * 255.0f);
                bb = 255;

            } else if (t < 0.75f) {
                float p = (t - 0.5f) / 0.25f;

                rr = (int) (p * 255.0f);
                gg = 255;
                bb = (int) ((1.0f - p) * 255.0f);

            } else {
                float p = (t - 0.75f) / 0.25f;

                rr = 255;
                gg = 255;
                bb = (int) (p * 255.0f);
            }

            int target =
                    Color.fromRGBA(
                            rr,
                            gg,
                            bb,
                            Color.getAlpha(c)
                    );

            pixels[i] =
                    Color.lerp(c, target, strength);
        }
    }

    // -------------------------------------------------------------------------
    // PIXELATION
    // -------------------------------------------------------------------------

    public static void pixelate(
            PixelGraphics g,
            int factor) {

        if (factor <= 1) {
            return;
        }

        int[] pixels = g.pixels;

        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y += factor) {
            for (int x = 0; x < w; x += factor) {

                int sampleX =
                        Math.min(x + factor / 2, w - 1);

                int sampleY =
                        Math.min(y + factor / 2, h - 1);

                int color =
                        pixels[sampleY * w + sampleX];

                for (int by = 0;
                     by < factor && y + by < h;
                     by++) {

                    for (int bx = 0;
                         bx < factor && x + bx < w;
                         bx++) {

                        pixels[(y + by) * w + (x + bx)] =
                                color;
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // BLUR
    // -------------------------------------------------------------------------

    /**
     * Simple square box blur.
     *
     * This implementation uses two passes to avoid repeatedly
     * scanning a full 2D neighborhood.
     */
    public static void boxBlur(
            PixelGraphics g,
            int radius) {

        if (radius <= 0) {
            return;
        }

        int w = g.width;
        int h = g.height;

        int[] pixels = g.pixels;
        int[] temp = new int[pixels.length];

        // Horizontal pass.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                long r = 0;
                long gr = 0;
                long b = 0;
                long a = 0;

                int count = 0;

                int minX = Math.max(0, x - radius);
                int maxX = Math.min(w - 1, x + radius);

                for (int sx = minX; sx <= maxX; sx++) {
                    int c = pixels[y * w + sx];

                    r += Color.getRed(c);
                    gr += Color.getGreen(c);
                    b += Color.getBlue(c);
                    a += Color.getAlpha(c);

                    count++;
                }

                temp[y * w + x] =
                        Color.fromRGBA(
                                (int) (r / count),
                                (int) (gr / count),
                                (int) (b / count),
                                (int) (a / count)
                        );
            }
        }

        // Vertical pass.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                long r = 0;
                long gr = 0;
                long b = 0;
                long a = 0;

                int count = 0;

                int minY = Math.max(0, y - radius);
                int maxY = Math.min(h - 1, y + radius);

                for (int sy = minY; sy <= maxY; sy++) {
                    int c = temp[sy * w + x];

                    r += Color.getRed(c);
                    gr += Color.getGreen(c);
                    b += Color.getBlue(c);
                    a += Color.getAlpha(c);

                    count++;
                }

                pixels[y * w + x] =
                        Color.fromRGBA(
                                (int) (r / count),
                                (int) (gr / count),
                                (int) (b / count),
                                (int) (a / count)
                        );
            }
        }
    }

    /**
     * Approximate Gaussian blur using repeated box blurs.
     *
     * This is intentionally implemented without floating-point
     * Gaussian kernels, making it considerably cheaper for a
     * software framebuffer.
     */
    public static void gaussianBlur(
            PixelGraphics g,
            int radius) {

        if (radius <= 0) {
            return;
        }

        int smallRadius =
                Math.max(1, radius / 2);

        boxBlur(g, smallRadius);
        boxBlur(g, smallRadius);
        boxBlur(g, smallRadius);
    }

    /**
     * Simple bloom:
     *
     * 1. Extract bright pixels.
     * 2. Blur them.
     * 3. Add them back to the original image.
     */
    public static void bloom(
            PixelGraphics g,
            int threshold,
            int radius,
            float strength) {

        if (radius <= 0 || strength <= 0.0f) {
            return;
        }

        strength = Math.max(0.0f, strength);

        int w = g.width;
        int h = g.height;

        int[] pixels = g.pixels;
        int[] original = pixels.clone();

        PixelGraphics bright =
                new PixelGraphics(w, h);

        int[] brightPixels = bright.pixels;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            int brightness =
                    (r * 77 + gr * 150 + b * 29) >> 8;

            if (brightness >= threshold) {
                brightPixels[i] = c;
            } else {
                brightPixels[i] =
                        Color.fromRGBA(
                                0,
                                0,
                                0,
                                Color.getAlpha(c)
                        );
            }
        }

        gaussianBlur(bright, radius);

        for (int i = 0; i < pixels.length; i++) {
            int base = original[i];
            int glow = brightPixels[i];

            int r = clamp255(
                    (int) (
                            Color.getRed(base)
                                    + Color.getRed(glow) * strength
                    )
            );

            int gr = clamp255(
                    (int) (
                            Color.getGreen(base)
                                    + Color.getGreen(glow) * strength
                    )
            );

            int b = clamp255(
                    (int) (
                            Color.getBlue(base)
                                    + Color.getBlue(glow) * strength
                    )
            );

            pixels[i] = Color.fromRGBA(
                    r,
                    gr,
                    b,
                    Color.getAlpha(base)
            );
        }
    }

    // -------------------------------------------------------------------------
    // OUTLINE
    // -------------------------------------------------------------------------

    /**
     * Adds an outline around pixels whose neighboring pixels differ.
     *
     * Particularly useful for sprites and pixel-art objects.
     */
    public static void outline(
            PixelGraphics g,
            int outlineColor,
            int tolerance) {

        int[] pixels = g.pixels;
        int[] source = pixels.clone();

        int w = g.width;
        int h = g.height;

        tolerance = Math.max(0, tolerance);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int index = y * w + x;
                int center = source[index];

                boolean edge = false;

                if (x > 0 &&
                        colorsDiffer(
                                center,
                                source[index - 1],
                                tolerance)) {
                    edge = true;
                }

                if (x < w - 1 &&
                        colorsDiffer(
                                center,
                                source[index + 1],
                                tolerance)) {
                    edge = true;
                }

                if (y > 0 &&
                        colorsDiffer(
                                center,
                                source[index - w],
                                tolerance)) {
                    edge = true;
                }

                if (y < h - 1 &&
                        colorsDiffer(
                                center,
                                source[index + w],
                                tolerance)) {
                    edge = true;
                }

                if (edge) {
                    pixels[index] = outlineColor;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static int clamp255(int value) {
        if (value < 0) {
            return 0;
        }

        if (value > 255) {
            return 255;
        }

        return value;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private static int quantizeChannel(
            int value,
            int levels) {

        if (levels <= 1) {
            return 0;
        }

        float normalized =
                value / 255.0f;

        int quantized =
                Math.round(
                        normalized * (levels - 1)
                );

        return clamp255(
                Math.round(
                        quantized * 255.0f
                                / (levels - 1)
                )
        );
    }

    private static boolean colorsDiffer(
            int a,
            int b,
            int tolerance) {

        return Math.abs(
                Color.getRed(a) -
                        Color.getRed(b)
        ) > tolerance
                ||
                Math.abs(
                        Color.getGreen(a) -
                                Color.getGreen(b)
                ) > tolerance
                ||
                Math.abs(
                        Color.getBlue(a) -
                                Color.getBlue(b)
                ) > tolerance;
    }
}