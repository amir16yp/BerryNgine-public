package engine;

import java.util.Random;

public final class SoundSynth {

    public enum Waveform {
        SINE,
        SQUARE,
        SAW,
        TRIANGLE,
        NOISE
    }

    private static final int SAMPLE_RATE = 44100;

    private Waveform waveform = Waveform.SINE;
    private float startFrequency = 440.0f;
    private float endFrequency = -1.0f;
    private float attack = 0.0f;
    private float decay = 0.0f;
    private float sustain = 0.2f;
    private float sustainLevel = 1.0f;
    private float release = 0.05f;
    private float gain = 1.0f;
    private float duty = 0.5f;
    private float vibratoDepth = 0.0f;
    private float vibratoRate = 0.0f;
    private long noiseSeed = 0;
    private boolean hasNoiseSeed = false;
    private float tremoloDepth = 0.0f;
    private float tremoloRate = 0.0f;
    private float lowpassCutoff = 0.0f;
    private float highpassCutoff = 0.0f;
    private float distortion = 0.0f;

    public static SoundSynth create() {
        return new SoundSynth();
    }

    // ---------------- PRESETS ----------------

    /** Descending square zap, good for shots. */
    public static SoundSynth laser() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(1200.0f, 200.0f)
                .sustain(0.08f)
                .release(0.05f);
    }

    /** Rising triangle chirp, good for pickups. */
    public static SoundSynth coin() {
        return create()
                .waveform(Waveform.TRIANGLE)
                .sweep(900.0f, 1400.0f)
                .attack(0.005f)
                .sustain(0.05f)
                .release(0.15f);
    }

    /** Low rumbling noise burst. */
    public static SoundSynth explosion() {
        return create()
                .waveform(Waveform.NOISE)
                .sweep(600.0f, 60.0f)
                .sustain(0.1f)
                .release(0.4f)
                .gain(0.8f);
    }

    /** Quick upward square hop. */
    public static SoundSynth jump() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(300.0f, 700.0f)
                .sustain(0.06f)
                .release(0.08f)
                .duty(0.3f);
    }

    /** Short harsh noise hit, good for damage. */
    public static SoundSynth hit() {
        return create()
                .waveform(Waveform.NOISE)
                .sweep(900.0f, 200.0f)
                .sustain(0.03f)
                .release(0.1f);
    }

    /** Rising vibrato sweep, good for power-ups. */
    public static SoundSynth powerup() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(400.0f, 1200.0f)
                .vibrato(0.05f, 30.0f)
                .sustain(0.2f)
                .release(0.15f);
    }

    /** Tiny UI click/blip. */
    public static SoundSynth blip() {
        return create()
                .waveform(Waveform.SINE)
                .frequency(800.0f)
                .sustain(0.03f)
                .release(0.03f);
    }

    public SoundSynth waveform(Waveform waveform) {
        this.waveform = waveform;
        return this;
    }

    public SoundSynth frequency(float hz) {
        this.startFrequency = hz;
        return this;
    }

    /** Sets frequency from a MIDI note number (69 = A4 = 440Hz). */
    public SoundSynth midiNote(int note) {
        return frequency(midiToHz(note));
    }

    /** Sweeps between two MIDI note numbers. */
    public SoundSynth sweepNotes(int startNote, int endNote) {
        return sweep(midiToHz(startNote), midiToHz(endNote));
    }

    /** Sweeps from the current start frequency by the given number of semitones. */
    public SoundSynth slideSemitones(float semitones) {
        this.endFrequency = startFrequency * (float) Math.pow(2.0, semitones / 12.0);
        return this;
    }

    public static float midiToHz(int note) {
        return (float) (440.0 * Math.pow(2.0, (note - 69) / 12.0));
    }

    public SoundSynth sweep(float startHz, float endHz) {
        this.startFrequency = startHz;
        this.endFrequency = endHz;
        return this;
    }

    public SoundSynth attack(float seconds) {
        this.attack = Math.max(0.0f, seconds);
        return this;
    }

    public SoundSynth decay(float seconds) {
        this.decay = Math.max(0.0f, seconds);
        return this;
    }

    public SoundSynth sustain(float seconds) {
        this.sustain = Math.max(0.0f, seconds);
        return this;
    }

    public SoundSynth sustainLevel(float level) {
        this.sustainLevel = Mathf.clamp01(level);
        return this;
    }

    public SoundSynth release(float seconds) {
        this.release = Math.max(0.0f, seconds);
        return this;
    }

    /** Convenience: sets the full ADSR envelope in one call. */
    public SoundSynth envelope(float attack, float decay, float sustain, float release) {
        return attack(attack).decay(decay).sustain(sustain).release(release);
    }

    /** Convenience: no attack/decay, plays at full level for the given time then releases quickly. */
    public SoundSynth duration(float seconds) {
        this.attack = 0.0f;
        this.decay = 0.0f;
        this.sustain = Math.max(0.0f, seconds);
        return this;
    }

    public SoundSynth gain(float gain) {
        this.gain = Math.max(0.0f, gain);
        return this;
    }

    public SoundSynth duty(float duty) {
        this.duty = Mathf.clamp01(duty);
        return this;
    }

    public SoundSynth vibrato(float depth, float rateHz) {
        this.vibratoDepth = Math.max(0.0f, depth);
        this.vibratoRate = Math.max(0.0f, rateHz);
        return this;
    }

    public SoundSynth noiseSeed(long seed) {
        this.noiseSeed = seed;
        this.hasNoiseSeed = true;
        return this;
    }

    /** Amplitude modulation: depth 0..1, rate in Hz. */
    public SoundSynth tremolo(float depth, float rateHz) {
        this.tremoloDepth = Mathf.clamp01(depth);
        this.tremoloRate = Math.max(0.0f, rateHz);
        return this;
    }

    /** One-pole lowpass filter. 0 disables. */
    public SoundSynth lowpass(float cutoffHz) {
        this.lowpassCutoff = Math.max(0.0f, cutoffHz);
        return this;
    }

    /** One-pole highpass filter. 0 disables. */
    public SoundSynth highpass(float cutoffHz) {
        this.highpassCutoff = Math.max(0.0f, cutoffHz);
        return this;
    }

    /** Soft-clip distortion: 0 = clean, 1 = heavy. */
    public SoundSynth distortion(float amount) {
        this.distortion = Mathf.clamp01(amount);
        return this;
    }

    public Sound build() {
        float duration = attack + decay + sustain + release;
        int totalFrames = Math.max(1, (int) (duration * SAMPLE_RATE));

        float attackEnd = attack * SAMPLE_RATE;
        float decayEnd = attackEnd + decay * SAMPLE_RATE;
        float sustainEnd = decayEnd + sustain * SAMPLE_RATE;

        float freqStart = startFrequency;
        float freqEnd = endFrequency >= 0.0f ? endFrequency : startFrequency;

        Random random = hasNoiseSeed ? new Random(noiseSeed) : new Random();
        short[] pcm = new short[totalFrames];

        double phase = 0.0;
        float noiseValue = 0.0f;
        int noiseCounter = 0;

        float lpAlpha = lowpassCutoff > 0.0f ? filterAlpha(lowpassCutoff) : 0.0f;
        float hpAlpha = highpassCutoff > 0.0f ? filterAlpha(highpassCutoff) : 0.0f;
        float lpState = 0.0f;
        float hpState = 0.0f;
        float hpPrevInput = 0.0f;
        float drive = 1.0f + distortion * 15.0f;

        for (int i = 0; i < totalFrames; i++) {
            float t = (float) i / totalFrames;
            float freq = freqStart + (freqEnd - freqStart) * t;

            if (vibratoDepth > 0.0f && vibratoRate > 0.0f) {
                float time = (float) i / SAMPLE_RATE;
                freq *= 1.0f + vibratoDepth * (float) Math.sin(2.0 * Math.PI * vibratoRate * time);
            }
            if (freq < 0.0f) {
                freq = 0.0f;
            }

            float sample;
            switch (waveform) {
                case SQUARE:
                    sample = (phase % 1.0) < duty ? 1.0f : -1.0f;
                    break;
                case SAW:
                    sample = (float) (2.0 * (phase % 1.0) - 1.0);
                    break;
                case TRIANGLE: {
                    double p = phase % 1.0;
                    sample = (float) (p < 0.5 ? 4.0 * p - 1.0 : 3.0 - 4.0 * p);
                    break;
                }
                case NOISE: {
                    int period = freq > 0.0f ? Math.max(1, (int) (SAMPLE_RATE / (freq * 2.0f))) : 1;
                    if (noiseCounter <= 0) {
                        noiseValue = random.nextFloat() * 2.0f - 1.0f;
                        noiseCounter = period;
                    }
                    noiseCounter--;
                    sample = noiseValue;
                    break;
                }
                case SINE:
                default:
                    sample = (float) Math.sin(2.0 * Math.PI * phase);
                    break;
            }

            phase += freq / SAMPLE_RATE;

            if (distortion > 0.0f) {
                sample = (float) Math.tanh(sample * drive);
            }

            if (lpAlpha > 0.0f) {
                lpState += lpAlpha * (sample - lpState);
                sample = lpState;
            }

            if (hpAlpha > 0.0f) {
                hpState = (1.0f - hpAlpha) * (hpState + sample - hpPrevInput);
                hpPrevInput = sample;
                sample = hpState;
            }

            float env;
            if (i < attackEnd) {
                env = i / attackEnd;
            } else if (i < decayEnd) {
                float p = (i - attackEnd) / Math.max(1.0f, decayEnd - attackEnd);
                env = 1.0f + (sustainLevel - 1.0f) * p;
            } else if (i < sustainEnd) {
                env = sustainLevel;
            } else {
                float p = (i - sustainEnd) / Math.max(1.0f, totalFrames - sustainEnd);
                env = sustainLevel * (1.0f - p);
            }

            if (tremoloDepth > 0.0f && tremoloRate > 0.0f) {
                float time = (float) i / SAMPLE_RATE;
                env *= 1.0f - tremoloDepth * 0.5f * (1.0f + (float) Math.sin(2.0 * Math.PI * tremoloRate * time));
            }

            int value = (int) (sample * env * gain * 32767.0f);
            if (value > 32767) {
                value = 32767;
            } else if (value < -32768) {
                value = -32768;
            }
            pcm[i] = (short) value;
        }

        Sound file = new Sound();
        file.channels = 1;
        file.samplerate = SAMPLE_RATE;
        file.samples = totalFrames;
        file.pcm = pcm;
        return file;
    }

    private static float filterAlpha(float cutoffHz) {
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / SAMPLE_RATE;
        return dt / (rc + dt);
    }
}
