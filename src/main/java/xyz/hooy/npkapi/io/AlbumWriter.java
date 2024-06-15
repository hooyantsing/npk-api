package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;

public interface AlbumWriter {

    void write(String path, Album album) throws IOException;

    String suffix();
}
