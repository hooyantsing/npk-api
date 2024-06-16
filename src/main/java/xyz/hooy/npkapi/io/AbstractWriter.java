package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
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
        createDirectoriesIfNotExists(path);
        doWrite(albums);
    }

    protected abstract void doWrite(List<Album> albums) throws IOException;

    public abstract String suffix();

    protected void createDirectoriesIfNotExists(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            Files.createDirectories(path);
        }
    }
}
