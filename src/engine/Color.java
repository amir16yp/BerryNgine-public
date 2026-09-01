package engine;

public final class Color {

    // =========================================================
    // BASIC COLOR ENCODING (ARGB packed int: 0xAARRGGBB)
    // =========================================================

    public static int fromRGB(int r, int g, int b) {
        return fromRGBA(r, g, b, 255);
    }

    public static int fromRGBA(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                (b & 0xFF);
    }

    // =========================================================
    // PREDEFINED COLORS (expanded palette)
    // =========================================================

    public static final int WHITE = fromRGBA(255, 255, 255, 255);
    public static final int BLACK = fromRGBA(0, 0, 0, 255);

    public static final int RED   = fromRGB(255, 0, 0);
    public static final int GREEN = fromRGB(0, 255, 0);
    public static final int BLUE  = fromRGB(0, 0, 255);

    public static final int YELLOW = fromRGB(255, 255, 0);
    public static final int CYAN   = fromRGB(0, 255, 255);
    public static final int MAGENTA= fromRGB(255, 0, 255);

    public static final int ORANGE = fromRGB(255, 165, 0);
    public static final int PINK   = fromRGB(255, 192, 203);
    public static final int PURPLE = fromRGB(128, 0, 128);
    public static final int BROWN  = fromRGB(139, 69, 19);

    public static final int GRAY       = fromRGB(128, 128, 128);
    public static final int LIGHT_GRAY = fromRGB(211, 211, 211);
    public static final int DARK_GRAY  = fromRGB(64, 64, 64);
    public static final int VERY_LIGHT_GRAY = fromRGB(238, 238, 238);
    public static final int VERY_DARK_GRAY  = fromRGB(32, 32, 32);
    public static final int SILVER     = fromRGB(192, 192, 192);
    public static final int CHARCOAL   = fromRGB(54, 69, 79);

    public static final int TRANSPARENT = fromRGBA(0, 0, 0, 0);

    public static final int DARK_RED     = fromRGB(139, 0, 0);
    public static final int DARK_GREEN   = fromRGB(0, 100, 0);
    public static final int DARK_BLUE    = fromRGB(0, 0, 139);
    public static final int MAROON       = fromRGB(128, 0, 0);
    public static final int NAVY         = fromRGB(0, 0, 128);
    public static final int OLIVE        = fromRGB(128, 128, 0);
    public static final int TEAL         = fromRGB(0, 128, 128);
    public static final int INDIGO       = fromRGB(75, 0, 130);
    public static final int FOREST_GREEN = fromRGB(34, 139, 34);
    public static final int MIDNIGHT_BLUE = fromRGB(25, 25, 112);
    public static final int STEEL_BLUE   = fromRGB(70, 130, 180);
    public static final int SKY_BLUE     = fromRGB(135, 206, 235);
    public static final int CHOCOLATE    = fromRGB(210, 105, 30);

    public static final int LIGHT_RED    = fromRGB(255, 128, 128);
    public static final int LIGHT_GREEN  = fromRGB(144, 238, 144);
    public static final int LIGHT_BLUE   = fromRGB(173, 216, 230);
    public static final int MINT         = fromRGB(189, 252, 201);
    public static final int LAVENDER     = fromRGB(230, 230, 250);
    public static final int BEIGE        = fromRGB(245, 245, 220);
    public static final int CREAM        = fromRGB(255, 253, 208);
    public static final int IVORY        = fromRGB(255, 255, 240);
    public static final int WHEAT        = fromRGB(245, 222, 179);
    public static final int TAN          = fromRGB(210, 180, 140);
    public static final int KHAKI        = fromRGB(240, 230, 140);
    public static final int PEACH        = fromRGB(255, 218, 185);

    public static final int LIME         = fromRGB(0, 255, 0);
    public static final int LIME_GREEN   = fromRGB(50, 205, 50);
    public static final int SPRING_GREEN = fromRGB(0, 255, 127);
    public static final int AQUA         = fromRGB(0, 255, 255);
    public static final int AZURE        = fromRGB(240, 255, 255);
    public static final int TURQUOISE    = fromRGB(64, 224, 208);
    public static final int VIOLET       = fromRGB(238, 130, 238);
    public static final int CRIMSON      = fromRGB(220, 20, 60);
    public static final int CORAL        = fromRGB(255, 127, 80);
    public static final int SALMON       = fromRGB(250, 128, 114);
    public static final int TOMATO       = fromRGB(255, 99, 71);
    public static final int ORANGE_RED   = fromRGB(255, 69, 0);
    public static final int GOLD         = fromRGB(255, 215, 0);
    public static final int BRONZE       = fromRGB(205, 127, 50);
    public static final int COPPER       = fromRGB(184, 115, 51);
    public static final int PLUM         = fromRGB(221, 160, 221);

    // =========================================================
    // CHANNEL EXTRACTION
    // =========================================================

    public static int getAlpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static boolean isTransparent(int color)
    {
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
            r = g = b = (int)(brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            int i = (int) h;
            float f = h - i;

            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));

            switch (i) {
                case 0: r = (int)(brightness * 255); g = (int)(t * 255); b = (int)(p * 255); break;
                case 1: r = (int)(q * 255); g = (int)(brightness * 255); b = (int)(p * 255); break;
                case 2: r = (int)(p * 255); g = (int)(brightness * 255); b = (int)(t * 255); break;
                case 3: r = (int)(p * 255); g = (int)(q * 255); b = (int)(brightness * 255); break;
                case 4: r = (int)(t * 255); g = (int)(p * 255); b = (int)(brightness * 255); break;
                default:r = (int)(brightness * 255); g = (int)(p * 255); b = (int)(q * 255); break;
            }
        }

        return fromRGBA(r, g, b, 255);
    }

    // =========================================================
    // BLENDING / INTERPOLATION
    // =========================================================

    public static int lerp(int c1, int c2, float t) {
        t = clamp01(t);

        int a1 = getAlpha(c1), r1 = getRed(c1), g1 = getGreen(c1), b1 = getBlue(c1);
        int a2 = getAlpha(c2), r2 = getRed(c2), g2 = getGreen(c2), b2 = getBlue(c2);

        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);

        return fromRGBA(r, g, b, a);
    }

    public static int multiply(int color, float factor) {
        int r = (int)(getRed(color) * factor);
        int g = (int)(getGreen(color) * factor);
        int b = (int)(getBlue(color) * factor);

        return fromRGBA(r, g, b, getAlpha(color));
    }

    public static int blend(int src, int dst) {
        float a = getAlpha(src) / 255f;

        int r = (int)(getRed(src) * a + getRed(dst) * (1 - a));
        int g = (int)(getGreen(src) * a + getGreen(dst) * (1 - a));
        int b = (int)(getBlue(src) * a + getBlue(dst) * (1 - a));

        return fromRGB(r, g, b);
    }

    // =========================================================
    // HEX PARSING
    // =========================================================

    public static int fromHex(String hex) {
        if (hex == null) throw new IllegalArgumentException("hex must not be null");
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        switch (h.length()) {
            case 6: return fromRGBA(
                    Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16),
                    255);
            case 8: return fromRGBA(
                    Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16),
                    Integer.parseInt(h.substring(6, 8), 16));
            default: throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
    }

    // =========================================================
    // UTILS
    // =========================================================

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

}