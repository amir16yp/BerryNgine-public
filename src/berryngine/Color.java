package berryngine;

public final class Color {

  // =========================================================
  // BASIC COLOR ENCODING (ARGB packed int: 0xAARRGGBB)
  // =========================================================

  public static int fromRGB(int r, int g, int b) {
    return fromRGBA(r, g, b, 255);
  }

  public static int fromRGBA(int r, int g, int b, int a) {
    return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
  }

  // =========================================================
  // PREDEFINED COLORS (expanded palette)
  // =========================================================

  public static final int WHITE = fromRGBA(255, 255, 255, 255);
  public static final int BLACK = fromRGBA(0, 0, 0, 255);

  public static final int RED = fromRGB(255, 0, 0);
  public static final int GREEN = fromRGB(0, 255, 0);
  public static final int BLUE = fromRGB(0, 0, 255);

  public static final int YELLOW = fromRGB(255, 255, 0);
  public static final int CYAN = fromRGB(0, 255, 255);
  public static final int MAGENTA = fromRGB(255, 0, 255);

  public static final int ORANGE = fromRGB(255, 165, 0);
  public static final int PINK = fromRGB(255, 192, 203);
  public static final int PURPLE = fromRGB(128, 0, 128);
  public static final int BROWN = fromRGB(139, 69, 19);

  public static final int GRAY = fromRGB(128, 128, 128);
  public static final int LIGHT_GRAY = fromRGB(211, 211, 211);
  public static final int DARK_GRAY = fromRGB(64, 64, 64);
  public static final int VERY_LIGHT_GRAY = fromRGB(238, 238, 238);
  public static final int VERY_DARK_GRAY = fromRGB(32, 32, 32);
  public static final int SILVER = fromRGB(192, 192, 192);
  public static final int CHARCOAL = fromRGB(54, 69, 79);

  public static final int TRANSPARENT = fromRGBA(0, 0, 0, 0);

  public static final int DARK_RED = fromRGB(139, 0, 0);
  public static final int DARK_GREEN = fromRGB(0, 100, 0);
  public static final int DARK_BLUE = fromRGB(0, 0, 139);
  public static final int MAROON = fromRGB(128, 0, 0);
  public static final int NAVY = fromRGB(0, 0, 128);
  public static final int OLIVE = fromRGB(128, 128, 0);
  public static final int TEAL = fromRGB(0, 128, 128);
  public static final int INDIGO = fromRGB(75, 0, 130);
  public static final int FOREST_GREEN = fromRGB(34, 139, 34);
  public static final int MIDNIGHT_BLUE = fromRGB(25, 25, 112);
  public static final int STEEL_BLUE = fromRGB(70, 130, 180);
  public static final int SKY_BLUE = fromRGB(135, 206, 235);
  public static final int CHOCOLATE = fromRGB(210, 105, 30);

  public static final int LIGHT_RED = fromRGB(255, 128, 128);
  public static final int LIGHT_GREEN = fromRGB(144, 238, 144);
  public static final int LIGHT_BLUE = fromRGB(173, 216, 230);
  public static final int MINT = fromRGB(189, 252, 201);
  public static final int LAVENDER = fromRGB(230, 230, 250);
  public static final int BEIGE = fromRGB(245, 245, 220);
  public static final int CREAM = fromRGB(255, 253, 208);
  public static final int IVORY = fromRGB(255, 255, 240);
  public static final int WHEAT = fromRGB(245, 222, 179);
  public static final int TAN = fromRGB(210, 180, 140);
  public static final int KHAKI = fromRGB(240, 230, 140);
  public static final int PEACH = fromRGB(255, 218, 185);

  public static final int LIME = fromRGB(0, 255, 0);
  public static final int LIME_GREEN = fromRGB(50, 205, 50);
  public static final int SPRING_GREEN = fromRGB(0, 255, 127);
  public static final int AQUA = fromRGB(0, 255, 255);
  public static final int AZURE = fromRGB(240, 255, 255);
  public static final int TURQUOISE = fromRGB(64, 224, 208);
  public static final int VIOLET = fromRGB(238, 130, 238);
  public static final int CRIMSON = fromRGB(220, 20, 60);
  public static final int CORAL = fromRGB(255, 127, 80);
  public static final int SALMON = fromRGB(250, 128, 114);
  public static final int TOMATO = fromRGB(255, 99, 71);
  public static final int ORANGE_RED = fromRGB(255, 69, 0);
  public static final int GOLD = fromRGB(255, 215, 0);
  public static final int BRONZE = fromRGB(205, 127, 50);
  public static final int COPPER = fromRGB(184, 115, 51);
  public static final int PLUM = fromRGB(221, 160, 221);

