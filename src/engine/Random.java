package engine;

import java.util.List;

public final class Random {

    private static final State DEFAULT = new State();

    private Random() {
    }

    public static final class State {
        private final java.util.Random rng = new java.util.Random();

        public State() {
        }

        public State(long seed) {
            rng.setSeed(seed);
        }

        public void setSeed(long seed) {
            rng.setSeed(seed);
        }

        public int nextInt() {
            return rng.nextInt();
        }

        public int nextInt(int bound) {
            return rng.nextInt(bound);
        }

        public int nextInt(int min, int max) {
            if (min >= max) return min;
            return min + rng.nextInt(max - min);
        }

        public float nextFloat() {
            return rng.nextFloat();
        }

        // =========================
        // ANGLES & ROTATION
        // =========================

        /**
         * Returns a random angle in radians between 0 and 2 PI.
         */
        public float nextAngle() {
            return nextFloat(0.0f, Mathf.TWO_PI);
        }

        /**
         * Returns a random angle in radians between min and max.
         */
        public float nextAngle(float min, float max) {
            return nextFloat(min, max);
        }

        /**
         * Returns a random angle in degrees between 0 and 360.
         */
        public float nextAngleDeg() {
            return nextFloat(0.0f, 360.0f);
        }

        /**
         * Returns a random angle in degrees between min and max.
         */
        public float nextAngleDeg(float min, float max) {
            return nextFloat(min, max);
        }

        public float nextFloat(float max) {
            return rng.nextFloat() * max;
        }

        public float nextFloat(float min, float max) {
            if (min >= max) return min;
            return min + rng.nextFloat() * (max - min);
        }

        public boolean nextBoolean() {
            return rng.nextBoolean();
        }

        public double nextDouble() {
            return rng.nextDouble();
        }

        public double nextDouble(double min, double max) {
            if (min >= max) return min;
            return min + rng.nextDouble() * (max - min);
        }

        public boolean chance(float probability) {
            return rng.nextFloat() < probability;
        }

        public int range(int min, int max) {
            return nextInt(min, max);
        }

        public float range(float min, float max) {
            return nextFloat(min, max);
        }

        public int sign() {
            return rng.nextBoolean() ? 1 : -1;
        }

        @SafeVarargs
        public final <T> T pick(T... array) {
            if (array == null || array.length == 0) return null;
            return array[rng.nextInt(array.length)];
        }

        public <T> T pick(List<T> list) {
            if (list == null || list.isEmpty()) return null;
            return list.get(rng.nextInt(list.size()));
        }

        public void shuffle(int[] array) {
            if (array == null) return;
            for (int i = array.length - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
            }
        }

        public <T> void shuffle(T[] array) {
            if (array == null) return;
            for (int i = array.length - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                T tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
            }
        }

        public <T> void shuffle(List<T> list) {
            if (list == null) return;
            for (int i = list.size() - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                T tmp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, tmp);
            }
        }

        public Vec2 insideUnitCircle() {
            float angle = nextFloat(0.0f, Mathf.TWO_PI);
            float radius = nextFloat();
            return new Vec2(Mathf.cos(angle) * radius, Mathf.sin(angle) * radius);
        }

        public Vec2 onUnitCircle() {
            float angle = nextFloat(0.0f, Mathf.TWO_PI);
            return new Vec2(Mathf.cos(angle), Mathf.sin(angle));
        }

        public Vec2 nextVec2(float minX, float minY, float maxX, float maxY) {
            return new Vec2(nextFloat(minX, maxX), nextFloat(minY, maxY));
        }

        public Vec2 nextVec2(Vec2 min, Vec2 max) {
            return new Vec2(nextFloat(min.x, max.x), nextFloat(min.y, max.y));
        }

        public Vec2 nextVec2InRect(float x, float y, float width, float height) {
            return nextVec2(x, y, x + width, y + height);
        }
    }

    public static State newState() {
        return new State();
    }

    public static State newState(long seed) {
        return new State(seed);
    }

    public static void setSeed(long seed) {
        DEFAULT.setSeed(seed);
    }

    public static int nextInt() {
        return DEFAULT.nextInt();
    }

    public static int nextInt(int bound) {
        return DEFAULT.nextInt(bound);
    }

    public static int nextInt(int min, int max) {
        return DEFAULT.nextInt(min, max);
    }

    public static float nextFloat() {
        return DEFAULT.nextFloat();
    }

    public static float nextFloat(float max) {
        return DEFAULT.nextFloat(max);
    }

    public static float nextFloat(float min, float max) {
        return DEFAULT.nextFloat(min, max);
    }

    public static boolean nextBoolean() {
        return DEFAULT.nextBoolean();
    }

    public static double nextDouble() {
        return DEFAULT.nextDouble();
    }

    public static double nextDouble(double min, double max) {
        return DEFAULT.nextDouble(min, max);
    }

    public static boolean chance(float probability) {
        return DEFAULT.chance(probability);
    }

    public static int range(int min, int max) {
        return DEFAULT.range(min, max);
    }

    public static float range(float min, float max) {
        return DEFAULT.range(min, max);
    }

    public static int sign() {
        return DEFAULT.sign();
    }

    public static float nextAngle() {
        return DEFAULT.nextAngle();
    }

    public static float nextAngle(float min, float max) {
        return DEFAULT.nextAngle(min, max);
    }

    public static float nextAngleDeg() {
        return DEFAULT.nextAngleDeg();
    }

    public static float nextAngleDeg(float min, float max) {
        return DEFAULT.nextAngleDeg(min, max);
    }

    @SafeVarargs
    public static <T> T pick(T... array) {
        return DEFAULT.pick(array);
    }

    public static <T> T pick(List<T> list) {
        return DEFAULT.pick(list);
    }

    public static void shuffle(int[] array) {
        DEFAULT.shuffle(array);
    }

    public static <T> void shuffle(T[] array) {
        DEFAULT.shuffle(array);
    }

    public static <T> void shuffle(List<T> list) {
        DEFAULT.shuffle(list);
    }

    public static Vec2 insideUnitCircle() {
        return DEFAULT.insideUnitCircle();
    }

    public static Vec2 onUnitCircle() {
        return DEFAULT.onUnitCircle();
    }

    public static Vec2 nextVec2(float minX, float minY, float maxX, float maxY) {
        return DEFAULT.nextVec2(minX, minY, maxX, maxY);
    }

    public static Vec2 nextVec2(Vec2 min, Vec2 max) {
        return DEFAULT.nextVec2(min, max);
    }

    public static Vec2 nextVec2InRect(float x, float y, float width, float height) {
        return DEFAULT.nextVec2InRect(x, y, width, height);
    }
}
