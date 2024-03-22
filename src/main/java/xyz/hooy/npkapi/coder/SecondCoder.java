package xyz.hooy.npkapi.coder;

import xyz.hooy.npkapi.constant.ImgType;
import xyz.hooy.npkapi.entity.ImgEntity;

import java.io.IOException;

public interface SecondCoder extends Coder {

    ImgEntity load(String loadPath) throws IOException;

    void save(String savePath, ImgEntity imgEntity) throws IOException;

    boolean match(ImgType imgType);
}
