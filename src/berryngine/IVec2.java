package berryngine;

public final class IVec2 {

    public int x, y;

    public IVec2() {
        this(0, 0);
    }

    public IVec2(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public IVec2(float x, float y)
    {
        this.x = (int) x;
        this.y = (int) y;
    }

    public IVec2(IVec2 v) {
        this.x = v.x;
        this.y = v.y;
    }

    public IVec2(Vec2 v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
    }

    // =========================
    // BASIC SETTERS
    // =========================

    public IVec2 set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public IVec2 set(IVec2 v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public IVec2 zero() {
        this.x = 0;
        this.y = 0;
        return this;
    }

    // =========================
    // ADD / SUB
    // =========================

    public IVec2 add(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public IVec2 add(IVec2 v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public IVec2 sub(int x, int y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public IVec2 sub(IVec2 v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    // =========================
    // SCALE / MULTIPLY
    // =========================

    public IVec2 scale(int s) {
        this.x *= s;
        this.y *= s;
        return this;
    }

    public IVec2 mul(int x, int y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public IVec2 mul(IVec2 v) {
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    // =========================
    // LENGTH / DISTANCE
    // =========================

    public float length() {
        return Mathf.fastSqrt(x * x + y * y);
    }

    public int lengthSq() {
        return x * x + y * y;
    }

    public float distanceTo(IVec2 v) {
        int dx = v.x - x;
        int dy = v.y - y;
        return Mathf.fastSqrt(dx * dx + dy * dy);
    }

    public int distanceToSq(IVec2 v) {
        int dx = v.x - x;
        int dy = v.y - y;
        return dx * dx + dy * dy;
    }

    // =========================
    // UTILITY
    // =========================

    public IVec2 copy() {
        return new IVec2(x, y);
    }

    public Vec2 toVec2() {
        return new Vec2(x, y);
    }

    public boolean isZero() {
        return x == 0 && y == 0;
    }

    public String toString() {
        return "IVec2(" + x + ", " + y + ")";
    }
}
