package engine;

import java.util.ArrayList;
import java.util.List;

public final class Animation {

    private final TextureAtlas atlas;
    private final int[] frames;
    private final float frameDuration;
    private boolean registered = false;
    private static final List<Animation> registeredAnimations = new ArrayList<>();
    private float elapsed;
    private int currentFrame;
    private boolean loop;
    private boolean finished;
    private Runnable onDone;

    public Animation(TextureAtlas atlas, int[] frames, float frameDuration) {
        this(atlas, frames, frameDuration, true);
    }

    public Animation(TextureAtlas atlas, int[] frames, float frameDuration, boolean loop) {
        if (frames == null || frames.length == 0) throw new IllegalArgumentException("frames must not be empty");
        this.atlas = atlas;
        this.frames = frames;
        this.frameDuration = frameDuration;
        this.loop = loop;
        this.elapsed = 0f;
        this.currentFrame = 0;
        this.finished = false;
    }

    public Animation register() {
        if (registered) {
            return this;
        }
        registered = true;
        registeredAnimations.add(this);
        return this;
    }

    public static Animation ofRange(TextureAtlas atlas, int startIndex, int endIndex, float frameDuration) {
        return ofRange(atlas, startIndex, endIndex, frameDuration, true);
    }

    public static Animation ofRange(TextureAtlas atlas, int startIndex, int endIndex, float frameDuration, boolean loop) {
        int count = endIndex - startIndex + 1;
        int[] frames = new int[count];
        for (int i = 0; i < count; i++) frames[i] = startIndex + i;
        return new Animation(atlas, frames, frameDuration, loop);
    }

    public void update(float dt) {
        if (finished) return;

        elapsed += dt;

        if (elapsed >= frameDuration) {
            elapsed -= frameDuration;
            currentFrame++;

            if (currentFrame >= frames.length) {
                if (loop) {
                    currentFrame = 0;
                    if (onDone != null) onDone.run();
                } else {
                    currentFrame = frames.length - 1;
                    finished = true;
                    if (onDone != null) onDone.run();
                }
            }
        }
    }

    public static void updateRegistered(float dt) {
        for (Animation animation : registeredAnimations) {
            animation.update(dt);
        }
    }

    public PixelGraphics getCurrentFrame() {
        return atlas.getTexture(frames[currentFrame]);
    }

    public int getCurrentFrameIndex() {
        return frames[currentFrame];
    }

    public boolean isFinished() {
        return finished;
    }

    public void reset() {
        elapsed = 0f;
        currentFrame = 0;
        finished = false;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
    }

    public float getProgress() {
        if (frames.length <= 1) return 1f;
        return Mathf.clamp01((currentFrame + elapsed / frameDuration) / frames.length);
    }

    public Animation onDone(Runnable callback) {
        this.onDone = callback;
        return this;
    }
}
