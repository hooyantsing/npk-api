package xyz.hooy.npkapi.io.impl;

import xyz.hooy.npkapi.component.BufferedAudio;
import xyz.hooy.npkapi.io.AlbumReader;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OggAlbumReader implements AlbumReader {
    @Override
    public Album read(String path) throws IOException {
        byte[] audioData = Files.readAllBytes(Paths.get(path));
        BufferedAudio bufferedAudio = new BufferedAudio(path, audioData);
        return new Album(bufferedAudio);
    }

    @Override
    public String suffix() {
        return "ogg";
    }
}
