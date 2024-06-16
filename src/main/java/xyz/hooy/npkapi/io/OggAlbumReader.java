package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.component.BufferedAudio;
import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OggAlbumReader extends AbstractAlbumReader{

    public OggAlbumReader(String path) {
        super(path);
    }

    @Override
    protected Album readSingleFile(Path path) throws IOException {
        byte[] audioData = Files.readAllBytes(path);
        BufferedAudio bufferedAudio = new BufferedAudio(path.toString(), audioData);
        return new Album(bufferedAudio);
    }

    @Override
    public AlbumSuffixModes support() {
        return AlbumSuffixModes.AUDIO;
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
