package xyz.hooy.npkapi.impl;

import xyz.hooy.npkapi.support.Bytes;

import java.awt.*;
import java.awt.image.*;

public class Version4Img extends ListableImg {

    public final static int VERSION = 4;

    protected Palette palette = new Palette();

    public Version4Img() {
        this.version = VERSION;
        this.delegateImgAccess = new Version4ImgAccess(this);
    }

    @Override
    public void addFrame(int index, BufferedImage image) {
        super.addFrame(index, Frame.TYPE_INDEXED, image);
    }

    @Override
    public void addFrame(int index, BufferedImage image, Rectangle[] rectangles) {
        super.addFrame(index, Frame.TYPE_INDEXED, image, rectangles);
    }

    @Override
    protected void supportedImageFrameTypeThrowException(int type) {
        if (!Frame.isIndexed(type)) {
            throw new IllegalArgumentException("Img(v4) type must be INDEXED.");
        }
    }

    @Override
    protected void conventImageToData(ImageFrame imageFrame) {
        int[] indexData = new int[imageFrame.width * imageFrame.height];
        imageFrame.image.getRGB(0, 0, imageFrame.width, imageFrame.height, indexData, 0, imageFrame.width);
        byte[] data = new byte[indexData.length];
        for (int i = 0; i < indexData.length; i++) {
            Color color = new Color(indexData[i]);
            if (!palette.contains(color)) {
                palette.add(color);
            }
            data[i] = palette.indexOf(color);
        }
        if (imageFrame.isCompressed()) {
            data = Bytes.compress(data);
        }
        imageFrame.rawData = data;
        imageFrame.length = data.length;
    }

    @Override
    protected void conventDataToImage(ImageFrame imageFrame) {
        byte[] data = imageFrame.rawData;
        if (imageFrame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        ColorModel colorModel = new IndexColorModel(8, palette.size(), palette.toArray(), 0, false, -1, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, imageFrame.width, imageFrame.height, 1, imageFrame.width, new int[]{0});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        imageFrame.image = new BufferedImage(colorModel, raster, false, null);
    }
}
