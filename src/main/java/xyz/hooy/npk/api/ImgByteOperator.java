package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;
import xyz.hooy.npk.api.consts.IndexConstant;
import xyz.hooy.npk.api.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static xyz.hooy.npk.api.util.ByteUtils.*;

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

    /**
     * @return 返回由图片型索引和指向型索引组成的列表
     */
    public List<AbstractIndex> getIndexs() {
        return traversalIndexs((indexTableOffset, indexs) -> {
            Reference reference = createReference(indexTableOffset);
            indexs.add(reference);
        });
    }

    /**
     * @return 先将指向型索引转换为图片型索引后，只含有图片型索引列表
     */
    public List<Texture> transferTextures() {
        return traversalIndexs((indexTableOffset, textures) -> {
            Reference reference = createReference(indexTableOffset);
            int indexNum = reference.getReferenceAttribute().getTo();
            textures.add(textures.get(indexNum));
        });
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

    private List traversalIndexs(BiConsumer<Integer, List> processReferences) {
        List<AbstractIndex> indexs = new ArrayList<>();
        int size = bytesToInt(indexSize);
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        for (int i = 0; i < size; i++) {
            int type = readIndexType(indexTableOffset);
            if (type != IndexConstant.TYPE_REFERENCE) {
                // 图片型索引项
                Texture texture = createTexture(indexTableOffset, indexDataOffset);
                indexs.add(texture);
                // 移动偏移
                indexDataOffset += readTextureLength(indexTableOffset);
                indexTableOffset += TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            } else {
                // 指向型索引项
                processReferences.accept(indexDataOffset, indexs);
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }
        return indexs;
    }

    private Integer readIndexType(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset, offset + 4));
    }

    private Boolean readTextureZlib(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8)) == IndexConstant.TEXTURE_NON_ZLIB;
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

    private Texture createTexture(int indexTableOffset, int indexDataOffset) {
        Texture texture = new Texture();
        TextureAttribute textureAttribute = texture.getTextureAttribute();
        textureAttribute.setType(readIndexType(indexTableOffset));
        textureAttribute.setWidth(readTextureWidth(indexTableOffset));
        textureAttribute.setHeight(readTextureHeight(indexTableOffset));
        textureAttribute.setX(readTextureX(indexTableOffset));
        textureAttribute.setY(readTextureY(indexTableOffset));
        textureAttribute.setFrameWidth(readTextureFrameWidth(indexTableOffset));
        textureAttribute.setFrameHeight(readTextureFrameHeight(indexTableOffset));
        texture.setTexture(readTextureFile(indexTableOffset, indexDataOffset));
        return texture;
    }

    private Reference createReference(int indexTableOffset) {
        Reference reference = new Reference();
        reference.getReferenceAttribute().setType(readIndexType(indexTableOffset));
        reference.getReferenceAttribute().setTo(readReferenceTo(indexTableOffset));
        return reference;
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
