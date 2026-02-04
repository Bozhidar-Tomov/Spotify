package bg.sofia.uni.fmi.mjt.spotify.common.models;

import java.io.Serializable;

import bg.sofia.uni.fmi.mjt.spotify.common.net.AudioFormatPayload;

public record SongMetadata(
                String id,
                String title,
                String artist,
                String filePath,
                AudioFormatPayload format) implements Serializable {
}