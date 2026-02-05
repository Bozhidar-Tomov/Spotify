package bg.sofia.uni.fmi.mjt.spotify.server.commands;

import bg.sofia.uni.fmi.mjt.spotify.server.SpotifySystem;
import bg.sofia.uni.fmi.mjt.spotify.server.streaming.AudioStreamer;
import bg.sofia.uni.fmi.mjt.spotify.common.models.Track;
import bg.sofia.uni.fmi.mjt.spotify.common.net.Response;
import bg.sofia.uni.fmi.mjt.spotify.common.net.ResponseSender;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.List;
import java.util.Arrays;

public class PlaySongCommand implements Command {

    @Override
    public Response execute(List<String> args, SpotifySystem system, ResponseSender client) {
        if (client == null || system == null || !system.isRunning()) {
            return Response.err();
        }

        AudioStreamer streamer = new AudioStreamer(client);

        if (args == null || args.size() != 1) {
            return new Response(400, "Usage: play <song name>", null);
        }

        List<Track> tracks = system.getTracks(args.get(0).strip().toLowerCase());

        if (tracks == null || tracks.isEmpty()) {
            return new Response(404, "Song '" + args.getFirst() + "' not found.", null);
        }

        if (tracks.size() != 1) {
            return new Response(400, "Multiple songs with title '" + args.getFirst() + "' found", null);
        }

        Track track = tracks.getFirst();

        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(track.metadata().filePath()))) {
            streamer.startStream(audioStream.getFormat(), "Playing " + track.metadata().title());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                byte[] chunk = (bytesRead == buffer.length) ? buffer : Arrays.copyOf(buffer, bytesRead);
                streamer.sendChunk(chunk);
            }

            streamer.endStream();
            return new Response(200, "Playback finished", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.err();
        }
    }
}