  // =========================================================
  // CHANNEL EXTRACTION
  // =========================================================

  public static int getAlpha(int color) {
    return (color >> 24) & 0xFF;
  }

  public static boolean isTransparent(int color) {
    return getAlpha(color) == 0;
  }

  public static int getRed(int color) {
    return (color >> 16) & 0xFF;
  }

  public static int getGreen(int color) {
    return (color >> 8) & 0xFF;
  }

  public static int getBlue(int color) {
    return color & 0xFF;
  }

  // =========================================================
  // ALPHA UTILITIES
  // =========================================================

  public static int withAlpha(int color, int alpha) {
    return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
  }

  public static int setRed(int color, int r) {
    return fromRGBA(r, getGreen(color), getBlue(color), getAlpha(color));
  }

  public static int setGreen(int color, int g) {
    return fromRGBA(getRed(color), g, getBlue(color), getAlpha(color));
  }

  public static int setBlue(int color, int b) {
    return fromRGBA(getRed(color), getGreen(color), b, getAlpha(color));
  }

  public static int setAlpha(int color, int a) {
    return fromRGBA(getRed(color), getGreen(color), getBlue(color), a);
  }

  // =========================================================
  // HSB / HSV -> RGB
  // =========================================================

  public static int fromHSB(float hue, float saturation, float brightness) {
    int r, g, b;

    if (saturation == 0.0f) {
      r = g = b = (int) (brightness * 255.0f + 0.5f);
    } else {
      float h = (hue - (float) Math.floor(hue)) * 6.0f;
      int i = (int) h;
      float f = h - i;

      float p = brightness * (1.0f - saturation);
      float q = brightness * (1.0f - saturation * f);
      float t = brightness * (1.0f - saturation * (1.0f - f));

      switch (i) {
        case 0:
          r = (int) (brightness * 255);
          g = (int) (t * 255);
          b = (int) (p * 255);
          break;
        case 1:
          r = (int) (q * 255);
          g = (int) (brightness * 255);
          b = (int) (p * 255);
          break;
        case 2:
          r = (int) (p * 255);
          g = (int) (brightness * 255);
          b = (int) (t * 255);
          break;
        case 3:
          r = (int) (p * 255);
          g = (int) (q * 255);
          b = (int) (brightness * 255);
          break;
        case 4:
          r = (int) (t * 255);
          g = (int) (p * 255);
          b = (int) (brightness * 255);
          break;
        default:
          r = (int) (brightness * 255);
          g = (int) (p * 255);
          b = (int) (q * 255);
          break;
      }
    }

    return fromRGBA(r, g, b, 255);
  }

  // =========================================================
  // BLENDING / INTERPOLATION
  // =========================================================

  /** {@code t} in 0..1; converts to Q8 and lerps channels with integer math. */
  public static int lerp(int c1, int c2, float t) {
    return lerp255(c1, c2, (int) (Mathf.clamp01(t) * 255f + 0.5f));
  }

  /** {@code t} in 0..255 (0 = c1, 255 = c2). */
  public static int lerp255(int c1, int c2, int t) {
    t = Mathf.clamp(t, 0, 255);

    int a1 = getAlpha(c1), r1 = getRed(c1), g1 = getGreen(c1), b1 = getBlue(c1);
    int a2 = getAlpha(c2), r2 = getRed(c2), g2 = getGreen(c2), b2 = getBlue(c2);

    int a = a1 + ((a2 - a1) * t) / 255;
    int r = r1 + ((r2 - r1) * t) / 255;
    int g = g1 + ((g2 - g1) * t) / 255;
    int b = b1 + ((b2 - b1) * t) / 255;

    return fromRGBA(r, g, b, a);
  }

  /** Step {@code i} of {@code steps} (inclusive endpoints when {@code i} is 0..steps). */
  public static int lerpSteps(int c1, int c2, int i, int steps) {
    if (steps <= 0) {
      return c1;
    }
    return lerp255(c1, c2, (i * 255) / steps);
  }

  /** {@code factor} 1.0 = unchanged; uses Q8 ({@code 256 = 1.0}). */
  public static int multiply(int color, float factor) {
    if (factor <= 0f) {
      return fromRGBA(0, 0, 0, getAlpha(color));
    }
    return multiply256(color, (int) (factor * 256f + 0.5f));
  }

