package berryngine;

import java.util.ArrayList;
import java.util.List;

public final class MusicSynth {
    public enum Waveform { SINE, SQUARE, SAW, TRIANGLE }

    public static final class Instrument {
        private Waveform waveform = Waveform.SINE;
        private float attack = 0.01f;
        private float decay = 0.08f;
        private float sustain = 0.7f;
        private float release = 0.15f;
        private float gain = 0.7f;
        private float duty = 0.5f;
        private float vibratoDepth;
        private float vibratoRate;
        private float harmonics;

        public static Instrument create() { return new Instrument(); }
        public Instrument waveform(Waveform value) { waveform = require(value, "waveform"); return this; }
        public Instrument envelope(float a, float d, float s, float r) { attack = positive(a); decay = positive(d); sustain = clamp01(s); release = positive(r); return this; }
        public Instrument gain(float value) { gain = positive(value); return this; }
        public Instrument duty(float value) { duty = clamp01(value); return this; }
        public Instrument vibrato(float depth, float rate) { vibratoDepth = positive(depth); vibratoRate = positive(rate); return this; }
        public Instrument harmonics(float value) { harmonics = clamp01(value); return this; }

        public static Instrument lead() { return create().waveform(Waveform.SAW).envelope(0.01f, 0.08f, 0.65f, 0.12f).harmonics(0.25f); }
        public static Instrument bass() { return create().waveform(Waveform.SQUARE).envelope(0.005f, 0.1f, 0.75f, 0.08f).gain(0.6f).duty(0.35f); }
        public static Instrument pad() { return create().waveform(Waveform.TRIANGLE).envelope(0.2f, 0.3f, 0.65f, 0.6f).gain(0.5f).harmonics(0.2f); }
        public static Instrument bell() { return create().waveform(Waveform.SINE).envelope(0.002f, 0.4f, 0.25f, 0.8f).harmonics(0.7f); }
    }

    public static final class Track {
        private final MusicSynth owner;
        private final Instrument instrument;
        private final List<Note> notes = new ArrayList<>();
        private float cursor;
        private float volume = 1.0f;
        private float pan;
        private int transpose;

        private Track(MusicSynth owner, Instrument instrument) { this.owner = owner; this.instrument = instrument; }
        public Track volume(float value) { volume = positive(value); return this; }
        public Track pan(float value) { pan = clamp(value, -1.0f, 1.0f); return this; }
        public Track transpose(int semitones) { transpose = semitones; return this; }
        public Track seek(float beat) { cursor = positive(beat); return this; }
        public Track rest(float beats) { cursor += checkedBeats(beats); return this; }
        public Track note(int midiNote, float beats) { return note(midiNote, beats, 1.0f); }
        public Track note(int midiNote, float beats, float velocity) {
            float duration = checkedBeats(beats);
            addNote(cursor, midiNote, duration, velocity);
            cursor += duration;
            return this;
        }
        public Track noteAt(float beat, int midiNote, float beats, float velocity) { addNote(positive(beat), midiNote, checkedBeats(beats), velocity); return this; }
        public Track chord(int[] midiNotes, float beats) { return chord(midiNotes, beats, 1.0f); }
        public Track chord(int[] midiNotes, float beats, float velocity) {
            if (midiNotes == null || midiNotes.length == 0) throw new IllegalArgumentException("midiNotes cannot be empty");
            float duration = checkedBeats(beats);
            for (int midiNote : midiNotes) addNote(cursor, midiNote, duration, velocity);
            cursor += duration;
            return this;
        }
        public Track arpeggio(int[] midiNotes, float stepBeats, float noteBeats, int repeats) {
            if (midiNotes == null || midiNotes.length == 0) throw new IllegalArgumentException("midiNotes cannot be empty");
            if (repeats < 0) throw new IllegalArgumentException("repeats cannot be negative");
            float step = checkedBeats(stepBeats);
            float duration = checkedBeats(noteBeats);
            for (int i = 0; i < midiNotes.length * repeats; i++) { addNote(cursor, midiNotes[i % midiNotes.length], duration, 1.0f); cursor += step; }
            return this;
        }
        public MusicSynth end() { return owner; }
        private void addNote(float beat, int midiNote, float beats, float velocity) {
            if (midiNote < 0 || midiNote > 127) throw new IllegalArgumentException("midiNote must be between 0 and 127");
            notes.add(new Note(beat, midiNote, beats, clamp01(velocity)));
        }
    }

