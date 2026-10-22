package berryngine;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Full-frame post-processing on a software framebuffer.
 *
 * <p>Accepts either a plain {@link PixelGraphics} or a {@link ThreadedPixelGraphics}. When given a
 * threaded target, queued draws are flushed via {@link ThreadedPixelGraphics#effect} before the
 * effect runs on the shared pixel buffer.
 */
public final class PostFX {

  private PostFX() {}

  /** Flush and defer when {@code g} is a threaded framebuffer; otherwise run immediately. */
  private static boolean defer(PixelGraphics g, Consumer<PixelGraphics> effect) {
    if (g instanceof ThreadedPixelGraphics) {
      ((ThreadedPixelGraphics) g).effect(effect);
      return true;
    }
    return false;
  }

  // -------------------------------------------------------------------------
  // COLOR
  // -------------------------------------------------------------------------

  public static void grayscale(PixelGraphics g) {
    if (defer(g, PostFX::grayscale)) return;

    for (int i = 0; i < g.pixels.length; i++) {
      g.pixels[i] = Color.grayscale(g.pixels[i]);
    }
  }

  public static void invert(PixelGraphics g) {
    if (defer(g, PostFX::invert)) return;

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = 255 - Color.getRed(c);
      int gr = 255 - Color.getGreen(c);
      int b = 255 - Color.getBlue(c);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  public static void tint(PixelGraphics g, int color, float amount) {
    if (defer(g, buf -> tint(buf, color, amount))) return;

    int t = (int) (Mathf.clamp01(amount) * 255f + 0.5f);

    for (int i = 0; i < g.pixels.length; i++) {
      g.pixels[i] = Color.lerp255(g.pixels[i], color, t);
    }
  }

  public static void brightness(PixelGraphics g, float amount) {
    if (defer(g, buf -> brightness(buf, amount))) return;

    int factor256 = Math.max(0, (int) (amount * 256f + 0.5f));

    for (int i = 0; i < g.pixels.length; i++) {
      g.pixels[i] = Color.multiply256(g.pixels[i], factor256);
    }
  }

  public static void contrast(PixelGraphics g, float amount) {
    if (defer(g, buf -> contrast(buf, amount))) return;

    int amountQ = (int) (amount * 256f + 0.5f);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = Color.getRed(c);
      int gr = Color.getGreen(c);
      int b = Color.getBlue(c);

      r = Mathf.clamp(128 + (((r - 128) * amountQ) >> 8), 0, 255);
      gr = Mathf.clamp(128 + (((gr - 128) * amountQ) >> 8), 0, 255);
      b = Mathf.clamp(128 + (((b - 128) * amountQ) >> 8), 0, 255);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  /** amount: 0 = grayscale 1 = unchanged > 1 = increased saturation */
  public static void saturation(PixelGraphics g, float amount) {
    if (defer(g, buf -> saturation(buf, amount))) return;

    int amountQ = (int) (amount * 256f + 0.5f);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = Color.getRed(c);
      int gr = Color.getGreen(c);
      int b = Color.getBlue(c);

      int gray = Color.luminance(c);

      r = Mathf.clamp(gray + (((r - gray) * amountQ) >> 8), 0, 255);
      gr = Mathf.clamp(gray + (((gr - gray) * amountQ) >> 8), 0, 255);
      b = Mathf.clamp(gray + (((b - gray) * amountQ) >> 8), 0, 255);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  /** Maps blackPoint -> 0 and whitePoint -> 255. */
  public static void levels(PixelGraphics g, int blackPoint, int whitePoint) {
    if (defer(g, buf -> levels(buf, blackPoint, whitePoint))) return;

    int black = Mathf.clamp(blackPoint, 0, 255);
    int white = Mathf.clamp(whitePoint, 0, 255);

    if (white <= black) {
      return;
    }

    int range = white - black;

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = Mathf.clamp(((Color.getRed(c) - black) * 255) / range, 0, 255);
      int gr = Mathf.clamp(((Color.getGreen(c) - black) * 255) / range, 0, 255);
      int b = Mathf.clamp(((Color.getBlue(c) - black) * 255) / range, 0, 255);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  /** Reduces each RGB channel to a fixed number of levels. */
  public static void posterize(PixelGraphics g, int levels) {
    if (defer(g, buf -> posterize(buf, levels))) return;

    if (levels <= 1) {
      grayscale(g);
      return;
    }

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = quantizeChannel(Color.getRed(c), levels);
      int gr = quantizeChannel(Color.getGreen(c), levels);
      int b = quantizeChannel(Color.getBlue(c), levels);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  public static void paletteQuantize(PixelGraphics g, int[] palette) {
    if (defer(g, buf -> paletteQuantize(buf, palette))) return;

    if (palette == null || palette.length == 0) {
      return;
    }

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];
      int best = Color.nearest(c, palette);

      g.pixels[i] =
          Color.fromRGBA(
              Color.getRed(best), Color.getGreen(best), Color.getBlue(best), Color.getAlpha(c));
    }
  }

  public static void paletteQuantizeDither(PixelGraphics g, int[] palette, float strength) {
    if (defer(g, buf -> paletteQuantizeDither(buf, palette, strength))) return;

    if (palette == null || palette.length == 0) {
      return;
    }

    int strength255 = (int) (Mathf.clamp01(strength) * 255f + 0.5f);

    final int[][] bayer = {
      {0, 8, 2, 10},
      {12, 4, 14, 6},
      {3, 11, 1, 9},
      {15, 7, 13, 5}
    };

    int w = g.width;
    int h = g.height;

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int index = y * w + x;
        int c = g.pixels[index];

        // ((bayer + 0.5)/16 - 0.5) * 255 * s ≈ ((bayer - 8) * strength255) / 16
        int threshold = ((bayer[y & 3][x & 3] - 8) * strength255) / 16;

        int r = Mathf.clamp(Color.getRed(c) + threshold, 0, 255);
        int gr = Mathf.clamp(Color.getGreen(c) + threshold, 0, 255);
        int b = Mathf.clamp(Color.getBlue(c) + threshold, 0, 255);

        int dithered = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
        int best = Color.nearest(dithered, palette);

        g.pixels[index] =
            Color.fromRGBA(
                Color.getRed(best), Color.getGreen(best), Color.getBlue(best), Color.getAlpha(c));
      }
    }
  }

  public static void colorReplace(PixelGraphics g, int from, int to, int tolerance) {
    if (defer(g, buf -> colorReplace(buf, from, to, tolerance))) return;

    int fr = Color.getRed(from);
    int fg = Color.getGreen(from);
    int fb = Color.getBlue(from);

    int tol = Math.max(0, tolerance);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int r = Color.getRed(c);
      int gr = Color.getGreen(c);
      int b = Color.getBlue(c);

      if (Math.abs(r - fr) <= tol && Math.abs(gr - fg) <= tol && Math.abs(b - fb) <= tol) {

        g.pixels[i] =
            Color.fromRGBA(
                Color.getRed(to), Color.getGreen(to), Color.getBlue(to), Color.getAlpha(c));
      }
    }
  }

  // -------------------------------------------------------------------------
  // DITHERING
  // -------------------------------------------------------------------------

  /**
   * 4x4 Bayer ordered dithering.
   *
   * <p>levels = number of colors per RGB channel.
   */
  public static void dither(PixelGraphics g, int levels) {
    if (defer(g, buf -> dither(buf, levels))) return;

    if (levels <= 1) {
      grayscale(g);
      return;
    }

    final int[] matrix = {
      0, 8, 2, 10,
      12, 4, 14, 6,
      3, 11, 1, 9,
      15, 7, 13, 5
    };

    int w = g.width;
    int h = g.height;

    int levelSpan = levels - 1;

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int index = y * w + x;
        int c = g.pixels[index];

        int threshold = matrix[(y & 3) * 4 + (x & 3)];

        // (threshold/16 - 0.5) * (255/(L-1)) ≈ ((threshold - 8) * 255) / (16 * (L-1))
        int offset = ((threshold - 8) * 255) / (16 * levelSpan);

        int r = Mathf.clamp(Color.getRed(c) + offset, 0, 255);
        int gr = Mathf.clamp(Color.getGreen(c) + offset, 0, 255);
        int b = Mathf.clamp(Color.getBlue(c) + offset, 0, 255);

        r = quantizeChannel(r, levels);
        gr = quantizeChannel(gr, levels);
        b = quantizeChannel(b, levels);

        g.pixels[index] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
      }
    }
  }

  // -------------------------------------------------------------------------
  // CRT / RETRO
  // -------------------------------------------------------------------------

  public static void scanlines(PixelGraphics g, int spacing, float strength) {
    if (defer(g, buf -> scanlines(buf, spacing, strength))) return;

    if (spacing <= 0) {
      return;
    }

    int darken256 = 256 - (int) (Mathf.clamp01(strength) * 256f + 0.5f);

    int w = g.width;
    int h = g.height;

    for (int y = 0; y < h; y += spacing) {
      for (int x = 0; x < w; x++) {
        int index = y * w + x;
        g.pixels[index] = Color.multiply256(g.pixels[index], darken256);
      }
    }
  }

  /** Adds random monochrome noise. */
  public static void noise(PixelGraphics g, float strength, long seed) {
    if (defer(g, buf -> noise(buf, strength, seed))) return;

    int amount = (int) (Mathf.clamp01(strength) * 255f + 0.5f);

    if (amount <= 0) {
      return;
    }

    Random random = new Random(seed);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int noise = random.nextInt(amount * 2 + 1) - amount;

      int r = Mathf.clamp(Color.getRed(c) + noise, 0, 255);
      int gr = Mathf.clamp(Color.getGreen(c) + noise, 0, 255);
      int b = Mathf.clamp(Color.getBlue(c) + noise, 0, 255);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(c));
    }
  }

  /** Convenience overload using a non-deterministic seed. */
  public static void noise(PixelGraphics g, float strength) {

    noise(g, strength, System.nanoTime());
  }

  /** Separates the red and blue channels horizontally. */
  public static void chromaticAberration(PixelGraphics g, int offset) {
    if (defer(g, buf -> chromaticAberration(buf, offset))) return;

    if (offset <= 0) {
      return;
    }

    int[] source = g.pixels.clone();

    int w = g.width;
    int h = g.height;

    for (int y = 0; y < h; y++) {
      int row = y * w;

      for (int x = 0; x < w; x++) {
        int redX = Mathf.clamp(x + offset, 0, w - 1);
        int blueX = Mathf.clamp(x - offset, 0, w - 1);

        int center = source[row + x];
        int red = source[row + redX];
        int blue = source[row + blueX];

        g.pixels[row + x] =
            Color.fromRGBA(
                Color.getRed(red),
                Color.getGreen(center),
                Color.getBlue(blue),
                Color.getAlpha(center));
      }
    }
  }

  /**
   * Barrel-like CRT curvature.
   *
   * <p>This is implemented with nearest-neighbor sampling so it remains appropriate for a software
   * framebuffer.
   */
  public static void screenCurve(PixelGraphics g, float strength) {
    if (defer(g, buf -> screenCurve(buf, strength))) return;

    if (strength == 0.0f) {
      return;
    }

    int[] source = g.pixels.clone();

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

        if (sourceX < 0 || sourceX >= w || sourceY < 0 || sourceY >= h) {

          g.pixels[index] = Color.fromRGBA(0, 0, 0, Color.getAlpha(source[index]));
        } else {
          g.pixels[index] = source[sourceY * w + sourceX];
        }
      }
    }
  }

  /** Simple RGB channel separation. */
  public static void colorFringing(PixelGraphics g, int offset) {

    chromaticAberration(g, offset);
  }

  /** Horizontal sine-wave distortion. */
  public static void horizontalDistortion(PixelGraphics g, float strength, float frequency) {
    if (defer(g, buf -> horizontalDistortion(buf, strength, frequency))) return;

    if (strength == 0.0f) {
      return;
    }

    int[] source = g.pixels.clone();

    int w = g.width;
    int h = g.height;

    for (int y = 0; y < h; y++) {
      int displacement = (int) (Mathf.sin(y * frequency) * strength);

      for (int x = 0; x < w; x++) {
        int sourceX = Mathf.clamp(x + displacement, 0, w - 1);

        g.pixels[y * w + x] = source[y * w + sourceX];
      }
    }
  }

  // -------------------------------------------------------------------------
  // VIGNETTE / LIGHTING
  // -------------------------------------------------------------------------

  public static void vignette(PixelGraphics g, float strength) {
    if (defer(g, buf -> vignette(buf, strength))) return;

    int s256 = (int) (Mathf.clamp01(strength) * 256f + 0.5f);

    int w = g.width;
    int h = g.height;

    int cx = w >> 1;
    int cy = h >> 1;
    int maxD2 = Math.max(1, cx * cx + cy * cy);

    for (int y = 0; y < h; y++) {
      int dy = y - cy;
      for (int x = 0; x < w; x++) {
        int dx = x - cx;
        int d2 = dx * dx + dy * dy;

        // factor = 1 - (d2/maxD2)*s  →  factor256 = 256 - d2*s256/maxD2
        int factor256 = 256 - (d2 * s256) / maxD2;
        if (factor256 < 0) {
          factor256 = 0;
        }

        g.pixels[y * w + x] = Color.multiply256(g.pixels[y * w + x], factor256);
      }
    }
  }

  /** Blends the entire screen toward a color. */
  public static void fade(PixelGraphics g, int color, float amount) {

    tint(g, color, amount);
  }

  /** Same basic operation as fade, useful semantically for flashes such as explosions or damage. */
  public static void flash(PixelGraphics g, int color, float amount) {

    tint(g, color, amount);
  }

  /**
   * Simple distance-based fog.
   *
   * <p>This operates on screen coordinates rather than world depth, so it is mainly useful for
   * stylized effects.
   */
  public static void fog(PixelGraphics g, int color, float amount) {

    tint(g, color, Mathf.clamp01(amount));
  }

  /** Green monochrome night-vision effect. */
  public static void nightVision(PixelGraphics g, float strength) {
    if (defer(g, buf -> nightVision(buf, strength))) return;

    int s = (int) (Mathf.clamp01(strength) * 255f + 0.5f);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int gray = Color.luminance(c);
      int green = Mathf.clamp((gray * 135) / 100, 0, 255);

      int target = Color.fromRGBA(0, green, 0, Color.getAlpha(c));

      g.pixels[i] = Color.lerp255(c, target, s);
    }
  }

  /** Stylized thermal / heat-vision effect. */
  public static void heatVision(PixelGraphics g, float strength) {
    if (defer(g, buf -> heatVision(buf, strength))) return;

    int s = (int) (Mathf.clamp01(strength) * 255f + 0.5f);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      int gray = Color.luminance(c);

      int rr;
      int gg;
      int bb;

      if (gray < 64) {
        // 0..63 → blue 0..255
        rr = 0;
        gg = 0;
        bb = gray * 4;
      } else if (gray < 128) {
        // 64..127 → green 0..255, blue 255
        rr = 0;
        gg = (gray - 64) * 4;
        bb = 255;
      } else if (gray < 192) {
        // 128..191 → red 0..255, green 255, blue 255→0
        rr = (gray - 128) * 4;
        gg = 255;
        bb = 255 - (gray - 128) * 4;
      } else {
        // 192..255 → red 255, green 255, blue 0..255
        rr = 255;
        gg = 255;
        bb = (gray - 192) * 4;
      }

      int target = Color.fromRGBA(rr, gg, bb, Color.getAlpha(c));

      g.pixels[i] = Color.lerp255(c, target, s);
    }
  }

  // -------------------------------------------------------------------------
  // PIXELATION
  // -------------------------------------------------------------------------

  public static void pixelate(PixelGraphics g, int factor) {
    if (defer(g, buf -> pixelate(buf, factor))) return;

    if (factor <= 1) {
      return;
    }

    int w = g.width;
    int h = g.height;

    for (int y = 0; y < h; y += factor) {
      for (int x = 0; x < w; x += factor) {
        int sampleX = Math.min(x + factor / 2, w - 1);

        int sampleY = Math.min(y + factor / 2, h - 1);

        int color = g.pixels[sampleY * w + sampleX];

        for (int by = 0; by < factor && y + by < h; by++) {
          for (int bx = 0; bx < factor && x + bx < w; bx++) {

            g.pixels[(y + by) * w + (x + bx)] = color;
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
   * <p>This implementation uses two passes to avoid repeatedly scanning a full 2D neighborhood.
   */
  public static void boxBlur(PixelGraphics g, int radius) {
    if (defer(g, buf -> boxBlur(buf, radius))) return;

    if (radius <= 0) {
      return;
    }

    int w = g.width;
    int h = g.height;

    int[] temp = new int[g.pixels.length];

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
          int c = g.pixels[y * w + sx];

          r += Color.getRed(c);
          gr += Color.getGreen(c);
          b += Color.getBlue(c);
          a += Color.getAlpha(c);

          count++;
        }

        temp[y * w + x] =
            Color.fromRGBA(
                (int) (r / count), (int) (gr / count), (int) (b / count), (int) (a / count));
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

        g.pixels[y * w + x] =
            Color.fromRGBA(
                (int) (r / count), (int) (gr / count), (int) (b / count), (int) (a / count));
      }
    }
  }

  /**
   * Approximate Gaussian blur using repeated box blurs.
   *
   * <p>This is intentionally implemented without floating-point Gaussian kernels, making it
   * considerably cheaper for a software framebuffer.
   */
  public static void gaussianBlur(PixelGraphics g, int radius) {
    if (defer(g, buf -> gaussianBlur(buf, radius))) return;

    if (radius <= 0) {
      return;
    }

    int smallRadius = Math.max(1, radius / 2);

    boxBlur(g, smallRadius);
    boxBlur(g, smallRadius);
    boxBlur(g, smallRadius);
  }

  /**
   * Simple bloom:
   *
   * <p>1. Extract bright pixels. 2. Blur them. 3. Add them back to the original image.
   */
  public static void bloom(PixelGraphics g, int threshold, int radius, float strength) {
    if (defer(g, buf -> bloom(buf, threshold, radius, strength))) return;

    if (radius <= 0 || strength <= 0.0f) {
      return;
    }

    int s256 = Math.max(0, (int) (strength * 256f + 0.5f));

    int w = g.width;
    int h = g.height;

    int[] original = g.pixels.clone();

    PixelGraphics bright = new PixelGraphics(w, h);

    for (int i = 0; i < g.pixels.length; i++) {
      int c = g.pixels[i];

      if (Color.luminance(c) >= threshold) {
        bright.pixels[i] = c;
      } else {
        bright.pixels[i] = Color.fromRGBA(0, 0, 0, Color.getAlpha(c));
      }
    }

    gaussianBlur(bright, radius);

    for (int i = 0; i < g.pixels.length; i++) {
      int base = original[i];
      int glow = bright.pixels[i];

      int r = Mathf.clamp(Color.getRed(base) + ((Color.getRed(glow) * s256) >> 8), 0, 255);

      int gr = Mathf.clamp(Color.getGreen(base) + ((Color.getGreen(glow) * s256) >> 8), 0, 255);

      int b = Mathf.clamp(Color.getBlue(base) + ((Color.getBlue(glow) * s256) >> 8), 0, 255);

      g.pixels[i] = Color.fromRGBA(r, gr, b, Color.getAlpha(base));
    }
  }

  // -------------------------------------------------------------------------
  // OUTLINE
  // -------------------------------------------------------------------------

  /**
   * Adds an outline around pixels whose neighboring pixels differ.
   *
   * <p>Particularly useful for sprites and pixel-art objects.
   */
  public static void outline(PixelGraphics g, int outlineColor, int tolerance) {
    if (defer(g, buf -> outline(buf, outlineColor, tolerance))) return;

    int[] source = g.pixels.clone();

    int w = g.width;
    int h = g.height;

    int tol = Math.max(0, tolerance);

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int index = y * w + x;
        int center = source[index];

        boolean edge = false;

        if (x > 0 && colorsDiffer(center, source[index - 1], tol)) {
          edge = true;
        }

        if (x < w - 1 && colorsDiffer(center, source[index + 1], tol)) {
          edge = true;
        }

        if (y > 0 && colorsDiffer(center, source[index - w], tol)) {
          edge = true;
        }

        if (y < h - 1 && colorsDiffer(center, source[index + w], tol)) {
          edge = true;
        }

        if (edge) {
          g.pixels[index] = outlineColor;
        }
      }
    }
  }

  // -------------------------------------------------------------------------
  // HELPERS
  // -------------------------------------------------------------------------

  private static int quantizeChannel(int value, int levels) {

    if (levels <= 1) {
      return 0;
    }

    int max = levels - 1;
    int quantized = (value * max + 127) / 255;
    return Mathf.clamp((quantized * 255) / max, 0, 255);
  }

  private static boolean colorsDiffer(int a, int b, int tolerance) {

    return Math.abs(Color.getRed(a) - Color.getRed(b)) > tolerance
        || Math.abs(Color.getGreen(a) - Color.getGreen(b)) > tolerance
        || Math.abs(Color.getBlue(a) - Color.getBlue(b)) > tolerance;
  }
}
