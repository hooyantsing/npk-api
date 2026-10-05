package xyz.hooy.npkit.codec;

import xyz.hooy.npkit.DdsTable;
import xyz.hooy.npkit.Frame;
import xyz.hooy.npkit.PaletteSource;

import java.awt.image.BufferedImage;

/**
 * 像素编解码策略接口。
 * <p>
 * 每种 IMG 帧类型（ARGB1555、ARGB4444、ARGB8888、INDEXED、DDS/FXT）
 * 对应一个编解码器实现，负责在 {@link Frame} 的原始字节数据与
 * {@link BufferedImage} 之间进行转换。
 * </p>
 * <p>
 * 通过策略模式将像素格式处理从 IMG 版本类中解耦，
 * 使各版本类专注于文件结构读写，编解码逻辑可独立测试和替换。
 * </p>
 */
public interface PixelCodec {

    /**
     * 将帧数据解码为 BufferedImage
     *
     * @param frame   包含原始像素数据的帧
     * @param context 解码上下文（如调色板等附加信息），可为 null
     * @return 解码后的图像
     */
    BufferedImage decode(Frame frame, CodecContext context);

    /**
     * 将 BufferedImage 编码为帧数据
     *
     * @param image   待编码的图像
     * @param context 编码上下文（如调色板等附加信息），可为 null
     * @return 编码后的帧
     */
    Frame encode(BufferedImage image, CodecContext context);

    /**
     * 编解码上下文，携带编解码过程中需要的附加信息。
     * 例如 INDEXED 模式需要调色板，DDS 模式需要 DdsTable 等。
     */
    class CodecContext {

        private PaletteSource palette;
        private DdsTable ddsTable;

        public CodecContext() {
        }

        public CodecContext(PaletteSource palette) {
            this.palette = palette;
        }

        public CodecContext(DdsTable ddsTable) {
            this.ddsTable = ddsTable;
        }

        public CodecContext(PaletteSource palette, DdsTable ddsTable) {
            this.palette = palette;
            this.ddsTable = ddsTable;
        }

        public PaletteSource getPalette() {
            return palette;
        }

        public void setPalette(PaletteSource palette) {
            this.palette = palette;
        }

        public DdsTable getDdsTable() {
            return ddsTable;
        }

        public void setDdsTable(DdsTable ddsTable) {
            this.ddsTable = ddsTable;
        }
    }
}
