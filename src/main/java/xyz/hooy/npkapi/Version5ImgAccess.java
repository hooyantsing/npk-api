package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Version5ImgAccess extends ListableImgAccess {

    private final Version5Img version5Img;

    public Version5ImgAccess(Version5Img version5Img) {
        super(version5Img);
        this.version5Img = version5Img;
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        int ddsSize = stream.readInt();
        int imgLength = stream.readInt(); // unused
        int colorNum = stream.readInt(); // unused
        byte[] paletteBytes = new byte[4 * colorNum]; // unused
        stream.read(paletteBytes);
        DdsTable ddsTable = new DdsTable();
        for (int i = 0; i < ddsSize; i++) {
            DdsTable.DDS dds = new DdsTable.DDS();
            dds.title = stream.readInt();
            dds.pixelFormat = stream.readInt();
            dds.index = stream.readInt();
            dds.fullLength = stream.readInt();
            dds.length = stream.readInt();
            dds.width = stream.readInt();
            dds.height = stream.readInt();
            ddsTable.put(dds);
        }
        version5Img.ddsTable = ddsTable;
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int length = 0;
        for (Frame frame : version5Img.frames) {
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
        stream.writeInt(version5Img.version);
        stream.writeInt(version5Img.frames.size());
        stream.writeInt(version5Img.ddsTable.size());
        stream.writeInt(0); // TODO: imgLength
        stream.writeInt(0); // colorNum
        for (DdsTable.DDS dds : version5Img.ddsTable.values()) {
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
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frames = new ArrayList<>(framesSize);
        for (int i = 0; i < framesSize; i++) {
            int type = stream.readInt();
            Frame frame = readFrame(stream, type);
            frames.add(frame);
        }
        for (DdsTable.DDS dds : version5Img.ddsTable.values()) {
            byte[] rawData = new byte[dds.fullLength];
            stream.read(rawData);
            dds.rawData = rawData;
        }
        for (int i = 0; i < framesSize; i++) {
            Frame frame = frames.get(i);
            if (frame.isReference()) {
                ReferenceFrame referenceFrame = (ReferenceFrame) frame;
                referenceFrame.frame = frames.get(referenceFrame.reference);
            } else {
                ImageFrame imageFrame = (ImageFrame) frame;
                readFrameRawData(stream, imageFrame);
            }
        }
        version5Img.frames = frames;
    }

    @Override
    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : version5Img.frames) {
            writeFrame(stream, frame);
        }
        for (DdsTable.DDS dds : version5Img.ddsTable.values()) {
            stream.write(dds.rawData);
        }
        for (Frame frame : version5Img.frames) {
            if (frame.isArgb() || frame.isIndexed()) {
                ImageFrame imageFrame = (ImageFrame) frame;
                writeFrameRawData(stream, imageFrame);
            }
        }
    }

    @Override
    protected Frame readFrame(ImageInputStream stream, int type) throws IOException {
        if (Frame.isFxtType(type)) {
            DdsImageFrame ddsImageFrame = new DdsImageFrame();
            ddsImageFrame.type = type;
            ddsImageFrame.compressed = stream.readInt();
            ddsImageFrame.width = stream.readInt();
            ddsImageFrame.height = stream.readInt();
            ddsImageFrame.length = stream.readInt();
            ddsImageFrame.x = stream.readInt();
            ddsImageFrame.y = stream.readInt();
            ddsImageFrame.frameWidth = stream.readInt();
            ddsImageFrame.frameHeight = stream.readInt();
            stream.skipBytes(4); // skip 4 bytes
            ddsImageFrame.ddsIndex = stream.readInt();
            ddsImageFrame.leftCut = stream.readInt();
            ddsImageFrame.upCut = stream.readInt();
            ddsImageFrame.rightCut = stream.readInt();
            ddsImageFrame.downCut = stream.readInt();
            stream.skipBytes(4); // skip 4 bytes
            return ddsImageFrame;
        } else {
            return super.readFrame(stream, type);
        }
    }

    @Override
    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            stream.writeInt(frame.type);
            DdsImageFrame ddsImageFrame = (DdsImageFrame) frame;
            stream.writeInt(ddsImageFrame.compressed);
            stream.writeInt(ddsImageFrame.width);
            stream.writeInt(ddsImageFrame.height);
            stream.writeInt(ddsImageFrame.length);
            stream.writeInt(ddsImageFrame.x);
            stream.writeInt(ddsImageFrame.y);
            stream.writeInt(ddsImageFrame.frameWidth);
            stream.writeInt(ddsImageFrame.frameHeight);
            stream.writeInt(0); // skip 4 bytes
            stream.writeInt(ddsImageFrame.ddsIndex);
            stream.writeInt(ddsImageFrame.leftCut);
            stream.writeInt(ddsImageFrame.upCut);
            stream.writeInt(ddsImageFrame.rightCut);
            stream.writeInt(ddsImageFrame.downCut);
        } else {
            super.writeFrame(stream, frame);
        }
    }

    @Override
    protected void readFrameRawData(ImageInputStream stream, ImageFrame imageFrame) throws IOException {
        if (imageFrame.isFxt()) {
            DdsImageFrame ddsImageFrame = (DdsImageFrame) imageFrame;
            DdsTable.DDS dds = version5Img.ddsTable.get(ddsImageFrame.ddsIndex);
            ddsImageFrame.rawData = dds.rawData;
        } else {
            super.readFrameRawData(stream, imageFrame);
        }
    }
}
