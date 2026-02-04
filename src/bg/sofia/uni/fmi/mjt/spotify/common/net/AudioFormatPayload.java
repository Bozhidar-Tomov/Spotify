package bg.sofia.uni.fmi.mjt.spotify.common.net;

import javax.sound.sampled.AudioFormat;

public record AudioFormatPayload(
        String encoding,
        float sampleRate,
        int sampleSizeInBits,
        int channels,
        int frameSize,
        float frameRate,
        boolean bigEndian) implements Payload {

    public static AudioFormatPayload from(AudioFormat format) {
        return new AudioFormatPayload(
                format.getEncoding().toString(),
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels(),
                format.getFrameSize(),
                format.getFrameRate(),
                format.isBigEndian());
    }

    public AudioFormat toAudioFormat() {
        return new AudioFormat(new AudioFormat.Encoding(encoding), sampleRate, sampleSizeInBits, channels, frameSize,
                frameRate, bigEndian);
    }
}
