package xyz.hooy.npkapi;

public interface Npk extends Access {

    void setImg(int index, Img img);

    void setOgg(int index, Ogg ogg);

    Img getImg(int index);

    Ogg getOgg(int index);

    boolean isImg(int index);

    int getTextureSize();
}
