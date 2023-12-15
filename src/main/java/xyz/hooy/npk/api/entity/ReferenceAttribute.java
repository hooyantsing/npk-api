package xyz.hooy.npk.api.entity;

import org.apache.commons.lang3.ArrayUtils;

import static xyz.hooy.npk.api.util.ByteUtils.intToBytes;
import static xyz.hooy.npk.api.util.ByteUtils.mergeByteArrays;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ReferenceAttribute extends AbstractIndexAttribute {

    private Integer to = -1;

    public byte[] toBytes() {
        return mergeByteArrays(intToBytes(type), intToBytes(to));
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }
}
