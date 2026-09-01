package xyz.hooy.npkit;

import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

public class Version2ImgWriter implements ImgWriter {

    public final static byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    protected List<Frame> frames = new ArrayList<>();

    @Override
    public int version() {
        return 2;
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        writeHeader(stream);
        writeFrames(stream);
    }

    @Override
    public void encodeFrame(BufferedImage image) {
        Frame frame = encodeFrame(image, Frame.TYPE_ARGB8888);
        frames.add(frame);
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
                byte[] data = new byte[frame.length];
                stream.read(data);
                frame.rawData = data;
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

    protected Frame encodeFrame(BufferedImage image, int frameType) {
        switch (frameType) {
            case Frame.TYPE_ARGB1555: {
                return encodeArgb1555Frame(image);
            }
            case Frame.TYPE_ARGB4444: {
                return encodeArgb4444Frame(image);
            }
            case Frame.TYPE_ARGB8888:
            default: {
                return encodeArgb8888Frame(image);
            }
        }
    }

    private Frame encodeArgb1555Frame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB1555;
        frame.compressed = (int) image.getProperty("FrameCompressed");
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
            data = compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    private Frame encodeArgb4444Frame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB4444;
        frame.compressed = (int) image.getProperty("FrameCompressed");
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
            data = compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    private Frame encodeArgb8888Frame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_ARGB8888;
        frame.compressed = (int) image.getProperty("FrameCompressed");
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
            data = compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    protected byte[] compress(byte[] bytes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream)) {
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stream.toByteArray();
    }
}
