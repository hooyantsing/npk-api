package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.Album;
import xyz.hooy.npkapi.util.NpkUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
public class NpkCoder implements Coder {

    public List<Album> load(String loadPath) throws IOException {
        List<Album> albums = NpkUtils.load(loadPath);
        log.info("Loaded file: {}.", loadPath);
        return albums;
    }

    public void save(String savePath, List<Album> albums) throws IOException {
        try {
            NpkUtils.save(savePath, albums);
            log.info("Saved file: {}.", savePath);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String suffix() {
        return "npk";
    }
}
