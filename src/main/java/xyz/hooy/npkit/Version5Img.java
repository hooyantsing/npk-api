package xyz.hooy.npkit;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Version5Img extends Version2Img {

    public final static String DDS_IMAGE = "DDS";

    protected DdsTable ddsTable;

    @Override
    public int version() {
        return 5;
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
        this.ddsTable = ddsTable;
    }

    @Override
    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frames = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.type = stream.readInt();
            readFrame(stream, frame);
            frames.add(frame);
        }
        for (DdsTable.DDS dds : ddsTable.values()) {
            byte[] rawData = new byte[dds.fullLength];
            stream.read(rawData);
            dds.rawData = rawData;
        }
        for (int i = 0; i < frameSize; i++) {
            Frame frame = frames.get(i);
            if (!frame.isReference()) {
                readFrameRawData(stream, frame);
            }
        }
        this.frames = frames;
    }

    @Override
    protected void readFrame(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isReference()) {
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
        } else {
            super.readFrame(stream, frame);
        }
    }

    @Override
    protected void readFrameRawData(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isFxt()) {
            DdsTable.DDS dds = ddsTable.get(frame.ddsIndex);
            frame.rawData = dds.rawData;
        } else {
            super.readFrameRawData(stream, frame);
        }
    }

    @Override
    protected BufferedImage conventFrameToImage(Frame frame) {
        if (frame.isFxt()) {
            boolean supportedDds = Arrays.asList(ImageIO.getReaderFormatNames()).contains(DDS_IMAGE);
            if (!supportedDds) {
                throw new UnsupportedOperationException("ImageIO plugin not supported read dds image.");
            }
            byte[] data = frame.rawData;
            if (frame.isCompressed()) {
                data = Bytes.decompress(data);
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BufferedImage image;
            try {
                image = ImageIO.read(inputStream); // TwelveMonkeys supported DDS.
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int width = frame.rightCut - frame.leftCut;
            int height = frame.downCut - frame.upCut;
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("DDS sub image width/height < 0.");
            }
            if (width > image.getWidth() || height > image.getHeight()) {
                throw new IllegalArgumentException("DDS sub image width/height > DDS image width/height.");
            }
            return cutSubimage(image, frame.leftCut, frame.upCut, width, height);
        } else {
            return super.conventFrameToImage(frame);
        }
    }

    private BufferedImage cutSubimage(BufferedImage image, int x, int y, int width, int height) {
        return image.getSubimage(x, y, width, height);
    }
}
