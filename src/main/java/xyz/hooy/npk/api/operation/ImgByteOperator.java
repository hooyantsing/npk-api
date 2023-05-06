package xyz.hooy.npk.api.operation;

import org.apache.commons.lang3.ArrayUtils;
import xyz.hooy.npk.api.constant.IndexConstant;
import xyz.hooy.npk.api.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static xyz.hooy.npk.api.util.ByteUtils.*;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ImgByteOperator {

    // IMG 原始文件
    protected final byte[] originalImgFile;

    // IMG 魔数
    protected byte[] magicNumber; // 16byte

    // 索引表长度
    protected byte[] indexTableLength; // 4byte

    // 保留
    protected static final byte[] imgReserve = new byte[4]; // 4byte 全0

    // IMG 版本
    protected byte[] imgVersion; // 4byte

    // 索引 总数
    protected byte[] indexSize; // 4byte

    // 索引 索引表
    protected byte[] indexTable;

    // 索引 数据
    protected byte[] indexData;

    protected static final Integer TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH = 36;
    protected static final Integer REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH = 8;

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
        return traversalIndexs((reference, indexs) -> {
            indexs.add(reference);
        });
    }

    /**
     * @return 先将指向型索引转换为图片型索引后，只含有图片型索引列表
     */
    public List<Texture> transferTextures() {
        return traversalIndexs((reference, textures) -> {
            int indexNum = reference.getReferenceAttribute().getTo();
            textures.add(textures.get(indexNum));
        });
    }

    public void addTexture(byte[] indexAttributes, byte[] textureData) {
        add(indexAttributes, textureData);
    }

    public void addReference(byte[] indexAttributes) {
        add(indexAttributes, null);
    }

    public void add(byte[] indexAttributes, byte[] textureData) {
        if (indexAttributes.length == TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH && ArrayUtils.isNotEmpty(textureData)) {
            // 索引表长度
            indexTableLength = intToBytes(bytesToInt(indexTableLength) + TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH);
            // 索引数据
            indexData = ArrayUtils.addAll(indexData, textureData);
        } else if (indexAttributes.length == REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            // 索引表长度
            indexTableLength = intToBytes(bytesToInt(indexTableLength) + REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH);
        } else {
            throw new RuntimeException(String.format("Failed to add IMG, index attributes length %s", indexAttributes.length));
        }

        // 索引总数
        indexSize = intToBytes(bytesToInt(indexSize) + 1);

        // 索引表
        indexTable = ArrayUtils.addAll(indexTable, indexAttributes);
    }

    public byte[] build() {
        return ArrayUtils.addAll(magicNumber,
                ArrayUtils.addAll(indexTableLength,
                        ArrayUtils.addAll(imgReserve,
                                ArrayUtils.addAll(imgVersion,
                                        ArrayUtils.addAll(indexSize,
                                                ArrayUtils.addAll(indexTable, indexData))))));
    }

    protected List traversalIndexs(BiConsumer<Reference, List> processReferences) {
        List indexs = new ArrayList<>();
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
                Reference reference = createReference(indexTableOffset);
                processReferences.accept(reference, indexs);
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }
        return indexs;
    }

    protected int readIndexType(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset, offset + 4));
    }

    protected boolean readTextureZlib(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8)) == IndexConstant.TEXTURE_NON_ZLIB;
    }

    protected int readTextureWidth(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 8, offset + 12));
    }

    protected int readTextureHeight(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 12, offset + 16));
    }

    protected int readTextureLength(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 16, offset + 20));
    }

    protected int readTextureX(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 20, offset + 24));
    }

    protected int readTextureY(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 24, offset + 28));
    }

    protected int readTextureFrameWidth(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 28, offset + 32));
    }

    protected int readTextureFrameHeight(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 32, offset + 36));
    }

    protected byte[] readTextureFile(int indexTableOffset, int indexDataOffset) {
        int length = readTextureLength(indexTableOffset);
        byte[] textureBytes = ArrayUtils.subarray(indexData, indexDataOffset, indexDataOffset + length);
        if (readTextureZlib(indexTableOffset)) {
            // 解压
            textureBytes = decompressZlib(textureBytes);
        }
        return textureBytes;
    }

    protected int readReferenceTo(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8));
    }

    protected Texture createTexture(int indexTableOffset, int indexDataOffset) {
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

    protected Reference createReference(int indexTableOffset) {
        Reference reference = new Reference();
        reference.getReferenceAttribute().setType(readIndexType(indexTableOffset));
        reference.getReferenceAttribute().setTo(readReferenceTo(indexTableOffset));
        return reference;
    }
}
