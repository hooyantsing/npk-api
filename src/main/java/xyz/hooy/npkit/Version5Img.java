package xyz.hooy.npkit;

import xyz.hooy.npkit.codec.DdsCodec;
import xyz.hooy.npkit.codec.PixelCodec;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IMG V5 格式实现 — 支持 DDS 纹理的图像。
 * <p>
 * V5 在 V2 的基础上增加了 DDS 纹理表（DdsTable）。
 * FXT1/FXT2/FXT3 类型的帧不直接存储像素数据，
 * 而是引用 DDS 表中的共享纹理，并通过裁剪区域获取子图像。
 * </p>
 *
 * <h3>V5 头部扩展结构</h3>
 * <pre>
 * [V2 Header]
 * [DDS Table]
 *   - ddsCount (int32)
 *   - imgLength (int32, unused)
 *   - colorNum (int32, unused in V5 but present)
 *   - palette[colorNum] (4 bytes each, unused)
 *   - dds[ddsCount]:
 *     - title (int32)
 *     - pixelFormat (int32)
 *     - index (int32)
 *     - fullLength (int32)
 *     - length (int32)
 *     - width (int32)
 *     - height (int32)
 * </pre>
 *
 * <h3>V5 帧表扩展</h3>
 * <p>
 * FXT 类型帧在标准帧描述后附加 DDS 裁剪信息：
 * ddsIndex, leftCut, upCut, rightCut, downCut
 * </p>
 */
public class Version5Img extends Version2Img {

    protected DdsTable ddsTable;

    public Version5Img() {
        super();
        // 注册 DDS 编解码器
        DdsCodec ddsCodec = new DdsCodec();
        registerCodec(Frame.TYPE_FXT1, ddsCodec);
        registerCodec(Frame.TYPE_FXT2, ddsCodec);
        registerCodec(Frame.TYPE_FXT3, ddsCodec);
    }

    @Override
    public int version() {
        return 5;
    }

    // ===== DDS 接口实现 =====

    @Override
    public int getDdsSourceCount() {
        return ddsTable != null ? ddsTable.size() : 0;
    }

