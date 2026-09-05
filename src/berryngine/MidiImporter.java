package berryngine;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public final class MidiImporter {
    private static final int DEFAULT_MICROSECONDS_PER_BEAT = 500000;

    private MidiImporter() {
    }

    public static MusicSynth load(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        try {
            return convert(MidiSystem.getSequence(file));
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("Invalid MIDI file: " + file, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MusicSynth load(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        try {
            return convert(MidiSystem.getSequence(new ByteArrayInputStream(data)));
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException("Invalid MIDI data", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MusicSynth convert(Sequence sequence) {
        if (sequence == null) {
            throw new IllegalArgumentException("sequence cannot be null");
        }
        if (sequence.getDivisionType() != Sequence.PPQ) {
            throw new IllegalArgumentException("Only PPQ MIDI sequences are supported");
        }

        int resolution = sequence.getResolution();
        MusicSynth music = MusicSynth.create().tempo(60000000.0f / findInitialTempo(sequence));
        Map<Integer, MusicSynth.Track> outputTracks = new HashMap<>();
        Map<Integer, ArrayDeque<ActiveNote>> activeNotes = new HashMap<>();
        int[] programs = new int[16];

        for (javax.sound.midi.Track sourceTrack : sequence.getTracks()) {
            for (int i = 0; i < sourceTrack.size(); i++) {
                MidiEvent event = sourceTrack.get(i);
                MidiMessage message = event.getMessage();
                if (!(message instanceof ShortMessage)) {
                    continue;
                }
                ShortMessage shortMessage = (ShortMessage) message;
                int channel = shortMessage.getChannel();
                int command = shortMessage.getCommand();
                if (command == ShortMessage.PROGRAM_CHANGE) {
                    programs[channel] = shortMessage.getData1();
                    continue;
                }
                if (channel == 9) {
                    continue;
                }
                int note = shortMessage.getData1();
                int velocity = shortMessage.getData2();
                int key = channel * 128 + note;
                if (command == ShortMessage.NOTE_ON && velocity > 0) {
                    activeNotes.computeIfAbsent(key, ignored -> new ArrayDeque<>())
                            .addLast(new ActiveNote(event.getTick(), velocity, programs[channel]));
                } else if (command == ShortMessage.NOTE_OFF || command == ShortMessage.NOTE_ON) {
                    ArrayDeque<ActiveNote> starts = activeNotes.get(key);
                    if (starts == null || starts.isEmpty()) {
                        continue;
                    }
                    ActiveNote start = starts.removeFirst();
                    long tickLength = Math.max(1L, event.getTick() - start.tick);
                    MusicSynth.Track target = outputTracks.get(channel);
                    if (target == null) {
                        target = music.track(instrumentForProgram(start.program));
                        outputTracks.put(channel, target);
                    }
                    target.noteAt(
                            (float) start.tick / resolution,
                            note,
                            (float) tickLength / resolution,
                            start.velocity / 127.0f
                    );
                }
            }
        }
        return music;
    }

    private static int findInitialTempo(Sequence sequence) {
        long earliestTick = Long.MAX_VALUE;
        int tempo = DEFAULT_MICROSECONDS_PER_BEAT;
        for (javax.sound.midi.Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (!(message instanceof MetaMessage) || ((MetaMessage) message).getType() != 0x51 || event.getTick() > earliestTick) {
                    continue;
                }
                byte[] data = ((MetaMessage) message).getData();
                if (data.length == 3) {
                    earliestTick = event.getTick();
                    tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | data[2] & 0xff;
                }
            }
        }
        return tempo > 0 ? tempo : DEFAULT_MICROSECONDS_PER_BEAT;
    }

    private static MusicSynth.Instrument instrumentForProgram(int program) {
        if (program >= 32 && program <= 39) {
            return MusicSynth.Instrument.bass();
        }
        if (program >= 80 && program <= 87) {
            return MusicSynth.Instrument.lead();
        }
        if (program >= 88 && program <= 103) {
            return MusicSynth.Instrument.pad();
        }
        if (program >= 8 && program <= 15) {
            return MusicSynth.Instrument.bell();
        }
        return MusicSynth.Instrument.create()
                .waveform(MusicSynth.Waveform.TRIANGLE)
                .envelope(0.01f, 0.1f, 0.65f, 0.18f)
                .gain(0.55f);
    }

    private static final class ActiveNote {
        final long tick;
        final int velocity;
        final int program;

        ActiveNote(long tick, int velocity, int program) {
            this.tick = tick;
            this.velocity = velocity;
            this.program = program;
        }
    }
}
