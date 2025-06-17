package xyz.hooy.npkapi.impl;

import xyz.hooy.npkapi.support.Bytes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Version5Img extends Version2Img {

    public final static int VERSION = 5;

    public final static String DDS_IMAGE = "DDS";

    DdsTable ddsTable = new DdsTable();

    public Version5Img() {
        this.version = VERSION;
        this.delegateImgAccess = new Version5ImgAccess(this);
    }

    @Override
    public void setFrame(int index, int type, BufferedImage image) {
        if (Frame.isFxtType(type)) {
            supportedImageFrameTypeThrowException(type);
            Rectangle rectangle = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
            setDdsImageFrame(index, type, image, new Rectangle[]{rectangle});
        } else {
            super.setFrame(index, type, image);
        }
    }

    @Override
    public void setFrame(int index, int type, BufferedImage image, Rectangle[] rectangles) {
        if (Frame.isFxtType(type)) {
            supportedImageFrameTypeThrowException(type);
            setDdsImageFrame(index, type, image, rectangles);
        } else {
            super.setFrame(index, type, image, rectangles);
        }
    }

    @Override
    protected void supportedImageFrameTypeThrowException(int type) {
        if (!(Frame.isArgbType(type) || Frame.isFxtType(type))) {
            throw new IllegalArgumentException("Img(v4) type must be FXT1/FXT2/FXT3/ARGB1555/ARGB4444/ARGB8888.");
        }
        if (Frame.isFxtType(type)) {
            boolean supportedDds = Arrays.asList(ImageIO.getWriterFormatNames()).contains(DDS_IMAGE);
            if (!supportedDds) {
                throw new UnsupportedOperationException("ImageIO plugin not supported write dds image, type FXT1/FXT2/FXT3 is not available.");
            }
        }
    }

    private void setDdsImageFrame(int index, int type, BufferedImage image, Rectangle[] rectangles) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, DDS_IMAGE, outputStream); // TwelveMonkeys NOT supported DDS.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DdsTable.DDS dds = new DdsTable.DDS();
        byte[] data = outputStream.toByteArray();
        dds.fullLength = data.length;
        data = Bytes.compress(data);
        dds.length = data.length;
        dds.width = image.getWidth();
        dds.height = image.getHeight();
        dds.rawData = data;
        ddsTable.put(dds);
        for (Rectangle rectangle : rectangles) {
            DdsImageFrame ddsImageFrame = new DdsImageFrame();
            ddsImageFrame.type = type;
            ddsImageFrame.image = image;
            ddsImageFrame.compressed = ImageFrame.COMPRESSED;
            ddsImageFrame.width = image.getWidth();
            ddsImageFrame.height = image.getHeight();
            ddsImageFrame.frameWidth = image.getWidth();
            ddsImageFrame.frameHeight = image.getHeight();
            ddsImageFrame.x = image.getMinX();
            ddsImageFrame.y = image.getMinY();
            ddsImageFrame.ddsIndex = dds.index;
            ddsImageFrame.leftCut = rectangle.x;
            ddsImageFrame.upCut = rectangle.y;
            ddsImageFrame.rightCut = rectangle.x + rectangle.width;
            ddsImageFrame.downCut = rectangle.y + rectangle.height;
            ddsImageFrame.rawData = dds.rawData;
            ddsImageFrame.length = dds.length;
            insertFrame(index++, ddsImageFrame);
        }
    }

    @Override
    protected void deleteFrame(int index) {
        Frame frame = frames.get(index);
        if (frame.isFxt()) {
            DdsImageFrame ddsImageFrame = (DdsImageFrame) frame;
            if (!checkDdsIsShared(ddsImageFrame)) {
                ddsTable.remove(ddsImageFrame.ddsIndex);
            }
        }
        super.deleteFrame(index);
    }

    private boolean checkDdsIsShared(DdsImageFrame ddsImageFrame) {
        for (int i = 0; i < getFrameSize(); i++) {
            Frame existedFrame = frames.get(i);
            if (existedFrame.isFxt()) {
                DdsImageFrame existedDdsImageFrame = (DdsImageFrame) existedFrame;
                if (ddsImageFrame != existedDdsImageFrame && ddsImageFrame.ddsIndex == existedDdsImageFrame.ddsIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BufferedImage getImage(int index) {
        Frame frame = frames.get(index);
        if (frame.isFxt()) {
            DdsImageFrame ddsImageFrame = (DdsImageFrame) frame;
            if (ddsImageFrame.image == null) {
                conventDataToDdsImage(ddsImageFrame);
            }
            int width = ddsImageFrame.rightCut - ddsImageFrame.leftCut;
            int height = ddsImageFrame.downCut - ddsImageFrame.upCut;
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("DDS sub image width/height < 0.");
            }
            if (width > ddsImageFrame.image.getWidth() || height > ddsImageFrame.image.getHeight()) {
                throw new IllegalArgumentException("DDS sub image width/height > DDS image width/height.");
            }
            return ddsImageFrame.image.getSubimage(ddsImageFrame.leftCut, ddsImageFrame.upCut, width, height);
        } else {
            return super.getImage(index);
        }
    }

    private void conventDataToDdsImage(DdsImageFrame ddsImageFrame) {
        boolean supportedDds = Arrays.asList(ImageIO.getReaderFormatNames()).contains(DDS_IMAGE);
        if (!supportedDds) {
            throw new UnsupportedOperationException("ImageIO plugin not supported read dds image.");
        }
        byte[] data = ddsImageFrame.rawData;
        if (ddsImageFrame.isCompressed()) {
            data = Bytes.decompress(data);
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        BufferedImage image;
        try {
            image = ImageIO.read(inputStream); // TwelveMonkeys supported DDS.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ddsImageFrame.image = image;
    }
}
