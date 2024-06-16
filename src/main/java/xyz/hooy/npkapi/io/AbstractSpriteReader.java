package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class AbstractSpriteReader extends AbstractReader {

    public AbstractSpriteReader(String path) {
        super(path);
    }

    @Override
    protected final List<Album> doRead() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            Album album = new Album();
            album.setPath(filePathToAlbumPath(path.getParent().getFileName().toString()) + "." + AlbumSuffixModes.IMAGE.getSuffix());
            album.addSprite(readSingleFile(path));
            log.info("Read file: " + path);
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkSupportedFiles();
            if (!paths.isEmpty()) {
                Album album = new Album();
                album.setPath(filePathToAlbumPath(path.getParent().getFileName().toString()) + "." + AlbumSuffixModes.IMAGE.getSuffix());
                for (Path path : paths) {
                    album.addSprite(readSingleFile(path));
                    log.info("Read file: " + path);
                }
                return Collections.singletonList(album);
            }
        }
        return Collections.emptyList();
    }

    protected abstract Sprite readSingleFile(Path path) throws IOException;
}
