package berryngine;

public final class Timer {

    private float duration;
    private float elapsed;
    private boolean finished;
    private boolean loop;
    private Runnable onDone;

    public Timer(float duration) {
        this(duration, false);
    }

    public Timer(float duration, boolean loop) {
        this.duration = duration;
        this.loop = loop;
        this.elapsed = 0f;
        this.finished = false;
    }

    public void update(float dt) {
        if (finished && !loop) return;

        elapsed += dt;

        if (elapsed >= duration) {
            if (loop) {
                elapsed -= duration;
                if (onDone != null) onDone.run();
            } else {
                elapsed = duration;
                finished = true;
                if (onDone != null) onDone.run();
            }
        }
    }

    public boolean isReady() {
        return finished || (loop && elapsed >= duration);
    }

    public boolean isDone() {
        return finished;
    }

    public void reset() {
        elapsed = 0f;
        finished = false;
    }

    public void reset(float newDuration) {
        this.duration = newDuration;
        reset();
    }

    public float getElapsed() {
        return elapsed;
    }

    public float getRemaining() {
        return Math.max(0f, duration - elapsed);
    }

    public float getProgress() {
        return duration <= 0f ? 1f : Mathf.clamp01(elapsed / duration);
    }

    public float getDuration() {
        return duration;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
    }

    public Timer onDone(Runnable callback) {
        this.onDone = callback;
        return this;
    }
}
