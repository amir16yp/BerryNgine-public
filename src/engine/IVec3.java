package engine;

public final class IVec3 {

    public int x, y, z;

    public IVec3() {
        this(0, 0, 0);
    }

    public IVec3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public IVec3(IVec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public IVec3(Vec3 v) {
        this.x = (int) v.x;
        this.y = (int) v.y;
        this.z = (int) v.z;
    }

    // =========================
    // BASIC SETTERS
    // =========================

    public IVec3 set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public IVec3 set(IVec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public IVec3 zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }

    // =========================
    // ADD / SUB
    // =========================

    public IVec3 add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public IVec3 add(IVec3 v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public IVec3 sub(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public IVec3 sub(IVec3 v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    // =========================
    // SCALE / MULTIPLY
    // =========================

    public IVec3 scale(int s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }

    public IVec3 mul(int x, int y, int z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public IVec3 mul(IVec3 v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    // =========================
    // LENGTH / DISTANCE
    // =========================

    public float length() {
        return Mathf.fastSqrt(x * x + y * y + z * z);
    }

    public int lengthSq() {
        return x * x + y * y + z * z;
    }

    public float distanceTo(IVec3 v) {
        int dx = v.x - x;
        int dy = v.y - y;
        int dz = v.z - z;
        return Mathf.fastSqrt(dx * dx + dy * dy + dz * dz);
    }

    public int distanceToSq(IVec3 v) {
        int dx = v.x - x;
        int dy = v.y - y;
        int dz = v.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    // =========================
    // UTILITY
    // =========================

    public IVec3 copy() {
        return new IVec3(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(x, y, z);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    public String toString() {
        return "IVec3(" + x + ", " + y + ", " + z + ")";
    }
}