    private static final class Note {
        final float beat;
        final int midiNote;
        final float beats;
        final float velocity;
        Note(float beat, int midiNote, float beats, float velocity) { this.beat = beat; this.midiNote = midiNote; this.beats = beats; this.velocity = velocity; }
    }

    private static final int SAMPLE_RATE = 44100;
    private final List<Track> tracks = new ArrayList<>();
    private float bpm = 120.0f;
    private float masterGain = 0.8f;
    private float swing;
    private float delayBeats;
    private float delayFeedback;

    private MusicSynth() {}
    public static MusicSynth create() { return new MusicSynth(); }
    public MusicSynth tempo(float value) { if (!Float.isFinite(value) || value <= 0.0f) throw new IllegalArgumentException("bpm must be positive"); bpm = value; return this; }
    public MusicSynth gain(float value) { masterGain = positive(value); return this; }
    public MusicSynth swing(float amount) { swing = clamp(amount, 0.0f, 0.49f); return this; }
    public MusicSynth delay(float beats, float feedback) { delayBeats = positive(beats); delayFeedback = clamp(feedback, 0.0f, 0.95f); return this; }
    public Track track(Instrument instrument) { Track track = new Track(this, require(instrument, "instrument")); tracks.add(track); return track; }

    public Sound build() {
        float secondsPerBeat = 60.0f / bpm;
        float endBeat = 0.0f;
        float longestRelease = 0.0f;
        for (Track track : tracks) {
            longestRelease = Math.max(longestRelease, track.instrument.release);
            for (Note note : track.notes) endBeat = Math.max(endBeat, swungBeat(note.beat) + note.beats);
        }
        float duration = endBeat * secondsPerBeat + longestRelease;
        if (delayBeats > 0.0f && delayFeedback > 0.0f) duration += delayBeats * secondsPerBeat * 4.0f;
        int frames = Math.max(1, (int) Math.ceil(duration * SAMPLE_RATE));
        float[] left = new float[frames];
        float[] right = new float[frames];

        for (Track track : tracks) {
            for (Note note : track.notes) renderNote(left, right, track, note, secondsPerBeat);
        }
        applyDelay(left, right, secondsPerBeat);

        float peak = 1.0f;
        for (int i = 0; i < frames; i++) peak = Math.max(peak, Math.max(Math.abs(left[i]), Math.abs(right[i])) * masterGain);
        Sound sound = new Sound();
        sound.channels = 2;
        sound.samplerate = SAMPLE_RATE;
        sound.samples = frames;
        sound.pcm = new short[frames * 2];
        for (int i = 0; i < frames; i++) {
            sound.pcm[i * 2] = (short) Math.round(left[i] * masterGain / peak * 32767.0f);
            sound.pcm[i * 2 + 1] = (short) Math.round(right[i] * masterGain / peak * 32767.0f);
        }
        return sound;
    }

    private void renderNote(float[] left, float[] right, Track track, Note note, float secondsPerBeat) {
        Instrument in = track.instrument;
        int start = (int) (swungBeat(note.beat) * secondsPerBeat * SAMPLE_RATE);
        float heldSeconds = note.beats * secondsPerBeat;
        int length = (int) Math.ceil((heldSeconds + in.release) * SAMPLE_RATE);
        float frequency = SoundSynth.midiToHz(note.midiNote + track.transpose);
        double phase = 0.0;
        float leftGain = (float) Math.sqrt((1.0f - track.pan) * 0.5f);
        float rightGain = (float) Math.sqrt((1.0f + track.pan) * 0.5f);
        for (int i = 0; i < length && start + i < left.length; i++) {
            float time = (float) i / SAMPLE_RATE;
            float currentFrequency = frequency;
            if (in.vibratoDepth > 0.0f && in.vibratoRate > 0.0f) currentFrequency *= 1.0f + in.vibratoDepth * (float) Math.sin(2.0 * Math.PI * in.vibratoRate * time);
            float sample = oscillator(in.waveform, phase, in.duty);
            if (in.harmonics > 0.0f) sample = (sample + oscillator(in.waveform, phase * 2.0, in.duty) * in.harmonics * 0.5f + oscillator(in.waveform, phase * 3.0, in.duty) * in.harmonics * 0.25f) / (1.0f + in.harmonics * 0.75f);
            phase += currentFrequency / SAMPLE_RATE;
            float value = sample * envelope(time, heldSeconds, in) * note.velocity * track.volume * in.gain;
            left[start + i] += value * leftGain;
            right[start + i] += value * rightGain;
        }
    }

