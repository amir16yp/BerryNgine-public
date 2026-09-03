package berryngine;

public final class PostFX {

    private PostFX() {
    }

    public static void grayscale(PixelGraphics g) {
        int[] pixels = g.pixels;
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);
            int gray = (r * 77 + gr * 150 + b * 29) >> 8;
            pixels[i] = Color.fromRGBA(gray, gray, gray, Color.getAlpha(c));
        }
    }

    public static void invert(PixelGraphics g) {
        int[] pixels = g.pixels;
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = 255 - Color.getRed(c);
            int gr = 255 - Color.getGreen(c);
            int b = 255 - Color.getBlue(c);
            pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
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
        amount = Mathf.clamp01(amount);
        int[] pixels = g.pixels;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = Color.multiply(pixels[i], amount);
        }
    }

    public static void scanlines(PixelGraphics g, int spacing, float strength) {
        if (spacing <= 0) return;
        strength = Mathf.clamp01(strength);
        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;
        for (int y = 0; y < h; y += spacing) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                pixels[idx] = Color.multiply(pixels[idx], 1.0f - strength);
            }
        }
    }

    public static void vignette(PixelGraphics g, float strength) {
        strength = Mathf.clamp01(strength);
        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float maxDist = Mathf.fastSqrt(cx * cx + cy * cy);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float dist = Mathf.fastSqrt(dx * dx + dy * dy) / maxDist;
                float factor = 1.0f - (dist * dist * strength);
                int idx = y * w + x;
                pixels[idx] = Color.multiply(pixels[idx], factor);
            }
        }
    }

    public static void pixelate(PixelGraphics g, int factor) {
        if (factor <= 1) return;
        int[] pixels = g.pixels;
        int w = g.width;
        int h = g.height;

        for (int y = 0; y < h; y += factor) {
            for (int x = 0; x < w; x += factor) {
                int sampleX = Math.min(x + factor / 2, w - 1);
                int sampleY = Math.min(y + factor / 2, h - 1);
                int color = pixels[sampleY * w + sampleX];

                for (int by = 0; by < factor && y + by < h; by++) {
                    for (int bx = 0; bx < factor && x + bx < w; bx++) {
                        pixels[(y + by) * w + (x + bx)] = color;
                    }
                }
            }
        }
    }

    public static void colorReplace(PixelGraphics g, int from, int to, int tolerance) {
        int[] pixels = g.pixels;
        int fr = Color.getRed(from);
        int fg = Color.getGreen(from);
        int fb = Color.getBlue(from);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int r = Color.getRed(c);
            int gr = Color.getGreen(c);
            int b = Color.getBlue(c);

            if (Math.abs(r - fr) <= tolerance &&
                    Math.abs(gr - fg) <= tolerance &&
                    Math.abs(b - fb) <= tolerance) {
                pixels[i] = Color.fromRGBA(Color.getRed(to), Color.getGreen(to), Color.getBlue(to), Color.getAlpha(c));
            }
        }
    }
}
