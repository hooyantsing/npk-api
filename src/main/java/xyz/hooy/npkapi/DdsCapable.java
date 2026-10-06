package xyz.hooy.npkapi;

import java.awt.*;

/**
 * DDS 纹理能力接口（仅 {@link Version5Img} 实现）。
 * <p>
 * IMG V5 通过共享的 DDS 纹理表（{@link DdsTable}）存储压缩纹理，
 * FXT 帧引用纹理的裁切区域而非自带像素数据。
 * 其余版本不实现本接口，调用方应以 {@code instanceof} 探测能力。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * if (img instanceof DdsCapable) {
 *     DdsCapable ddsImg = (DdsCapable) img;
 *     int index = ddsImg.addDdsSource(new DdsSource(ddsFileBytes, Frame.TYPE_FXT3, 256, 256));
 *     // 裁切纹理 (0,0)-(64,64) 区域，画布 64x64，绘制原点 (0,0)
 *     ddsImg.addDdsImage(index, new Rectangle(0, 0, 64, 64), new Rectangle(0, 0, 64, 64));
 * }
 * }</pre>
 */
public interface DdsCapable extends Img {

    /**
     * 获取 DDS 纹理源数量。
     *
     * @return DDS 纹理源数量
     */
    int getDdsSourceCount();

    /**
     * 按索引获取 DDS 纹理源。
     *
     * @param index DDS 索引（0 ~ getDdsSourceCount()-1）
     * @return 对应纹理源
     * @throws IndexOutOfBoundsException 索引越界
     */
    DdsSource getDdsSource(int index);

    /**
     * 登记一张纹理源并返回其分配索引。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整纹理数据：DXT 格式（0x12/0x13/0x14）
     * 为完整 DDS 文件字节（本库不提供 BufferedImage → DDS 的 DXT 压缩能力）；
     * 裸像素格式（0x0E/0x0F/0x10）为 width × height × 每像素字节数的未压缩像素。
     * </p>
     *
     * @param ddsSource 携带完整纹理数据的纹理源
     * @return 分配的 DDS 索引，供 {@link #addDdsImage} 引用
     * @throws NpkException 纹理源为 null、缺少数据、数据与声明不符或索引与既有纹理冲突
     */
    int addDdsSource(DdsSource ddsSource);

    /**
     * 追加一个不旋转的纹理裁切帧（等价于
     * {@code addDdsImage(ddsSourceIndex, textureCrop, frameCanvas, false)}）。
     *
     * @param ddsSourceIndex DDS 索引（来自 {@link #addDdsSource} 的返回值）
     * @param textureCrop    纹理裁切区域（x=左边界、y=上边界、width/height=区域尺寸，均须为正）
     * @param frameCanvas    帧画布（x/y=图像起始绘制点坐标，width/height=帧域尺寸，须为正）
     * @throws NpkException DDS 索引/裁切区域/画布参数非法
     */
    void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas);

    /**
     * 追加一个引用纹理裁切区域的 DDS 引用帧。
     * <p>
     * 帧类型取自所引用纹理的像素格式（DXT 或裸像素格式均可），
     * 贴图宽高由裁切区域得出；{@code rotated} 为 true 时表示该区域在图集中
     * 以逆时针 90° 旋转存放，帧宽高为旋转还原后的尺寸（宽高互换），
     * 解码时自动还原。详细参数约束见
     * {@link Version5Img#addDdsImage(int, Rectangle, Rectangle, boolean)}。
     * </p>
     *
     * @param ddsSourceIndex DDS 索引（来自 {@link #addDdsSource} 的返回值）
     * @param textureCrop    纹理裁切区域（x=左边界、y=上边界、width/height=区域尺寸，均须为正）
     * @param frameCanvas    帧画布（x/y=图像起始绘制点坐标，width/height=帧域尺寸，须为正）
     * @param rotated        该区域是否在图集中旋转 90° 存放（解码时逆时针还原）
     * @throws NpkException DDS 索引/裁切区域/画布参数非法
     */
    void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas, boolean rotated);
}
