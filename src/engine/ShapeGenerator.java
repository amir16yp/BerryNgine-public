package engine;

public final class ShapeGenerator {

    private ShapeGenerator() {}

    public static PixelGraphics circle(int radius, int color) {
        int size = radius * 2 + 1;
        PixelGraphics g = new PixelGraphics(size, size);
        int r2 = radius * radius;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - radius;
                int dy = y - radius;
                if (dx * dx + dy * dy <= r2) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics ring(int radius, int thickness, int color) {
        int size = radius * 2 + 1;
        PixelGraphics g = new PixelGraphics(size, size);
        int r2 = radius * radius;
        int inner = Math.max(0, radius - thickness);
        int inner2 = inner * inner;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - radius;
                int dy = y - radius;
                int d2 = dx * dx + dy * dy;
                if (d2 <= r2 && d2 >= inner2) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics circle(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int cx = width / 2;
        int cy = height / 2;
        int radius = Math.min(width, height) / 2;
        int r2 = radius * radius;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= r2) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics filledCircle(int radius, int color) {
        return circle(radius, color);
    }

    public static PixelGraphics filledCircle(int width, int height, int color) {
        return circle(width, height, color);
    }

    public static PixelGraphics outlineCircle(int radius, int thickness, int color) {
        return ring(radius, thickness, color);
    }

    public static PixelGraphics outlineCircle(int width, int height, int thickness, int color) {
        return ring(width, height, thickness, color);
    }

    public static PixelGraphics ring(int width, int height, int thickness, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int cx = width / 2;
        int cy = height / 2;
        int radius = Math.min(width, height) / 2;
        int r2 = radius * radius;
        int inner = Math.max(0, radius - thickness);
        int inner2 = inner * inner;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int dx = x - cx;
                int dy = y - cy;
                int d2 = dx * dx + dy * dy;
                if (d2 <= r2 && d2 >= inner2) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics rectangle(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        g.clear(color);
        return g;
    }

    public static PixelGraphics filledRectangle(int width, int height, int color) {
        return rectangle(width, height, color);
    }

    public static PixelGraphics outlineRectangle(int width, int height, int thickness, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        g.fillRect(0, 0, width, thickness, color);
        g.fillRect(0, height - thickness, width, thickness, color);
        g.fillRect(0, thickness, thickness, height - thickness * 2, color);
        g.fillRect(width - thickness, thickness, thickness, height - thickness * 2, color);
        return g;
    }

    public static PixelGraphics roundedRectangle(int width, int height, int radius, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        fillRoundRect(g, 0, 0, width, height, radius, color);
        return g;
    }

    public static PixelGraphics outlineRoundedRectangle(int width, int height, int radius, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        drawRoundRect(g, 0, 0, width, height, radius, color);
        return g;
    }

    public static PixelGraphics filledRoundedRectangle(int width, int height, int radius, int color) {
        return roundedRectangle(width, height, radius, color);
    }

    public static PixelGraphics triangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;

        PixelGraphics g = new PixelGraphics(w, h);
        fillTriangle(g, x1 - minX, y1 - minY, x2 - minX, y2 - minY, x3 - minX, y3 - minY, color);
        return g;
    }

    public static PixelGraphics outlineTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;

        PixelGraphics g = new PixelGraphics(w, h);
        g.drawLine(x1 - minX, y1 - minY, x2 - minX, y2 - minY, color);
        g.drawLine(x2 - minX, y2 - minY, x3 - minX, y3 - minY, color);
        g.drawLine(x3 - minX, y3 - minY, x1 - minX, y1 - minY, color);
        return g;
    }

    public static PixelGraphics outlineTriangle(IVec2 p1, IVec2 p2, IVec2 p3, int color) {
        return outlineTriangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, color);
    }

