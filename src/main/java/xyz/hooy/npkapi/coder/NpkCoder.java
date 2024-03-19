package xyz.hooy.npkapi.coder;

import lombok.extern.slf4j.Slf4j;
import xyz.hooy.npkapi.entity.ImgEntity;
import xyz.hooy.npkapi.util.NpkUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class NpkCoder implements Coder {
    @Override
    public List<ImgEntity> load(List<String> loadPaths) throws IOException {
        List<ImgEntity> imgEntities = new ArrayList<>();
        for (String loadPath : loadPaths) {
            List<ImgEntity> load = NpkUtils.load(loadPath);
            imgEntities.addAll(load);
            log.info("Loaded file: {}", loadPath);
        }
        return imgEntities;
    }

    @Override
    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        try {
            String npkName = UUID.randomUUID() + ".npk";
            String savedName = Paths.get(savePath, npkName).toString();
            NpkUtils.save(savedName, imgEntities);
            log.info("Saved file: {}", savedName);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getSuffix() {
        return "npk";
    }
}
