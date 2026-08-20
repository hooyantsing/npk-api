package xyz.hooy.npk.codec;

import xyz.hooy.npk.Texture;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class Version2ImgDecoder implements ImgDecoder {

    public final static byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    protected int framesSize;

    protected List<Frame> frames;

    @Override
    public Texture decode(ImageInputStream inputStream) throws IOException {
        decodeHeader(inputStream);
        decodeFrames(inputStream);
        return generateTexture();
    }

    @Override
    public int version() {
        return 2;
    }

    protected void decodeHeader(ImageInputStream inputStream) throws IOException {
        byte[] imgMagicBytes = new byte[IMG_MAGIC.length];
        inputStream.read(imgMagicBytes);
        validateMagicCode(imgMagicBytes);
        int tableLength = inputStream.readInt(); // unused
        inputStream.skipBytes(4); // skip 4 bytes
        int version = inputStream.readInt();
        validateImgVersion(version);
        this.framesSize = inputStream.readInt();
    }

    protected void validateMagicCode(byte[] magicBytes) {
        if (!Arrays.equals(IMG_MAGIC, magicBytes)) {
            throw new IllegalArgumentException("Not a Img file.");
        }
    }

    protected void validateImgVersion(int version) {
        if (version() != version) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot process Img(v" + version + ")");
        }
    }

    protected void decodeFrames(ImageInputStream inputStream) throws IOException {
        List<Frame> frames = new ArrayList<>(framesSize);
        for (int i = 0; i < framesSize; i++) {
            Frame frame = decodeFrame(inputStream);
            frames.add(frame);
        }
        for (int i = 0; i < framesSize; i++) {
            Frame frame = frames.get(i);
            if (!frame.isReference()) {
                decodeFrameData(inputStream, frame);
            }
        }
        this.frames = frames;
    }

    protected Frame decodeFrame(ImageInputStream inputStream) throws IOException {
        Frame frame = new Frame();
        frame.type = inputStream.readInt();
        if (frame.isReference()) {
            frame.reference = inputStream.readInt();
        } else {
            frame.compressed = inputStream.readInt();
            frame.width = inputStream.readInt();
            frame.height = inputStream.readInt();
            frame.length = inputStream.readInt();
            frame.x = inputStream.readInt();
            frame.y = inputStream.readInt();
            frame.frameWidth = inputStream.readInt();
            frame.frameHeight = inputStream.readInt();
        }
        return frame;
    }

    protected void decodeFrameData(ImageInputStream stream, Frame frame) throws IOException {
        byte[] data = new byte[frame.length];
        stream.read(data);
        frame.rawData = data;
    }

    protected Texture generateTexture() {
        List<BufferedImage> images = new ArrayList<>();
        for (Frame frame : frames) {
            BufferedImage image = conventFrameToImage(frame);
            if (Objects.nonNull(image)) {
                images.add(image);
            }
        }
        return new Texture(images);
    }

    protected BufferedImage conventFrameToImage(Frame frame) {
        frame = getImageFrame(frame);
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = decompress(data);
        }
        switch (frame.type) {
            case Frame.TYPE_ARGB1555: {
                return toArgb1555Image(frame, data);
            }
            case Frame.TYPE_ARGB4444: {
                return toArgb4444Image(frame, data);
            }
            case Frame.TYPE_ARGB8888: {
                return toArgb8888Image(frame, data);
            }
        }
        return null;
    }

    protected Frame getImageFrame(Frame frame) {
        if (frame.isReference()) {
            frame = frames.get(frame.reference);
            return getImageFrame(frame);
        }
        return frame;
    }

    protected byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(outputStream)) {
            inflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    protected byte[] compress(byte[] bytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    private BufferedImage toArgb1555Image(Frame frame, byte[] data) {
        int[] masks = new int[]{0x7C00, 0x3E0, 0x1F, 0x8000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private BufferedImage toArgb4444Image(Frame frame, byte[] data) {
        int[] masks = new int[]{0xF00, 0xF0, 0xF, 0xF000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private BufferedImage toArgb8888Image(Frame frame, byte[] data) {
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, frame.width, frame.height, 4, frame.width * 4, new int[]{0, 1, 2, 3});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private short[] shortsMergedFrom(byte[] bytes) {
        int i = 0;
        int index = 0;
        short[] merged = new short[bytes.length / 2];
        while (i < merged.length) {
            byte right = bytes[index++];
            byte left = bytes[index++];
            merged[i++] = (short) ((left << 8) | (right & 0xFF));
        }
        return merged;
    }
}
