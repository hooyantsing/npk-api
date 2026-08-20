package xyz.hooy.npk.codec;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Version4ImgEncoder extends Version2ImgEncoder {

    protected Palette palette;

    @Override
    public int version() {
        return 4;
    }

    @Override
    protected void encodeHeader(ImageOutputStream stream) throws IOException {
        super.encodeHeader(stream);
        stream.writeInt(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            Color color = palette.get(i);
            stream.writeByte(color.getRed());
            stream.writeByte(color.getGreen());
            stream.writeByte(color.getBlue());
            stream.writeByte(color.getAlpha());
        }
    }

    @Override
    protected Frame conventImageToFrame(BufferedImage image) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_INDEXED;
        frame.compressed = (int) image.getProperty("FrameCompressed");
        frame.width = image.getWidth();
        frame.height = image.getHeight();
        frame.x = image.getMinX();
        frame.y = image.getMinY();
        frame.frameWidth = image.getWidth();
        frame.frameHeight = image.getHeight();
        int[] indexData = new int[frame.width * frame.height];
        image.getRGB(0, 0, frame.width, frame.height, indexData, 0, frame.width);
        byte[] data = new byte[indexData.length];
        for (int i = 0; i < indexData.length; i++) {
            Color color = new Color(indexData[i]);
            if (!palette.contains(color)) {
                palette.add(color);
            }
            data[i] = palette.indexOf(color);
        }
        if (frame.isCompressed()) {
            data = compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }
}
