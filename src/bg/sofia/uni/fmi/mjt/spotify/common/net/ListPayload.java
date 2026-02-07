package bg.sofia.uni.fmi.mjt.spotify.common.net;

import java.util.List;

public record ListPayload<T>(List<T> data) implements Payload{
}
