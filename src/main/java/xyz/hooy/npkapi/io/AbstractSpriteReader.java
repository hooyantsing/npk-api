package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public abstract class AbstractSpriteReader extends AbstractReader {

    public AbstractSpriteReader(String path) {
        super(path);
    }

    @Override
    protected final List<Album> doRead() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            Album album = new Album();
            album.setPath(filePathToAlbumPath(path.getFileName().toString()));
            album.addSprite(readSingleFile(path));
            log.info("Read file: " + path);
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkSupportedFiles();
            if (!paths.isEmpty()) {
                Map<String, Album> albumMap = new HashMap<>();
                for (Path path : paths) {
                    String albumPath = filePathToAlbumPath(path.getFileName().toString());
                    Album album = albumMap.computeIfAbsent(albumPath, k -> {
                        Album newAlbum = new Album();
                        newAlbum.setPath(albumPath);
                        return newAlbum;
                    });
                    album.addSprite(readSingleFile(path));
                    log.info("Read file: " + path);
                }
                return new ArrayList<>(albumMap.values());
            }
        }
        return Collections.emptyList();
    }

    private String filePathToAlbumPath(String filePath) {
        filePath = filePath.replace(" ", "/");
        if (filePath.contains("#")) {
            filePath = filePath.substring(0, filePath.lastIndexOf("#"));
        }
        return filePath + "." + AlbumSuffixModes.IMAGE.getSuffix();
    }

    protected abstract Sprite readSingleFile(Path path) throws IOException;
}
