package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractReader {

    protected final Path path;

    public AbstractReader(String path) {
        this.path = Paths.get(path);
    }

    public final List<Album> read() throws IOException {
        return doRead();
    }

    protected abstract List<Album> doRead() throws IOException;

    public abstract String suffix();

    protected List<Path> walkSupportedFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).filter(this::supportedFileSuffix).collect(Collectors.toList());
        }
    }

    protected boolean supportedFileSuffix(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).equalsIgnoreCase(suffix());
        }
        return false;
    }
}
