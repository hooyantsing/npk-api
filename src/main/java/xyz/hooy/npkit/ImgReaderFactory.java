package xyz.hooy.npkit;

public interface ImgReaderFactory {

    int version();

    ImgReader newInstance();
}
