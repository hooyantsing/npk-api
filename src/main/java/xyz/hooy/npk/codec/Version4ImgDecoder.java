package xyz.hooy.npk.codec;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

public class Version4ImgDecoder extends Version2ImgDecoder {

    protected Palette palette;

    @Override
    public int version() {
        return 4;
    }

    @Override
    protected void decodeHeader(ImageInputStream inputStream) throws IOException {
        super.decodeHeader(inputStream);
        int colorNum = inputStream.readInt();
        byte[] paletteBytes = new byte[4 * colorNum];
        inputStream.read(paletteBytes);
        Palette palette = new Palette();
        for (int i = 0; i < paletteBytes.length; i++) {
            Color color = new Color(paletteBytes[i], paletteBytes[i + 1], paletteBytes[i + 2], paletteBytes[i + 3]);
            palette.add(color);
        }
        this.palette = palette;
    }

    @Override
    protected BufferedImage conventFrameToImage(Frame frame) {
        frame = getImageFrame(frame);
        byte[] data = frame.rawData;
        if (frame.isCompressed()) {
            data = decompress(data);
        }
        ColorModel colorModel = new IndexColorModel(8, palette.size(), palette.toArray(), 0, false, -1, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, frame.width, frame.height, 1, frame.width, new int[]{0});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, null);
    }
}