  /** Fixed-point multiply: {@code factor256 == 256} means 1.0. */
  public static int multiply256(int color, int factor256) {
    if (factor256 <= 0) {
      return fromRGBA(0, 0, 0, getAlpha(color));
    }

    int r = Mathf.clamp((getRed(color) * factor256) >> 8, 0, 255);
    int g = Mathf.clamp((getGreen(color) * factor256) >> 8, 0, 255);
    int b = Mathf.clamp((getBlue(color) * factor256) >> 8, 0, 255);

    return fromRGBA(r, g, b, getAlpha(color));
  }

  /** Src-over composite. Alpha 0 returns {@code dst}; alpha 255 returns {@code src}. */
  public static int composite(int src, int dst) {
    int srcA = src >>> 24;
    if (srcA == 0) return dst;
    if (srcA == 255) return src;
    int inv = 255 - srcA;
    int a = srcA + ((dst >>> 24) * inv) / 255;
    int r = (((src >>> 16) & 255) * srcA + ((dst >>> 16) & 255) * inv) / 255;
    int g = (((src >>> 8) & 255) * srcA + ((dst >>> 8) & 255) * inv) / 255;
    int b = ((src & 255) * srcA + (dst & 255) * inv) / 255;
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  public static int blend(int src, int dst) {
    int a = getAlpha(src);
    if (a == 0) {
      return fromRGB(getRed(dst), getGreen(dst), getBlue(dst));
    }
    if (a == 255) {
      return fromRGB(getRed(src), getGreen(src), getBlue(src));
    }

    int inv = 255 - a;
    int r = (getRed(src) * a + getRed(dst) * inv) / 255;
    int g = (getGreen(src) * a + getGreen(dst) * inv) / 255;
    int b = (getBlue(src) * a + getBlue(dst) * inv) / 255;

    return fromRGB(r, g, b);
  }

  // =========================================================
  // HEX PARSING
  // =========================================================

  public static int fromHex(String hex) {
    if (hex == null) throw new IllegalArgumentException("hex must not be null");
    String h = hex.startsWith("#") ? hex.substring(1) : hex;
    switch (h.length()) {
      case 6:
        return fromRGBA(
            Integer.parseInt(h.substring(0, 2), 16),
            Integer.parseInt(h.substring(2, 4), 16),
            Integer.parseInt(h.substring(4, 6), 16),
            255);
      case 8:
        return fromRGBA(
            Integer.parseInt(h.substring(0, 2), 16),
            Integer.parseInt(h.substring(2, 4), 16),
            Integer.parseInt(h.substring(4, 6), 16),
            Integer.parseInt(h.substring(6, 8), 16));
      default:
        throw new IllegalArgumentException("Invalid hex color: " + hex);
    }
  }

  // =========================================================
  // HSB / HSV EXTRACTION
  // Hue: 0..1, Saturation: 0..1, Brightness: 0..1
  // =========================================================

  public static float getHueHSB(int color) {
    float r = getRed(color) / 255f;
    float g = getGreen(color) / 255f;
    float b = getBlue(color) / 255f;

    float max = Math.max(r, Math.max(g, b));
    float min = Math.min(r, Math.min(g, b));
    float delta = max - min;

    if (delta == 0f) return 0f;

    float hue;
    if (max == r) {
      hue = ((g - b) / delta) % 6f;
    } else if (max == g) {
      hue = (b - r) / delta + 2f;
    } else {
      hue = (r - g) / delta + 4f;
    }

    hue /= 6f;
    return hue < 0f ? hue + 1f : hue;
  }

  public static float getSaturationHSB(int color) {
    float r = getRed(color) / 255f;
    float g = getGreen(color) / 255f;
    float b = getBlue(color) / 255f;

    float max = Math.max(r, Math.max(g, b));
    float min = Math.min(r, Math.min(g, b));

    return max == 0f ? 0f : (max - min) / max;
  }

  public static float getBrightnessHSB(int color) {
    return Math.max(getRed(color), Math.max(getGreen(color), getBlue(color))) / 255f;
  }

  // =========================================================
  // HSL -> RGB
  // Hue: 0..1, Saturation: 0..1, Lightness: 0..1
  // =========================================================

  public static int fromHSL(float hue, float saturation, float lightness) {
    hue = hue - (float) Math.floor(hue);
    saturation = Mathf.clamp01(saturation);
    lightness = Mathf.clamp01(lightness);

    float r, g, b;

    if (saturation == 0f) {
      r = g = b = lightness;
    } else {
      float q =
          lightness < 0.5f
              ? lightness * (1f + saturation)
              : lightness + saturation - lightness * saturation;

      float p = 2f * lightness - q;

      r = hueToRGB(p, q, hue + 1f / 3f);
      g = hueToRGB(p, q, hue);
      b = hueToRGB(p, q, hue - 1f / 3f);
    }

    return fromRGBA(Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f), 255);
  }

