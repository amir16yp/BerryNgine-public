package engine;

/**
 * Mathf - Fast math utility class with precached trigonometric values
 * Improves performance by storing precomputed values of sin, cos, and tan
 * for commonly used angles.
 */
public final class Mathf {

    // Size of the lookup tables (higher values give more precision)
    private static final int TABLE_SIZE = 4096;
    public static float PI = (float) Math.PI;
    public static float TWO_PI = PI * 2.0f;
    private static final float PI_180 = 0.017453292519943295f;
    private static final float PI_180_INV = 57.29577951308232f;
    // Maximum angle in radians (2π)
    private static final float MAX_ANGLE = PI * 2.0f;

    // Lookup tables for trigonometric functions
    private static final float[] SIN_TABLE = new float[TABLE_SIZE];
    private static final float[] COS_TABLE = new float[TABLE_SIZE];
    private static final float[] TAN_TABLE = new float[TABLE_SIZE];


    // Static initializer to populate the tables
    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            float angle = i * MAX_ANGLE / TABLE_SIZE;
            SIN_TABLE[i] = (float) Math.sin(angle);
            COS_TABLE[i] = (float) Math.cos(angle);
            TAN_TABLE[i] = (float) Math.tan(angle);
        }
        System.out.println("Mathf: Trigonometric tables initialized with " + TABLE_SIZE + " entries");
    }

    private Mathf() {
    }

    // Conversion factor from radians to table index
    private static final float TABLE_FACTOR = TABLE_SIZE / MAX_ANGLE;

    public static float normalizeAngle(float angle) {
        // Use modulo to bring the angle within a 2π range
        angle = (angle % (2f * PI));

        // Adjust to the range [-π, π]
        if (angle > PI) {
            angle -= 2 * PI;
        } else if (angle < -PI) {
            angle += 2 * PI;
        }

        return angle;
    }


    /**
     * Fast sine calculation using table lookup
     *
     * @param angle Angle in radians
     * @return Approximate sine value
     */
    public static float sin(float angle) {
        // Normalize angle to [0, 2π)
        angle = angle % MAX_ANGLE;
        if (angle < 0) angle += MAX_ANGLE;

        // Convert to table index
        int index = (int) (angle * TABLE_FACTOR) & (TABLE_SIZE - 1);
        return SIN_TABLE[index];
    }


    /**
     * Fast cosine calculation using table lookup
     *
     * @param angle Angle in radians
     * @return Approximate cosine value
     */
    public static float cos(float angle) {
        // Normalize angle to [0, 2π)
        angle = angle % MAX_ANGLE;
        if (angle < 0) angle += MAX_ANGLE;

        // Convert to table index
        int index = (int) (angle * TABLE_FACTOR) & (TABLE_SIZE - 1);
        return COS_TABLE[index];
    }


    /**
     * Fast tangent calculation using table lookup
     *
     * @param angle Angle in radians
     * @return Approximate tangent value
     */
    public static float tan(float angle) {
        // Normalize angle to [0, 2π)
        angle = angle % MAX_ANGLE;
        if (angle < 0) angle += MAX_ANGLE;

        // Convert to table index
        int index = (int) (angle * TABLE_FACTOR) & (TABLE_SIZE - 1);
        return TAN_TABLE[index];
    }

    /**
     * Fast inverse tangent approximation
     *
     * @param y Y coordinate
     * @param x X coordinate
     * @return Approximate arctangent in radians
     */
    public static float atan2(float y, float x) {
        if (x == 0.0) return (y > 0f) ? (float) Math.PI / 2f : (y < 0f ? (float) -Math.PI / 2f : 0f);

        float absZ = Math.abs(y / x);
        float angle;

        if (absZ < 1.0) {
            angle = absZ / (1.0f + 0.28f * absZ * absZ);
        } else {
            angle = (float) Math.PI / 2f - absZ / (absZ * absZ + 0.28f);
        }

        if (x < 0) {
            angle = (float) Math.PI - angle;
        }
        return (y < 0f) ? -angle : angle;
    }

    /**
     * Converts degrees to radians
     *
     * @param degrees Angle in degrees
     * @return Angle in radians
     */
    public static float toRadians(float degrees) {
        // Using multiplication is faster than division
        // PI/180 = 0.017453292519943295
        return degrees * PI_180;
    }

    /**
     * Converts radians to degrees
     *
     * @param radians Angle in radians
     * @return Angle in degrees
     */
    public static float toDegrees(float radians) {
        // Using multiplication is faster than division
        // 180/PI = 57.29577951308232
        return radians * PI_180_INV;
    }

    /**
     * Fast approximation of square root for distance calculations
     *
     * @param x Value to calculate square root of
     * @return Approximated square root
     */
    public static float fastInvSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);   // magic number
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - xhalf * x * x); // 1 Newton-Raphson iteration
        return x;
    }

    public static float fastSqrt(float x) {
        return x * fastInvSqrt(x);
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return fastSqrt(dx * dx + dy * dy);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static float clamp01(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    public static float lerpUnclamped(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float smoothstep(float a, float b, float t) {
        t = clamp01((t - a) / (b - a));
        return t * t * (3f - 2f * t);
    }

    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return outMin + (value - inMin) / (inMax - inMin) * (outMax - outMin);
    }
}