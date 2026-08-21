package xyz.hooy.npk.codec;

import xyz.hooy.npk.Texture;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

public class Version2ImgEncoder implements ImgEncoder {

    public final static byte[] IMG_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', ' ', 'I', 'm', 'g', ' ', 'F', 'i', 'l', 'e', '\0'};

    protected List<Frame> frames;

    @Override
    public void encode(ImageOutputStream stream, Texture texture) throws IOException {
        setTexture(texture);
        encodeHeader(stream);
        encodeFrames(stream);
    }

    @Override
    public int version() {
        return 2;
    }

    protected void setTexture(Texture texture) {
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < texture.getImages().size(); i++) {
            Frame frame;
            BufferedImage image = texture.getImages().get(i);
            int reference = texture.getImages().lastIndexOf(image);
            if (reference < i) {
                frame = new Frame();
                frame.reference = reference;
            } else {
                frame = conventImageToFrame(image);
            }
            frames.add(frame);
        }
        this.frames = frames;
    }

    protected Frame conventImageToFrame(BufferedImage image) {
        int frameType = (int) image.getProperty("FrameType");
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

    protected void encodeHeader(ImageOutputStream stream) throws IOException {
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

    protected void encodeFrames(ImageOutputStream stream) throws IOException {
        for (Frame frame : frames) {
            encodeFrame(stream, frame);
        }
        for (Frame frame : frames) {
            if (frame.isArgb()) {
                encodeFrameData(stream, frame);
            }
        }
    }

    protected void encodeFrame(ImageOutputStream stream, Frame frame) throws IOException {
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

    protected void encodeFrameData(ImageInputStream stream, Frame frame) throws IOException {
        byte[] data = new byte[frame.length];
        stream.read(data);
        frame.rawData = data;
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

    private Frame conventArgb1555ImageToFrame(BufferedImage image) {
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

    private Frame conventArgb4444ImageToFrame(BufferedImage image) {
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

    private Frame conventArgb8888ImageToFrame(BufferedImage image) {
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
}
