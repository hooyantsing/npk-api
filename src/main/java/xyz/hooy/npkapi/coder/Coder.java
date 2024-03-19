package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.IOException;
import java.util.List;

public interface Coder {

    List<ImgEntity> load(List<String> loadPaths) throws IOException;

    void save(String savePath, List<ImgEntity> imgEntities) throws IOException;

    String getSuffix();
}
