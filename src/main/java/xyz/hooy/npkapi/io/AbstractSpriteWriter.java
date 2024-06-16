package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.entity.Album;
import xyz.hooy.npkapi.npk.entity.Sprite;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public abstract class AbstractSpriteWriter extends AbstractWriter {

    public AbstractSpriteWriter(String path) {
        super(path);
    }

    @Override
    protected final void doWrite(List<Album> albums) throws IOException {
        for (Album album : albums) {
            for (int i = 0; i < album.getSprites().size(); i++) {
                String fileName = albumPathToFilePath(album.getPath(), i);
                Path writePath = Paths.get(path.toString(), fileName);
                writeSingleFile(writePath, album.getSprites().get(i));
                log.info("Write file: " + writePath);
            }
        }
    }

    private String albumPathToFilePath(String albumPath, int index) {
        albumPath = albumPath.replace("/", " ");
        if (albumPath.contains(".")) {
            albumPath = albumPath.substring(0, albumPath.lastIndexOf("."));
        }
        return albumPath + "#" + index + "." + suffix();
    }

    protected abstract void writeSingleFile(Path path, Sprite sprite) throws IOException;
}
