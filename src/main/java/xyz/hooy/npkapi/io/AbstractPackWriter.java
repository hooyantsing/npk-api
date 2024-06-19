package xyz.hooy.npkapi.io;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public abstract class AbstractPackWriter extends AbstractWriter {

    public AbstractPackWriter(String path) {
        super(path);
    }

    @Override
    protected final void doWrite(List<Album> albums) throws IOException {
        Path writePath = path;
        writeSingleFile(writePath, albums);
        log.info("Write file: " + writePath);
    }

    protected abstract void writeSingleFile(Path path, List<Album> albums) throws IOException;
}
