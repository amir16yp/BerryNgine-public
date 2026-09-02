package engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SoundSystem implements AutoCloseable {

    private static final int OUTPUT_SAMPLE_RATE = 44100;
    private static final int CHANNELS = 2;
    private static final int BITS = 16;
    private static final int BYTES_PER_SAMPLE = 2;
    private static final int FRAME_SAMPLES = 512;
    private static final int BYTES_PER_FRAME = FRAME_SAMPLES * CHANNELS * BYTES_PER_SAMPLE;

    private final AudioFormat format;
    private final SourceDataLine line;
    private final Thread mixerThread;
    private final byte[] outputBuffer;
    private final int[] mixBuffer;

    private final Map<Sound, SoundClip> clipCache = new HashMap<>();
    private final Object lock = new Object();

    private Voice[] voices = new Voice[64];
    private int voiceCount = 0;
    private int nextHandle = 1;

    private volatile float masterVolume = 1.0f;
    private volatile boolean running = true;
    private final boolean available;

    public SoundSystem() {
        AudioFormat fmt = null;
        SourceDataLine l = null;
        boolean ok = false;
        try {
            fmt = new AudioFormat(OUTPUT_SAMPLE_RATE, BITS, CHANNELS, true, false);
            l = AudioSystem.getSourceDataLine(fmt);
            l.open(fmt, BYTES_PER_FRAME * 4);
            l.start();
            ok = true;
        } catch (LineUnavailableException | IllegalArgumentException e) {
            System.err.println("SoundSystem: audio line unavailable, sound disabled (" + e.getMessage() + ")");
        }

        this.format = fmt;
        this.line = l;
        this.available = ok;

        if (ok) {
            this.outputBuffer = new byte[BYTES_PER_FRAME];
            this.mixBuffer = new int[FRAME_SAMPLES * CHANNELS];
            this.mixerThread = new Thread(this::mixLoop, "SoundSystem-Mixer");
            this.mixerThread.setDaemon(true);
            this.mixerThread.setPriority(Thread.MAX_PRIORITY);
            this.mixerThread.start();
        } else {
            this.outputBuffer = null;
            this.mixBuffer = null;
            this.mixerThread = null;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public int play(Sound file) {
        return play(file, 1.0f, false);
    }

    public int play(Sound file, float volume) {
        return play(file, volume, false);
    }

    public int play(Sound file, float volume, boolean loop) {
        if (!available || file == null || volume <= 0.0f) {
            return -1;
        }
        if (file.samples <= 0 || file.samplerate <= 0) {
            return -1;
        }

        SoundClip clip = getOrCreateClip(file);
        if (clip.frames <= 0) {
            return -1;
        }

        synchronized (lock) {
            if (voiceCount == voices.length) {
                voices = Arrays.copyOf(voices, voices.length * 2);
            }
            Voice v = new Voice();
            v.handle = nextHandle++;
            v.clip = clip;
            v.position = 0;
            v.volume = volume;
            v.loop = loop;
            voices[voiceCount++] = v;
            return v.handle;
        }
    }

    public void setVolume(int handle, float volume) {
        if (!available || handle <= 0) {
            return;
        }
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) {
                    voices[i].volume = volume;
                    return;
                }
            }
        }
    }

    public void stop(int handle) {
        if (!available || handle <= 0) {
            return;
        }
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) {
                    removeVoice(i);
                    return;
                }
            }
        }
    }

    public void pause(int handle) {
        if (!available || handle <= 0) return;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) {
                    voices[i].paused = true;
                    return;
                }
            }
        }
    }

    public void resume(int handle) {
        if (!available || handle <= 0) return;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) {
                    voices[i].paused = false;
                    return;
                }
            }
        }
    }

    public boolean isPaused(int handle) {
        if (!available || handle <= 0) return false;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) return voices[i].paused;
            }
        }
        return false;
    }

    public void stopAll() {
        if (!available) {
            return;
        }
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                voices[i] = null;
            }
            voiceCount = 0;
        }
    }

    public void pauseAll() {
        if (!available) return;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                voices[i].paused = true;
            }
        }
    }

    public void resumeAll() {
        if (!available) return;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                voices[i].paused = false;
            }
        }
    }

    public boolean isPlaying(int handle) {
        if (!available || handle <= 0) return false;
        synchronized (lock) {
            for (int i = 0; i < voiceCount; i++) {
                if (voices[i].handle == handle) return true;
            }
        }
        return false;
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Mathf.clamp01(volume);
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    @Override
    public void close() {
        running = false;
        if (mixerThread != null) {
            try {
                mixerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        if (line != null) {
            line.stop();
            line.drain();
            line.close();
        }
    }

    private void mixLoop() {
        while (running) {
            int availableBytes = line.available();
            if (availableBytes < BYTES_PER_FRAME) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            mixFrame(FRAME_SAMPLES);
            line.write(outputBuffer, 0, BYTES_PER_FRAME);
        }
    }

    private void mixFrame(int samples) {
        Arrays.fill(mixBuffer, 0);

        synchronized (lock) {
            int i = 0;
            while (i < voiceCount) {
                Voice v = voices[i];
                SoundClip clip = v.clip;
                float vol = v.volume * masterVolume;
                int outPos = 0;
                int remaining = samples;
                boolean finished = false;

                if (v.paused) {
                    i++;
                    continue;
                }

                while (remaining > 0) {
                    int framesUntilEnd = clip.frames - v.position;
                    if (framesUntilEnd <= 0) {
                        if (v.loop) {
                            v.position = 0;
                            framesUntilEnd = clip.frames;
                        } else {
                            finished = true;
                            break;
                        }
                    }

                    int framesToMix = Math.min(remaining, framesUntilEnd);
                    for (int j = 0; j < framesToMix; j++) {
                        int idx = v.position * 2;
                        int l = (int) (clip.samples[idx] * vol);
                        int r = (int) (clip.samples[idx + 1] * vol);
                        mixBuffer[outPos * 2] += l;
                        mixBuffer[outPos * 2 + 1] += r;
                        outPos++;
                        v.position++;
                    }
                    remaining -= framesToMix;
                }

                if (finished) {
                    removeVoice(i);
                } else {
                    i++;
                }
            }
        }

        int ptr = 0;
        for (int i = 0; i < samples * CHANNELS; i++) {
            int v = mixBuffer[i];
            if (v > 32767) v = 32767;
            else if (v < -32768) v = -32768;
            outputBuffer[ptr++] = (byte) v;
            outputBuffer[ptr++] = (byte) (v >> 8);
        }
    }

    private void removeVoice(int index) {
        voices[index] = voices[--voiceCount];
        voices[voiceCount] = null;
    }

    private SoundClip getOrCreateClip(Sound file) {
        SoundClip clip = clipCache.get(file);
        if (clip == null) {
            clip = convert(file);
            clipCache.put(file, clip);
        }
        return clip;
    }

    private SoundClip convert(Sound file) {
        int inputFrames = file.samples;
        int inputChannels = file.channels;
        int inputRate = file.samplerate;

        int outputFrames = (int) ((long) inputFrames * OUTPUT_SAMPLE_RATE / inputRate);
        if (outputFrames <= 0) {
            outputFrames = 1;
        }
        short[] out = new short[outputFrames * 2];

        if (inputRate == OUTPUT_SAMPLE_RATE && inputChannels == 2) {
            System.arraycopy(file.pcm, 0, out, 0, inputFrames * 2);
            return new SoundClip(out, outputFrames);
        }

        if (inputRate == OUTPUT_SAMPLE_RATE) {
            for (int i = 0; i < inputFrames; i++) {
                int l;
                int r;
                if (inputChannels == 1) {
                    l = r = file.pcm[i];
                } else {
                    l = file.pcm[i * inputChannels];
                    r = file.pcm[i * inputChannels + 1];
                }
                out[i * 2] = (short) l;
                out[i * 2 + 1] = (short) r;
            }
            return new SoundClip(out, outputFrames);
        }

        float ratio = (float) inputRate / OUTPUT_SAMPLE_RATE;
        for (int i = 0; i < outputFrames; i++) {
            float srcPos = i * ratio;
            int p0 = (int) srcPos;
            int p1 = p0 + 1;
            if (p1 >= inputFrames) {
                p1 = inputFrames - 1;
            }
            float frac = srcPos - p0;

            if (inputChannels == 1) {
                int s0 = file.pcm[p0];
                int s1 = file.pcm[p1];
                int s = (int) (s0 + (s1 - s0) * frac);
                out[i * 2] = (short) s;
                out[i * 2 + 1] = (short) s;
            } else {
                int base0 = p0 * inputChannels;
                int base1 = p1 * inputChannels;
                int s0L = file.pcm[base0];
                int s1L = file.pcm[base1];
                int s0R = file.pcm[base0 + 1];
                int s1R = file.pcm[base1 + 1];
                int l = (int) (s0L + (s1L - s0L) * frac);
                int r = (int) (s0R + (s1R - s0R) * frac);
                out[i * 2] = (short) l;
                out[i * 2 + 1] = (short) r;
            }
        }

        return new SoundClip(out, outputFrames);
    }

    private static final class SoundClip {
        final short[] samples;
        final int frames;

        SoundClip(short[] samples, int frames) {
            this.samples = samples;
            this.frames = frames;
        }
    }

    private static final class Voice {
        int handle;
        SoundClip clip;
        int position;
        float volume;
        boolean loop;
        boolean paused;
    }
}
