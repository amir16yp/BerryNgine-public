package berryngine;

public final class QOIDecoder {

    private QOIDecoder() {
    }

    public static PixelGraphics decode(byte[] data) {
        if (data == null || data.length < 14) {
            throw new IllegalArgumentException("Invalid QOI data");
        }

        // --- Header ---
        if (!"qoif".equals(new String(data, 0, 4))) {
            throw new IllegalArgumentException("Invalid QOI magic");
        }

        int width =
                ((data[4] & 0xFF) << 24) |
                        ((data[5] & 0xFF) << 16) |
                        ((data[6] & 0xFF) << 8) |
                        (data[7] & 0xFF);

        int height =
                ((data[8] & 0xFF) << 24) |
                        ((data[9] & 0xFF) << 16) |
                        ((data[10] & 0xFF) << 8) |
                        (data[11] & 0xFF);

        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];

        int[] index = new int[64];

        int r = 0, g = 0, b = 0, a = 255;
        int p = 14;

        for (int i = 0; i < pixelCount; i++) {

            if (p >= data.length) {
                throw new IllegalArgumentException("Unexpected end of QOI stream");
            }

            int b1 = data[p++] & 0xFF;

            if (b1 == 0xFE) {
                // RGB
                r = data[p++] & 0xFF;
                g = data[p++] & 0xFF;
                b = data[p++] & 0xFF;

            } else if (b1 == 0xFF) {
                // RGBA
                r = data[p++] & 0xFF;
                g = data[p++] & 0xFF;
                b = data[p++] & 0xFF;
                a = data[p++] & 0xFF;

            } else {
                int tag = b1 & 0xC0;

                if (tag == 0x00) {
                    // INDEX
                    int idx = b1 & 0x3F;
                    int px = index[idx];

                    a = (px >>> 24) & 0xFF;
                    r = (px >>> 16) & 0xFF;
                    g = (px >>> 8) & 0xFF;
                    b = px & 0xFF;

                } else if (tag == 0x40) {
                    // DIFF
                    int dr = ((b1 >> 4) & 0x03) - 2;
                    int dg = ((b1 >> 2) & 0x03) - 2;
                    int db = (b1 & 0x03) - 2;

                    r = (r + dr) & 0xFF;
                    g = (g + dg) & 0xFF;
                    b = (b + db) & 0xFF;

                } else if (tag == 0x80) {
                    // LUMA
                    int b2 = data[p++] & 0xFF;

                    int dg = (b1 & 0x3F) - 32;
                    int dr = ((b2 >> 4) & 0x0F) - 8 + dg;
                    int db = (b2 & 0x0F) - 8 + dg;

                    r = (r + dr) & 0xFF;
                    g = (g + dg) & 0xFF;
                    b = (b + db) & 0xFF;

                } else {
                    // RUN
                    int run = (b1 & 0x3F) + 1;

                    // emit current pixel once, then repeat it
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;

                    int hash = (r * 3 + g * 5 + b * 7 + a * 11) & 63;
                    index[hash] = argb;

                    pixels[i++] = argb;

                    for (int runc = 1; runc < run && i < pixelCount; runc++) {
                        pixels[i] = argb;
                        i++;
                    }

                    i--; // compensate outer loop increment
                    continue;
                }
            }

            // ALWAYS output ARGB
            int argb = (a << 24) | (r << 16) | (g << 8) | b;
            pixels[i] = argb;

            int hash = (r * 3 + g * 5 + b * 7 + a * 11) & 63;
            index[hash] = argb;
        }

        return new PixelGraphics(pixels, width, height);
    }
}