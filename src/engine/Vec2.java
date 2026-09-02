package engine;

public final class Vec2 {

    public float x, y;

    public Vec2() {
        this(0, 0);
    }

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(int x, int y) {
        this.x = (float) x;
        this.y = (float) y;
    }

    public Vec2(Vec2 v) {
        this.x = v.x;
        this.y = v.y;
    }

    // =========================
    // BASIC SETTERS
    // =========================

    public Vec2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vec2 set(Vec2 v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vec2 zero() {
        this.x = 0;
        this.y = 0;
        return this;
    }

    // =========================
    // ADD / SUB
    // =========================

    public Vec2 add(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Vec2 add(Vec2 v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vec2 sub(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vec2 sub(Vec2 v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    // =========================
    // SCALE / MULTIPLY
    // =========================

    public Vec2 scale(float s) {
        this.x *= s;
        this.y *= s;
        return this;
    }

    public Vec2 mul(float x, float y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public Vec2 mul(Vec2 v) {
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

    public float lengthSq() {
        return x * x + y * y;
    }

    public Vec2 normalize() {
        float len = length();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public Vec2 limit(float max) {
        float lenSq = lengthSq();
        if (lenSq > max * max) {
            float len = (float) Math.sqrt(lenSq);
            x = (x / len) * max;
            y = (y / len) * max;
        }
        return this;
    }

    // =========================
    // DOT / ANGLES
    // =========================

    public float dot(Vec2 v) {
        return x * v.x + y * v.y;
    }

    public float angle() {
        return (float) Math.atan2(y, x);
    }

    public float distanceTo(Vec2 v) {
        float dx = v.x - x;
        float dy = v.y - y;
        return Mathf.fastSqrt(dx * dx + dy * dy);
    }

    public float distanceToSq(Vec2 v) {
        float dx = v.x - x;
        float dy = v.y - y;
        return dx * dx + dy * dy;
    }

    // =========================
    // ROTATION
    // =========================

    public Vec2 rotate(float angle) {
        float cos = Mathf.cos(angle);
        float sin = Mathf.sin(angle);

        float nx = x * cos - y * sin;
        float ny = x * sin + y * cos;

        x = nx;
        y = ny;

        return this;
    }

    // =========================
    // VELOCITY & MOTION UTILITIES
    // =========================

    /**
     * Sets the magnitude of this velocity vector without changing its angle.
     */
    public Vec2 setLength(float len) {
        return normalize().scale(len);
    }

    /**
     * Integrates velocity into a position vector over delta time: position += velocity * dt
     */
    public Vec2 addScaled(Vec2 velocity, float dt) {
        this.x += velocity.x * dt;
        this.y += velocity.y * dt;
        return this;
    }

    /**
     * Applies simple linear drag/friction: velocity *= max(0, 1 - damping * dt)
     */
    public Vec2 applyDamping(float damping, float dt) {
        float factor = Math.max(0.0f, 1.0f - damping * dt);
        this.x *= factor;
        this.y *= factor;
        return this;
    }

    /**
     * Applies force acceleration over time (F = ma => a = F/m): velocity += (force / mass) * dt
     */
    public Vec2 applyForce(Vec2 force, float mass, float dt) {
        if (mass <= 0) return this;
        this.x += (force.x / mass) * dt;
        this.y += (force.y / mass) * dt;
        return this;
    }

    /**
     * Bounces/reflects this velocity vector against a surface normal.
     *
     * @param normal     Normalized surface vector
     * @param bounciness Coefficient of restitution (0.0 = no bounce, 1.0 = perfect bounce)
     */
    public Vec2 reflect(Vec2 normal, float bounciness) {
        float dot = this.dot(normal);
        this.x = this.x - (1.0f + bounciness) * dot * normal.x;
        this.y = this.y - (1.0f + bounciness) * dot * normal.y;
        return this;
    }

    /**
     * Linear interpolation towards a target velocity (useful for smooth acceleration/steering).
     */
    public Vec2 lerp(Vec2 target, float t) {
        this.x += (target.x - this.x) * t;
        this.y += (target.y - this.y) * t;
        return this;
    }

    /**
     * Creates a normalized direction unit vector pointing toward a target position.
     */
    public Vec2 directionTo(Vec2 target) {
        Vec2 dir = new Vec2(target.x - x, target.y - y);
        return dir.normalize();
    }

    /**
     * Creates a new velocity vector from an angle (radians) and speed.
     */
    public static Vec2 fromAngle(float angle, float speed) {
        return new Vec2(Mathf.cos(angle) * speed, Mathf.sin(angle) * speed);
    }

    // =========================
    // UTILITY
    // =========================

    public Vec2 copy() {
        return new Vec2(x, y);
    }

    public boolean isZero() {
        return x == 0 && y == 0;
    }

    public String toString() {
        return "Vec2(" + x + ", " + y + ")";
    }
}