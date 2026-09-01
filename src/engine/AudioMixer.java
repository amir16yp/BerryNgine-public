package engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AudioMixer {

    public static final String MASTER = "master";
    public static final String SFX = "sfx";
    public static final String MUSIC = "music";
    public static final String UI = "ui";

    private final SoundSystem soundSystem;
    private final Map<String, Float> groupVolumes = new HashMap<>();
    private final Map<Integer, VoiceEntry> voices = new HashMap<>();

    private static final class VoiceEntry {
        final String group;
        float baseVolume;

        VoiceEntry(String group, float baseVolume) {
            this.group = group;
            this.baseVolume = baseVolume;
        }
    }

    public AudioMixer(SoundSystem soundSystem) {
        this.soundSystem = soundSystem;
    }

    public void addDefaultGroups() {
        addGroup(SFX);
        addGroup(MUSIC);
        addGroup(UI);
    }

    public void addGroup(String group) {
        groupVolumes.putIfAbsent(group, 1.0f);
    }

    public int play(String group, Sound file) {
        return play(group, file, 1.0f);
    }

    public int play(String group, Sound file, float volume) {
        return play(group, file, volume, false);
    }

    public int play(String group, Sound file, float volume, boolean loop) {
        if (soundSystem == null || file == null) {
            return -1;
        }

        float groupVolume = getGroupVolume(group);
        int handle = soundSystem.play(file, volume * groupVolume, loop);
        if (handle > 0) {
            voices.put(handle, new VoiceEntry(group, volume));
        }
        return handle;
    }

    public void setVolume(int handle, float volume) {
        VoiceEntry voice = voices.get(handle);
        if (voice == null) {
            soundSystem.setVolume(handle, volume);
            return;
        }
        voice.baseVolume = volume;
        soundSystem.setVolume(handle, volume * getGroupVolume(voice.group));
    }

    public void setGroupVolume(String group, float volume) {
        volume = Mathf.clamp01(volume);
        groupVolumes.put(group, volume);
        for (Map.Entry<Integer, VoiceEntry> entry : voices.entrySet()) {
            VoiceEntry voice = entry.getValue();
            if (voice.group.equals(group) && soundSystem.isPlaying(entry.getKey())) {
                soundSystem.setVolume(entry.getKey(), voice.baseVolume * volume);
            }
        }
    }

    public float getGroupVolume(String group) {
        return groupVolumes.getOrDefault(group, 1.0f);
    }

    public void setMasterVolume(float volume) {
        soundSystem.setMasterVolume(Mathf.clamp01(volume));
    }

    public float getMasterVolume() {
        return soundSystem.getMasterVolume();
    }

    public void stop(int handle) {
        soundSystem.stop(handle);
        voices.remove(handle);
    }

    public void pause(int handle) {
        soundSystem.pause(handle);
    }

    public void resume(int handle) {
        soundSystem.resume(handle);
    }

    public void stopGroup(String group) {
        Iterator<Map.Entry<Integer, VoiceEntry>> it = voices.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, VoiceEntry> entry = it.next();
            if (entry.getValue().group.equals(group)) {
                soundSystem.stop(entry.getKey());
                it.remove();
            }
        }
    }

    public void pauseGroup(String group) {
        for (Map.Entry<Integer, VoiceEntry> entry : voices.entrySet()) {
            if (entry.getValue().group.equals(group)) {
                soundSystem.pause(entry.getKey());
            }
        }
    }

    public void resumeGroup(String group) {
        for (Map.Entry<Integer, VoiceEntry> entry : voices.entrySet()) {
            if (entry.getValue().group.equals(group)) {
                soundSystem.resume(entry.getKey());
            }
        }
    }

    public void stopAll() {
        soundSystem.stopAll();
        voices.clear();
    }

    public void pauseAll() {
        soundSystem.pauseAll();
    }

    public void resumeAll() {
        soundSystem.resumeAll();
    }

    public void cleanup() {
        Iterator<Map.Entry<Integer, VoiceEntry>> it = voices.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, VoiceEntry> entry = it.next();
            if (!soundSystem.isPlaying(entry.getKey())) {
                it.remove();
            }
        }
    }

    public void close() {
        stopAll();
    }
}
