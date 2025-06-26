package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ListableImgAccess implements Access {

    public final static byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    private final ListableImg img;

    protected int framesSize = -1;

    public ListableImgAccess(ListableImg img) {
        this.img = img;
    }

    @Override
    public void read(ImageInputStream stream) throws IOException {
        readHeader(stream);
        readFrames(stream);
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        writeHeader(stream);
        writeFrames(stream);
    }

    protected void readHeader(ImageInputStream stream) throws IOException {
        byte[] imgMagicBytes = new byte[IMG_MAGIC.length];
        stream.read(imgMagicBytes);
        verifyMagicCode(imgMagicBytes);
        int tableLength = stream.readInt(); // unused
        stream.skipBytes(4); // skip 4 bytes
        int version = stream.readInt();
        verifyImgVersion(version);
        this.framesSize = stream.readInt();
    }

    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int length = 0;
        for (Frame frame : img.frames) {
            if (frame.isReference()) {
                length += 8;
            } else {
                length += 36;
            }
        }
        stream.writeInt(length);
        stream.writeInt(0); // skip 4 bytes
        stream.writeInt(img.version);
        stream.writeInt(img.frames.size());
    }

    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frames = new ArrayList<>(framesSize);
        for (int i = 0; i < framesSize; i++) {
            int type = stream.readInt();
            Frame frame = readFrame(stream, type);
            frames.add(frame);
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
        img.frames = frames;
    }

    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : img.frames) {
            writeFrame(stream, frame);
        }
        for (Frame frame : img.frames) {
            if (!frame.isReference()) {
                ImageFrame imageFrame = (ImageFrame) frame;
                writeFrameRawData(stream, imageFrame);
            }
        }
    }

    protected Frame readFrame(ImageInputStream stream, int type) throws IOException {
        if (Frame.isReferenceType(type)) {
            ReferenceFrame frame = new ReferenceFrame();
            frame.type = type;
            frame.reference = stream.readInt();
            return frame;
        } else {
            ImageFrame frame = new ImageFrame();
            frame.type = type;
            frame.compressed = stream.readInt();
            frame.width = stream.readInt();
            frame.height = stream.readInt();
            frame.length = stream.readInt();
            frame.x = stream.readInt();
            frame.y = stream.readInt();
            frame.frameWidth = stream.readInt();
            frame.frameHeight = stream.readInt();
            return frame;
        }
    }

    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.type);
        if (frame.isReference()) {
            ReferenceFrame referenceFrame = (ReferenceFrame) frame;
            stream.writeInt(referenceFrame.reference);
        } else {
            ImageFrame imageFrame = (ImageFrame) frame;
            stream.writeInt(imageFrame.compressed);
            stream.writeInt(imageFrame.width);
            stream.writeInt(imageFrame.height);
            stream.writeInt(imageFrame.length);
            stream.writeInt(imageFrame.x);
            stream.writeInt(imageFrame.y);
            stream.writeInt(imageFrame.frameWidth);
            stream.writeInt(imageFrame.frameHeight);
        }
    }

    protected void readFrameRawData(ImageInputStream stream, ImageFrame imageFrame) throws IOException {
        byte[] data = new byte[imageFrame.length];
        stream.read(data);
        imageFrame.rawData = data;
    }

    protected void writeFrameRawData(ImageOutputStream stream, ImageFrame imageFrame) throws IOException {
        stream.write(imageFrame.rawData);
    }

    protected void verifyMagicCode(byte[] magicBytes) {
        if (!Arrays.equals(IMG_MAGIC, magicBytes)) {
            throw new IllegalArgumentException("Not a Img file.");
        }
    }

    protected void verifyImgVersion(int version) {
        if (img.version != version) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot process Img(v" + version + ")");
        }
    }
}
