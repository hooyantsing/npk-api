package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;

public interface AlbumReader {

    Album read(String path) throws IOException;

    String suffix();
}
