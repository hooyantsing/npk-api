package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class AbstractAlbumReader extends AbstractReader {

    public AbstractAlbumReader(String path) {
        super(path);
    }

    @Override
    protected final List<Album> doRead() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            Album album = readSingleFile(path);
            album.setPath(replaceFileSuffix(path, support().getSuffix()));
            log.info("Read file: " + path);
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Album> albums = new ArrayList<>();
            List<Path> paths = walkFile();
            for (Path path : paths) {
                if (supportedFileSuffix(path)) {
                    Album album = readSingleFile(path);
                    album.setPath(replaceFileSuffix(path, support().getSuffix()));
                    albums.add(album);
                    log.info("Read file: " + path);
                }
            }
            return albums;
        }
        return Collections.emptyList();
    }

    protected abstract Album readSingleFile(Path path) throws IOException;

    public abstract AlbumSuffixModes support();
}
