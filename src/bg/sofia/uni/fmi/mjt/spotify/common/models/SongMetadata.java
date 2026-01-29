package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;
import java.nio.file.Path;

record SongMetadata(
        String id,
        String title,
        String artist,
        Path filePath,
        float sampleRate,
        int sampleSizeInBits,
        int channels,
        int frameSize,
        float frameRate,
        boolean bigEndian,
        String encoding) implements Serializable {}