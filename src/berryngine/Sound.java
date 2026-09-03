package berryngine;

/**
 * Raw PCM sound data, playable through SoundSystem / AudioMixer.
 */
public class Sound {
    public int channels;
    public int samplerate;
    public int samples;
    public short[] pcm;
}
