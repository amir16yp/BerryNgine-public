package berryngine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

/**
 * Loads a Standard MIDI file into a {@link MusicSynth}.
 *
 * <p>PPQ and SMPTE timing, the tempo map, channel-10 drums, General MIDI programs, and the
 * performance controllers that change pitch, dynamics, and articulation are imported onto ordinary
 * tracks. The returned song can be edited with the {@link MusicSynth} track and instrument API.
 * Patches are procedural, not a sample-based GM player.
 */
public final class MidiImporter {
  private static final int CC_BANK = 0;
  private static final int CC_MODULATION = 1;
  private static final int CC_PORTAMENTO_TIME = 5;
  private static final int CC_DATA_MSB = 6;
  private static final int CC_VOLUME = 7;
  private static final int CC_PAN = 10;
  private static final int CC_EXPRESSION = 11;
  private static final int CC_BANK_LSB = 32;
  private static final int CC_DATA_LSB = 38;
  private static final int CC_SUSTAIN = 64;
  private static final int CC_PORTAMENTO = 65;
  private static final int CC_SOSTENUTO = 66;
  private static final int CC_SOFT = 67;
  private static final int CC_RELEASE = 72;
  private static final int CC_ATTACK = 73;
  private static final int CC_BRIGHTNESS = 74;
  private static final int CC_PORTAMENTO_NOTE = 84;
  private static final int CC_REVERB = 91;
  private static final int CC_TREMOLO = 92;
  private static final int CC_CHORUS = 93;
  private static final int CC_DATA_INC = 96;
  private static final int CC_DATA_DEC = 97;
  private static final int CC_NRPN_LSB = 98;
  private static final int CC_NRPN_MSB = 99;
  private static final int CC_RPN_LSB = 100;
  private static final int CC_RPN_MSB = 101;
  private static final int CC_ALL_SOUND_OFF = 120;
  private static final int CC_RESET_CONTROLLERS = 121;
  private static final int CC_ALL_NOTES_OFF = 123;
  private static final int CC_MONO = 126;
  private static final int CC_POLY = 127;

  private MidiImporter() {}

