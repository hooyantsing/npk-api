package xyz.hooy.npkapi.impl;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Hashtable;

public class Version2Img extends ListableImg {

    public final static int VERSION = 2;

    public Version2Img() {
        this.version = VERSION;
        this.delegateImgAccess = new Version2ImgAccess(this);
    }

    @Override
    public void addFrame(int index, BufferedImage image) {
        super.addFrame(index, Frame.TYPE_ARGB8888, image);
    }

    @Override
    public void addFrame(int index, BufferedImage image, Rectangle[] rectangles) {
        super.addFrame(index, Frame.TYPE_ARGB8888, image, rectangles);
    }

    @Override
    protected void supportedImageFrameTypeThrowException(int type) {
        if (!Frame.isArgbType(type)) {
            throw new IllegalArgumentException("Img(v2) type must be ARGB1555/ARGB4444/ARGB8888.");
        }
    }

    @Override
    protected void conventImageToData(ImageFrame imageFrame) {
        int[] argbData = new int[imageFrame.width * imageFrame.height];
        imageFrame.image.getRGB(0, 0, imageFrame.width, imageFrame.height, argbData, 0, imageFrame.width);
        byte[] data;
        switch (imageFrame.type) {
            case Frame.TYPE_ARGB1555: {
                data = toArgb1555Data(argbData);
                break;
            }
            case Frame.TYPE_ARGB4444: {
                data = toArgb4444Data(argbData);
                break;
            }
            case Frame.TYPE_ARGB8888:
            default: {
                imageFrame.type = Frame.TYPE_ARGB8888;
                data = toArgb8888Data(argbData);
                break;
            }
        }
        if (imageFrame.isCompressed()) {
            data = compress(data);
        }
        imageFrame.rawData = data;
        imageFrame.length = data.length;
    }

    @Override
    protected void conventDataToImage(ImageFrame imageFrame) {
        byte[] data = imageFrame.rawData;
        if (imageFrame.isCompressed()) {
            data = decompress(data);
        }
        switch (imageFrame.type) {
            case Frame.TYPE_ARGB1555: {
                toArgb1555Image(imageFrame, data);
                return;
            }
            case Frame.TYPE_ARGB4444: {
                toArgb4444Image(imageFrame, data);
                return;
            }
            case Frame.TYPE_ARGB8888: {
                toArgb8888Image(imageFrame, data);
                return;
            }
        }
    }

    private byte[] toArgb1555Data(int[] argbData) {
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
        return data;
    }

    private byte[] toArgb4444Data(int[] argbData) {
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
        return data;
    }

    private byte[] toArgb8888Data(int[] argbData) {
        int i = 0;
        int index = 0;
        byte[] data = new byte[argbData.length * 4];
        while (i < data.length) {
            int pixel = argbData[index++];
            data[i++] = (byte) ((pixel >> 24) & 0xFF);
            data[i++] = (byte) ((pixel >> 16) & 0xFF);
            data[i++] = (byte) ((pixel >> 8) & 0xFF);
            data[i++] = (byte) (pixel & 0xFF);
        }
        return data;
    }

    private void toArgb1555Image(ImageFrame imageFrame, byte[] data) {
        int[] masks = new int[]{0x7C00, 0x3E0, 0x1F, 0x8000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, imageFrame.width, imageFrame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        imageFrame.image = new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private void toArgb4444Image(ImageFrame imageFrame, byte[] data) {
        int[] masks = new int[]{0xF00, 0xF0, 0xF, 0xF000};
        ColorModel colorModel = new DirectColorModel(16, masks[0], masks[1], masks[2], masks[3]);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(DataBuffer.TYPE_USHORT, imageFrame.width, imageFrame.height, masks);
        short[] shortData = shortsMergedFrom(data);
        DataBuffer dataBuffer = new DataBufferUShort(shortData, shortData.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        imageFrame.image = new BufferedImage(colorModel, raster, false, new Hashtable<>());
    }

    private void toArgb8888Image(ImageFrame imageFrame, byte[] data) {
        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, imageFrame.width, imageFrame.height, 4, imageFrame.width * 4, new int[]{0, 1, 2, 3});
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        imageFrame.image = new BufferedImage(colorModel, raster, false, new Hashtable<>());
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
