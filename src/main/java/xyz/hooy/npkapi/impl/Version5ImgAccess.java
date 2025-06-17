package xyz.hooy.npkapi.impl;

import javax.imageio.stream.ImageInputStream;
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
    protected Frame readFrame(ImageInputStream stream, int type) throws IOException {
        if (Frame.isFxtType(type)) {
            DdsImageFrame frame = new DdsImageFrame();
            frame.type = type;
            frame.compressed = stream.readInt();
            frame.width = stream.readInt();
            frame.height = stream.readInt();
            frame.length = stream.readInt();
            frame.x = stream.readInt();
            frame.y = stream.readInt();
            frame.frameWidth = stream.readInt();
            frame.frameHeight = stream.readInt();
            stream.skipBytes(4); // skip 4 bytes
            frame.ddsIndex = stream.readInt();
            frame.leftCut = stream.readInt();
            frame.upCut = stream.readInt();
            frame.rightCut = stream.readInt();
            frame.downCut = stream.readInt();
            stream.skipBytes(4); // skip 4 bytes
            return frame;
        } else {
            return super.readFrame(stream, type);
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
