package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.IOException;
import java.util.List;

public class JpgCoder extends PngCoder implements Coder {
    @Override
    public List<ImgEntity> load(List<String> loadPaths) throws IOException {
        return super.load(loadPaths);
    }

    @Override
    public void save(String savePath, List<ImgEntity> imgEntities) throws IOException {
        super.save(savePath, imgEntities);
    }

    @Override
    public String getSuffix() {
        return "jpg";
    }
}