    @Override
    public BufferedImage getDdsSource(int index) {
        if (ddsTable == null) {
            return null;
        }
        DdsTable.DDS dds = ddsTable.get(index);
        if (dds == null || dds.getRawData() == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(dds.getRawData()));
        } catch (IOException e) {
            throw new NpkException("Failed to decode DDS source at index " + index, e);
        }
    }

    @Override
    public DdsTable getDdsTable() {
        return ddsTable;
    }

    // ===== Header 读写 =====

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);

        int ddsCount = stream.readInt();
        stream.readInt(); // imgLength (unused)
        int colorNum = stream.readInt(); // palette in V5 header (usually unused)
        if (colorNum > 0) {
            byte[] paletteBytes = new byte[4 * colorNum];
            stream.readFully(paletteBytes);
        }

        DdsTable table = new DdsTable();
        for (int i = 0; i < ddsCount; i++) {
            DdsTable.DDS dds = new DdsTable.DDS();
            dds.setTitle(stream.readInt());
            dds.setPixelFormat(stream.readInt());
            dds.setIndex(stream.readInt());
            dds.setFullLength(stream.readInt());
            dds.setLength(stream.readInt());
            dds.setWidth(stream.readInt());
            dds.setHeight(stream.readInt());
            table.put(dds);
        }
        this.ddsTable = table;
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);

        int ddsCount = ddsTable != null ? ddsTable.size() : 0;
        stream.writeInt(ddsCount);
        stream.writeInt(0); // imgLength (unused)
        stream.writeInt(0); // colorNum (no palette in V5)

        if (ddsTable != null) {
            for (DdsTable.DDS dds : ddsTable.values()) {
                stream.writeInt(dds.getTitle());
                stream.writeInt(dds.getPixelFormat());
                stream.writeInt(dds.getIndex());
                stream.writeInt(dds.getFullLength());
                stream.writeInt(dds.getLength());
                stream.writeInt(dds.getWidth());
                stream.writeInt(dds.getHeight());
            }
        }
    }

    // ===== Frame 读写 =====

    @Override
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frameList = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.setType(stream.readInt());
            readFrame(stream, frame);
            frameList.add(frame);
        }

        // 先读 DDS 纹理原始数据
        if (ddsTable != null) {
            for (DdsTable.DDS dds : ddsTable.values()) {
                byte[] data = new byte[dds.getFullLength()];
                stream.readFully(data);
                dds.setRawData(data);
            }
        }

        // 再读非 DDS 帧的像素数据
        for (int i = 0; i < frameSize; i++) {
            Frame frame = frameList.get(i);
            if (!frame.isReference() && !frame.isFxt()) {
                readFrameRawData(stream, frame);
            }
        }

        // FXT 帧的 rawData 来自 DDS 表
        for (Frame frame : frameList) {
            if (frame.isFxt() && ddsTable != null) {
                DdsTable.DDS dds = ddsTable.get(frame.getDdsIndex());
                if (dds != null) {
                    frame.setRawData(dds.getRawData());
                }
            }
        }

        this.frames = frameList;
    }

    @Override
    protected void readFrame(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            // FXT 帧：标准图像帧描述 + DDS 裁剪信息
            frame.setCompressed(stream.readInt());
            frame.setWidth(stream.readInt());
            frame.setHeight(stream.readInt());
            frame.setLength(stream.readInt());
            frame.setX(stream.readInt());
            frame.setY(stream.readInt());
            frame.setFrameWidth(stream.readInt());
            frame.setFrameHeight(stream.readInt());
            stream.skipBytes(4); // padding
            frame.setDdsIndex(stream.readInt());
            frame.setLeftCut(stream.readInt());
            frame.setUpCut(stream.readInt());
            frame.setRightCut(stream.readInt());
            frame.setDownCut(stream.readInt());
            stream.skipBytes(4); // padding
        } else if (frame.isReference()) {
            frame.setReference(stream.readInt());
        } else {
            super.readFrame(stream, frame);
        }
    }

    @Override
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        // 写帧表
        for (Frame frame : frames) {
            writeFrame(stream, frame);
        }
        // 写 DDS 数据
        if (ddsTable != null) {
            for (DdsTable.DDS dds : ddsTable.values()) {
                if (dds.getRawData() != null) {
                    stream.write(dds.getRawData());
                }
            }
        }
        // 写非 FXT、非引用帧的像素数据
        for (Frame frame : frames) {
            if (!frame.isReference() && !frame.isFxt()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    @Override
    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.getType());
        if (frame.isFxt()) {
            stream.writeInt(frame.getCompressed());
            stream.writeInt(frame.getWidth());
            stream.writeInt(frame.getHeight());
            stream.writeInt(frame.getLength());
            stream.writeInt(frame.getX());
            stream.writeInt(frame.getY());
            stream.writeInt(frame.getFrameWidth());
            stream.writeInt(frame.getFrameHeight());
            stream.writeInt(0); // padding
            stream.writeInt(frame.getDdsIndex());
            stream.writeInt(frame.getLeftCut());
            stream.writeInt(frame.getUpCut());
            stream.writeInt(frame.getRightCut());
            stream.writeInt(frame.getDownCut());
            stream.writeInt(0); // padding
        } else if (frame.isReference()) {
            stream.writeInt(frame.getReference());
        } else {
            // 标准 ARGB 帧
            stream.writeInt(frame.getCompressed());
            stream.writeInt(frame.getWidth());
            stream.writeInt(frame.getHeight());
            stream.writeInt(frame.getLength());
            stream.writeInt(frame.getX());
            stream.writeInt(frame.getY());
            stream.writeInt(frame.getFrameWidth());
            stream.writeInt(frame.getFrameHeight());
        }
    }

    // ===== 编解码 =====

    @Override
    protected BufferedImage decodeFrame(Frame frame) {
        if (frame.isReference()) {
            return getImage(frame.getReference());
        }
        if (frame.isFxt()) {
            PixelCodec codec = getCodec(frame.getType());
            return codec.decode(frame, createCodecContext());
        }
        // 非 FXT 帧按 V2 方式处理
        return super.decodeFrame(frame);
    }

    @Override
    protected PixelCodec.CodecContext createCodecContext() {
        return new PixelCodec.CodecContext(null, ddsTable);
    }

    @Override
    protected int calculateTableLength() {
        int length = 0;
        for (Frame frame : frames) {
            length += 4; // type
            if (frame.isFxt()) {
                length += 60; // 15 int fields (including 2 paddings)
            } else if (frame.isReference()) {
                length += 4;
            } else {
                length += 32;
            }
        }
        return length;
    }
}
