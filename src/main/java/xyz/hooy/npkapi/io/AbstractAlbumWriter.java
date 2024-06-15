package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractAlbumWriter extends AbstractWriter {

    public AbstractAlbumWriter(String path) {
        super(path);
    }

    @Override
    protected void write(Path path, List<Album> albums) throws IOException {
        for (Album album : albums) {
            write(path, album);
        }
    }

    protected abstract void write(Path path, Album album) throws IOException;
}
