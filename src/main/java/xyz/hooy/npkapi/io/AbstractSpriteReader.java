package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSpriteReader extends AbstractReader {

    public AbstractSpriteReader(String path) {
        super(path);
    }

    @Override
    public final List<Album> read() throws IOException {
        Album album = new Album();
        album.setPath("TODO");
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            album.addSprite(read(path));
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkFile();
            for (Path path : paths) {
                if (supportedFileSuffix(path)) {
                    album.addSprite(read(path));
                }
            }
            return Collections.singletonList(album);
        }
        return Collections.emptyList();
    }

    protected abstract Sprite read(Path singleFile) throws IOException;
}
