package berryngine;

public final class Vec3 {

    public float x, y, z;

    public Vec3() {
        this(0, 0, 0);
    }

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    // =========================
    // BASIC SETTERS
    // =========================

    public Vec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3 set(Vec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vec3 zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }

    // =========================
    // ADD / SUB
    // =========================

    public Vec3 add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vec3 add(Vec3 v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vec3 sub(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vec3 sub(Vec3 v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    // =========================
    // SCALE / MULTIPLY
    // =========================

    public Vec3 scale(float s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }

    public Vec3 mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vec3 mul(Vec3 v) {
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

    public float lengthSq() {
        return x * x + y * y + z * z;
    }

    public Vec3 normalize() {
        float len = length();
        if (len != 0) {
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    public Vec3 limit(float max) {
        float lenSq = lengthSq();
        if (lenSq > max * max) {
            float len = (float) Math.sqrt(lenSq);
            x = (x / len) * max;
            y = (y / len) * max;
            z = (z / len) * max;
        }
        return this;
    }

    // =========================
    // DOT / CROSS
    // =========================

    public float dot(Vec3 v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public Vec3 cross(Vec3 v) {
        float nx = y * v.z - z * v.y;
        float ny = z * v.x - x * v.z;
        float nz = x * v.y - y * v.x;
        this.x = nx;
        this.y = ny;
        this.z = nz;
        return this;
    }

    public float distanceTo(Vec3 v) {
        float dx = v.x - x;
        float dy = v.y - y;
        float dz = v.z - z;
        return Mathf.fastSqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distanceToSq(Vec3 v) {
        float dx = v.x - x;
        float dy = v.y - y;
        float dz = v.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    // =========================
    // ROTATION
    // =========================

    public Vec3 rotateX(float angle) {
        float cos = Mathf.cos(angle);
        float sin = Mathf.sin(angle);
        float ny = y * cos - z * sin;
        float nz = y * sin + z * cos;
        y = ny;
        z = nz;
        return this;
    }

    public Vec3 rotateY(float angle) {
        float cos = Mathf.cos(angle);
        float sin = Mathf.sin(angle);
        float nx = x * cos + z * sin;
        float nz = -x * sin + z * cos;
        x = nx;
        z = nz;
        return this;
    }

    public Vec3 rotateZ(float angle) {
        float cos = Mathf.cos(angle);
        float sin = Mathf.sin(angle);
        float nx = x * cos - y * sin;
        float ny = x * sin + y * cos;
        x = nx;
        y = ny;
        return this;
    }

    // =========================
    // UTILITY
    // =========================

    public IVec3 toIVec3()
    {
        return new IVec3(x, y, z);
    }

    public Vec3 copy() {
        return new Vec3(x, y, z);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    public String toString() {
        return "Vec3(" + x + ", " + y + ", " + z + ")";
    }
}
