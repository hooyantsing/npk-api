package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.constant.AlbumSuffixModes;
import xyz.hooy.npkapi.entity.Album;

import java.io.IOException;

public interface AlbumCoder extends Coder {

    Album load(String loadPath) throws IOException;

    void save(String savePath, Album album) throws IOException;

    AlbumSuffixModes support();
}
