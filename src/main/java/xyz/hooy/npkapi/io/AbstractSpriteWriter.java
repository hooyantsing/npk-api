package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractSpriteWriter extends AbstractWriter {

    public AbstractSpriteWriter(String path) {
        super(path);
    }

    @Override
    protected void write(Path path, List<Album> albums) throws IOException {
        for (Album album : albums) {
            for (Sprite sprite : album.getSprites()) {
                write(path, sprite);
            }
        }
    }

    protected abstract void write(Path path, Sprite sprite) throws IOException;
}
