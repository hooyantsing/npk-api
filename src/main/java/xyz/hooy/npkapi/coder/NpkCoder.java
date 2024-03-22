package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.util.NpkUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
public class NpkCoder implements Coder {

    public List<ImgEntity> load(String loadPath) throws IOException {
        List<ImgEntity> load = NpkUtils.load(loadPath);
        log.info("Loaded file: {}.", loadPath);
        return load;
    }

    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        try {
            NpkUtils.save(savePath, imgEntities);
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
