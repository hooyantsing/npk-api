package xyz.hooy.npk.api.entity;

import org.apache.commons.lang3.ArrayUtils;

import static xyz.hooy.npk.api.util.ByteUtils.intToBytes;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class TextureAttribute extends AbstractIndexAttribute {

    private Integer compress;
    private Integer width;
    private Integer height;
    private Integer x;
    private Integer y;
    private Integer frameWidth;
    private Integer frameHeight;

    public byte[] toBytes() {
        return ArrayUtils.addAll(intToBytes(type),
                ArrayUtils.addAll(intToBytes(compress),
                        ArrayUtils.addAll(intToBytes(width),
                                ArrayUtils.addAll(intToBytes(height),
                                        ArrayUtils.addAll(intToBytes(x),
                                                ArrayUtils.addAll(intToBytes(y),
                                                        ArrayUtils.addAll(intToBytes(frameWidth), intToBytes(frameHeight))))))));
    }

    public Integer getCompress() {
        return compress;
    }

    public void setCompress(Integer compress) {
        this.compress = compress;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(Integer frameWidth) {
        this.frameWidth = frameWidth;
    }

    public Integer getFrameHeight() {
        return frameHeight;
    }

    public void setFrameHeight(Integer frameHeight) {
        this.frameHeight = frameHeight;
    }

    @Override
    public String toString() {
        return "TextureAttribute{" +
                "width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                ", frameWidth=" + frameWidth +
                ", frameHeight=" + frameHeight +
                '}';
    }
}
