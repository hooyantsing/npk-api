package xyz.hooy.npkapi;

/**
 * IMG V2 格式实现 — 基础真彩色图像。
 * <p>
 * V2 支持 ARGB1555、ARGB4444、ARGB8888 三种像素格式和引用帧，
 * 无调色板与 DDS 纹理表。读写流程全部继承自 {@link AbstractImg} 模板。
 * </p>
 *
 * @see AbstractImg
 */
public class Version2Img extends AbstractImg {

    @Override
    public int getVersion() {
        return 2;
    }
}
