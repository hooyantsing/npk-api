package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class AbstractPackReader extends AbstractReader {

    public AbstractPackReader(String path) {
        super(path);
    }

    @Override
    protected final List<Album> doRead() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            List<Album> albums = readSingleFile(path);
            log.info("Read file: " + path);
            return albums;
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkSupportedFiles();
            if (!paths.isEmpty()) {
                List<Album> allAlbums = new ArrayList<>();
                for (Path path : paths) {
                    List<Album> albums = readSingleFile(path);
                    allAlbums.addAll(albums);
                    log.info("Read file: " + path);
                }
                return allAlbums;
            }
        }
        return Collections.emptyList();
    }

    protected abstract List<Album> readSingleFile(Path path) throws IOException;
}
