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
            album.setPath(filePathToAlbumPath(path.getFileName().toString()));
            log.info("Read file: " + path);
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkSupportedFiles();
            if (!paths.isEmpty()) {
                List<Album> albums = new ArrayList<>();
                for (Path path : paths) {
                    Album album = readSingleFile(path);
                    album.setPath(filePathToAlbumPath(path.getFileName().toString()));
                    albums.add(album);
                    log.info("Read file: " + path);
                }
                return albums;
            }
        }
        return Collections.emptyList();
    }

    private String filePathToAlbumPath(String filePath) {
        filePath = filePath.replace(" ", "/");
        if (filePath.contains(".")) {
            filePath = filePath.substring(0, filePath.lastIndexOf("."));
        }
        return filePath + "." + support().getSuffix();
    }

    protected abstract Album readSingleFile(Path path) throws IOException;

    public abstract AlbumSuffixModes support();
}
