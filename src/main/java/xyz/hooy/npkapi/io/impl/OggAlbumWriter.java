package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.io.AlbumWriter;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OggAlbumWriter implements AlbumWriter {
    @Override
    public void write(String path, Album album) throws IOException {
        byte[] audioData = album.getData();
        Files.write(Paths.get(path),audioData);
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