  private static float hueToRGB(float p, float q, float t) {
    if (t < 0f) t += 1f;
    if (t > 1f) t -= 1f;

    if (t < 1f / 6f) return p + (q - p) * 6f * t;
    if (t < 1f / 2f) return q;
    if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;

    return p;
  }

  // =========================================================
  // RGB -> HSL EXTRACTION
  // Hue: 0..1, Saturation: 0..1, Lightness: 0..1
  // =========================================================

  public static float getHueHSL(int color) {
    return getHueHSB(color);
  }

  public static float getSaturationHSL(int color) {
    float r = getRed(color) / 255f;
    float g = getGreen(color) / 255f;
    float b = getBlue(color) / 255f;

    float max = Math.max(r, Math.max(g, b));
    float min = Math.min(r, Math.min(g, b));
    float delta = max - min;
    float lightness = (max + min) / 2f;

    if (delta == 0f) return 0f;

    return delta / (1f - Math.abs(2f * lightness - 1f));
  }

  public static float getLightnessHSL(int color) {
    float r = getRed(color) / 255f;
    float g = getGreen(color) / 255f;
    float b = getBlue(color) / 255f;

    float max = Math.max(r, Math.max(g, b));
    float min = Math.min(r, Math.min(g, b));

    return (max + min) / 2f;
  }

  // =========================================================
  // COLOR MODEL CONVENIENCE METHODS
  // =========================================================

  public static int withHueHSB(int color, float hue) {
    return fromHSB(hue, getSaturationHSB(color), getBrightnessHSB(color));
  }

  public static int withSaturationHSB(int color, float saturation) {
    return fromHSB(getHueHSB(color), Mathf.clamp01(saturation), getBrightnessHSB(color));
  }

  public static int withBrightnessHSB(int color, float brightness) {
    return fromHSB(getHueHSB(color), getSaturationHSB(color), Mathf.clamp01(brightness));
  }

  public static int withHueHSL(int color, float hue) {
    return fromHSL(hue, getSaturationHSL(color), getLightnessHSL(color));
  }

  public static int withSaturationHSL(int color, float saturation) {
    return fromHSL(getHueHSL(color), Mathf.clamp01(saturation), getLightnessHSL(color));
  }

  public static int withLightnessHSL(int color, float lightness) {
    return fromHSL(getHueHSL(color), getSaturationHSL(color), Mathf.clamp01(lightness));
  }

  public static int lighten(int color, float amount) {
    return withLightnessHSL(color, Mathf.clamp01(getLightnessHSL(color) + amount));
  }

  public static int darken(int color, float amount) {
    return withLightnessHSL(color, Mathf.clamp01(getLightnessHSL(color) - amount));
  }

  /** ITU-R BT.601 luma as 0..255 (integer form of 0.299/0.587/0.114). */
  public static int luminance(int color) {
    return (getRed(color) * 77 + getGreen(color) * 150 + getBlue(color) * 29) >> 8;
  }

  public static int grayscale(int color) {
    int gray = luminance(color);
    return fromRGBA(gray, gray, gray, getAlpha(color));
  }

  /**
   * Nearest palette entry by RGB squared distance. Alpha of {@code color} is preserved by the
   * caller.
   */
  public static int nearest(int color, int[] palette) {
    if (palette == null || palette.length == 0) {
      return color;
    }

    int r = getRed(color);
    int g = getGreen(color);
    int b = getBlue(color);

    int bestColor = palette[0];
    int bestDistance = Integer.MAX_VALUE;

    for (int i = 0; i < palette.length; i++) {
      int p = palette[i];
      int dr = r - getRed(p);
      int dg = g - getGreen(p);
      int db = b - getBlue(p);
      int distance = dr * dr + dg * dg + db * db;

      if (distance < bestDistance) {
        bestDistance = distance;
        bestColor = p;
        if (distance == 0) {
          break;
        }
      }
    }

    return bestColor;
  }
}
