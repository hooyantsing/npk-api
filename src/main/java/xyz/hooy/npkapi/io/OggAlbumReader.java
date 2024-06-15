package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.component.BufferedAudio;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OggAlbumReader extends AbstractAlbumReader{

    public OggAlbumReader(String path) {
        super(path);
    }

    @Override
    protected Album read(Path singleFile) throws IOException {
        byte[] audioData = Files.readAllBytes(path);
        BufferedAudio bufferedAudio = new BufferedAudio(path.toString(), audioData);
        return new Album(bufferedAudio);
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
