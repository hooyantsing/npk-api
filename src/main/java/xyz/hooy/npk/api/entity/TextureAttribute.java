package xyz.hooy.npk.api.entity;

import org.apache.commons.lang3.ArrayUtils;
import xyz.hooy.npk.api.constant.IndexConstant;

import static xyz.hooy.npk.api.util.ByteUtils.intToBytes;
import static xyz.hooy.npk.api.util.ByteUtils.mergeByteArrays;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class TextureAttribute extends AbstractIndexAttribute {

    private Integer compress = IndexConstant.TEXTURE_NON_ZLIB;
    private Integer width = 0;
    private Integer height = 0;
    private Integer x = 0;
    private Integer y = 0;
    private Integer frameWidth = 0;
    private Integer frameHeight = 0;

    public byte[] toBytes() {
        return mergeByteArrays(intToBytes(type), intToBytes(compress), intToBytes(width), intToBytes(height), intToBytes(x), intToBytes(y), intToBytes(frameWidth), intToBytes(frameHeight));
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
