package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.util.Collection;

public record CollectionPayload<T>(Collection<T> data) implements Payload {
}
