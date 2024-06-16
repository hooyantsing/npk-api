package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public abstract class AbstractAlbumWriter extends AbstractWriter {

    public AbstractAlbumWriter(String path) {
        super(path);
    }

    @Override
    protected final void doWrite(List<Album> albums) throws IOException {
        for (Album album : albums) {
            Path writePath = Paths.get(path.toString(), album.getPath() + "." + suffix());
            writeSingleFile(writePath, album);
            log.info("Write file: " + writePath);
        }
    }

    protected abstract void writeSingleFile(Path path, Album album) throws IOException;
}
