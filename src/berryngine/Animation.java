package berryngine;

import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

public final class Animation {

    private final TextureAtlas atlas;
    private final int[] frames;
    private final float frameDuration;
    private boolean registered = false;
    private static final List<WeakReference<Animation>> registeredAnimations = new ArrayList<>();
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

    /** Register for automatic updates. Keep a reference while the animation is in use. */
    public synchronized Animation register() {
        if (registered) {
            return this;
        }
        registered = true;
        synchronized (registeredAnimations) {
            registeredAnimations.removeIf(reference -> reference.get() == null);
            registeredAnimations.add(new WeakReference<>(this));
        }
        return this;
    }

    /** Stop automatic updates when this animation is no longer in use. */
    public synchronized void unregister() {
        registered = false;
        synchronized (registeredAnimations) {
            registeredAnimations.removeIf(reference -> {
                Animation animation = reference.get();
                return animation == null || animation == this;
            });
        }
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
        List<Animation> snapshot = new ArrayList<>();
        synchronized (registeredAnimations) {
            Iterator<WeakReference<Animation>> iterator = registeredAnimations.iterator();
            while (iterator.hasNext()) {
                Animation animation = iterator.next().get();
                if (animation == null) iterator.remove();
                else snapshot.add(animation);
            }
        }
        for (Animation animation : snapshot) {
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
