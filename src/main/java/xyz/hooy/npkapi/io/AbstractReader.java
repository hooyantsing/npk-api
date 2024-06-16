package xyz.hooy.npkapi.io;

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

    protected List<Path> walkFile() throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }
    }

    protected boolean supportedFileSuffix(Path path) {
        final String suffixMark = ".";
        String fileName = path.getFileName().toString();
        if (fileName.contains(suffixMark)) {
            return fileName.substring(fileName.lastIndexOf(suffixMark) + 1).equalsIgnoreCase(suffix());
        }
        return false;
    }

    protected String replaceFileSuffix(Path path, String suffix) {
        final String suffixMark = ".";
        String fileName = path.getFileName().toString();
        if (fileName.contains(suffixMark)) {
            return fileName.substring(0, fileName.lastIndexOf(suffixMark) + 1) + "." + suffix;
        }
        return fileName + "." + suffix;
    }
}
