package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class AbstractWriter {

    protected final Path path;

    public AbstractWriter(String path) {
        this.path = Paths.get(path);
    }

    public final void write(List<Album> albums) throws IOException {
        if (Files.isDirectory(path)) {
            write(path, albums);
        }
        throw new UnsupportedEncodingException("TODO");
    }

    protected abstract void write(Path path, List<Album> albums) throws IOException;

    public abstract String suffix();
}
