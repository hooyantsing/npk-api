package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;

import static xyz.hooy.npk.api.ByteUtils.*;

public class ImgByteOperator {

    // IMG 原始文件
    private final byte[] originalImgFile;

    // IMG 魔数
    private byte[] magicNumber; // 16byte

    // 索引表长度
    private byte[] indexTableLength; // 4byte

    // 保留
    private static final byte[] imgReserve = new byte[4]; // 4byte 全0

    // IMG 版本
    private byte[] imgVersion; // 4byte

    // 索引 总数
    private byte[] indexSize; // 4byte

    // 索引 索引表
    private byte[] indexTable;

    // 索引 数据
    private byte[] indexData;

    private static final Integer TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH = 36;
    private static final Integer REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH = 8;

    public ImgByteOperator(byte[] originalImgFile) {
        this.originalImgFile = originalImgFile;
        this.magicNumber = ArrayUtils.subarray(originalImgFile, 0, 16);
        this.indexTableLength = ArrayUtils.subarray(originalImgFile, 16, 20);
        this.imgVersion = ArrayUtils.subarray(originalImgFile, 24, 28);
        this.indexSize = ArrayUtils.subarray(originalImgFile, 28, 32);
        this.indexTable = ArrayUtils.subarray(originalImgFile, 32, bytesToInt(indexTableLength));
        this.indexData = ArrayUtils.subarray(originalImgFile, bytesToInt(indexTableLength), originalImgFile.length);
    }

    public ImgByteOperator addTexture(byte[] indexAttributes, byte[] textureData) {
        return add(indexAttributes, textureData);
    }

    public ImgByteOperator addReference(byte[] indexAttributes) {
        return add(indexAttributes, null);
    }

    public ImgByteOperator add(byte[] indexAttributes, byte[] textureData) {
        if (indexAttributes.length == TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH && ArrayUtils.isNotEmpty(textureData)) {
            // 索引表长度
            indexTableLength = intToBytes(bytesToInt(indexTableLength) + TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH);
            // 索引数据
            indexData = ArrayUtils.addAll(indexData, textureData);
        } else if (indexAttributes.length == REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            // 索引表长度
            indexTableLength = intToBytes(bytesToInt(indexTableLength) + REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH);
        } else {
            throw new RuntimeException("");
        }

        // 索引总数
        indexSize = intToBytes(bytesToInt(indexSize) + 1);

        // 索引表
        indexTable = ArrayUtils.addAll(indexTable, indexAttributes);
        return this;
    }

    public byte[] build() {
        return ArrayUtils.addAll(magicNumber,
                ArrayUtils.addAll(indexTableLength,
                        ArrayUtils.addAll(imgReserve,
                                ArrayUtils.addAll(imgVersion,
                                        ArrayUtils.addAll(indexSize,
                                                ArrayUtils.addAll(indexTable, indexData))))));
    }
}
