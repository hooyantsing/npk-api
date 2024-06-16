package xyz.hooy.npkapi.io;

import xyz.hooy.npkapi.npk.NpkCore;
import xyz.hooy.npkapi.npk.entity.Album;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NpkReader extends AbstractReader {

    public NpkReader(String path) {
        super(path);
    }

    @Override
    public final List<Album> doRead() throws IOException {
        if (Files.isRegularFile(path) && supportedFileSuffix(path)) {
            return NpkCore.load(path.toString());
        } else if (Files.isDirectory(path)) {
            List<Path> paths = walkSupportedFiles();
            if (!paths.isEmpty()) {
                List<Album> allAlbums = new ArrayList<>();
                for (Path path : paths) {
                    List<Album> albums = NpkCore.load(path.toString());
                    allAlbums.addAll(albums);
                }
                return allAlbums;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
