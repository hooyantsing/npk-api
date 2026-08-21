package xyz.hooy.npk.codec;

import xyz.hooy.npk.Texture;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class Version5ImgEncoder extends Version2ImgEncoder {

    public final static String DDS_IMAGE = "DDS";

    protected DdsTable ddsTable;

    @Override
    public int version() {
        return 5;
    }

    @Override
    protected void setTexture(Texture texture) {
        this.ddsTable = new DdsTable();
        super.setTexture(texture);
    }

    @Override
    protected void encodeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int length = 0;
        for (Frame frame : frames) {
            if (frame.isFxt()) {
                length += 64;
            } else if (frame.isReference()) {
                length += 8;
            } else {
                length += 36;
            }
        }
        stream.writeInt(length);
        stream.writeInt(0); // skip 4 bytes
        stream.writeInt(version());
        stream.writeInt(frames.size());
        stream.writeInt(ddsTable.size());
        stream.writeInt(0); // TODO: imgLength
        stream.writeInt(0); // colorNum
        for (DdsTable.DDS dds : ddsTable.values()) {
            stream.writeInt(dds.title);
            stream.writeInt(dds.pixelFormat);
            stream.writeInt(dds.index);
            stream.writeInt(dds.fullLength);
            stream.writeInt(dds.length);
            stream.writeInt(dds.width);
            stream.writeInt(dds.height);
        }
    }

    @Override
    protected void encodeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            encodeFrame(stream, frame);
        }
        for (DdsTable.DDS dds : ddsTable.values()) {
            stream.write(dds.rawData);
        }
        for (Frame frame : frames) {
            if (frame.isArgb() || frame.isIndexed()) {
                encodeFrameData(stream, frame);
            }
        }
    }

    @Override
    protected void encodeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            stream.writeInt(frame.type);
            stream.writeInt(frame.compressed);
            stream.writeInt(frame.width);
            stream.writeInt(frame.height);
            stream.writeInt(frame.length);
            stream.writeInt(frame.x);
            stream.writeInt(frame.y);
            stream.writeInt(frame.frameWidth);
            stream.writeInt(frame.frameHeight);
            stream.writeInt(0); // skip 4 bytes
            stream.writeInt(frame.ddsIndex);
            stream.writeInt(frame.leftCut);
            stream.writeInt(frame.upCut);
            stream.writeInt(frame.rightCut);
            stream.writeInt(frame.downCut);
        } else {
            super.encodeFrame(stream, frame);
        }
    }

    @Override
    protected Frame conventImageToFrame(BufferedImage image) {
        Object frameHighlightValue = image.getProperty("FrameHighlight");
        if (Objects.nonNull(frameHighlightValue)) {
            Rectangle rectangle = (Rectangle) frameHighlightValue;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, DDS_IMAGE, outputStream); // TwelveMonkeys NOT supported DDS.
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            DdsTable.DDS dds = new DdsTable.DDS();
            byte[] data = outputStream.toByteArray();
            dds.fullLength = data.length;
            data = compress(data);
            dds.length = data.length;
            dds.width = image.getWidth();
            dds.height = image.getHeight();
            dds.rawData = data;
            ddsTable.put(dds);
            Frame frame = new Frame();
            frame.type = Frame.TYPE_FXT1;
            frame.compressed = Frame.COMPRESSED;
            frame.width = image.getWidth();
            frame.height = image.getHeight();
            frame.frameWidth = image.getWidth();
            frame.frameHeight = image.getHeight();
            frame.x = image.getMinX();
            frame.y = image.getMinY();
            frame.ddsIndex = dds.index;
            frame.leftCut = rectangle.x;
            frame.upCut = rectangle.y;
            frame.rightCut = rectangle.x + rectangle.width;
            frame.downCut = rectangle.y + rectangle.height;
            frame.rawData = dds.rawData;
            frame.length = dds.length;
            return frame;
        } else {
            return super.conventImageToFrame(image);
        }
    }
}