    public static PixelGraphics triangle(IVec2 p1, IVec2 p2, IVec2 p3, int color) {
        return triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, color);
    }

    public static PixelGraphics rightTriangle(int width, int height, int color) {
        return triangle(0, 0, width - 1, height - 1, 0, height - 1, color);
    }

    public static PixelGraphics filledTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        return triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public static PixelGraphics filledTriangle(IVec2 p1, IVec2 p2, IVec2 p3, int color) {
        return triangle(p1, p2, p3, color);
    }

    public static PixelGraphics filledRightTriangle(int width, int height, int color) {
        return rightTriangle(width, height, color);
    }

    public static PixelGraphics line(int x1, int y1, int x2, int y2, int color, int thickness) {
        int half = thickness / 2;
        int minX = Math.min(x1, x2) - half;
        int minY = Math.min(y1, y2) - half;
        int maxX = Math.max(x1, x2) + half;
        int maxY = Math.max(y1, y2) + half;
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;

        PixelGraphics g = new PixelGraphics(w, h);
        int ox = x1 - minX;
        int oy = y1 - minY;
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) steps = 1;

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = (int) (ox + dx * t);
            int py = (int) (oy + dy * t);
            g.fillRect(px - half, py - half, thickness, thickness, color);
        }
        return g;
    }

    public static PixelGraphics line(int width, int height, int color, int thickness) {
        return line(0, 0, width - 1, height - 1, color, thickness);
    }

    public static PixelGraphics cross(int width, int height, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int half = thickness / 2;
        int cx = width / 2;
        int cy = height / 2;
        g.fillRect(cx - half, 0, thickness, height, color);
        g.fillRect(0, cy - half, width, thickness, color);
        return g;
    }

    public static PixelGraphics plus(int width, int height, int color, int thickness) {
        return cross(width, height, color, thickness);
    }

    public static PixelGraphics diamond(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int[] x = { width / 2, width - 1, width / 2, 0 };
        int[] y = { 0, height / 2, height - 1, height / 2 };
        fillPolygon(g, x, y, 4, color);
        return g;
    }

    public static PixelGraphics ellipse(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float rx = width / 2.0f;
        float ry = height / 2.0f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float dx = (x - cx) / rx;
                float dy = (y - cy) / ry;
                if (dx * dx + dy * dy <= 1.0f) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics polygon(int width, int height, int sides, int color) {
        if (sides < 3) sides = 3;
        PixelGraphics g = new PixelGraphics(width, height);
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float radius = Math.min(width, height) / 2.0f;
        int[] xPoints = new int[sides];
        int[] yPoints = new int[sides];
        for (int i = 0; i < sides; i++) {
            float angle = i * Mathf.TWO_PI / sides - Mathf.PI / 2.0f;
            xPoints[i] = (int) (cx + Mathf.cos(angle) * radius);
            yPoints[i] = (int) (cy + Mathf.sin(angle) * radius);
        }
        fillPolygon(g, xPoints, yPoints, sides, color);
        return g;
    }

    public static PixelGraphics star(int width, int height, int points, int color) {
        if (points < 2) points = 2;
        PixelGraphics g = new PixelGraphics(width, height);
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float outer = Math.min(width, height) / 2.0f;
        float inner = outer * 0.4f;
        int count = points * 2;
        int[] xPoints = new int[count];
        int[] yPoints = new int[count];
        for (int i = 0; i < count; i++) {
            float angle = i * Mathf.PI / points - Mathf.PI / 2.0f;
            float r = (i % 2 == 0) ? outer : inner;
            xPoints[i] = (int) (cx + Mathf.cos(angle) * r);
            yPoints[i] = (int) (cy + Mathf.sin(angle) * r);
        }
        fillPolygon(g, xPoints, yPoints, count, color);
        return g;
    }

    public static PixelGraphics checkerboard(int width, int height, int size, int color1, int color2) {
        PixelGraphics g = new PixelGraphics(width, height);
        for (int y = 0; y < height; y += size) {
            for (int x = 0; x < width; x += size) {
                boolean even = ((x / size) + (y / size)) % 2 == 0;
                g.fillRect(x, y, Math.min(size, width - x), Math.min(size, height - y), even ? color1 : color2);
            }
        }
        return g;
    }

    public static PixelGraphics arrow(int width, int height, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int half = thickness / 2;
        int cy = height / 2;
        int headW = width / 3;
        int headH = height;
        int shaftX = width - headW;
        int shaftY = cy - half;
        g.fillRect(0, shaftY, shaftX, thickness, color);
        int[] x = { shaftX, width - 1, shaftX };
        int[] y = { 0, cy, height - 1 };
        fillPolygon(g, x, y, 3, color);
        return g;
    }

    public static PixelGraphics chevron(int width, int height, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int[] x = { 0, width / 2, width, width - thickness, width / 2, thickness };
        int[] y = { 0, height - thickness, 0, 0, thickness, 0 };
        fillPolygon(g, x, y, 6, color);
        return g;
    }

    public static PixelGraphics heart(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        float cx = width / 2.0f;
        float cy = height * 0.65f;
        float scale = Math.min(width, height) / 16.0f;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float dx = (x - cx) / scale;
                float dy = (cy - y) / scale;
                float a = dx * dx + dy * dy - 1.0f;
                if (a * a * a - dx * dx * dy * dy * dy <= 0.0f) {
                    g.setPixel(x, y, color);
                }
            }
        }
        return g;
    }

    public static PixelGraphics bolt(int width, int height, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int[] x = { width / 3, width * 2 / 3, width / 2, width * 2 / 3, width / 3, width / 2 };
        int[] y = { 0, 0, height / 2, height / 2, height, height / 2 };
        fillPolygon(g, x, y, 6, color);
        return g;
    }

    public static PixelGraphics capsule(int width, int height, int color) {
        int radius = Math.min(width, height) / 2;
        return roundedRectangle(width, height, radius, color);
    }

    public static PixelGraphics pie(int width, int height, float startAngle, float sweepAngle, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int radius = Math.min(width, height) / 2;
        fillArc(g, width / 2, height / 2, radius, startAngle, sweepAngle, color);
        return g;
    }

    public static PixelGraphics arc(int width, int height, float startAngle, float sweepAngle, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int radius = Math.min(width, height) / 2 - thickness / 2;
        int cx = width / 2;
        int cy = height / 2;
        for (int t = 0; t < thickness; t++) {
            drawArcRaster(g, cx, cy, radius - t, startAngle, sweepAngle, color);
        }
        return g;
    }

    public static PixelGraphics wave(int width, int height, float amplitude, float frequency, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int cy = height / 2;
        int half = thickness / 2;
        for (int x = 0; x < width; x++) {
            float angle = x * frequency * Mathf.TWO_PI / width;
            int y = (int) (cy + Mathf.sin(angle) * amplitude);
            g.fillRect(x, y - half, 1, thickness, color);
        }
        return g;
    }

    public static PixelGraphics zigzag(int width, int height, int peaks, int color, int thickness) {
        PixelGraphics g = new PixelGraphics(width, height);
        int step = width / Math.max(1, peaks);
        for (int i = 0; i < peaks; i++) {
            int x1 = i * step;
            int x2 = Math.min((i + 1) * step, width - 1);
            int y1 = (i % 2 == 0) ? 0 : height - 1;
            int y2 = (i % 2 == 0) ? height - 1 : 0;
            g.drawImage(line(x1, y1, x2, y2, color, thickness), x1, 0);
        }
        return g;
    }

    public static PixelGraphics target(int width, int height, int rings, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        int cx = width / 2;
        int cy = height / 2;
        int maxRadius = Math.min(width, height) / 2;
        for (int i = 0; i < rings; i++) {
            int radius = maxRadius * (i + 1) / rings;
            drawArcRaster(g, cx, cy, radius, 0, 360, color);
        }
        g.drawHorizontalLine(0, cy, width, color);
        g.drawVLine(cx, 0, height, color);
        return g;
    }

    public static PixelGraphics grid(int width, int height, int cellSize, int color) {
        PixelGraphics g = new PixelGraphics(width, height);
        for (int y = 0; y <= height; y += cellSize) {
            g.drawHorizontalLine(0, y, width, color);
        }
        for (int x = 0; x <= width; x += cellSize) {
            g.drawVLine(x, 0, height, color);
        }
        return g;
    }

    public static PixelGraphics radialGradient(int width, int height, int center, int edge) {
        PixelGraphics g = new PixelGraphics(width, height);
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float maxDist = Mathf.fastSqrt(cx * cx + cy * cy);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float t = Mathf.fastSqrt(dx * dx + dy * dy) / maxDist;
                g.setPixel(x, y, Color.lerp(center, edge, t));
            }
        }
        return g;
    }

    public static PixelGraphics verticalGradient(int width, int height, int top, int bottom) {
        PixelGraphics g = new PixelGraphics(width, height);
        for (int y = 0; y < height; y++) {
            float t = y / (float) Math.max(1, height - 1);
            int color = Color.lerp(top, bottom, t);
            g.drawHorizontalLine(0, y, width, color);
        }
        return g;
    }

    public static PixelGraphics horizontalGradient(int width, int height, int left, int right) {
        PixelGraphics g = new PixelGraphics(width, height);
        for (int x = 0; x < width; x++) {
            float t = x / (float) Math.max(1, width - 1);
            int color = Color.lerp(left, right, t);
            g.drawVLine(x, 0, height, color);
        }
        return g;
    }

    public static PixelGraphics polygon(int[] xPoints, int[] yPoints, int color) {
        if (xPoints == null || yPoints == null || xPoints.length < 3) {
            return new PixelGraphics(1, 1);
        }
        int n = Math.min(xPoints.length, yPoints.length);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            minX = Math.min(minX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }
        PixelGraphics g = new PixelGraphics(maxX - minX + 1, maxY - minY + 1);
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = xPoints[i] - minX;
            ys[i] = yPoints[i] - minY;
        }
        fillPolygon(g, xs, ys, n, color);
        return g;
    }

    public static PixelGraphics polygon(IVec2[] points, int color) {
        if (points == null || points.length < 3) return new PixelGraphics(1, 1);
        int n = points.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = points[i].x;
            ys[i] = points[i].y;
        }
        return polygon(xs, ys, color);
    }

    public static PixelGraphics outlinePolygon(int[] xPoints, int[] yPoints, int color) {
        if (xPoints == null || yPoints == null || xPoints.length < 2) {
            return new PixelGraphics(1, 1);
        }
        int n = Math.min(xPoints.length, yPoints.length);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            minX = Math.min(minX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }
        PixelGraphics g = new PixelGraphics(maxX - minX + 1, maxY - minY + 1);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            g.drawLine(xPoints[i] - minX, yPoints[i] - minY, xPoints[j] - minX, yPoints[j] - minY, color);
        }
        return g;
    }

    public static PixelGraphics outlinePolygon(IVec2[] points, int color) {
        if (points == null || points.length < 2) return new PixelGraphics(1, 1);
        int n = points.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = points[i].x;
            ys[i] = points[i].y;
        }
        return outlinePolygon(xs, ys, color);
    }

    public static PixelGraphics oval(int radius, int color) {
        return circle(radius, color);
    }

    public static PixelGraphics outlineOval(int radius, int thickness, int color) {
        return ring(radius, thickness, color);
    }

    // --- Rasterization helpers (moved from PixelGraphics) ---

    private static void fillPolygon(PixelGraphics g, int[] xPoints, int[] yPoints, int nPoints, int color) {
        if (nPoints < 3) return;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < nPoints; i++) {
            if (yPoints[i] < minY) minY = yPoints[i];
            if (yPoints[i] > maxY) maxY = yPoints[i];
        }

        minY = Math.max(minY, 0);
        maxY = Math.min(maxY, g.height - 1);

        int[] nodeX = new int[nPoints];

        for (int y = minY; y <= maxY; y++) {
            int nodes = 0;
            int j = nPoints - 1;

            for (int i = 0; i < nPoints; i++) {
                int yi = yPoints[i];
                int yj = yPoints[j];

                if ((yi < y && yj >= y) || (yj < y && yi >= y)) {
                    int xi = xPoints[i];
                    int xj = xPoints[j];

                    nodeX[nodes++] = xi + (y - yi) * (xj - xi) / (yj - yi);
                }
                j = i;
            }

            for (int i = 0; i < nodes - 1; i++) {
                for (int k = i + 1; k < nodes; k++) {
                    if (nodeX[i] > nodeX[k]) {
                        int tmp = nodeX[i];
                        nodeX[i] = nodeX[k];
                        nodeX[k] = tmp;
                    }
                }
            }

            for (int i = 0; i < nodes; i += 2) {
                int xStart = Math.max(nodeX[i], 0);
                int xEnd = Math.min(nodeX[i + 1], g.width - 1);

                int offset = y * g.width + xStart;
                for (int x = xStart; x <= xEnd; x++) {
                    g.pixels[offset++] = color;
                }
            }
        }
    }

    private static void fillTriangle(PixelGraphics g, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        if (y1 > y2) { int tx = x1; x1 = x2; x2 = tx; int ty = y1; y1 = y2; y2 = ty; }
        if (y1 > y3) { int tx = x1; x1 = x3; x3 = tx; int ty = y1; y1 = y3; y3 = ty; }
        if (y2 > y3) { int tx = x2; x2 = x3; x3 = tx; int ty = y2; y2 = y3; y3 = ty; }

        if (y1 == y3) return;

        for (int y = y1; y <= y2; y++) {
            int xA = triangleEdgeX(x1, y1, x2, y2, y);
            int xB = triangleEdgeX(x1, y1, x3, y3, y);
            triangleFillSpan(g, y, xA, xB, color);
        }
        for (int y = y2 + 1; y <= y3; y++) {
            int xA = triangleEdgeX(x2, y2, x3, y3, y);
            int xB = triangleEdgeX(x1, y1, x3, y3, y);
            triangleFillSpan(g, y, xA, xB, color);
        }
    }

    private static int triangleEdgeX(int x1, int y1, int x2, int y2, int y) {
        if (y1 == y2) return x1;
        return x1 + (x2 - x1) * (y - y1) / (y2 - y1);
    }

    private static void triangleFillSpan(PixelGraphics g, int y, int x1, int x2, int color) {
        if (y < 0 || y >= g.height) return;
        int start = Math.max(0, Math.min(x1, x2));
        int end = Math.min(g.width - 1, Math.max(x1, x2));
        if (start > end) return;
        int offset = y * g.width + start;
        for (int x = start; x <= end; x++) {
            g.pixels[offset++] = color;
        }
    }

    private static void drawArcRaster(PixelGraphics g, float xc, float yc, float radius, float startAngle, float sweepAngle, int color) {
        if (radius <= 0) return;

        float circumference = 2.0f * Mathf.PI * radius;
        float stepCount = circumference * (Math.abs(sweepAngle) / 360.0f);
        float angleStep = sweepAngle / stepCount;

        for (float i = 0; i <= stepCount; i++) {
            float currentAngle = startAngle + (i * angleStep);
            float rad = Mathf.toRadians(currentAngle);

            int px = (int) (xc + Mathf.cos(rad) * radius);
            int py = (int) (yc + Mathf.sin(rad) * radius);

            g.blendPixel(px, py, color);
        }
    }

    private static void fillArc(PixelGraphics g, int xc, int yc, int radius, float startAngle, float sweepAngle, int color) {
        float r2 = radius * radius;

        float start = (startAngle % 360 + 360) % 360;
        float end = (start + sweepAngle);

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float distSq = (float) (x * x + y * y);

                if (distSq <= r2) {
                    float angle = (float) Math.toDegrees(Math.atan2(y, x));
                    float normalizedAngle = (angle % 360 + 360) % 360;

                    if (isAngleInSweep(normalizedAngle, start, end)) {
                        g.blendPixel(xc + x, yc + y, color);
                    }
                }
            }
        }
    }

    private static boolean isAngleInSweep(float angle, float start, float end) {
        if (end > start) {
            return angle >= start && angle <= end;
        } else {
            return angle >= start || angle <= (end % 360 + 360) % 360;
        }
    }

    private static void fillRoundRect(PixelGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;

        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));

        int r2 = radius * radius;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                boolean inside =
                        (i >= radius && i < w - radius) ||
                                (j >= radius && j < h - radius);

                if (inside) {
                    g.setPixel(x + i, y + j, color);
                } else {
                    int cx = (i < radius) ? radius : (w - radius - 1);
                    int cy = (j < radius) ? radius : (h - radius - 1);

                    int dx = i - cx;
                    int dy = j - cy;

                    if (dx * dx + dy * dy <= r2) {
                        g.setPixel(x + i, y + j, color);
                    }
                }
            }
        }
    }

    private static void drawRoundRect(PixelGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;

        radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        int r2 = radius * radius;

        int x2 = x + w - 1;
        int y2 = y + h - 1;

        g.drawHorizontalLine(x + radius, y, w - 2 * radius, color);
        g.drawHorizontalLine(x + radius, y2, w - 2 * radius, color);

        g.drawVLine(x, y + radius, y + h - radius - 1, color);
        g.drawVLine(x2, y + radius, y + h - radius - 1, color);

        for (int j = -radius; j <= radius; j++) {
            for (int i = -radius; i <= radius; i++) {

                if (i * i + j * j <= r2 && i * i + j * j >= r2 - 2 * radius) {
                    g.setPixel(x + radius + i, y + radius + j, color);
                    g.setPixel(x2 - radius + i, y + radius + j, color);
                    g.setPixel(x + radius + i, y2 - radius + j, color);
                    g.setPixel(x2 - radius + i, y2 - radius + j, color);
                }
            }
        }
    }
}
