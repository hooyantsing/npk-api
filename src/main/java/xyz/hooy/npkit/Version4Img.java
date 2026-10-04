package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

public class Version4Img extends Version2Img {

    protected Palette palette;

    @Override
    public int version() {
        return 4;
    }

    @Override
    protected BufferedImage conventFrameToImage(Frame frame) {
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        ColorModel colorModel = new IndexColorModel(8, palette.size(), palette.toArray(), 0, false, -1, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, frame.width, frame.height, 1, frame.width, new int[]{0});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, null);
    }

    @Override
    protected Frame conventImageToFrame(BufferedImage image, int frameType) {
        Frame frame = new Frame();
        frame.type = Frame.TYPE_INDEXED;
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
            data = Bytes.compress(data);
        }
        frame.rawData = data;
        frame.length = data.length;
        return frame;
    }

    @Override
    protected void readHeader(ImageInputStream stream) throws IOException {
        super.readHeader(stream);
        int colorNum = stream.readInt();
        byte[] paletteBytes = new byte[4 * colorNum];
        stream.read(paletteBytes);
        Palette palette = new Palette();
        for (int i = 0; i < paletteBytes.length; i++) {
            Color color = new Color(paletteBytes[i], paletteBytes[i + 1], paletteBytes[i + 2], paletteBytes[i + 3]);
            palette.add(color);
        }
        this.palette = palette;
    }

    @Override
    protected void writeHeader(ImageOutputStream stream) throws IOException {
        super.writeHeader(stream);
        stream.writeInt(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            Color color = palette.get(i);
            stream.writeByte(color.getRed());
            stream.writeByte(color.getGreen());
            stream.writeByte(color.getBlue());
            stream.writeByte(color.getAlpha());
        }
    }
}
