package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultNpk implements Npk {

    protected Access delegateNpkAccess;

    protected List<Object> textures = new ArrayList<>();

    public DefaultNpk() {
        this.delegateNpkAccess = new DefaultNpkAccess(this);
    }

    @Override
    public void read(ImageInputStream stream) throws IOException {
        delegateNpkAccess.read(stream);
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        delegateNpkAccess.write(stream);
    }

    @Override
    public void setImg(int index, Img img) {
        textures.add(index, img);
    }

    @Override
    public void setOgg(int index, Ogg ogg) {
        textures.add(index, ogg);
    }

    @Override
    public Img getImg(int index) {
        return (Img) textures.get(index);
    }

    @Override
    public Ogg getOgg(int index) {
        return (Ogg) textures.get(index);
    }

    @Override
    public boolean isImg(int index) {
        Object texture = textures.get(index);
        return texture instanceof Img;
    }

    @Override
    public int getTextureSize() {
        return textures.size();
    }
}
