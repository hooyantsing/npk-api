package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAlbumReader extends AbstractReader {

    public AbstractAlbumReader(String path) {
        super(path);
    }

    @Override
    public final List<Album> read() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            Album album = read(path);
            album.setPath("TODO");
            return Collections.singletonList(album);
        } else if (Files.isDirectory(path)) {
            List<Album> albums = new ArrayList<>();
            List<Path> paths = walkFile();
            for (Path path : paths) {
                if (supportedFileSuffix(path)) {
                    Album album = read(path);
                    album.setPath("TODO");
                    albums.add(album);
                }
            }
            return albums;
        }
        return Collections.emptyList();
    }

    protected abstract Album read(Path singleFile) throws IOException;
}
