package engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QOADecoder {

    public static final int QOA_SLICE_LEN = 20;
    public static final int QOA_LMS_LEN = 4;
    public static final int QOA_MAGIC = 0x716f6166; // "qoaf"

    private static final int[][] DEQUANT_TAB = {
            {1, -1, 3, -3, 5, -5, 7, -7},
            {5, -5, 18, -18, 32, -32, 49, -49},
            {16, -16, 53, -53, 95, -95, 147, -147},
            {34, -34, 113, -113, 203, -203, 315, -315},
            {63, -63, 210, -210, 378, -378, 588, -588},
            {104, -104, 345, -345, 621, -621, 966, -966},
            {158, -158, 528, -528, 950, -950, 1477, -1477},
            {228, -228, 760, -760, 1368, -1368, 2128, -2128},
            {316, -316, 1053, -1053, 1895, -1895, 2947, -2947},
            {422, -422, 1405, -1405, 2529, -2529, 3934, -3934},
            {548, -548, 1828, -1828, 3290, -3290, 5117, -5117},
            {696, -696, 2320, -2320, 4176, -4176, 6496, -6496},
            {868, -868, 2893, -2893, 5207, -5207, 8099, -8099},
            {1064, -1064, 3548, -3548, 6386, -6386, 9933, -9933},
            {1286, -1286, 4288, -4288, 7718, -7718, 12005, -12005},
            {1536, -1536, 5120, -5120, 9216, -9216, 14336, -14336},
    };

    public static class LMS {
        int[] history = new int[QOA_LMS_LEN];
        int[] weights = new int[QOA_LMS_LEN];
    }

    private static int clampS16(int v) {
        if (v < -32768) return -32768;
        if (v > 32767) return 32767;
        return v;
    }

    private static int lmsPredict(LMS lms) {
        int prediction = 0;

        for (int i = 0; i < QOA_LMS_LEN; i++) {
            prediction += lms.weights[i] * lms.history[i];
        }

        return prediction >> 13;
    }

    private static void lmsUpdate(LMS lms, int sample, int residual) {
        int delta = residual >> 4;

        for (int i = 0; i < QOA_LMS_LEN; i++) {
            lms.weights[i] += (lms.history[i] < 0) ? -delta : delta;
        }

        System.arraycopy(lms.history, 1, lms.history, 0, QOA_LMS_LEN - 1);
        lms.history[QOA_LMS_LEN - 1] = sample;
    }

    public static Sound decode(byte[] data) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);

        long fileHeader = bb.getLong();

        int magic = (int) (fileHeader >>> 32);

        if (magic != QOA_MAGIC) {
            throw new IOException("Invalid QOA file");
        }

        int totalSamples = (int) (fileHeader & 0xffffffffL);

        long frameHeader = bb.getLong();

        int channels = (int) ((frameHeader >>> 56) & 0xff);
        int samplerate = (int) ((frameHeader >>> 32) & 0xffffff);

        bb.position(8);

        short[] pcm = new short[totalSamples * channels];

        LMS[] lms = new LMS[channels];

        for (int i = 0; i < channels; i++) {
            lms[i] = new LMS();
        }

        int sampleIndex = 0;

        while (bb.remaining() >= 8 && sampleIndex < totalSamples) {

            frameHeader = bb.getLong();

            channels = (int) ((frameHeader >>> 56) & 0xff);
            samplerate = (int) ((frameHeader >>> 32) & 0xffffff);
            int samples = (int) ((frameHeader >>> 16) & 0xffff);
            int frameSize = (int) (frameHeader & 0xffff);

            for (int c = 0; c < channels; c++) {

                long history = bb.getLong();
                long weights = bb.getLong();

                for (int i = 0; i < QOA_LMS_LEN; i++) {
                    lms[c].history[i] = (short) (history >>> 48);
                    history <<= 16;

                    lms[c].weights[i] = (short) (weights >>> 48);
                    weights <<= 16;
                }
            }

            for (int samplePos = 0; samplePos < samples; samplePos += QOA_SLICE_LEN) {

                for (int c = 0; c < channels; c++) {

                    long slice = bb.getLong();

                    int scalefactor = (int) ((slice >>> 60) & 0xF);

                    slice <<= 4;

                    int sliceEnd = Math.min(samplePos + QOA_SLICE_LEN, samples);

                    for (int s = samplePos; s < sliceEnd; s++) {

                        int predicted = lmsPredict(lms[c]);

                        int quantized = (int) ((slice >>> 61) & 0x7);

                        int dequantized =
                                DEQUANT_TAB[scalefactor][quantized];

                        int reconstructed =
                                clampS16(predicted + dequantized);

                        pcm[(sampleIndex + s) * channels + c] =
                                (short) reconstructed;

                        slice <<= 3;

                        lmsUpdate(
                                lms[c],
                                reconstructed,
                                dequantized
                        );
                    }
                }
            }

            sampleIndex += samples;
        }

        Sound result = new Sound();
        result.channels = channels;
        result.samplerate = samplerate;
        result.samples = totalSamples;
        result.pcm = pcm;

        return result;
    }

}