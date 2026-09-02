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
    private float endFrequency = Float.NaN;

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

    private SoundSynth() {
    }

    public static SoundSynth create() {
        return new SoundSynth();
    }

    // ============================================================
    // PRESETS
    // ============================================================

    /**
     * Descending square zap, good for shots.
     */
    public static SoundSynth laser() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(1200.0f, 200.0f)
                .sustain(0.08f)
                .release(0.05f);
    }

    /**
     * Rising triangle chirp, good for pickups.
     */
    public static SoundSynth coin() {
        return create()
                .waveform(Waveform.TRIANGLE)
                .sweep(900.0f, 1400.0f)
                .attack(0.005f)
                .sustain(0.05f)
                .release(0.15f);
    }

    /**
     * Low rumbling noise burst.
     */
    public static SoundSynth explosion() {
        return create()
                .waveform(Waveform.NOISE)
                .sweep(600.0f, 60.0f)
                .sustain(0.1f)
                .release(0.4f)
                .gain(0.8f);
    }

    /**
     * Quick upward square hop.
     */
    public static SoundSynth jump() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(300.0f, 700.0f)
                .sustain(0.06f)
                .release(0.08f)
                .duty(0.3f);
    }

    /**
     * Short harsh noise hit, good for damage.
     */
    public static SoundSynth hit() {
        return create()
                .waveform(Waveform.NOISE)
                .sweep(900.0f, 200.0f)
                .sustain(0.03f)
                .release(0.1f);
    }

    /**
     * Rising vibrato sweep, good for power-ups.
     */
    public static SoundSynth powerup() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(400.0f, 1200.0f)
                .vibrato(0.05f, 30.0f)
                .sustain(0.2f)
                .release(0.15f);
    }

    /**
     * Tiny UI click/blip.
     */
    public static SoundSynth blip() {
        return create()
                .waveform(Waveform.SINE)
                .frequency(800.0f)
                .sustain(0.03f)
                .release(0.03f);
    }

    /**
     * Short upward UI confirmation sound.
     */
    public static SoundSynth select() {
        return create()
                .waveform(Waveform.SINE)
                .sweep(500.0f, 900.0f)
                .attack(0.002f)
                .sustain(0.025f)
                .release(0.04f);
    }

    /**
     * Low descending error sound.
     */
    public static SoundSynth error() {
        return create()
                .waveform(Waveform.SQUARE)
                .sweep(350.0f, 100.0f)
                .sustain(0.08f)
                .release(0.1f)
                .duty(0.5f);
    }

    /**
     * Short rising charge sound.
     */
    public static SoundSynth charge() {
        return create()
                .waveform(Waveform.SAW)
                .sweep(150.0f, 900.0f)
                .sustain(0.25f)
                .release(0.1f)
                .lowpass(5000.0f);
    }

    /**
     * Short descending death sound.
     */
    public static SoundSynth death() {
        return create()
                .waveform(Waveform.TRIANGLE)
                .sweep(500.0f, 80.0f)
                .sustain(0.25f)
                .release(0.2f);
    }

    /**
     * Short teleport-like sweep.
     */
    public static SoundSynth teleport() {
        return create()
                .waveform(Waveform.SINE)
                .sweep(150.0f, 1600.0f)
                .sustain(0.25f)
                .release(0.2f)
                .vibrato(0.08f, 12.0f);
    }

    // ============================================================
    // BASIC OSCILLATOR SETTINGS
    // ============================================================

    public SoundSynth waveform(Waveform waveform) {
        if (waveform == null) {
            throw new IllegalArgumentException("waveform cannot be null");
        }

        this.waveform = waveform;
        return this;
    }

    public SoundSynth frequency(float hz) {
        this.startFrequency = Math.max(0.0f, hz);
        this.endFrequency = Float.NaN;
        return this;
    }

    /**
     * Sets frequency from a MIDI note number.
     *
     * 69 = A4 = 440 Hz.
     */
    public SoundSynth midiNote(int note) {
        return frequency(midiToHz(note));
    }

    /**
     * Sweeps between two MIDI note numbers.
     */
    public SoundSynth sweepNotes(int startNote, int endNote) {
        return sweep(
                midiToHz(startNote),
                midiToHz(endNote)
        );
    }

    /**
     * Slides from the current starting frequency
     * by the specified number of semitones.
     */
    public SoundSynth slideSemitones(float semitones) {
        if (!Float.isFinite(semitones)) {
            throw new IllegalArgumentException(
                    "semitones must be finite"
            );
        }

        this.endFrequency = startFrequency *
                (float) Math.pow(2.0, semitones / 12.0);

        return this;
    }

    /**
     * Converts a MIDI note number to frequency.
     *
     * MIDI 69 = A4 = 440 Hz.
     */
    public static float midiToHz(int note) {
        return (float) (
                440.0 *
                        Math.pow(2.0, (note - 69) / 12.0)
        );
    }

    /**
     * Creates a pitch sweep.
     *
     * The sweep is exponential in frequency, which produces
     * a more natural musical pitch movement than a linear Hz sweep.
     */
    public SoundSynth sweep(float startHz, float endHz) {
        if (!Float.isFinite(startHz) || !Float.isFinite(endHz)) {
            throw new IllegalArgumentException(
                    "frequencies must be finite"
            );
        }

        this.startFrequency = Math.max(0.0f, startHz);
        this.endFrequency = Math.max(0.0f, endHz);

        return this;
    }

    // ============================================================
    // ENVELOPE
    // ============================================================

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

    /**
     * Sets the complete ADSR envelope.
     */
    public SoundSynth envelope(
            float attack,
            float decay,
            float sustain,
            float release) {

        return attack(attack)
                .decay(decay)
                .sustain(sustain)
                .release(release);
    }

    /**
     * Convenience method for a sound with no attack or decay.
     */
    public SoundSynth duration(float seconds) {
        this.attack = 0.0f;
        this.decay = 0.0f;
        this.sustain = Math.max(0.0f, seconds);

        return this;
    }

    // ============================================================
    // AMPLITUDE
    // ============================================================

    public SoundSynth gain(float gain) {
        this.gain = Math.max(0.0f, gain);
        return this;
    }

    // ============================================================
    // OSCILLATOR MODULATION
    // ============================================================

    /**
     * Sets square-wave duty cycle.
     *
     * 0.5 = normal square wave.
     */
    public SoundSynth duty(float duty) {
        this.duty = Mathf.clamp01(duty);
        return this;
    }

    /**
     * Adds vibrato to the oscillator.
     *
     * depth:
     *   0.0 = none
     *   0.05 = ±5% frequency modulation
     *
     * rateHz:
     *   Vibrato frequency.
     */
    public SoundSynth vibrato(float depth, float rateHz) {
        this.vibratoDepth = Math.max(0.0f, depth);
        this.vibratoRate = Math.max(0.0f, rateHz);

        return this;
    }

    /**
     * Sets a deterministic noise seed.
     */
    public SoundSynth noiseSeed(long seed) {
        this.noiseSeed = seed;
        this.hasNoiseSeed = true;

        return this;
    }

    /**
     * Enables random noise generation.
     */
    public SoundSynth randomNoise() {
        this.hasNoiseSeed = false;
        return this;
    }

    /**
     * Amplitude modulation.
     *
     * depth:
     *   0 = none
     *   1 = maximum modulation
     *
     * rateHz:
     *   Modulation frequency.
     */
    public SoundSynth tremolo(float depth, float rateHz) {
        this.tremoloDepth = Mathf.clamp01(depth);
        this.tremoloRate = Math.max(0.0f, rateHz);

        return this;
    }

    // ============================================================
    // FILTERS
    // ============================================================

    /**
     * One-pole low-pass filter.
     *
     * 0 disables the filter.
     */
    public SoundSynth lowpass(float cutoffHz) {
        this.lowpassCutoff = Math.max(0.0f, cutoffHz);
        return this;
    }

    /**
     * One-pole high-pass filter.
     *
     * 0 disables the filter.
     */
    public SoundSynth highpass(float cutoffHz) {
        this.highpassCutoff = Math.max(0.0f, cutoffHz);
        return this;
    }

    // ============================================================
    // DISTORTION
    // ============================================================

    /**
     * Soft-clipping distortion.
     *
     * 0 = clean
     * 1 = heavy
     */
    public SoundSynth distortion(float amount) {
        this.distortion = Mathf.clamp01(amount);
        return this;
    }

    // ============================================================
    // BUILD
    // ============================================================

    /**
     * Generates the synthesized PCM sound.
     */
    public Sound build() {

        float duration =
                attack +
                        decay +
                        sustain +
                        release;

        int totalFrames = Math.max(
                1,
                (int) Math.ceil(duration * SAMPLE_RATE)
        );

        /*
         * Envelope boundaries are expressed in samples.
         */
        float attackEnd =
                attack * SAMPLE_RATE;

        float decayEnd =
                attackEnd +
                        decay * SAMPLE_RATE;

        float sustainEnd =
                decayEnd +
                        sustain * SAMPLE_RATE;

        /*
         * If there is no sweep, end frequency is the
         * starting frequency.
         */
        float freqStart = startFrequency;

        float freqEnd =
                Float.isNaN(endFrequency)
                        ? startFrequency
                        : endFrequency;

        /*
         * Avoid invalid exponential interpolation.
         *
         * A frequency of zero cannot participate in
         * logarithmic interpolation, so fall back to
         * linear interpolation when either endpoint is zero.
         */
        boolean exponentialSweep =
                freqStart > 0.0f &&
                        freqEnd > 0.0f;

        Random random =
                hasNoiseSeed
                        ? new Random(noiseSeed)
                        : new Random();

        short[] pcm =
                new short[totalFrames];

        double phase = 0.0;

        float noiseValue = 0.0f;
        int noiseCounter = 0;

        /*
         * Filter coefficients.
         */
        float lpAlpha =
                lowpassCutoff > 0.0f
                        ? filterAlpha(lowpassCutoff)
                        : 0.0f;

        float hpAlpha =
                highpassCutoff > 0.0f
                        ? filterAlpha(highpassCutoff)
                        : 0.0f;

        float lpState = 0.0f;
        float hpState = 0.0f;
        float hpPrevInput = 0.0f;

        /*
         * Distortion drive.
         */
        float drive =
                1.0f +
                        distortion * 15.0f;

        // ========================================================
        // SAMPLE GENERATION
        // ========================================================

        for (int i = 0; i < totalFrames; i++) {

            /*
             * Normalized sound position.
             *
             * Using totalFrames - 1 means the final sample
             * actually reaches t = 1.0.
             */
            float t =
                    totalFrames <= 1
                            ? 1.0f
                            : (float) i /
                              (float) (totalFrames - 1);

            // ----------------------------------------------------
            // FREQUENCY
            // ----------------------------------------------------

            float freq;

            if (exponentialSweep) {

                /*
                 * Exponential interpolation in frequency.
                 *
                 * This corresponds to linear movement in
                 * logarithmic pitch space.
                 */
                freq =
                        freqStart *
                                (float) Math.pow(
                                        freqEnd / freqStart,
                                        t
                                );

            } else {

                /*
                 * Linear interpolation is required when
                 * either frequency is zero.
                 */
                freq =
                        freqStart +
                                (freqEnd - freqStart) * t;
            }

            // ----------------------------------------------------
            // VIBRATO
            // ----------------------------------------------------

            if (vibratoDepth > 0.0f &&
                    vibratoRate > 0.0f) {

                float time =
                        (float) i / SAMPLE_RATE;

                freq *=
                        1.0f +
                                vibratoDepth *
                                        (float) Math.sin(
                                                2.0 *
                                                        Math.PI *
                                                        vibratoRate *
                                                        time
                                        );
            }

            freq = Math.max(0.0f, freq);

            // ----------------------------------------------------
            // OSCILLATOR
            // ----------------------------------------------------

            float sample;

            switch (waveform) {

                case SQUARE: {
                    double p = phase % 1.0;

                    sample =
                            p < duty
                                    ? 1.0f
                                    : -1.0f;

                    break;
                }

                case SAW: {
                    double p = phase % 1.0;

                    sample =
                            (float) (
                                    2.0 * p - 1.0
                            );

                    break;
                }

                case TRIANGLE: {
                    double p = phase % 1.0;

                    sample =
                            (float) (
                                    p < 0.5
                                            ? 4.0 * p - 1.0
                                            : 3.0 - 4.0 * p
                            );

                    break;
                }

                case NOISE: {

                    /*
                     * Sample-and-hold noise.
                     *
                     * Higher frequency = shorter random
                     * value duration.
                     */
                    int period =
                            freq > 0.0f
                                    ? Math.max(
                                    1,
                                    (int) (
                                            SAMPLE_RATE /
                                            (freq * 2.0f)
                                    )
                            )
                                    : 1;

                    if (noiseCounter <= 0) {

                        noiseValue =
                                random.nextFloat() *
                                        2.0f -
                                        1.0f;

                        noiseCounter = period;
                    }

                    noiseCounter--;

                    sample = noiseValue;

                    break;
                }

                case SINE:
                default:

                    sample =
                            (float) Math.sin(
                                    2.0 *
                                            Math.PI *
                                            phase
                            );

                    break;
            }

            // ----------------------------------------------------
            // PHASE
            // ----------------------------------------------------

            phase +=
                    freq / SAMPLE_RATE;

            /*
             * Keep phase reasonably small during long sounds.
             */
            if (phase >= 1048576.0) {
                phase %= 1.0;
            }

            // ----------------------------------------------------
            // DISTORTION
            // ----------------------------------------------------

            if (distortion > 0.0f) {

                sample =
                        (float) Math.tanh(
                                sample * drive
                        );
            }

            // ----------------------------------------------------
            // LOW-PASS
            // ----------------------------------------------------

            if (lpAlpha > 0.0f) {

                lpState +=
                        lpAlpha *
                                (sample - lpState);

                sample = lpState;
            }

            // ----------------------------------------------------
            // HIGH-PASS
            // ----------------------------------------------------

            if (hpAlpha > 0.0f) {

                hpState =
                        (1.0f - hpAlpha) *
                                (hpState +
                                        sample -
                                        hpPrevInput);

                hpPrevInput = sample;

                sample = hpState;
            }

            // ----------------------------------------------------
            // ADSR ENVELOPE
            // ----------------------------------------------------

            float env;

            if (attackEnd > 0.0f &&
                    i < attackEnd) {

                /*
                 * Attack:
                 *
                 * 0 → 1
                 */
                env =
                        i / attackEnd;

            } else if (
                    decay > 0.0f &&
                            i < decayEnd) {

                /*
                 * Decay:
                 *
                 * 1 → sustain level
                 */
                float p =
                        (i - attackEnd) /
                                Math.max(
                                        1.0f,
                                        decayEnd - attackEnd
                                );

                env =
                        1.0f +
                                (sustainLevel - 1.0f) *
                                        p;

            } else if (
                    i < sustainEnd) {

                /*
                 * Sustain.
                 */
                env = sustainLevel;

            } else {

                /*
                 * Release:
                 *
                 * sustain level → 0
                 */
                float releaseLength =
                        Math.max(
                                1.0f,
                                totalFrames - sustainEnd
                        );

                float p =
                        (i - sustainEnd) /
                                releaseLength;

                p = Mathf.clamp01(p);

                env =
                        sustainLevel *
                                (1.0f - p);
            }

            // ----------------------------------------------------
            // TREMOLO
            // ----------------------------------------------------

            if (tremoloDepth > 0.0f &&
                    tremoloRate > 0.0f) {

                float time =
                        (float) i / SAMPLE_RATE;

                float modulation =
                        0.5f +
                                0.5f *
                                        (float) Math.sin(
                                                2.0 *
                                                        Math.PI *
                                                        tremoloRate *
                                                        time
                                        );

                /*
                 * modulation is 0..1.
                 *
                 * The resulting amplitude is:
                 *
                 * 1 → 1 - depth
                 */
                env *=
                        1.0f -
                                tremoloDepth *
                                        modulation;
            }

            // ----------------------------------------------------
            // OUTPUT
            // ----------------------------------------------------

            float output =
                    sample *
                            env *
                            gain;

            int value =
                    Math.round(
                            output * 32767.0f
                    );

            /*
             * Clamp to signed 16-bit PCM.
             */
            if (value > 32767) {
                value = 32767;
            } else if (value < -32768) {
                value = -32768;
            }

            pcm[i] = (short) value;
        }

        // ========================================================
        // SOUND OBJECT
        // ========================================================

        Sound file = new Sound();

        file.channels = 1;
        file.samplerate = SAMPLE_RATE;
        file.samples = totalFrames;
        file.pcm = pcm;

        return file;
    }

    // ============================================================
    // FILTER MATH
    // ============================================================

    /**
     * Calculates the coefficient for a first-order filter.
     */
    private static float filterAlpha(float cutoffHz) {

        /*
         * Prevent absurd cutoff values from producing
         * strange coefficients.
         */
        cutoffHz =
                Math.max(
                        0.0001f,
                        Math.min(
                                cutoffHz,
                                SAMPLE_RATE * 0.49f
                        )
                );

        float rc =
                1.0f /
                        (
                                2.0f *
                                        (float) Math.PI *
                                        cutoffHz
                        );

        float dt =
                1.0f / SAMPLE_RATE;

        return dt / (rc + dt);
    }
}
