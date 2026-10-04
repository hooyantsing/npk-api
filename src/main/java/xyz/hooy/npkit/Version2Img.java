package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class Version2Img implements Img, Processer {

    protected String name;

    protected List<Frame> frames = new ArrayList<>();

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getImageCount() {
        return 0;
    }

    @Override
    public BufferedImage getImage(int index) {
        Frame frame = frames.get(index);
        if (frame.isReference()) {
            return getImage(frame.reference);
        } else {
            return conventFrameToImage(frame);
        }
    }

    @Override
    public void addImage(BufferedImage image, int frameType) {
        Frame frame = conventImageToFrame(image, frameType);
        frames.add(frame);
    }

    @Override
    public void addImage(int referenceIndex) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void addImage(BufferedImage ddsImage, Rectangle[] subviews) {

    }

    @Override
    public void addImage(BufferedImage[] palettesImage) {

    }

    @Override
    public int getDdsSourceCount() {
        return 0;
    }

    @Override
    public BufferedImage getDdsSource(int index) {
        return null;
    }

    @Override
    public int getPaletteSourceCount() {
        return 0;
    }

    @Override
    public Palette getPaletteSource(int index) {
        return null;
    }

    @Override
    public void activePaletteSource(int index) {

    }

    protected BufferedImage conventFrameToImage(Frame frame) {
        switch (frame.type) {
            case Frame.TYPE_ARGB1555: {
                return conventFrameToArgb1555Image(frame);
            }
            case Frame.TYPE_ARGB4444: {
                return conventFrameToArgb4444Image(frame);
            }
            case Frame.TYPE_ARGB8888:
            default: {
                return conventFrameToArgb8888Image(frame);
            }
        }
    }

    private BufferedImage conventFrameToArgb1555Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        int[] masks = new int[]{0x7C00, 0x3E0, 0x1F, 0x8000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = Bytes.shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private BufferedImage conventFrameToArgb4444Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        int[] masks = new int[]{0xF00, 0xF0, 0xF, 0xF000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = Bytes.shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private BufferedImage conventFrameToArgb8888Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, frame.width, frame.height, 4, frame.width * 4, new int[]{0, 1, 2, 3});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    protected Frame conventImageToFrame(BufferedImage image, int frameType) {
        switch (frameType) {
            case Frame.TYPE_ARGB1555: {
                return conventArgb1555ImageToFrame(image);
            }
            case Frame.TYPE_ARGB4444: {
                return conventArgb4444ImageToFrame(image);
            }
            case Frame.TYPE_ARGB8888:
            default: {
                return conventArgb8888ImageToFrame(image);
            }
        }
    }

    private Frame conventArgb1555ImageToFrame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB1555;
        frame.width = image.getWidth();
        frame.height = image.getHeight();
        frame.x = image.getMinX();
        frame.y = image.getMinY();
        frame.frameWidth = image.getWidth();
        frame.frameHeight = image.getHeight();
        int[] argbData = new int[frame.width * frame.height];
        image.getRGB(0, 0, frame.width, frame.height, argbData, 0, frame.width);
        int i = 0;
        int index = 0;
        byte[] data = new byte[argbData.length * 2];
        while (i < data.length) {
            int pixel = argbData[index++];
            int a = ((pixel >> 24) & 0xFF) > 0 ? 1 : 0;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            r = (byte) (r >> 3);
            g = (byte) (g >> 3);
            b = (byte) (b >> 3);
            data[i++] = (byte) (((g & 7) << 5) | b);
            data[i++] = (byte) (a | (r << 2) | (g >> 3));
        }
        if (frame.isCompressed()) {
            data = Bytes.compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    private Frame conventArgb4444ImageToFrame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB4444;
        frame.width = image.getWidth();
        frame.height = image.getHeight();
        frame.x = image.getMinX();
        frame.y = image.getMinY();
        frame.frameWidth = image.getWidth();
        frame.frameHeight = image.getHeight();
        int[] argbData = new int[frame.width * frame.height];
        image.getRGB(0, 0, frame.width, frame.height, argbData, 0, frame.width);
        int i = 0;
        int index = 0;
        byte[] data = new byte[argbData.length * 2];
        while (i < data.length) {
            int pixel = argbData[index++];
            int a = (pixel >> 24) & 0xFF;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            data[i++] = (byte) (g | (b >> 4));
            data[i++] = (byte) (a | (r >> 4));
        }
        if (frame.isCompressed()) {
            data = Bytes.compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    private Frame conventArgb8888ImageToFrame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB8888;
        frame.width = image.getWidth();
        frame.height = image.getHeight();
        frame.x = image.getMinX();
        frame.y = image.getMinY();
        frame.frameWidth = image.getWidth();
        frame.frameHeight = image.getHeight();
        int[] argbData = new int[frame.width * frame.height];
        image.getRGB(0, 0, frame.width, frame.height, argbData, 0, frame.width);
        int i = 0;
        int index = 0;
        byte[] data = new byte[argbData.length * 4];
        while (i < data.length) {
            int pixel = argbData[index++];
            data[i++] = (byte) ((pixel >> 16) & 0xFF);
            data[i++] = (byte) ((pixel >> 8) & 0xFF);
            data[i++] = (byte) (pixel & 0xFF);
            data[i++] = (byte) ((pixel >> 24) & 0xFF);
        }
        if (frame.isCompressed()) {
            data = Bytes.compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    public final byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    protected int frameSize;

    @Override
    public void read(ImageOutputStream stream) throws IOException {
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
        if (!Arrays.equals(IMG_MAGIC, imgMagicBytes)) {
            throw new IllegalArgumentException("Not a Img file.");
        }
        int tableLength = stream.readInt(); // unused
        stream.skipBytes(4); // skip 4 bytes
        int version = stream.readInt();
        if (version() != version) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot process Img(v" + version + ")");
        }
        this.frameSize = stream.readInt();
    }

    protected void readFrames(ImageInputStream stream) throws IOException {
        List<Frame> frames = new ArrayList<>(frameSize);
        for (int i = 0; i < frameSize; i++) {
            Frame frame = new Frame();
            frame.type = stream.readInt();
            readFrame(stream, frame);
            frames.add(frame);
        }
        for (int i = 0; i < frameSize; i++) {
            Frame frame = frames.get(i);
            if (!frame.isReference()) {
                readFrameRawData(stream, frame);
            }
        }
        this.frames = frames;
    }

    protected void readFrame(ImageInputStream stream, Frame frame) throws IOException {
        if (frame.isReference()) {
            frame.reference = stream.readInt();
        } else {
            frame.compressed = stream.readInt();
            frame.width = stream.readInt();
            frame.height = stream.readInt();
            frame.length = stream.readInt();
            frame.x = stream.readInt();
            frame.y = stream.readInt();
            frame.frameWidth = stream.readInt();
            frame.frameHeight = stream.readInt();
        }
    }

    protected void readFrameRawData(ImageInputStream stream, Frame frame) throws IOException {
        byte[] data = new byte[frame.length];
        stream.read(data);
        frame.rawData = data;
    }

    protected void writeHeader(ImageOutputStream stream) throws IOException {
        stream.write(IMG_MAGIC);
        int length = 0;
        for (Frame frame : frames) {
            if (frame.isReference()) {
                length += 8;
            } else {
                length += 36;
            }
        }
        stream.writeInt(length);
        stream.writeInt(0); // skip 4 bytes
        stream.writeInt(version());
        stream.writeInt(frames.size());
    }

    protected void writeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            writeFrame(stream, frame);
        }
        for (Frame frame : frames) {
            if (frame.isArgb()) {
                writeFrameRawData(stream, frame);
            }
        }
    }

    protected void writeFrame(ImageOutputStream stream, Frame frame) throws IOException {
        stream.writeInt(frame.type);
        if (frame.isReference()) {
            stream.writeInt(frame.reference);
        } else {
            stream.writeInt(frame.compressed);
            stream.writeInt(frame.width);
            stream.writeInt(frame.height);
            stream.writeInt(frame.length);
            stream.writeInt(frame.x);
            stream.writeInt(frame.y);
            stream.writeInt(frame.frameWidth);
            stream.writeInt(frame.frameHeight);
        }
    }

    protected void writeFrameRawData(ImageOutputStream stream, Frame frame) throws IOException {
        stream.write(frame.rawData);
    }
}
