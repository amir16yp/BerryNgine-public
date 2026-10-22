package berryngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MusicSynth {
  public enum Waveform {
    SINE,
    SQUARE,
    SAW,
    TRIANGLE,
    NOISE
  }

  /** General MIDI percussion groups used by channel 10. */
  public enum Drum {
    KICK,
    SNARE,
    HAT,
    TOM,
    CYMBAL,
    PERCUSSION;

    public static Drum fromNote(int midiNote) {
      if (midiNote < 0 || midiNote > 127)
        throw new IllegalArgumentException("midiNote must be between 0 and 127");
      switch (midiNote) {
        case 35:
        case 36:
          return KICK;
        case 37:
        case 38:
        case 39:
        case 40:
          return SNARE;
        case 42:
        case 44:
        case 46:
          return HAT;
        case 41:
        case 43:
        case 45:
        case 47:
        case 48:
        case 50:
          return TOM;
        case 49:
        case 51:
        case 52:
        case 53:
        case 55:
        case 57:
        case 59:
          return CYMBAL;
        default:
          return PERCUSSION;
      }
    }

    public String label() {
      switch (this) {
        case KICK:
          return "Kick";
        case SNARE:
          return "Snare";
        case HAT:
          return "Hi-Hat";
        case TOM:
          return "Tom";
        case CYMBAL:
          return "Cymbal";
        case PERCUSSION:
        default:
          return "Percussion";
      }
    }
  }

  public static final class Instrument {
    private static final String[] GM_NAMES = {
      "Acoustic Grand Piano",
      "Bright Acoustic Piano",
      "Electric Grand Piano",
      "Honky-tonk Piano",
      "Electric Piano 1",
      "Electric Piano 2",
      "Harpsichord",
      "Clavinet",
      "Celesta",
      "Glockenspiel",
      "Music Box",
      "Vibraphone",
      "Marimba",
      "Xylophone",
      "Tubular Bells",
      "Dulcimer",
      "Drawbar Organ",
      "Percussive Organ",
      "Rock Organ",
      "Church Organ",
      "Reed Organ",
      "Accordion",
      "Harmonica",
      "Tango Accordion",
      "Acoustic Guitar (nylon)",
      "Acoustic Guitar (steel)",
      "Electric Guitar (jazz)",
      "Electric Guitar (clean)",
      "Electric Guitar (muted)",
      "Overdriven Guitar",
      "Distortion Guitar",
      "Guitar Harmonics",
      "Acoustic Bass",
      "Electric Bass (finger)",
      "Electric Bass (pick)",
      "Fretless Bass",
      "Slap Bass 1",
      "Slap Bass 2",
      "Synth Bass 1",
      "Synth Bass 2",
      "Violin",
      "Viola",
      "Cello",
      "Contrabass",
      "Tremolo Strings",
      "Pizzicato Strings",
      "Orchestral Harp",
      "Timpani",
      "String Ensemble 1",
      "String Ensemble 2",
      "Synth Strings 1",
      "Synth Strings 2",
      "Choir Aahs",
      "Voice Oohs",
      "Synth Voice",
      "Orchestra Hit",
      "Trumpet",
      "Trombone",
      "Tuba",
      "Muted Trumpet",
      "French Horn",
      "Brass Section",
      "Synth Brass 1",
      "Synth Brass 2",
      "Soprano Sax",
      "Alto Sax",
      "Tenor Sax",
      "Baritone Sax",
      "Oboe",
      "English Horn",
      "Bassoon",
      "Clarinet",
      "Piccolo",
      "Flute",
      "Recorder",
      "Pan Flute",
      "Blown Bottle",
      "Shakuhachi",
      "Whistle",
      "Ocarina",
      "Lead 1 (square)",
      "Lead 2 (sawtooth)",
      "Lead 3 (calliope)",
      "Lead 4 (chiff)",
      "Lead 5 (charang)",
      "Lead 6 (voice)",
      "Lead 7 (fifths)",
      "Lead 8 (bass + lead)",
      "Pad 1 (new age)",
      "Pad 2 (warm)",
      "Pad 3 (polysynth)",
      "Pad 4 (choir)",
      "Pad 5 (bowed)",
      "Pad 6 (metallic)",
      "Pad 7 (halo)",
      "Pad 8 (sweep)",
      "FX 1 (rain)",
      "FX 2 (soundtrack)",
      "FX 3 (crystal)",
      "FX 4 (atmosphere)",
      "FX 5 (brightness)",
      "FX 6 (goblins)",
      "FX 7 (echoes)",
      "FX 8 (sci-fi)",
      "Sitar",
      "Banjo",
      "Shamisen",
      "Koto",
      "Kalimba",
      "Bag pipe",
      "Fiddle",
      "Shanai",
      "Tinkle Bell",
      "Agogo",
      "Steel Drums",
      "Woodblock",
      "Taiko Drum",
      "Melodic Tom",
      "Synth Drum",
      "Reverse Cymbal",
      "Guitar Fret Noise",
      "Breath Noise",
      "Seashore",
      "Bird Tweet",
      "Telephone Ring",
      "Helicopter",
      "Applause",
      "Gunshot"
    };

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
    private float lowpass;
    private float highpass;
    private float noise;
    private float tremoloDepth;
    private float tremoloRate;

    public static Instrument create() {
      return new Instrument();
    }

    public Instrument copy() {
      Instrument copy = new Instrument();
      copy.waveform = waveform;
      copy.attack = attack;
      copy.decay = decay;
      copy.sustain = sustain;
      copy.release = release;
      copy.gain = gain;
      copy.duty = duty;
      copy.vibratoDepth = vibratoDepth;
      copy.vibratoRate = vibratoRate;
      copy.harmonics = harmonics;
      copy.lowpass = lowpass;
      copy.highpass = highpass;
      copy.noise = noise;
      copy.tremoloDepth = tremoloDepth;
      copy.tremoloRate = tremoloRate;
      return copy;
    }

    public Instrument waveform(Waveform value) {
      waveform = require(value, "waveform");
      return this;
    }

    public Instrument envelope(float a, float d, float s, float r) {
      attack = positive(a);
      decay = positive(d);
      sustain = clamp01(s);
      release = positive(r);
      return this;
    }

    public Instrument gain(float value) {
      gain = positive(value);
      return this;
    }

    public Instrument duty(float value) {
      duty = clamp01(value);
      return this;
    }

    public Instrument vibrato(float depth, float rate) {
      vibratoDepth = positive(depth);
      vibratoRate = positive(rate);
      return this;
    }

    public Instrument harmonics(float value) {
      harmonics = clamp01(value);
      return this;
    }

    /** One-pole low-pass in Hz. Zero disables it. */
    public Instrument lowpass(float hz) {
      lowpass = positive(hz);
      return this;
    }

    /** One-pole high-pass in Hz. Zero disables it. */
    public Instrument highpass(float hz) {
      highpass = positive(hz);
      return this;
    }

    /** Mixes sample-and-hold noise into the oscillator. 0 is dry, 1 is all noise. */
    public Instrument noise(float amount) {
      noise = clamp01(amount);
      return this;
    }

    public Instrument tremolo(float depth, float rate) {
      tremoloDepth = clamp01(depth);
      tremoloRate = positive(rate);
      return this;
    }

    public Waveform waveform() {
      return waveform;
    }

    public float attack() {
      return attack;
    }

    public float decay() {
      return decay;
    }

    public float sustain() {
      return sustain;
    }

    public float release() {
      return release;
    }

    public float gain() {
      return gain;
    }

    public float duty() {
      return duty;
    }

    public float vibratoDepth() {
      return vibratoDepth;
    }

    public float vibratoRate() {
      return vibratoRate;
    }

    public float harmonics() {
      return harmonics;
    }

    public float lowpass() {
      return lowpass;
    }

    public float highpass() {
      return highpass;
    }

    public float noise() {
      return noise;
    }

    public float tremoloDepth() {
      return tremoloDepth;
    }

    public float tremoloRate() {
      return tremoloRate;
    }

    public static Instrument lead() {
      return create().waveform(Waveform.SAW).envelope(0.01f, 0.08f, 0.65f, 0.12f).harmonics(0.25f);
    }

    public static Instrument bass() {
      return create()
          .waveform(Waveform.SQUARE)
          .envelope(0.005f, 0.1f, 0.75f, 0.08f)
          .gain(0.6f)
          .duty(0.35f);
    }

    public static Instrument pad() {
      return create()
          .waveform(Waveform.TRIANGLE)
          .envelope(0.2f, 0.3f, 0.65f, 0.6f)
          .gain(0.5f)
          .harmonics(0.2f);
    }

    public static Instrument bell() {
      return create().waveform(Waveform.SINE).envelope(0.002f, 0.4f, 0.25f, 0.8f).harmonics(0.7f);
    }

    public static Instrument piano() {
      return gm(0);
    }

    public static Instrument organ() {
      return gm(19);
    }

    public static Instrument guitar() {
      return gm(25);
    }

    public static Instrument strings() {
      return gm(48);
    }

    public static Instrument brass() {
      return gm(61);
    }

    public static Instrument flute() {
      return gm(73);
    }

    public static Instrument choir() {
      return gm(52);
    }

    public static Instrument mallet() {
      return gm(12);
    }

    public static Instrument drumKick() {
      return drum(Drum.KICK);
    }

    public static Instrument drumSnare() {
      return drum(Drum.SNARE);
    }

    public static Instrument drumHat() {
      return drum(Drum.HAT);
    }

    public static Instrument drumTom() {
      return drum(Drum.TOM);
    }

    public static Instrument drumCymbal() {
      return drum(Drum.CYMBAL);
    }

    public static Instrument drumPercussion() {
      return drum(Drum.PERCUSSION);
    }

    /** Procedural patch for a General MIDI program, 0 through 127. */
    public static Instrument gm(int program) {
      if (program < 0 || program > 127)
        throw new IllegalArgumentException("program must be between 0 and 127");
      Instrument special = special(program);
      if (special != null) return special;
      Instrument instrument = family(program);
      vary(instrument, program & 7);
      return instrument;
    }

    public static String gmName(int program) {
      if (program < 0 || program > 127)
        throw new IllegalArgumentException("program must be between 0 and 127");
      return GM_NAMES[program];
    }

    /** Case-insensitive General MIDI program name lookup. */
    public static int program(String name) {
      if (name == null) throw new IllegalArgumentException("name cannot be null");
      String wanted = name.trim();
      for (int i = 0; i < GM_NAMES.length; i++) {
        if (GM_NAMES[i].equalsIgnoreCase(wanted)) return i;
      }
      throw new IllegalArgumentException("unknown GM program: " + name);
    }

    public static Instrument drum(int midiNote) {
      return drum(Drum.fromNote(midiNote), 0);
    }

    public static Instrument drum(Drum voice) {
      return drum(voice, 0);
    }

    /**
     * Procedural drum voice. {@code kitProgram} is the channel-10 program (0 standard, 8 room, 16
     * power, 24 electronic, 25 analog, 32 jazz, 40 brush, 48 orchestra, 56 sound effects).
     */
    public static Instrument drum(Drum voice, int kitProgram) {
      if (voice == null) throw new IllegalArgumentException("voice cannot be null");
      if (kitProgram < 0 || kitProgram > 127)
        throw new IllegalArgumentException("kitProgram must be between 0 and 127");
      Instrument instrument;
      switch (voice) {
        case KICK:
          instrument =
              create()
                  .waveform(Waveform.SINE)
                  .envelope(0.001f, 0.1f, 0.12f, 0.05f)
                  .gain(0.9f)
                  .harmonics(0.2f)
                  .lowpass(420f)
                  .noise(0.18f);
          break;
        case SNARE:
          instrument =
              create()
                  .waveform(Waveform.TRIANGLE)
                  .envelope(0.001f, 0.08f, 0.08f, 0.04f)
                  .gain(0.72f)
                  .harmonics(0.15f)
                  .noise(0.75f)
                  .highpass(220f);
          break;
        case HAT:
          instrument =
              create()
                  .waveform(Waveform.NOISE)
                  .envelope(0.001f, 0.025f, 0.04f, 0.02f)
                  .gain(0.42f)
                  .noise(1f)
                  .highpass(5500f);
          break;
        case TOM:
          instrument =
              create()
                  .waveform(Waveform.SINE)
                  .envelope(0.002f, 0.14f, 0.22f, 0.08f)
                  .gain(0.7f)
                  .harmonics(0.28f)
                  .lowpass(700f)
                  .noise(0.1f);
          break;
        case CYMBAL:
          instrument =
              create()
                  .waveform(Waveform.NOISE)
                  .envelope(0.002f, 0.18f, 0.22f, 0.4f)
                  .gain(0.38f)
                  .noise(1f)
                  .highpass(3200f);
          break;
        case PERCUSSION:
        default:
          instrument =
              create()
                  .waveform(Waveform.TRIANGLE)
                  .envelope(0.001f, 0.07f, 0.12f, 0.05f)
                  .gain(0.55f)
                  .harmonics(0.4f)
                  .noise(0.35f);
          break;
      }
      applyKit(instrument, voice, kitProgram);
      return instrument;
    }

    private static void applyKit(Instrument instrument, Drum voice, int kit) {
      int family = kit == 25 ? 25 : kit & ~7;
      switch (family) {
        case 8:
          instrument.envelope(
              instrument.attack, instrument.decay, instrument.sustain, instrument.release * 1.35f);
          break;
        case 16:
          instrument
              .gain(instrument.gain * 1.15f)
              .envelope(
                  instrument.attack, instrument.decay * 0.75f, instrument.sustain, instrument.release);
          break;
        case 24:
          if (voice == Drum.KICK || voice == Drum.TOM)
            instrument.waveform(Waveform.SQUARE).lowpass(voice == Drum.KICK ? 500f : 900f);
          else instrument.noise(Math.min(1f, instrument.noise + 0.15f));
          break;
        case 25:
          instrument.waveform(voice == Drum.HAT || voice == Drum.CYMBAL ? Waveform.NOISE : Waveform.SQUARE);
          instrument.envelope(0.001f, 0.06f, 0.1f, 0.04f);
          break;
        case 32:
          instrument.gain(instrument.gain * 0.82f);
          if (voice == Drum.SNARE) instrument.noise(0.88f);
          break;
        case 40:
          instrument.noise(Math.min(1f, instrument.noise + 0.2f)).gain(instrument.gain * 0.75f);
          break;
        case 48:
          instrument
              .gain(instrument.gain * 0.85f)
              .envelope(
                  instrument.attack, instrument.decay, instrument.sustain, instrument.release * 1.25f);
          break;
        case 56:
          instrument.waveform(Waveform.NOISE).noise(1f).envelope(0.001f, 0.04f, 0.05f, 0.06f);
          break;
        default:
          break;
      }
    }

    private static Instrument family(int program) {
      switch (program >> 3) {
        case 0:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.004f, 0.2f, 0.4f, 0.22f)
              .gain(0.62f)
              .harmonics(0.34f)
              .lowpass(2600f);
        case 1:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.001f, 0.22f, 0.12f, 0.5f)
              .gain(0.58f)
              .harmonics(0.72f);
        case 2:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.006f, 0.04f, 0.9f, 0.08f)
              .gain(0.48f)
              .duty(0.5f)
              .harmonics(0.18f);
        case 3:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.002f, 0.14f, 0.25f, 0.12f)
              .gain(0.55f)
              .harmonics(0.42f)
              .lowpass(3400f);
        case 4:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.004f, 0.08f, 0.8f, 0.07f)
              .gain(0.62f)
              .duty(0.32f)
              .lowpass(900f);
        case 5:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.05f, 0.16f, 0.72f, 0.26f)
              .gain(0.46f)
              .harmonics(0.16f)
              .lowpass(2800f)
              .vibrato(0.008f, 5.4f);
        case 6:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.1f, 0.2f, 0.7f, 0.38f)
              .gain(0.44f)
              .harmonics(0.2f)
              .lowpass(2600f)
              .vibrato(0.01f, 5.1f);
        case 7:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.03f, 0.1f, 0.78f, 0.14f)
              .gain(0.5f)
              .harmonics(0.28f)
              .lowpass(2100f);
        case 8:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.02f, 0.1f, 0.68f, 0.12f)
              .gain(0.48f)
              .duty(0.42f)
              .harmonics(0.12f)
              .vibrato(0.012f, 5.2f);
        case 9:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.03f, 0.08f, 0.75f, 0.12f)
              .gain(0.5f)
              .harmonics(0.18f)
              .vibrato(0.016f, 5.0f);
        case 10:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.01f, 0.08f, 0.66f, 0.12f)
              .gain(0.54f)
              .harmonics(0.38f);
        case 11:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.22f, 0.28f, 0.66f, 0.55f)
              .gain(0.42f)
              .harmonics(0.22f);
        case 12:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.04f, 0.3f, 0.4f, 0.45f)
              .gain(0.4f)
              .harmonics(0.5f)
              .vibrato(0.02f, 4.2f)
              .lowpass(3000f);
        case 13:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.01f, 0.14f, 0.55f, 0.18f)
              .gain(0.5f)
              .harmonics(0.32f);
        case 14:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.001f, 0.09f, 0.18f, 0.1f)
              .gain(0.55f)
              .harmonics(0.3f);
        default:
          return create().waveform(Waveform.NOISE).envelope(0.01f, 0.1f, 0.2f, 0.2f).noise(1f).gain(0.4f);
      }
    }

    private static void vary(Instrument instrument, int variant) {
      float attack = instrument.attack;
      float decay = instrument.decay;
      float sustain = instrument.sustain;
      instrument.harmonics(clamp01(instrument.harmonics + (variant - 3) * 0.03f));
      instrument.envelope(attack, decay, sustain, instrument.release * (0.88f + variant * 0.04f));
      instrument.gain(instrument.gain * (0.94f + variant * 0.015f));
      if (instrument.lowpass > 0f) instrument.lowpass(instrument.lowpass * (0.82f + variant * 0.05f));
      instrument.duty(clamp01(instrument.duty + (variant - 3) * 0.015f));
      if ((variant & 1) == 1) {
        float rate = instrument.vibratoRate > 0f ? instrument.vibratoRate : 5.2f;
        instrument.vibrato(instrument.vibratoDepth + 0.004f, rate);
      }
    }

    private static Instrument special(int program) {
      switch (program) {
        case 4:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.002f, 0.18f, 0.28f, 0.3f)
              .harmonics(0.55f)
              .gain(0.55f);
        case 5:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.004f, 0.16f, 0.3f, 0.28f)
              .harmonics(0.4f)
              .vibrato(0.01f, 6f)
              .tremolo(0.25f, 7f)
              .gain(0.52f);
        case 6:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.001f, 0.08f, 0.15f, 0.06f)
              .duty(0.4f)
              .harmonics(0.45f)
              .gain(0.5f);
        case 7:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.001f, 0.05f, 0.1f, 0.04f)
              .duty(0.28f)
              .harmonics(0.2f)
              .highpass(300f)
              .gain(0.5f);
        case 13:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.001f, 0.05f, 0.05f, 0.08f)
              .harmonics(0.85f)
              .gain(0.55f);
        case 14:
          return bell().gain(0.6f);
        case 28:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.001f, 0.04f, 0.08f, 0.03f)
              .harmonics(0.3f)
              .highpass(400f)
              .gain(0.5f);
        case 29:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.005f, 0.12f, 0.45f, 0.1f)
              .harmonics(0.65f)
              .lowpass(2800f)
              .gain(0.5f);
        case 30:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.004f, 0.1f, 0.55f, 0.1f)
              .harmonics(0.85f)
              .lowpass(2200f)
              .gain(0.48f);
        case 31:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.002f, 0.12f, 0.15f, 0.15f)
              .harmonics(0.9f)
              .gain(0.45f);
        case 38:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.005f, 0.08f, 0.75f, 0.06f)
              .harmonics(0.35f)
              .lowpass(700f)
              .gain(0.6f);
        case 39:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.002f, 0.06f, 0.7f, 0.05f)
              .duty(0.25f)
              .lowpass(600f)
              .gain(0.58f);
        case 44:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.04f, 0.12f, 0.7f, 0.25f)
              .harmonics(0.15f)
              .tremolo(0.55f, 8f)
              .lowpass(2600f)
              .gain(0.46f);
        case 45:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.001f, 0.07f, 0.1f, 0.06f)
              .harmonics(0.35f)
              .gain(0.55f);
        case 46:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.001f, 0.18f, 0.15f, 0.35f)
              .harmonics(0.55f)
              .gain(0.5f);
        case 47:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.004f, 0.25f, 0.2f, 0.35f)
              .harmonics(0.25f)
              .lowpass(350f)
              .gain(0.7f);
        case 52:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.12f, 0.2f, 0.7f, 0.4f)
              .harmonics(0.12f)
              .vibrato(0.015f, 5.5f)
              .lowpass(1800f)
              .gain(0.46f);
        case 53:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.08f, 0.16f, 0.65f, 0.3f)
              .harmonics(0.2f)
              .vibrato(0.018f, 5.2f)
              .gain(0.48f);
        case 54:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.06f, 0.14f, 0.6f, 0.28f)
              .harmonics(0.3f)
              .vibrato(0.02f, 5f)
              .lowpass(2000f)
              .gain(0.45f);
        case 55:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.002f, 0.08f, 0.2f, 0.18f)
              .harmonics(0.7f)
              .gain(0.6f);
        case 80:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.008f, 0.08f, 0.7f, 0.1f)
              .duty(0.5f)
              .harmonics(0.2f)
              .gain(0.5f);
        case 82:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.02f, 0.1f, 0.6f, 0.12f)
              .harmonics(0.45f)
              .vibrato(0.02f, 5.5f)
              .gain(0.5f);
        case 86:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.01f, 0.08f, 0.65f, 0.12f)
              .harmonics(0.95f)
              .gain(0.48f);
        case 104:
          return create()
              .waveform(Waveform.SAW)
              .envelope(0.01f, 0.12f, 0.45f, 0.15f)
              .harmonics(0.4f)
              .vibrato(0.012f, 5f)
              .lowpass(2400f)
              .gain(0.5f);
        case 105:
          return create()
              .waveform(Waveform.TRIANGLE)
              .envelope(0.001f, 0.06f, 0.12f, 0.05f)
              .harmonics(0.5f)
              .gain(0.52f);
        case 108:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.001f, 0.16f, 0.15f, 0.25f)
              .harmonics(0.6f)
              .gain(0.5f);
        case 114:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.001f, 0.1f, 0.12f, 0.18f)
              .harmonics(0.8f)
              .gain(0.55f);
        case 115:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.001f, 0.03f, 0.05f, 0.03f)
              .duty(0.3f)
              .gain(0.5f);
        case 116:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.002f, 0.12f, 0.15f, 0.1f)
              .harmonics(0.3f)
              .lowpass(400f)
              .noise(0.15f)
              .gain(0.7f);
        case 117:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.002f, 0.16f, 0.2f, 0.1f)
              .harmonics(0.25f)
              .lowpass(800f)
              .gain(0.65f);
        case 118:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.001f, 0.06f, 0.08f, 0.05f)
              .noise(0.45f)
              .lowpass(1200f)
              .gain(0.55f);
        case 119:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.25f, 0.05f, 0.2f, 0.15f)
              .noise(1f)
              .highpass(2500f)
              .gain(0.4f);
        case 120:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.001f, 0.03f, 0.02f, 0.04f)
              .noise(1f)
              .highpass(1200f)
              .gain(0.35f);
        case 121:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.05f, 0.1f, 0.3f, 0.15f)
              .noise(1f)
              .highpass(800f)
              .lowpass(3000f)
              .gain(0.3f);
        case 122:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.2f, 0.3f, 0.7f, 0.4f)
              .noise(1f)
              .lowpass(1200f)
              .gain(0.35f);
        case 123:
          return create()
              .waveform(Waveform.SINE)
              .envelope(0.005f, 0.04f, 0.1f, 0.08f)
              .harmonics(0.7f)
              .vibrato(0.03f, 12f)
              .gain(0.4f);
        case 124:
          return create()
              .waveform(Waveform.SQUARE)
              .envelope(0.01f, 0.04f, 0.5f, 0.04f)
              .duty(0.5f)
              .gain(0.35f);
        case 125:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.05f, 0.1f, 0.8f, 0.1f)
              .noise(1f)
              .lowpass(400f)
              .tremolo(0.4f, 18f)
              .gain(0.4f);
        case 126:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.01f, 0.2f, 0.5f, 0.3f)
              .noise(1f)
              .highpass(1500f)
              .gain(0.35f);
        case 127:
          return create()
              .waveform(Waveform.NOISE)
              .envelope(0.001f, 0.08f, 0.05f, 0.2f)
              .noise(1f)
              .lowpass(900f)
              .gain(0.7f);
        default:
          return null;
      }
    }
  }

  /** One note already placed on a track. Mutators edit the song in place. */
  public static final class Note {
    private float beat;
    private int midiNote;
    private float beats;
    private float velocity;
    private float expression = 1f;
    private float expressionEnd = 1f;
    private float pan;
    private float modulation;
    private float modulationEnd;
    private float tuning;
    private float tremolo;
    private float brightness = 1f;
    private float attackScale = 1f;
    private float releaseScale = 1f;
    private float[] bendBeats;
    private float[] bendSemis;

    private Note(float beat, int midiNote, float beats, float velocity) {
      this.beat = positive(beat);
      this.midiNote = checkedMidi(midiNote);
      this.beats = checkedBeats(beats);
      this.velocity = clamp01(velocity);
    }

    public float beat() {
      return beat;
    }

    public Note beat(float value) {
      beat = positive(value);
      return this;
    }

    public int midi() {
      return midiNote;
    }

    public Note midi(int value) {
      midiNote = checkedMidi(value);
      return this;
    }

    public float length() {
      return beats;
    }

    public Note length(float value) {
      beats = checkedBeats(value);
      return this;
    }

    public float velocity() {
      return velocity;
    }

    public Note velocity(float value) {
      velocity = clamp01(value);
      return this;
    }

    public float expression() {
      return expression;
    }

    public float expressionEnd() {
      return expressionEnd;
    }

    public Note expression(float value) {
      expression = clamp(value, 0f, 8f);
      expressionEnd = expression;
      return this;
    }

    public Note expression(float start, float end) {
      expression = clamp(start, 0f, 8f);
      expressionEnd = clamp(end, 0f, 8f);
      return this;
    }

    /** Pan offset added to the track pan. */
    public float pan() {
      return pan;
    }

    public Note pan(float offset) {
      pan = clamp(offset, -2f, 2f);
      return this;
    }

    /** Extra vibrato depth, as a fraction of frequency. */
    public float modulation() {
      return modulation;
    }

    public float modulationEnd() {
      return modulationEnd;
    }

    public Note modulation(float depth) {
      modulation = clamp(depth, 0f, 4f);
      modulationEnd = modulation;
      return this;
    }

    public Note modulation(float start, float end) {
      modulation = clamp(start, 0f, 4f);
      modulationEnd = clamp(end, 0f, 4f);
      return this;
    }

    /** Constant pitch offset in semitones, replacing any bend curve. */
    public float pitch() {
      return bendSemis == null || bendSemis.length == 0 ? 0f : bendSemis[0];
    }

    public Note pitch(float semitones) {
      return bend(new float[] {0f}, new float[] {semitones});
    }

    public Note clearBend() {
      bendBeats = null;
      bendSemis = null;
      return this;
    }

    /** Piecewise-linear pitch bend. Offsets are beats from the note start. */
    public Note bend(float[] beatOffsets, float[] semitones) {
      if (beatOffsets == null || semitones == null)
        throw new IllegalArgumentException("bend arrays cannot be null");
      if (beatOffsets.length != semitones.length)
        throw new IllegalArgumentException("bend arrays must be the same length");
      if (beatOffsets.length == 0) return clearBend();
      float[] beatsCopy = Arrays.copyOf(beatOffsets, beatOffsets.length);
      float[] semisCopy = Arrays.copyOf(semitones, semitones.length);
      for (int i = 0; i < beatsCopy.length; i++) {
        if (!Float.isFinite(beatsCopy[i]) || beatsCopy[i] < 0f)
          throw new IllegalArgumentException("bend beats must be zero or positive");
        if (i > 0 && beatsCopy[i] < beatsCopy[i - 1])
          throw new IllegalArgumentException("bend beats must be sorted");
        semisCopy[i] = clamp(semisCopy[i], -128f, 128f);
      }
      bendBeats = beatsCopy;
      bendSemis = semisCopy;
      return this;
    }

    public int bendCount() {
      return bendBeats == null ? 0 : bendBeats.length;
    }

    public float bendBeat(int index) {
      return bendPoint(bendBeats, index);
    }

    public float bendSemis(int index) {
      return bendPoint(bendSemis, index);
    }

    /** Microtuning in semitones, kept separate from the pitch-bend curve. */
    public float tuning() {
      return tuning;
    }

    public Note tuning(float semitones) {
      tuning = clamp(semitones, -96f, 96f);
      return this;
    }

    public float tremolo() {
      return tremolo;
    }

    public Note tremolo(float depth) {
      tremolo = clamp01(depth);
      return this;
    }

    public float brightness() {
      return brightness;
    }

    public Note brightness(float scale) {
      brightness = positive(scale);
      return this;
    }

    public float attackScale() {
      return attackScale;
    }

    public Note attackScale(float scale) {
      attackScale = positive(scale);
      return this;
    }

    public float releaseScale() {
      return releaseScale;
    }

    public Note releaseScale(float scale) {
      releaseScale = positive(scale);
      return this;
    }

    public Note shift(int semitones) {
      int shifted = midiNote + semitones;
      if (shifted < 0) shifted = 0;
      if (shifted > 127) shifted = 127;
      midiNote = shifted;
      return this;
    }

    private static float bendPoint(float[] values, int index) {
      if (values == null || index < 0 || index >= values.length)
        throw new IllegalArgumentException("bend index out of range");
      return values[index];
    }
  }

  public static final class Track {
    private final MusicSynth owner;
    private Instrument instrument;
    private final List<Note> notes = new ArrayList<>();
    private float cursor;
    private float volume = 1.0f;
    private float pan;
    private int transpose;
    private float detune;
    private String name;
    private int channel = -1;
    private int program = -1;
    private int bank;
    private boolean percussion;
    private Drum drum;
    private boolean muted;
    private boolean solo;
    private float reverb;
    private float chorus;

    private Track(MusicSynth owner, Instrument instrument) {
      this.owner = owner;
      this.instrument = instrument;
    }

    public Instrument instrument() {
      return instrument;
    }

    public Track instrument(Instrument value) {
      instrument = require(value, "instrument");
      return this;
    }

    public String name() {
      return name;
    }

    public Track name(String value) {
      name = require(value, "name");
      return this;
    }

    /** MIDI channel 0–15, or -1 when the track was not imported. */
    public int channel() {
      return channel;
    }

    public Track channel(int value) {
      if (value < -1 || value > 15)
        throw new IllegalArgumentException("channel must be -1 or between 0 and 15");
      channel = value;
      return this;
    }

    /**
     * General MIDI program 0–127, or -1 when unset. Percussion tracks store the drum-kit program
     * instead.
     */
    public int program() {
      return program;
    }

    public Track program(int value) {
      if (value < -1 || value > 127)
        throw new IllegalArgumentException("program must be -1 or between 0 and 127");
      program = value;
      return this;
    }

    public int bank() {
      return bank;
    }

    public Track bank(int value) {
      if (value < 0 || value > 16383)
        throw new IllegalArgumentException("bank must be between 0 and 16383");
      bank = value;
      return this;
    }

    public boolean percussion() {
      return percussion;
    }

    public Track percussion(boolean value) {
      percussion = value;
      return this;
    }

    public Drum drum() {
      return drum;
    }

    public Track drum(Drum value) {
      drum = value;
      if (value != null) percussion = true;
      return this;
    }

    public Track volume(float value) {
      volume = positive(value);
      return this;
    }

    public float volume() {
      return volume;
    }

    public Track pan(float value) {
      pan = clamp(value, -1.0f, 1.0f);
      return this;
    }

    public float pan() {
      return pan;
    }

    public Track transpose(int semitones) {
      transpose = semitones;
      return this;
    }

    public int transpose() {
      return transpose;
    }

    /** Fractional transpose in semitones, applied on top of {@link #transpose(int)}. */
    public Track detune(float semitones) {
      detune = clamp(semitones, -96f, 96f);
      return this;
    }

    public float detune() {
      return detune;
    }

    public Track reverb(float amount) {
      reverb = clamp01(amount);
      return this;
    }

    public float reverb() {
      return reverb;
    }

    public Track chorus(float amount) {
      chorus = clamp01(amount);
      return this;
    }

    public float chorus() {
      return chorus;
    }

    public Track mute(boolean value) {
      muted = value;
      return this;
    }

    public Track mute() {
      return mute(true);
    }

    public Track unmute() {
      return mute(false);
    }

    public boolean muted() {
      return muted;
    }

    public Track solo(boolean value) {
      solo = value;
      return this;
    }

    public boolean soloed() {
      return solo;
    }

    public Track seek(float beat) {
      cursor = positive(beat);
      return this;
    }

    public float cursor() {
      return cursor;
    }

    public Track rest(float beats) {
      cursor += checkedBeats(beats);
      return this;
    }

    public Track note(int midiNote, float beats) {
      return note(midiNote, beats, 1.0f);
    }

    public Track note(int midiNote, float beats, float velocity) {
      float duration = checkedBeats(beats);
      place(cursor, midiNote, duration, velocity);
      cursor += duration;
      return this;
    }

    public Track noteAt(float beat, int midiNote, float beats, float velocity) {
      place(positive(beat), midiNote, checkedBeats(beats), velocity);
      return this;
    }

    public Track chord(int[] midiNotes, float beats) {
      return chord(midiNotes, beats, 1.0f);
    }

    public Track chord(int[] midiNotes, float beats, float velocity) {
      if (midiNotes == null || midiNotes.length == 0)
        throw new IllegalArgumentException("midiNotes cannot be empty");
      float duration = checkedBeats(beats);
      for (int midiNote : midiNotes) place(cursor, midiNote, duration, velocity);
      cursor += duration;
      return this;
    }

    public Track arpeggio(int[] midiNotes, float stepBeats, float noteBeats, int repeats) {
      if (midiNotes == null || midiNotes.length == 0)
        throw new IllegalArgumentException("midiNotes cannot be empty");
      if (repeats < 0) throw new IllegalArgumentException("repeats cannot be negative");
      float step = checkedBeats(stepBeats);
      float duration = checkedBeats(noteBeats);
      for (int i = 0; i < midiNotes.length * repeats; i++) {
        place(cursor, midiNotes[i % midiNotes.length], duration, 1.0f);
        cursor += step;
      }
      return this;
    }

    public int noteCount() {
      return notes.size();
    }

    public Note getNote(int index) {
      if (index < 0 || index >= notes.size())
        throw new IllegalArgumentException("note index out of range");
      return notes.get(index);
    }

    public List<Note> notes() {
      return Collections.unmodifiableList(new ArrayList<>(notes));
    }

    public Track removeNote(int index) {
      if (index < 0 || index >= notes.size())
        throw new IllegalArgumentException("note index out of range");
      notes.remove(index);
      return this;
    }

    public Track clearNotes() {
      notes.clear();
      return this;
    }

    public Track forNotes(Consumer<Note> action) {
      require(action, "action");
      for (int i = 0; i < notes.size(); i++) action.accept(notes.get(i));
      return this;
    }

    /** Adds semitones to every note, clamped to the MIDI range. */
    public Track shift(int semitones) {
      for (Note note : notes) note.shift(semitones);
      return this;
    }

    public Track scaleVelocity(float factor) {
      if (!Float.isFinite(factor) || factor < 0f)
        throw new IllegalArgumentException("factor must be zero or positive");
      for (Note note : notes) note.velocity(note.velocity * factor);
      return this;
    }

    /** Drops imported bend, expression, and modulation so the notes play plainly. */
    public Track resetPerformance() {
      for (Note note : notes) {
        note.expression(1f)
            .pan(0f)
            .modulation(0f)
            .tuning(0f)
            .tremolo(0f)
            .brightness(1f)
            .attackScale(1f)
            .releaseScale(1f)
            .clearBend();
      }
      return this;
    }

    public MusicSynth remove() {
      owner.tracks.remove(this);
      return owner;
    }

    public MusicSynth end() {
      return owner;
    }

    Note place(float beat, int midiNote, float beats, float velocity) {
      Note note = new Note(beat, midiNote, beats, velocity);
      notes.add(note);
      return note;
    }

    void sortNotes() {
      notes.sort((a, b) -> Float.compare(a.beat, b.beat));
    }
  }

  public enum MarkerKind {
    TEXT,
    COPYRIGHT,
    LYRIC,
    MARKER,
    CUE
  }

  public static final class Marker {
    private final float beat;
    private final String text;
    private final MarkerKind kind;

    private Marker(float beat, String text, MarkerKind kind) {
      this.beat = beat;
      this.text = text;
      this.kind = kind;
    }

    public float beat() {
      return beat;
    }

    public String text() {
      return text;
    }

    public MarkerKind kind() {
      return kind;
    }
  }

  public static final class TimeSignature {
    private final float beat;
    private final int numerator;
    private final int denominator;

    private TimeSignature(float beat, int numerator, int denominator) {
      this.beat = beat;
      this.numerator = numerator;
      this.denominator = denominator;
    }

    public float beat() {
      return beat;
    }

    public int numerator() {
      return numerator;
    }

    public int denominator() {
      return denominator;
    }
  }

  public static final class KeySignature {
    private final float beat;
    private final int accidentals;
    private final boolean minor;

    private KeySignature(float beat, int accidentals, boolean minor) {
      this.beat = beat;
      this.accidentals = accidentals;
      this.minor = minor;
    }

    public float beat() {
      return beat;
    }

    /** Negative for flats, positive for sharps, from -7 to 7. */
    public int accidentals() {
      return accidentals;
    }

    public boolean minor() {
      return minor;
    }
  }

  private static final class TempoPoint {
    final float beat;
    float bpm;

    TempoPoint(float beat, float bpm) {
      this.beat = beat;
      this.bpm = bpm;
    }
  }

  private static final int SAMPLE_RATE = 44100;
  private final List<Track> tracks = new ArrayList<>();
  private final List<TempoPoint> tempoMap = new ArrayList<>();
  private final List<Marker> markers = new ArrayList<>();
  private final List<TimeSignature> timeSignatures = new ArrayList<>();
  private final List<KeySignature> keySignatures = new ArrayList<>();
  private float bpm = 120.0f;
  private float masterGain = 0.8f;
  private float swing;
  private float delayBeats;
  private float delayFeedback;
  private String title = "";
  private String copyright = "";

  private MusicSynth() {}

  public static MusicSynth create() {
    return new MusicSynth();
  }

  public MusicSynth tempo(float value) {
    if (!Float.isFinite(value) || value <= 0.0f)
      throw new IllegalArgumentException("bpm must be positive");
    bpm = value;
    tempoMap.clear();
    tempoMap.add(new TempoPoint(0f, value));
    return this;
  }

  public float tempo() {
    return bpmAt(0f);
  }

  /** Inserts or replaces a tempo change. {@link #tempo(float)} clears the map first. */
  public MusicSynth tempoAt(float beat, float value) {
    float at = beatValue(beat);
    if (!Float.isFinite(value) || value <= 0.0f)
      throw new IllegalArgumentException("bpm must be positive");
    for (int i = 0; i < tempoMap.size(); i++) {
      TempoPoint point = tempoMap.get(i);
      if (point.beat == at) {
        point.bpm = value;
        if (at == 0f) bpm = value;
        return this;
      }
      if (point.beat > at) {
        tempoMap.add(i, new TempoPoint(at, value));
        return this;
      }
    }
    tempoMap.add(new TempoPoint(at, value));
    return this;
  }

  public float tempoAt(float beat) {
    return bpmAt(beatValue(beat));
  }

  public MusicSynth gain(float value) {
    masterGain = positive(value);
    return this;
  }

  public float gain() {
    return masterGain;
  }

  public MusicSynth swing(float amount) {
    swing = clamp(amount, 0.0f, 0.49f);
    return this;
  }

  public float swing() {
    return swing;
  }

  public MusicSynth delay(float beats, float feedback) {
    delayBeats = positive(beats);
    delayFeedback = clamp(feedback, 0.0f, 0.95f);
    return this;
  }

  public float delayBeats() {
    return delayBeats;
  }

  public float delayFeedback() {
    return delayFeedback;
  }

  public MusicSynth title(String value) {
    title = require(value, "title");
    return this;
  }

  public String title() {
    return title;
  }

  public MusicSynth copyright(String value) {
    copyright = require(value, "copyright");
    return this;
  }

  public String copyright() {
    return copyright;
  }

  public Track track(Instrument instrument) {
    Track track = new Track(this, require(instrument, "instrument"));
    tracks.add(track);
    return track;
  }

  public int trackCount() {
    return tracks.size();
  }

  public Track track(int index) {
    if (index < 0 || index >= tracks.size())
      throw new IllegalArgumentException("track index out of range");
    return tracks.get(index);
  }

  public Track track(String name) {
    Track track = find(require(name, "name"));
    if (track == null) throw new IllegalArgumentException("no track named " + name);
    return track;
  }

  public Track find(String name) {
    if (name == null) throw new IllegalArgumentException("name cannot be null");
    for (Track track : tracks) {
      if (name.equals(track.name)) return track;
    }
    return null;
  }

  /** Snapshot of the tracks. Removing from the list does nothing; {@link Track#remove()} does. */
  public List<Track> tracks() {
    return Collections.unmodifiableList(new ArrayList<>(tracks));
  }

  public List<Track> tracksOn(int channel) {
    List<Track> found = new ArrayList<>();
    for (Track track : tracks) if (track.channel == channel) found.add(track);
    return Collections.unmodifiableList(found);
  }

  /** Melodic tracks only. Percussion keeps its kit program separate from this lookup. */
  public List<Track> tracksWithProgram(int program) {
    List<Track> found = new ArrayList<>();
    for (Track track : tracks) {
      if (!track.percussion && track.program == program) found.add(track);
    }
    return Collections.unmodifiableList(found);
  }

  public List<Track> tracksWithDrum(Drum drum) {
    if (drum == null) throw new IllegalArgumentException("drum cannot be null");
    List<Track> found = new ArrayList<>();
    for (Track track : tracks) if (track.drum == drum) found.add(track);
    return Collections.unmodifiableList(found);
  }

  public MusicSynth instrument(int index, Instrument instrument) {
    track(index).instrument(instrument);
    return this;
  }

  public MusicSynth instrument(String name, Instrument instrument) {
    track(name).instrument(instrument);
    return this;
  }

  /** Replaces the instrument on every non-percussion track that uses this GM program. */
  public MusicSynth replaceProgram(int program, Instrument instrument) {
    Instrument replacement = require(instrument, "instrument");
    for (Track track : tracks) {
      if (!track.percussion && track.program == program) track.instrument(replacement);
    }
    return this;
  }

  public MusicSynth replaceChannel(int channel, Instrument instrument) {
    Instrument replacement = require(instrument, "instrument");
    for (Track track : tracks) if (track.channel == channel) track.instrument(replacement);
    return this;
  }

  public MusicSynth replaceDrum(Drum drum, Instrument instrument) {
    if (drum == null) throw new IllegalArgumentException("drum cannot be null");
    Instrument replacement = require(instrument, "instrument");
    for (Track track : tracks) if (track.drum == drum) track.instrument(replacement);
    return this;
  }

  /**
   * Replaces every track's instrument. Return {@link Track#instrument()} from the function to leave
   * a track unchanged.
   */
  public MusicSynth mapInstruments(Function<Track, Instrument> choice) {
    require(choice, "choice");
    for (Track track : tracks) track.instrument(require(choice.apply(track), "instrument"));
    return this;
  }

  public MusicSynth removeTrack(int index) {
    if (index < 0 || index >= tracks.size())
      throw new IllegalArgumentException("track index out of range");
    tracks.remove(index);
    return this;
  }

  public MusicSynth removeChannel(int channel) {
    for (int i = tracks.size() - 1; i >= 0; i--) {
      if (tracks.get(i).channel == channel) tracks.remove(i);
    }
    return this;
  }

  public MusicSynth removeProgram(int program) {
    for (int i = tracks.size() - 1; i >= 0; i--) {
      Track track = tracks.get(i);
      if (!track.percussion && track.program == program) tracks.remove(i);
    }
    return this;
  }

  public MusicSynth removeDrum(Drum drum) {
    if (drum == null) throw new IllegalArgumentException("drum cannot be null");
    for (int i = tracks.size() - 1; i >= 0; i--) {
      if (tracks.get(i).drum == drum) tracks.remove(i);
    }
    return this;
  }

  /** Adds semitones to every track's transpose. */
  public MusicSynth transpose(int semitones) {
    for (Track track : tracks) track.transpose += semitones;
    return this;
  }

  public MusicSynth muteAll() {
    for (Track track : tracks) track.muted = true;
    return this;
  }

  public MusicSynth unmuteAll() {
    for (Track track : tracks) track.muted = false;
    return this;
  }

  /** Solos one track and clears solo on the others. */
  public MusicSynth solo(int index) {
    Track selected = track(index);
    for (Track track : tracks) track.solo = track == selected;
    return this;
  }

  public MusicSynth solo(String name) {
    Track selected = track(name);
    for (Track track : tracks) track.solo = track == selected;
    return this;
  }

  public MusicSynth clearSolo() {
    for (Track track : tracks) track.solo = false;
    return this;
  }

  public MusicSynth resetPerformance() {
    for (Track track : tracks) track.resetPerformance();
    return this;
  }

  public MusicSynth timeSignature(float beat, int numerator, int denominator) {
    if (numerator <= 0 || denominator <= 0)
      throw new IllegalArgumentException("time signature must be positive");
    timeSignatures.add(new TimeSignature(beatValue(beat), numerator, denominator));
    return this;
  }

  public List<TimeSignature> timeSignatures() {
    return Collections.unmodifiableList(new ArrayList<>(timeSignatures));
  }

  public MusicSynth keySignature(float beat, int accidentals, boolean minor) {
    if (accidentals < -7 || accidentals > 7)
      throw new IllegalArgumentException("accidentals must be between -7 and 7");
    keySignatures.add(new KeySignature(beatValue(beat), accidentals, minor));
    return this;
  }

  public List<KeySignature> keySignatures() {
    return Collections.unmodifiableList(new ArrayList<>(keySignatures));
  }

  public MusicSynth marker(float beat, String text, MarkerKind kind) {
    markers.add(new Marker(beatValue(beat), require(text, "text"), require(kind, "kind")));
    return this;
  }

  public List<Marker> markers() {
    return Collections.unmodifiableList(new ArrayList<>(markers));
  }

  public Marker findMarker(String text) {
    if (text == null) throw new IllegalArgumentException("text cannot be null");
    for (Marker marker : markers) if (text.equals(marker.text)) return marker;
    return null;
  }

  public Sound build() {
    boolean anySolo = false;
    for (Track track : tracks) if (track.solo) anySolo = true;
    float endBeat = 0.0f;
    float longestRelease = 0.0f;
    boolean anyReverb = false;
    for (Track track : tracks) {
      if (!audible(track, anySolo)) continue;
      for (Note note : track.notes) {
        longestRelease = Math.max(longestRelease, track.instrument.release * note.releaseScale);
        endBeat = Math.max(endBeat, swungBeat(note.beat) + note.beats);
        if (track.reverb > 0f) anyReverb = true;
      }
    }
    float duration = beatToSeconds(endBeat) + longestRelease;
    if (delayBeats > 0.0f && delayFeedback > 0.0f)
      duration += (beatToSeconds(delayBeats) - beatToSeconds(0f)) * 4.0f;
    if (anyReverb) duration += 1.4f;
    int frames = Math.max(1, (int) Math.ceil(duration * SAMPLE_RATE));
    float[] left = new float[frames];
    float[] right = new float[frames];
    float[] sendLeft = anyReverb ? new float[frames] : null;
    float[] sendRight = anyReverb ? new float[frames] : null;

    for (Track track : tracks) {
      if (!audible(track, anySolo)) continue;
      for (Note note : track.notes) renderNote(left, right, sendLeft, sendRight, track, note);
    }
    if (anyReverb) applyReverb(left, right, sendLeft, sendRight);
    applyDelay(left, right);

    float peak = 1.0f;
    for (int i = 0; i < frames; i++)
      peak = Math.max(peak, Math.max(Math.abs(left[i]), Math.abs(right[i])) * masterGain);
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

  private static boolean audible(Track track, boolean anySolo) {
    if (track.muted) return false;
    return !anySolo || track.solo;
  }

  private void renderNote(
      float[] left, float[] right, float[] sendLeft, float[] sendRight, Track track, Note note) {
    Instrument in = track.instrument;
    float startBeat = swungBeat(note.beat);
    float startSeconds = beatToSeconds(startBeat);
    float heldSeconds = beatToSeconds(note.beat + note.beats) - beatToSeconds(note.beat);
    if (heldSeconds < 0f) heldSeconds = 0f;
    float attack = in.attack * note.attackScale;
    float release = in.release * note.releaseScale;
    int start = (int) (startSeconds * SAMPLE_RATE);
    int length = (int) Math.ceil((heldSeconds + release) * SAMPLE_RATE);
    float lowpassHz = in.lowpass;
    if (note.brightness != 1f) {
      if (lowpassHz <= 0f) lowpassHz = 5000f;
      lowpassHz *= note.brightness;
    }
    float lpAlpha = lowpassHz > 0f ? filterAlpha(lowpassHz) : 0f;
    float hpAlpha = in.highpass > 0f ? filterAlpha(in.highpass) : 0f;
    float lpState = 0f;
    float hpState = 0f;
    float hpPrev = 0f;
    double phase = 0.0;
    double chorusPhase = 0.37;
    int noiseState = note.midiNote * 747796405 + Float.floatToRawIntBits(note.beat) + 1;
    if (noiseState == 0) noiseState = 1;
    float noiseHold = 0f;
    int noiseLeft = 0;
    int[] bendCursor = new int[1];
    float secondsPerBeat = note.beats > 0f && heldSeconds > 0f ? heldSeconds / note.beats : 0.5f;
    float leftGainBase = 0f;
    float rightGainBase = 0f;
    boolean wrotePan = false;

    for (int i = 0; i < length && start + i < left.length; i++) {
      float time = (float) i / SAMPLE_RATE;
      float beatOffset = secondsPerBeat > 0f ? time / secondsPerBeat : 0f;
      float amount = heldSeconds <= 0f ? 1f : Math.min(1f, time / heldSeconds);
      float semis =
          note.midiNote
              + track.transpose
              + track.detune
              + note.tuning
              + curve(note.bendBeats, note.bendSemis, beatOffset, bendCursor);
      float frequency = hz(semis);
      float depth = in.vibratoDepth + Mathf.lerpUnclamped(note.modulation, note.modulationEnd, amount);
      float rate = in.vibratoRate;
      if (rate <= 0f && depth > in.vibratoDepth) rate = 5.5f;
      float currentFrequency = frequency;
      if (depth > 0f && rate > 0f)
        currentFrequency *= 1f + depth * (float) Math.sin(2.0 * Math.PI * rate * time);

      int period = noisePeriod(currentFrequency);
      if ((in.waveform == Waveform.NOISE || in.noise > 0f) && noiseLeft <= 0) {
        noiseState = xorshift(noiseState);
        noiseHold = (noiseState & 0xffff) / 32768f - 1f;
        noiseLeft = period;
      }
      if (in.waveform == Waveform.NOISE || in.noise > 0f) noiseLeft--;
      float sample;
      if (in.waveform == Waveform.NOISE) {
        sample = noiseHold;
      } else {
        sample = oscillator(in.waveform, phase, in.duty);
        if (in.harmonics > 0f)
          sample =
              (sample
                      + oscillator(in.waveform, phase * 2.0, in.duty) * in.harmonics * 0.5f
                      + oscillator(in.waveform, phase * 3.0, in.duty) * in.harmonics * 0.25f)
                  / (1f + in.harmonics * 0.75f);
        if (in.noise > 0f) sample = sample * (1f - in.noise) + noiseHold * in.noise;
      }
      phase += currentFrequency / SAMPLE_RATE;
      if (phase >= 1048576.0) phase %= 1.0;

      if (track.chorus > 0f) {
        float wide;
        if (in.waveform == Waveform.NOISE) {
          wide = ((xorshift(noiseState ^ 0x5bd1e995) & 0xffff) / 32768f) - 1f;
        } else {
          float cents = 8f * track.chorus * (float) Math.sin(2.0 * Math.PI * 0.65 * time);
          float ratio = (float) Math.pow(2.0, cents / 1200.0);
          wide = oscillator(in.waveform, chorusPhase, in.duty);
          chorusPhase += currentFrequency * ratio / SAMPLE_RATE;
        }
        sample = (sample + wide * track.chorus) / (1f + track.chorus);
      }

      if (lpAlpha > 0f) {
        lpState += lpAlpha * (sample - lpState);
        sample = lpState;
      }
      if (hpAlpha > 0f) {
        float next = (1f - hpAlpha) * (hpState + sample - hpPrev);
        hpPrev = sample;
        hpState = next;
        sample = hpState;
      }

      float env = envelope(time, heldSeconds, attack, in.decay, in.sustain, release);
      float tremoloDepth = Math.max(in.tremoloDepth, note.tremolo);
      if (tremoloDepth > 0f) {
        float tremoloRate = in.tremoloRate > 0f ? in.tremoloRate : 6f;
        float tremolo = 0.5f + 0.5f * (float) Math.sin(2.0 * Math.PI * tremoloRate * time);
        env *= 1f - tremoloDepth * tremolo;
      }
      float expression = Mathf.lerpUnclamped(note.expression, note.expressionEnd, amount);
      float value = sample * env * note.velocity * expression * track.volume * in.gain;
      if (!wrotePan) {
        float pan = clamp(track.pan + note.pan, -1f, 1f);
        leftGainBase = (float) Math.sqrt((1f - pan) * 0.5f);
        rightGainBase = (float) Math.sqrt((1f + pan) * 0.5f);
        wrotePan = true;
      }
      if (start + i < 0) continue;
      left[start + i] += value * leftGainBase;
      right[start + i] += value * rightGainBase;
      if (sendLeft != null && track.reverb > 0f) {
        sendLeft[start + i] += value * leftGainBase * track.reverb;
        sendRight[start + i] += value * rightGainBase * track.reverb;
      }
    }
  }

  private static int noisePeriod(float frequency) {
    return frequency > 0f ? Math.max(1, (int) (SAMPLE_RATE / (frequency * 2f))) : 1;
  }

  private static int xorshift(int state) {
    int x = state;
    x ^= x << 13;
    x ^= x >>> 17;
    x ^= x << 5;
    return x == 0 ? 1 : x;
  }

  private static void applyReverb(float[] left, float[] right, float[] sendLeft, float[] sendRight) {
    int[] tapsLeft = {1116, 1356, 1422, 1617};
    int[] tapsRight = {1188, 1277, 1491, 1557};
    float feedback = 0.46f;
    float[][] memoryLeft = new float[tapsLeft.length][];
    float[][] memoryRight = new float[tapsRight.length][];
    int[] posLeft = new int[tapsLeft.length];
    int[] posRight = new int[tapsRight.length];
    for (int tap = 0; tap < tapsLeft.length; tap++) {
      memoryLeft[tap] = new float[tapsLeft[tap]];
      memoryRight[tap] = new float[tapsRight[tap]];
    }
    for (int i = 0; i < left.length; i++) {
      float wetLeft = 0f;
      float wetRight = 0f;
      for (int tap = 0; tap < tapsLeft.length; tap++) {
        float delayedLeft = memoryLeft[tap][posLeft[tap]];
        memoryLeft[tap][posLeft[tap]] = sendLeft[i] + delayedLeft * feedback;
        posLeft[tap]++;
        if (posLeft[tap] == tapsLeft[tap]) posLeft[tap] = 0;
        wetLeft += delayedLeft;
        float delayedRight = memoryRight[tap][posRight[tap]];
        memoryRight[tap][posRight[tap]] = sendRight[i] + delayedRight * feedback;
        posRight[tap]++;
        if (posRight[tap] == tapsRight[tap]) posRight[tap] = 0;
        wetRight += delayedRight;
      }
      left[i] += wetLeft * 0.16f;
      right[i] += wetRight * 0.16f;
    }
  }

  private void applyDelay(float[] left, float[] right) {
    int delaySamples = (int) ((beatToSeconds(delayBeats) - beatToSeconds(0f)) * SAMPLE_RATE);
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

  private float beatToSeconds(float beat) {
    if (tempoMap.isEmpty()) return beat * 60.0f / bpm;
    float seconds = 0f;
    float cursor = 0f;
    float rate = bpm;
    for (int i = 0; i < tempoMap.size(); i++) {
      TempoPoint point = tempoMap.get(i);
      if (point.beat <= cursor) {
        rate = point.bpm;
        continue;
      }
      float next = Math.min(point.beat, beat);
      if (next > cursor) {
        seconds += (next - cursor) * 60.0f / rate;
        cursor = next;
      }
      if (beat <= point.beat) return seconds;
      rate = point.bpm;
    }
    if (beat > cursor) seconds += (beat - cursor) * 60.0f / rate;
    return seconds;
  }

  private float bpmAt(float beat) {
    if (tempoMap.isEmpty()) return bpm;
    float rate = bpm;
    for (TempoPoint point : tempoMap) {
      if (point.beat > beat) break;
      rate = point.bpm;
    }
    return rate;
  }

  private static float curve(float[] xs, float[] ys, float x, int[] cursor) {
    if (xs == null || xs.length == 0) return 0f;
    if (x <= xs[0]) return ys[0];
    int i = cursor[0];
    while (i + 1 < xs.length && xs[i + 1] <= x) i++;
    cursor[0] = i;
    if (i + 1 >= xs.length) return ys[i];
    float span = xs[i + 1] - xs[i];
    if (span <= 0.0000001f) return ys[i];
    float t = (x - xs[i]) / span;
    return Mathf.lerpUnclamped(ys[i], ys[i + 1], t);
  }

  private static float envelope(
      float time, float held, float attack, float decay, float sustain, float release) {
    if (attack > 0.0f && time < attack) return time / attack;
    if (decay > 0.0f && time < attack + decay)
      return Mathf.lerpUnclamped(1.0f, sustain, (time - attack) / decay);
    if (time < held) return sustain;
    if (release <= 0.0f) return 0.0f;
    return sustain * Math.max(0.0f, 1.0f - (time - held) / release);
  }

  private static float oscillator(Waveform waveform, double phase, float duty) {
    double p = phase - Math.floor(phase);
    switch (waveform) {
      case SQUARE:
        return p < duty ? 1.0f : -1.0f;
      case SAW:
        return (float) (2.0 * p - 1.0);
      case TRIANGLE:
        return (float) (1.0 - 4.0 * Math.abs(p - 0.5));
      case NOISE:
        return 0f;
      case SINE:
      default:
        return (float) Math.sin(2.0 * Math.PI * p);
    }
  }

  private static float hz(double semis) {
    return (float) (440.0 * Math.pow(2.0, (semis - 69.0) / 12.0));
  }

  private static float filterAlpha(float cutoffHz) {
    cutoffHz = Mathf.clamp(cutoffHz, 0.0001f, SAMPLE_RATE * 0.49f);
    float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
    float dt = 1.0f / SAMPLE_RATE;
    return dt / (rc + dt);
  }

  public static int note(String name) {
    if (name == null || name.length() < 2) throw new IllegalArgumentException("invalid note name");
    String value = name.trim().toUpperCase();
    int semitone;
    switch (value.charAt(0)) {
      case 'C':
        semitone = 0;
        break;
      case 'D':
        semitone = 2;
        break;
      case 'E':
        semitone = 4;
        break;
      case 'F':
        semitone = 5;
        break;
      case 'G':
        semitone = 7;
        break;
      case 'A':
        semitone = 9;
        break;
      case 'B':
        semitone = 11;
        break;
      default:
        throw new IllegalArgumentException("invalid note name: " + name);
    }
    int index = 1;
    if (index < value.length() && (value.charAt(index) == '#' || value.charAt(index) == 'B')) {
      semitone += value.charAt(index) == '#' ? 1 : -1;
      index++;
    }
    int octave;
    try {
      octave = Integer.parseInt(value.substring(index));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("invalid note name: " + name);
    }
    return (octave + 1) * 12 + semitone;
  }

  public static int[] chord(int root, int... intervals) {
    if (intervals == null) throw new IllegalArgumentException("intervals cannot be null");
    int[] notes = new int[intervals.length + 1];
    notes[0] = root;
    for (int i = 0; i < intervals.length; i++) notes[i + 1] = root + intervals[i];
    return notes;
  }

  private static int checkedMidi(int midiNote) {
    if (midiNote < 0 || midiNote > 127)
      throw new IllegalArgumentException("midiNote must be between 0 and 127");
    return midiNote;
  }

  private static float checkedBeats(float value) {
    if (!Float.isFinite(value) || value <= 0.0f)
      throw new IllegalArgumentException("beats must be positive");
    return value;
  }

  private static float beatValue(float value) {
    if (!Float.isFinite(value) || value < 0.0f)
      throw new IllegalArgumentException("beat must be zero or positive");
    return value;
  }

  private static float positive(float value) {
    if (!Float.isFinite(value)) throw new IllegalArgumentException("value must be finite");
    return Math.max(0.0f, value);
  }

  private static float clamp01(float value) {
    return clamp(value, 0.0f, 1.0f);
  }

  private static float clamp(float value, float min, float max) {
    if (!Float.isFinite(value)) throw new IllegalArgumentException("value must be finite");
    return Mathf.clamp(value, min, max);
  }

  private static <T> T require(T value, String name) {
    if (value == null) throw new IllegalArgumentException(name + " cannot be null");
    return value;
  }
}
