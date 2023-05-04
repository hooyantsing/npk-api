package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;
import xyz.hooy.npk.api.model.Index;
import xyz.hooy.npk.api.model.Reference;
import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.model.TextureAttribute;

import java.util.ArrayList;
import java.util.List;

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
        this.indexTable = ArrayUtils.subarray(originalImgFile, 32, 32 + bytesToInt(indexTableLength));
        this.indexData = ArrayUtils.subarray(originalImgFile, 32 + bytesToInt(indexTableLength), originalImgFile.length);
    }

    public List<Index> getIndexs() {
        List<Index> indexs = new ArrayList<>();
        int size = bytesToInt(indexSize);
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        for (int i = 0; i < size; i++) {
            int type = readIndexType(indexTableOffset);
            if (type != 0x11) {
                // 图片型索引项
                Texture texture = new Texture();
                TextureAttribute textureAttribute = texture.getTextureAttribute();
                textureAttribute.setType(type);
                textureAttribute.setWidth(readTextureWidth(indexTableOffset));
                textureAttribute.setHeight(readTextureHeight(indexTableOffset));
                textureAttribute.setX(readTextureX(indexTableOffset));
                textureAttribute.setY(readTextureY(indexTableOffset));
                textureAttribute.setFrameWidth(readTextureFrameWidth(indexTableOffset));
                textureAttribute.setFrameHeight(readTextureFrameHeight(indexTableOffset));
                texture.setTexture(readTextureFile(indexTableOffset, indexDataOffset));
                indexs.add(texture);
                // 移动偏移
                indexDataOffset += readTextureLength(indexTableOffset);
                indexTableOffset += TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            } else {
                // 指向性索引项
                Reference reference = new Reference();
                reference.getReferenceAttribute().setType(type);
                reference.getReferenceAttribute().setTo(readReferenceTo(indexTableOffset));
                indexs.add(reference);
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }
        return indexs;
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

    private Integer readIndexType(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset, offset + 4));
    }

    private Boolean readTextureZlib(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8)) == 0x06;
    }

    private Integer readTextureWidth(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 8, offset + 12));
    }

    private Integer readTextureHeight(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 12, offset + 16));
    }

    private Integer readTextureLength(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 16, offset + 20));
    }

    private Integer readTextureX(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 20, offset + 24));
    }

    private Integer readTextureY(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 24, offset + 28));
    }

    private Integer readTextureFrameWidth(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 28, offset + 32));
    }

    private Integer readTextureFrameHeight(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 32, offset + 36));
    }

    private byte[] readTextureFile(int indexTableOffset, int indexDataOffset) {
        int length = readTextureLength(indexTableOffset);
        byte[] textureBytes = ArrayUtils.subarray(indexData, indexDataOffset, indexDataOffset + length);
        if (readTextureZlib(indexTableOffset)) {
            // 解压
            textureBytes = decompressZlib(textureBytes);
        }
        return textureBytes;
    }

    private Integer readReferenceTo(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8));
    }

    public byte[] getMagicNumber() {
        return magicNumber;
    }

    public byte[] getIndexTableLength() {
        return indexTableLength;
    }

    public byte[] getImgVersion() {
        return imgVersion;
    }

    public byte[] getIndexSize() {
        return indexSize;
    }

    public byte[] getIndexTable() {
        return indexTable;
    }

    public byte[] getIndexData() {
        return indexData;
    }
}
