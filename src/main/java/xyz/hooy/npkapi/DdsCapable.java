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
 * if (img instanceof DdsCapableImg) {
 *     DdsCapableImg ddsImg = (DdsCapableImg) img;
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
     * 登记一张 DDS 纹理源并返回其分配索引。
     * <p>
     * 传入的 {@link DdsSource} 须携带完整的 DDS 文件字节；
     * 本库不提供 BufferedImage → DDS 的 DXT 压缩能力。
     * </p>
     *
     * @param ddsSource 携带完整 DDS 文件字节的纹理源
     * @return 分配的 DDS 索引，供 {@link #addDdsImage} 引用
     * @throws NpkException 纹理源为 null、缺少数据或索引与既有纹理冲突
     */
    int addDdsSource(DdsSource ddsSource);

    /**
     * 追加一个引用 DDS 纹理裁切区域的 FXT 帧。
     * <p>
     * 帧类型取自所引用 DDS 的像素格式（须为 FXT1/FXT3/FXT5），
     * 贴图宽高由裁切区域得出；详细参数约束见
     * {@link Version5Img#addDdsImage(int, Rectangle, Rectangle)}。
     * </p>
     *
     * @param ddsSourceIndex DDS 索引（来自 {@link #addDdsSource} 的返回值）
     * @param textureCrop    纹理裁切区域（x=左边界、y=上边界、width/height=区域尺寸，均须为正）
     * @param frameCanvas    帧画布（x/y=图像起始绘制点坐标，width/height=帧域尺寸，须为正）
     * @throws NpkException DDS 索引/像素格式/裁切区域/画布参数非法
     */
    void addDdsImage(int ddsSourceIndex, Rectangle textureCrop, Rectangle frameCanvas);
}
