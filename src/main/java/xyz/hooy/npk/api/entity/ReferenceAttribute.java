package xyz.hooy.npk.api.entity;

import org.apache.commons.lang3.ArrayUtils;

import static xyz.hooy.npk.api.util.ByteUtils.intToBytes;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ReferenceAttribute extends AbstractIndexAttribute {

    private Integer to;

    public byte[] toBytes() {
        return ArrayUtils.addAll(intToBytes(type), intToBytes(to));
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }
}
