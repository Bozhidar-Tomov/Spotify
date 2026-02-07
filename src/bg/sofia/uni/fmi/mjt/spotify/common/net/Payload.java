package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.io.Serializable;

public sealed interface Payload extends Serializable
                permits CollectionPayload, BinaryPayload, UserDtoPayload, AudioFormatPayload {
}
