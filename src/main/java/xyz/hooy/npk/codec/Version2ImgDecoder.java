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
import java.util.zip.InflaterOutputStream;

public class Version2ImgDecoder implements ImgDecoder {

    public final static byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    protected int framesSize;

    protected List<Frame> frames;

    @Override
    public Texture decode(ImageInputStream stream) throws IOException {
        decodeHeader(stream);
        decodeFrames(stream);
        return getTexture();
    }

    @Override
    public int version() {
        return 2;
    }

    protected void decodeHeader(ImageInputStream stream) throws IOException {
        byte[] imgMagicBytes = new byte[IMG_MAGIC.length];
        stream.read(imgMagicBytes);
        validateMagicCode(imgMagicBytes);
        int tableLength = stream.readInt(); // unused
        stream.skipBytes(4); // skip 4 bytes
        int version = stream.readInt();
        validateImgVersion(version);
        this.framesSize = stream.readInt();
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

    protected void decodeFrames(ImageInputStream stream) throws IOException {
        List<Frame> frames = new ArrayList<>(framesSize);
        for (int i = 0; i < framesSize; i++) {
            Frame frame = decodeFrame(stream);
            frames.add(frame);
        }
        for (int i = 0; i < framesSize; i++) {
            Frame frame = frames.get(i);
            if (!frame.isReference()) {
                decodeFrameData(stream, frame);
            }
        }
        this.frames = frames;
    }

    protected Frame decodeFrame(ImageInputStream stream) throws IOException {
        Frame frame = new Frame();
        frame.type = stream.readInt();
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
        return frame;
    }

    protected void decodeFrameData(ImageInputStream stream, Frame frame) throws IOException {
        byte[] data = new byte[frame.length];
        stream.read(data);
        frame.rawData = data;
    }

    protected Texture getTexture() {
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
        frame = findImageFrame(frame);
        switch (frame.type) {
            case Frame.TYPE_ARGB1555: {
                return conventFrameToArgb1555Image(frame);
            }
            case Frame.TYPE_ARGB4444: {
                return conventFrameToArgb4444Image(frame);
            }
            case Frame.TYPE_ARGB8888: {
                return conventFrameToArgb8888Image(frame);
            }
        }
        return null;
    }

    protected Frame findImageFrame(Frame frame) {
        if (frame.isReference()) {
            frame = frames.get(frame.reference);
            return findImageFrame(frame);
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

    private BufferedImage conventFrameToArgb1555Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = decompress(data);
        }
        int[] masks = new int[]{0x7C00, 0x3E0, 0x1F, 0x8000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        Hashtable<Object, Object> properties = new Hashtable<>();
        properties.put("FrameType", frame.type);
        properties.put("FrameCompressed", frame.compressed);
        return new BufferedImage(colorModel, raster, false, properties);
    }

    private BufferedImage conventFrameToArgb4444Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = decompress(data);
        }
        int[] masks = new int[]{0xF00, 0xF0, 0xF, 0xF000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, frame.width, frame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        Hashtable<Object, Object> properties = new Hashtable<>();
        properties.put("FrameType", frame.type);
        properties.put("FrameCompressed", frame.compressed);
        return new BufferedImage(colorModel, raster, false, properties);
    }

    private BufferedImage conventFrameToArgb8888Image(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = decompress(data);
        }
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, frame.width, frame.height, 4, frame.width * 4, new int[]{0, 1, 2, 3});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        Hashtable<Object, Object> properties = new Hashtable<>();
        properties.put("FrameType", frame.type);
        properties.put("FrameCompressed", frame.compressed);
        return new BufferedImage(colorModel, raster, false, properties);
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
