package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OggAlbumWriter extends AbstractAlbumWriter{

    public OggAlbumWriter(String path) {
        super(path);
    }

    @Override
    protected void write(Path path, Album album) throws IOException {
        byte[] audioData = album.getData();
        Files.write(path,audioData);
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