    private void applyDelay(float[] left, float[] right, float secondsPerBeat) {
        int delaySamples = (int) (delayBeats * secondsPerBeat * SAMPLE_RATE);
        if (delaySamples <= 0 || delayFeedback <= 0.0f) return;
        for (int i = delaySamples; i < left.length; i++) {
            float delayedLeft = right[i - delaySamples] * delayFeedback;
            float delayedRight = left[i - delaySamples] * delayFeedback;
            left[i] += delayedLeft;
            right[i] += delayedRight;
        }
    }

    private float swungBeat(float beat) {
        int eighth = (int) Math.floor(beat * 2.0f + 0.00001f);
        return (eighth & 1) == 1 ? beat + swing * 0.5f : beat;
    }

    private static float envelope(float time, float held, Instrument in) {
        if (in.attack > 0.0f && time < in.attack) return time / in.attack;
        if (in.decay > 0.0f && time < in.attack + in.decay) return 1.0f + (in.sustain - 1.0f) * ((time - in.attack) / in.decay);
        if (time < held) return in.sustain;
        if (in.release <= 0.0f) return 0.0f;
        return in.sustain * Math.max(0.0f, 1.0f - (time - held) / in.release);
    }

    private static float oscillator(Waveform waveform, double phase, float duty) {
        double p = phase - Math.floor(phase);
        switch (waveform) {
            case SQUARE: return p < duty ? 1.0f : -1.0f;
            case SAW: return (float) (2.0 * p - 1.0);
            case TRIANGLE: return (float) (1.0 - 4.0 * Math.abs(p - 0.5));
            case SINE:
            default: return (float) Math.sin(2.0 * Math.PI * p);
        }
    }

    public static int note(String name) {
        if (name == null || name.length() < 2) throw new IllegalArgumentException("invalid note name");
        String value = name.trim().toUpperCase();
        int semitone;
        switch (value.charAt(0)) {
            case 'C': semitone = 0; break; case 'D': semitone = 2; break; case 'E': semitone = 4; break;
            case 'F': semitone = 5; break; case 'G': semitone = 7; break; case 'A': semitone = 9; break;
            case 'B': semitone = 11; break; default: throw new IllegalArgumentException("invalid note name: " + name);
        }
        int index = 1;
        if (index < value.length() && (value.charAt(index) == '#' || value.charAt(index) == 'B')) { semitone += value.charAt(index) == '#' ? 1 : -1; index++; }
        int octave;
        try { octave = Integer.parseInt(value.substring(index)); } catch (NumberFormatException e) { throw new IllegalArgumentException("invalid note name: " + name); }
        return (octave + 1) * 12 + semitone;
    }

    public static int[] chord(int root, int... intervals) {
        if (intervals == null) throw new IllegalArgumentException("intervals cannot be null");
        int[] notes = new int[intervals.length + 1];
        notes[0] = root;
        for (int i = 0; i < intervals.length; i++) notes[i + 1] = root + intervals[i];
        return notes;
    }

    private static float checkedBeats(float value) { if (!Float.isFinite(value) || value <= 0.0f) throw new IllegalArgumentException("beats must be positive"); return value; }
    private static float positive(float value) { if (!Float.isFinite(value)) throw new IllegalArgumentException("value must be finite"); return Math.max(0.0f, value); }
    private static float clamp01(float value) { return clamp(value, 0.0f, 1.0f); }
    private static float clamp(float value, float min, float max) { if (!Float.isFinite(value)) throw new IllegalArgumentException("value must be finite"); return Math.max(min, Math.min(max, value)); }
    private static <T> T require(T value, String name) { if (value == null) throw new IllegalArgumentException(name + " cannot be null"); return value; }
}