  public static MusicSynth load(File file) {
    if (file == null) throw new IllegalArgumentException("file cannot be null");
    try {
      return convert(MidiSystem.getSequence(file));
    } catch (InvalidMidiDataException e) {
      throw new IllegalArgumentException("Invalid MIDI file: " + file, e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static MusicSynth load(byte[] data) {
    if (data == null) throw new IllegalArgumentException("data cannot be null");
    try {
      return convert(MidiSystem.getSequence(new ByteArrayInputStream(data)));
    } catch (InvalidMidiDataException e) {
      throw new IllegalArgumentException("Invalid MIDI data", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static MusicSynth convert(Sequence sequence) {
    if (sequence == null) throw new IllegalArgumentException("sequence cannot be null");
    return new Converter(sequence).convert();
  }

  private static final class Converter {
    private final Sequence sequence;
    private final Timing timing;
    private final MusicSynth music = MusicSynth.create();
    private final Channel[] channels = new Channel[16];
    private final List<Live>[] active;
    private final Map<Part, MusicSynth.Track> parts = new HashMap<>();
    private final Map<MusicSynth.Track, Integer> sources = new IdentityHashMap<>();
    private final String[] trackNames;
    private final String[] instrumentNames;
    private final List<Event> events;

    @SuppressWarnings("unchecked")
    Converter(Sequence sequence) {
      this.sequence = sequence;
      events = collect(sequence);
      timing = Timing.from(sequence, events);
      trackNames = new String[sequence.getTracks().length];
      instrumentNames = new String[sequence.getTracks().length];
      active = (List<Live>[]) new List<?>[16];
      for (int i = 0; i < 16; i++) {
        channels[i] = new Channel();
        active[i] = new ArrayList<>();
      }
    }

    MusicSynth convert() {
      timing.apply(music);
      for (Event event : events) {
        MidiMessage message = event.message;
        if (message instanceof MetaMessage) meta(event, (MetaMessage) message);
        else if (message instanceof SysexMessage) sysex(event.tick, (SysexMessage) message);
        else if (message instanceof ShortMessage) voice(event);
      }
      long end = sequence.getTickLength();
      for (int channel = 0; channel < 16; channel++) {
        List<Live> open = new ArrayList<>(active[channel]);
        for (Live live : open) finish(live, Math.max(end, live.startTick + 1));
      }
      for (MusicSynth.Track track : music.tracks()) track.sortNotes();
      nameTracks();
      return music;
    }

    private void meta(Event event, MetaMessage message) {
      byte[] data = message.getData();
      float beat = (float) timing.beat(event.tick);
      switch (message.getType()) {
        case 0x01:
          addMarker(beat, text(data), MusicSynth.MarkerKind.TEXT);
          break;
        case 0x02:
          String copyright = text(data);
          if (!copyright.isEmpty() && music.copyright().isEmpty()) music.copyright(copyright);
          break;
        case 0x03:
          trackNames[event.track] = clean(text(data));
          if (event.track == 0 && music.title().isEmpty() && trackNames[event.track] != null)
            music.title(trackNames[event.track]);
          break;
        case 0x04:
          instrumentNames[event.track] = clean(text(data));
          break;
        case 0x05:
          addMarker(beat, text(data), MusicSynth.MarkerKind.LYRIC);
          break;
        case 0x06:
          addMarker(beat, text(data), MusicSynth.MarkerKind.MARKER);
          break;
        case 0x07:
          addMarker(beat, text(data), MusicSynth.MarkerKind.CUE);
          break;
        case 0x58:
          if (data.length >= 2) {
            int numerator = data[0] & 0xff;
            int power = data[1] & 0xff;
            int denominator = power >= 8 ? 256 : 1 << power;
            if (numerator > 0 && denominator > 0) music.timeSignature(beat, numerator, denominator);
          }
          break;
        case 0x59:
          if (data.length >= 2) {
            int accidentals = Mathf.clamp(data[0], -7, 7);
            music.keySignature(beat, accidentals, (data[1] & 0xff) != 0);
          }
          break;
        default:
          break;
      }
    }

    private void addMarker(float beat, String value, MusicSynth.MarkerKind kind) {
      if (value == null || value.isEmpty()) return;
      music.marker(beat, value, kind);
    }

    private void sysex(long tick, SysexMessage message) {
      byte[] data = message.getData();
      if (data.length >= 4
          && (data[0] & 0xff) == 0x7e
          && (data[2] & 0xff) == 0x09
          && ((data[3] & 0xff) == 0x01 || (data[3] & 0xff) == 0x02 || (data[3] & 0xff) == 0x03)) {
        resetSynth(tick);
        return;
      }
      if (data.length >= 8
          && (data[0] & 0xff) == 0x41
          && (data[2] & 0xff) == 0x42
          && (data[3] & 0xff) == 0x12
          && (data[4] & 0xff) == 0x40
          && (data[5] & 0xff) == 0x00
          && (data[6] & 0xff) == 0x7f) {
        resetSynth(tick);
        return;
      }
      if (data.length >= 7
          && (data[0] & 0xff) == 0x43
          && (data[2] & 0xff) == 0x4c
          && (data[3] & 0xff) == 0x00
          && (data[4] & 0xff) == 0x00
          && (data[5] & 0xff) == 0x7e) {
        resetSynth(tick);
        return;
      }
      if (data.length >= 6
          && (data[0] & 0xff) == 0x7f
          && (data[2] & 0xff) == 0x04
          && (data[3] & 0xff) == 0x01) {
        music.gain(0.8f * ((data[5] & 0x7f) / 127f));
      }
    }

    private void resetSynth(long tick) {
      for (int channel = 0; channel < 16; channel++) {
        finishAll(channel, tick, true);
        channels[channel].resetAll();
      }
    }

    private void voice(Event event) {
      ShortMessage message = (ShortMessage) event.message;
      int channel = message.getChannel();
      if (channel < 0 || channel > 15) return;
      int command = message.getCommand();
      int data1 = message.getData1();
      int data2 = message.getData2();
      switch (command) {
        case ShortMessage.NOTE_OFF:
          noteOff(channel, data1, event.tick, false);
          break;
        case ShortMessage.NOTE_ON:
          if (data2 == 0) noteOff(channel, data1, event.tick, false);
          else noteOn(event.track, channel, data1, data2, event.tick);
          break;
        case ShortMessage.POLY_PRESSURE:
          for (Live live : active[channel]) {
            if (live.note == data1) {
              live.polyPressure = data2;
              capture(live, false);
            }
          }
          break;
        case ShortMessage.CONTROL_CHANGE:
          control(channel, data1, data2, event.tick);
          break;
        case ShortMessage.PROGRAM_CHANGE:
          channels[channel].program = data1 & 127;
          break;
        case ShortMessage.CHANNEL_PRESSURE:
          channels[channel].pressure = data1 & 127;
          touch(channel);
          break;
        case ShortMessage.PITCH_BEND:
          channels[channel].bend = ((data2 & 127) << 7) | (data1 & 127);
          float semis = bendSemis(channels[channel]);
          for (Live live : active[channel]) addBend(live, event.tick, semis);
          break;
        default:
          break;
      }
    }

    private void noteOn(int sourceTrack, int channel, int note, int velocity, long tick) {
      Channel state = channels[channel];
      if (state.mono) finishAll(channel, tick, true);
      else finishKey(channel, note, tick, true);
      boolean drums = channel == 9;
      MusicSynth.Drum voice = drums ? MusicSynth.Drum.fromNote(note) : null;
      MusicSynth.Track track = ensureTrack(sourceTrack, channel, state, voice);
      int source = state.portamentoNote >= 0 ? state.portamentoNote : state.lastNote;
      Live live = new Live(channel, note, velocity, tick, track);
      if (state.portamento && state.portamentoTime > 0 && source >= 0 && source != note) {
        float seconds = (float) Math.pow(state.portamentoTime / 127.0, 2.0) * 2f;
        float bpm = music.tempoAt((float) timing.beat(tick));
        live.portamentoFrom = source;
        live.portamentoBeats = seconds * bpm / 60f;
      }
      state.lastNote = note;
      capture(live, true);
      live.pan = panOf(state.pan);
      addBend(live, tick, bendSemis(state));
      active[channel].add(live);
    }

    private void noteOff(int channel, int note, long tick, boolean force) {
      Live live = oldest(channel, note);
      if (live == null) return;
      Channel state = channels[channel];
      if (!force && (state.sustain || live.sostenuto)) {
        live.pending = true;
        return;
      }
      finish(live, tick);
    }

    private void finishKey(int channel, int note, long tick, boolean force) {
      List<Live> found = new ArrayList<>();
      for (Live live : active[channel]) if (live.note == note) found.add(live);
      for (Live live : found) {
        if (!force && (channels[channel].sustain || live.sostenuto)) live.pending = true;
        else finish(live, tick);
      }
    }

    private void finishAll(int channel, long tick, boolean force) {
      List<Live> found = new ArrayList<>(active[channel]);
      for (Live live : found) {
        if (!force && (channels[channel].sustain || live.sostenuto)) live.pending = true;
        else finish(live, tick);
      }
    }

    private Live oldest(int channel, int note) {
      for (Live live : active[channel]) if (live.note == note) return live;
      return null;
    }

    private void control(int channel, int controller, int value, long tick) {
      Channel state = channels[channel];
      int data = value & 127;
      switch (controller) {
        case CC_BANK:
          state.bankMsb = data;
          break;
        case CC_BANK_LSB:
          state.bankLsb = data;
          break;
        case CC_MODULATION:
          state.modulation = data;
          touch(channel);
          break;
        case CC_PORTAMENTO_TIME:
          state.portamentoTime = data;
          break;
        case CC_DATA_MSB:
          state.entryMsb = data;
          applyData(channel, tick);
          break;
        case CC_VOLUME:
          state.volume = data;
          touch(channel);
          break;
        case CC_PAN:
          state.pan = data;
          float pan = panOf(data);
          for (Live live : active[channel]) live.pan = pan;
          break;
        case CC_EXPRESSION:
          state.expression = data;
          touch(channel);
          break;
        case CC_DATA_LSB:
          state.entryLsb = data;
          applyData(channel, tick);
          break;
        case CC_SUSTAIN:
          state.sustain = data >= 64;
          if (!state.sustain) releasePending(channel, tick, false);
          break;
        case CC_PORTAMENTO:
          state.portamento = data >= 64;
          break;
        case CC_SOSTENUTO:
          if (data >= 64) {
            state.sostenuto = true;
            for (Live live : active[channel]) if (!live.pending) live.sostenuto = true;
          } else {
            state.sostenuto = false;
            releasePending(channel, tick, true);
          }
          break;
        case CC_SOFT:
          state.soft = data >= 64;
          touch(channel);
          break;
        case CC_RELEASE:
          state.releaseScale = data / 64f;
          touch(channel);
          break;
        case CC_ATTACK:
          state.attackScale = data / 64f;
          touch(channel);
          break;
        case CC_BRIGHTNESS:
          state.brightness = data / 64f;
          touch(channel);
          break;
        case CC_PORTAMENTO_NOTE:
          state.portamentoNote = data;
          break;
        case CC_REVERB:
          state.reverb = data;
          for (MusicSynth.Track track : parts.values()) {
            if (track.channel() == channel) track.reverb(data / 127f);
          }
          break;
        case CC_TREMOLO:
          state.tremolo = data;
          touch(channel);
          break;
        case CC_CHORUS:
          state.chorus = data;
          for (MusicSynth.Track track : parts.values()) {
            if (track.channel() == channel) track.chorus(data / 127f);
          }
          break;
        case CC_DATA_INC:
          bumpData(state, 1);
          applyData(channel, tick);
          break;
        case CC_DATA_DEC:
          bumpData(state, -1);
          applyData(channel, tick);
          break;
        case CC_NRPN_LSB:
        case CC_NRPN_MSB:
          state.nrpn = true;
          break;
        case CC_RPN_LSB:
          state.rpnLsb = data;
          state.nrpn = false;
          break;
        case CC_RPN_MSB:
          state.rpnMsb = data;
          state.nrpn = false;
          break;
        case CC_ALL_SOUND_OFF:
          finishAll(channel, tick, true);
          break;
        case CC_RESET_CONTROLLERS:
          state.resetControllers();
          for (Live live : new ArrayList<>(active[channel])) {
            addBend(live, tick, 0f);
            live.sostenuto = false;
            if (live.pending) finish(live, tick);
            else capture(live, false);
          }
          break;
        case CC_ALL_NOTES_OFF:
          finishAll(channel, tick, false);
          break;
        case CC_MONO:
          state.mono = true;
          finishAll(channel, tick, false);
          break;
        case CC_POLY:
          state.mono = false;
          finishAll(channel, tick, false);
          break;
        default:
          break;
      }
    }

    private void bumpData(Channel state, int direction) {
      int value = ((state.entryMsb & 127) << 7) | (state.entryLsb & 127);
      value = Mathf.clamp(value + direction, 0, 16383);
      state.entryMsb = (value >> 7) & 127;
      state.entryLsb = value & 127;
    }

    private void applyData(int channel, long tick) {
      Channel state = channels[channel];
      if (state.nrpn || (state.rpnMsb == 127 && state.rpnLsb == 127)) return;
      int rpn = ((state.rpnMsb & 127) << 7) | (state.rpnLsb & 127);
      switch (rpn) {
        case 0:
          state.bendRange = state.entryMsb + Math.min(state.entryLsb, 100) / 100f;
          float semis = bendSemis(state);
          for (Live live : active[channel]) addBend(live, tick, semis);
          break;
        case 1:
          int tuning = ((state.entryMsb & 127) << 7) | (state.entryLsb & 127);
          state.fineCents = (tuning - 8192) * (100f / 8192f);
          touch(channel);
          break;
        case 2:
          state.coarse = state.entryMsb - 64;
          touch(channel);
          break;
        case 5:
          state.modDepthCents = state.entryMsb * 100f + Math.min(state.entryLsb, 100);
          touch(channel);
          break;
        default:
          break;
      }
    }

    private void touch(int channel) {
      for (Live live : active[channel]) capture(live, false);
    }

    private void releasePending(int channel, long tick, boolean sostenuto) {
      List<Live> release = new ArrayList<>();
      for (Live live : active[channel]) {
        if (!sostenuto && live.pending && !live.sostenuto) release.add(live);
        if (sostenuto) {
          if (live.sostenuto && live.pending) release.add(live);
          live.sostenuto = false;
        }
      }
      for (Live live : release) finish(live, tick);
    }

    private MusicSynth.Track ensureTrack(
        int sourceTrack, int channel, Channel state, MusicSynth.Drum voice) {
      Part part = new Part(channel, state.bank(), state.program, voice);
      MusicSynth.Track track = parts.get(part);
      if (track != null) return track;
      MusicSynth.Instrument instrument =
          voice == null
              ? MusicSynth.Instrument.gm(state.program)
              : MusicSynth.Instrument.drum(voice, state.program);
      track = music.track(instrument);
      track.channel(channel).program(state.program).bank(state.bank());
      if (voice != null) track.drum(voice);
      track.volume(state.volume / 127f).pan(panOf(state.pan));
      track.reverb(state.reverb / 127f).chorus(state.chorus / 127f);
      parts.put(part, track);
      sources.put(track, sourceTrack);
      return track;
    }

    private void capture(Live live, boolean start) {
      Channel state = channels[live.channel];
      float pressure = Math.max(state.pressure, live.polyPressure);
      float level = level(state, live.track, pressure);
      float modulation = modulationDepth(state, pressure);
      live.expressionEnd = level;
      live.modulationEnd = modulation;
      live.tremolo = state.tremolo / 127f;
      live.brightness = state.brightness;
      live.attackScale = state.attackScale;
      live.releaseScale = state.releaseScale;
      live.tuning = state.coarse + state.fineCents / 100f;
      if (start) {
        live.expression = level;
        live.modulation = modulation;
      }
    }

    private static float level(Channel state, MusicSynth.Track track, float pressure) {
      float volume = state.volume / 127f;
      float base = track.volume();
      float relative = base > 0.0001f ? volume / base : 1f;
      float expression = state.expression / 127f;
      float soft = state.soft ? 0.7f : 1f;
      float touch = 1f + 0.25f * (pressure / 127f);
      return Mathf.clamp(relative * expression * soft * touch, 0f, 8f);
    }

    private static float modulationDepth(Channel state, float pressure) {
      float wheel = state.modulation / 127f;
      float touch = (pressure / 127f) * 0.5f;
      double ratio = Math.pow(2.0, (state.modDepthCents * Math.max(wheel, touch)) / 1200.0) - 1.0;
      return Mathf.clamp((float) ratio, 0f, 2f);
    }

    private static float bendSemis(Channel state) {
      return (state.bend - 8192) / 8192f * state.bendRange;
    }

    private void finish(Live live, long tick) {
      if (live.done) return;
      live.done = true;
      active[live.channel].remove(live);
      long end = Math.max(tick, live.startTick + 1);
      float start = (float) timing.beat(live.startTick);
      float length = (float) (timing.beat(end) - timing.beat(live.startTick));
      if (!(length > 0.0001f)) length = 0.0001f;
      MusicSynth.Note note = live.track.place(start, live.note, length, live.velocity / 127f);
      note.expression(live.expression, live.expressionEnd)
          .modulation(live.modulation, live.modulationEnd)
          .tremolo(live.tremolo)
          .brightness(live.brightness)
          .attackScale(live.attackScale)
          .releaseScale(live.releaseScale)
          .tuning(live.tuning)
          .pan(live.pan - live.track.pan());
      writeBend(note, live);
    }

    private void writeBend(MusicSynth.Note out, Live live) {
      if (live.bendTicks.isEmpty() && live.portamentoFrom < 0) return;
      double origin = timing.beat(live.startTick);
      List<Float> beats = new ArrayList<>();
      List<Float> semis = new ArrayList<>();
      for (int i = 0; i < live.bendTicks.size(); i++) {
        float beat = (float) (timing.beat(live.bendTicks.get(i)) - origin);
        if (beat < 0f) beat = 0f;
        push(beats, semis, beat, live.bendSemis.get(i) + glideOffset(live, beat));
      }
      if (live.portamentoFrom >= 0 && live.portamentoBeats > 0f) {
        push(beats, semis, 0f, (live.portamentoFrom - live.note) + wheelAt(live, 0f));
        push(beats, semis, live.portamentoBeats, wheelAt(live, live.portamentoBeats));
      }
      boolean audible = false;
      for (Float semi : semis) {
        if (Math.abs(semi) > 0.015f) audible = true;
      }
      if (!audible) return;
      float[] beatArray = new float[beats.size()];
      float[] semiArray = new float[semis.size()];
      for (int i = 0; i < beatArray.length; i++) {
        beatArray[i] = beats.get(i);
        semiArray[i] = semis.get(i);
      }
      out.bend(beatArray, semiArray);
    }

    private float wheelAt(Live live, float beat) {
      if (live.bendTicks.isEmpty()) return 0f;
      double origin = timing.beat(live.startTick);
      int last = live.bendTicks.size() - 1;
      float endBeat = (float) (timing.beat(live.bendTicks.get(last)) - origin);
      if (beat >= endBeat) return live.bendSemis.get(last);
      int index = 0;
      while (index + 1 < live.bendTicks.size()) {
        float next = (float) (timing.beat(live.bendTicks.get(index + 1)) - origin);
        if (next >= beat) break;
        index++;
      }
      float left = (float) (timing.beat(live.bendTicks.get(index)) - origin);
      if (beat <= left || index + 1 >= live.bendTicks.size()) return live.bendSemis.get(index);
      float right = (float) (timing.beat(live.bendTicks.get(index + 1)) - origin);
      float span = right - left;
      if (span <= 0.0000001f) return live.bendSemis.get(index);
      float t = (beat - left) / span;
      return Mathf.lerpUnclamped(live.bendSemis.get(index), live.bendSemis.get(index + 1), t);
    }

    private static float glideOffset(Live live, float beat) {
      if (live.portamentoFrom < 0 || live.portamentoBeats <= 0f || beat >= live.portamentoBeats)
        return 0f;
      return (live.portamentoFrom - live.note) * (1f - beat / live.portamentoBeats);
    }

    private static void push(List<Float> beats, List<Float> semis, float beat, float semi) {
      for (int i = 0; i < beats.size(); i++) {
        if (Math.abs(beats.get(i) - beat) <= 0.00001f) {
          semis.set(i, semi);
          return;
        }
        if (beats.get(i) > beat) {
          beats.add(i, beat);
          semis.add(i, semi);
          return;
        }
      }
      beats.add(beat);
      semis.add(semi);
    }

    private static void addBend(Live live, long tick, float semis) {
      int last = live.bendSemis.size() - 1;
      if (last >= 0 && live.bendTicks.get(last) == tick) {
        live.bendSemis.set(last, semis);
        return;
      }
      if (last >= 0 && Math.abs(live.bendSemis.get(last) - semis) < 0.0001f) return;
      live.bendTicks.add(tick);
      live.bendSemis.add(semis);
    }

    private void nameTracks() {
      for (MusicSynth.Track track : music.tracks()) {
        Integer source = sources.get(track);
        String fromTrack = source == null ? null : trackNames[source];
        String fromInstrument = source == null ? null : instrumentNames[source];
        String voice = voiceName(track);
        String base;
        if (fromTrack != null && track.percussion()) base = fromTrack + " " + voice;
        else if (fromTrack != null) base = fromTrack;
        else if (fromInstrument != null) base = fromInstrument;
        else base = voice;
        if (music.find(base) != null) {
          String detailed =
              base.equals(voice) ? base + " " + (track.channel() + 1) : base + " (" + voice + ")";
          base = detailed;
          int suffix = 2;
          while (music.find(base) != null) {
            base = detailed + " " + suffix;
            suffix++;
          }
        }
        track.name(base);
      }
    }

    private static String voiceName(MusicSynth.Track track) {
      if (track.percussion() && track.drum() != null)
        return track.drum().label() + kitLabel(track.program());
      if (track.program() >= 0) return MusicSynth.Instrument.gmName(track.program());
      return "Track";
    }

    private static String kitLabel(int program) {
      int kit = program == 25 ? 25 : program & ~7;
      switch (kit) {
        case 0:
          return "";
        case 8:
          return " Room";
        case 16:
          return " Power";
        case 24:
          return " Electronic";
        case 25:
          return " Analog";
        case 32:
          return " Jazz";
        case 40:
          return " Brush";
        case 48:
          return " Orchestra";
        case 56:
          return " SFX";
        default:
          return " " + program;
      }
    }
  }

  private static final class Timing {
    private final boolean ppq;
    private final double ticksPerBeat;
    private final double ticksPerSecond;
    private final double[] tempoTick;
    private final float[] tempoBpm;
    private final double[] spanStartSecond;
    private final double[] spanStartBeat;
    private final double[] spanBpm;
    private final double[] spanEndSecond;

    private Timing(
        boolean ppq,
        double ticksPerBeat,
        double ticksPerSecond,
        double[] tempoTick,
        float[] tempoBpm,
        double[] spanStartSecond,
        double[] spanStartBeat,
        double[] spanBpm,
        double[] spanEndSecond) {
      this.ppq = ppq;
      this.ticksPerBeat = ticksPerBeat;
      this.ticksPerSecond = ticksPerSecond;
      this.tempoTick = tempoTick;
      this.tempoBpm = tempoBpm;
      this.spanStartSecond = spanStartSecond;
      this.spanStartBeat = spanStartBeat;
      this.spanBpm = spanBpm;
      this.spanEndSecond = spanEndSecond;
    }

    static Timing from(Sequence sequence, List<Event> events) {
      float division = sequence.getDivisionType();
      int resolution = sequence.getResolution();
      if (resolution <= 0) throw new IllegalArgumentException("MIDI resolution must be positive");
      if (division != Sequence.PPQ && !(division > 0f))
        throw new IllegalArgumentException("Unsupported MIDI division type");
      List<double[]> tempos = new ArrayList<>();
      for (Event event : events) {
        if (!(event.message instanceof MetaMessage)) continue;
        MetaMessage meta = (MetaMessage) event.message;
        if (meta.getType() != 0x51) continue;
        byte[] data = meta.getData();
        if (data.length != 3) continue;
        int microseconds = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
        if (microseconds <= 0) continue;
        double[] tempo = new double[] {event.tick, 60000000.0 / microseconds};
        if (!tempos.isEmpty() && tempos.get(tempos.size() - 1)[0] == tempo[0])
          tempos.set(tempos.size() - 1, tempo);
        else tempos.add(tempo);
      }
      if (division == Sequence.PPQ) {
        double[] ticks = new double[tempos.size()];
        float[] bpms = new float[tempos.size()];
        for (int i = 0; i < tempos.size(); i++) {
          ticks[i] = tempos.get(i)[0];
          bpms[i] = (float) tempos.get(i)[1];
        }
        return new Timing(true, resolution, 0, ticks, bpms, null, null, null, null);
      }
      return smpte(division * (double) resolution, tempos);
    }

    private static Timing smpte(double ticksPerSecond, List<double[]> tempos) {
      double second = 0;
      double beat = 0;
      double bpm = 120;
      int index = 0;
      while (index < tempos.size() && tempos.get(index)[0] / ticksPerSecond <= 0) {
        bpm = tempos.get(index)[1];
        index++;
      }
      List<double[]> spans = new ArrayList<>();
      while (index < tempos.size()) {
        double eventSecond = tempos.get(index)[0] / ticksPerSecond;
        double eventBpm = tempos.get(index)[1];
        index++;
        if (eventSecond <= second) {
          bpm = eventBpm;
          continue;
        }
        spans.add(new double[] {second, beat, bpm, eventSecond});
        beat += (eventSecond - second) * bpm / 60.0;
        second = eventSecond;
        bpm = eventBpm;
      }
      spans.add(new double[] {second, beat, bpm, Double.POSITIVE_INFINITY});
      double[] startSecond = new double[spans.size()];
      double[] startBeat = new double[spans.size()];
      double[] spanBpm = new double[spans.size()];
      double[] endSecond = new double[spans.size()];
      for (int i = 0; i < spans.size(); i++) {
        startSecond[i] = spans.get(i)[0];
        startBeat[i] = spans.get(i)[1];
        spanBpm[i] = spans.get(i)[2];
        endSecond[i] = spans.get(i)[3];
      }
      return new Timing(
          false, 0, ticksPerSecond, null, null, startSecond, startBeat, spanBpm, endSecond);
    }

    void apply(MusicSynth target) {
      if (ppq) {
        target.tempo(120f);
        for (int i = 0; i < tempoBpm.length; i++) {
          float beat = (float) (tempoTick[i] / ticksPerBeat);
          if (beat <= 0f) target.tempo(tempoBpm[i]);
          else target.tempoAt(beat, tempoBpm[i]);
        }
        return;
      }
      target.tempo((float) spanBpm[0]);
      for (int i = 1; i < spanBpm.length; i++)
        target.tempoAt((float) spanStartBeat[i], (float) spanBpm[i]);
    }

    double beat(long tick) {
      if (ppq) return tick / ticksPerBeat;
      double seconds = tick / ticksPerSecond;
      for (int i = 0; i < spanBpm.length; i++) {
        if (seconds <= spanEndSecond[i] || i == spanBpm.length - 1)
          return spanStartBeat[i] + (seconds - spanStartSecond[i]) * spanBpm[i] / 60.0;
      }
      return 0;
    }
  }

  private static List<Event> collect(Sequence sequence) {
    List<Event> events = new ArrayList<>();
    javax.sound.midi.Track[] tracks = sequence.getTracks();
    int order = 0;
    for (int track = 0; track < tracks.length; track++) {
      javax.sound.midi.Track source = tracks[track];
      for (int i = 0; i < source.size(); i++) {
        MidiEvent event = source.get(i);
        events.add(new Event(event.getTick(), order++, track, event.getMessage()));
      }
    }
    events.sort(
        (a, b) -> {
          int compare = Long.compare(a.tick, b.tick);
          if (compare != 0) return compare;
          compare = Integer.compare(priority(a.message), priority(b.message));
          if (compare != 0) return compare;
          return Integer.compare(a.order, b.order);
        });
    return events;
  }

  private static int priority(MidiMessage message) {
    if (message instanceof MetaMessage) return 1;
    if (message instanceof SysexMessage) return 2;
    if (message instanceof ShortMessage) {
      ShortMessage shortMessage = (ShortMessage) message;
      switch (shortMessage.getCommand()) {
        case ShortMessage.NOTE_OFF:
          return 4;
        case ShortMessage.NOTE_ON:
          return shortMessage.getData2() == 0 ? 4 : 6;
        case ShortMessage.CONTROL_CHANGE:
        case ShortMessage.PROGRAM_CHANGE:
        case ShortMessage.PITCH_BEND:
        case ShortMessage.CHANNEL_PRESSURE:
        case ShortMessage.POLY_PRESSURE:
          return 5;
        default:
          return 7;
      }
    }
    return 7;
  }

  private static String text(byte[] data) {
    if (data == null || data.length == 0) return "";
    int length = data.length;
    while (length > 0 && data[length - 1] == 0) length--;
    return new String(data, 0, length, StandardCharsets.ISO_8859_1).trim();
  }

  private static String clean(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static float panOf(int value) {
    value = Mathf.clamp(value, 0, 127);
    return Mathf.clamp((value - 64) / 63f, -1f, 1f);
  }

  private static final class Event {
    final long tick;
    final int order;
    final int track;
    final MidiMessage message;

    Event(long tick, int order, int track, MidiMessage message) {
      this.tick = tick;
      this.order = order;
      this.track = track;
      this.message = message;
    }
  }

  private static final class Part {
    final int channel;
    final int bank;
    final int program;
    final MusicSynth.Drum drum;

    Part(int channel, int bank, int program, MusicSynth.Drum drum) {
      this.channel = channel;
      this.bank = bank;
      this.program = program;
      this.drum = drum;
    }

    public boolean equals(Object other) {
      if (!(other instanceof Part)) return false;
      Part part = (Part) other;
      return channel == part.channel
          && bank == part.bank
          && program == part.program
          && drum == part.drum;
    }

    public int hashCode() {
      int hash = channel;
      hash = hash * 31 + bank;
      hash = hash * 31 + program;
      hash = hash * 31 + (drum == null ? 0 : drum.ordinal() + 1);
      return hash;
    }
  }

  private static final class Channel {
    int volume = 100;
    int pan = 64;
    int expression = 127;
    int modulation;
    int portamentoTime;
    boolean portamento;
    int portamentoNote = -1;
    boolean sustain;
    boolean sostenuto;
    boolean soft;
    int pressure;
    int bend = 8192;
    float bendRange = 2f;
    float fineCents;
    int coarse;
    float modDepthCents = 50f;
    int program;
    int bankMsb;
    int bankLsb;
    int reverb;
    int chorus;
    int tremolo;
    float attackScale = 1f;
    float releaseScale = 1f;
    float brightness = 1f;
    boolean mono;
    boolean nrpn;
    int rpnMsb = 127;
    int rpnLsb = 127;
    int entryMsb;
    int entryLsb;
    int lastNote = -1;

    int bank() {
      return (bankMsb << 7) | bankLsb;
    }

    void resetControllers() {
      modulation = 0;
      expression = 127;
      sustain = false;
      sostenuto = false;
      soft = false;
      portamento = false;
      pressure = 0;
      bend = 8192;
      tremolo = 0;
      nrpn = false;
      rpnMsb = 127;
      rpnLsb = 127;
      entryMsb = 0;
      entryLsb = 0;
    }

    void resetAll() {
      resetControllers();
      volume = 100;
      pan = 64;
      portamentoTime = 0;
      portamentoNote = -1;
      bendRange = 2f;
      fineCents = 0f;
      coarse = 0;
      modDepthCents = 50f;
      program = 0;
      bankMsb = 0;
      bankLsb = 0;
      reverb = 0;
      chorus = 0;
      attackScale = 1f;
      releaseScale = 1f;
      brightness = 1f;
      mono = false;
      lastNote = -1;
    }
  }

  private static final class Live {
    final int channel;
    final int note;
    final int velocity;
    final long startTick;
    final MusicSynth.Track track;
    boolean pending;
    boolean sostenuto;
    boolean done;
    int polyPressure;
    float expression = 1f;
    float expressionEnd = 1f;
    float modulation;
    float modulationEnd;
    float tremolo;
    float brightness = 1f;
    float attackScale = 1f;
    float releaseScale = 1f;
    float tuning;
    float pan;
    int portamentoFrom = -1;
    float portamentoBeats;
    final List<Long> bendTicks = new ArrayList<>();
    final List<Float> bendSemis = new ArrayList<>();

    Live(int channel, int note, int velocity, long startTick, MusicSynth.Track track) {
      this.channel = channel;
      this.note = note;
      this.velocity = velocity;
      this.startTick = startTick;
      this.track = track;
    }
  }
}
