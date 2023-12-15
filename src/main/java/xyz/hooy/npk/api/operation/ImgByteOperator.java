package xyz.hooy.npk.api.operation;

import org.apache.commons.lang3.ArrayUtils;
import xyz.hooy.npk.api.constant.IndexConstant;
import xyz.hooy.npk.api.entity.AbstractIndex;
import xyz.hooy.npk.api.entity.ReferenceEntity;
import xyz.hooy.npk.api.entity.TextureAttribute;
import xyz.hooy.npk.api.entity.TextureEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        List<AbstractIndex> indexs = new ArrayList<>();
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        int size = bytesToInt(indexSize);
        for (int i = 0; i < size; i++) {
            if (readIsTexture(indexTableOffset)) {
                // 图片型索引项
                TextureEntity texture = createTexture(indexTableOffset, indexDataOffset);
                indexs.add(texture);
                // 移动偏移
                indexDataOffset += readTextureLength(indexTableOffset);
                indexTableOffset += TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            } else {
                // 指向型索引项
                ReferenceEntity reference = createReference(indexTableOffset);
                indexs.add(reference);
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }
        return indexs;
    }

    /**
     * @return 先将指向型索引转换为图片型索引后，只含有图片型索引列表
     */
    public List<TextureEntity> transferTextures() {
        List<TextureEntity> textures = new ArrayList<>();
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        int size = bytesToInt(indexSize);
        for (int i = 0; i < size; i++) {
            if (readIsTexture(indexTableOffset)) {
                // 图片型索引项
                TextureEntity texture = createTexture(indexTableOffset, indexDataOffset);
                textures.add(texture);
                // 移动偏移
                indexDataOffset += readTextureLength(indexTableOffset);
                indexTableOffset += TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            } else {
                // 指向型索引项
                ReferenceEntity reference = createReference(indexTableOffset);
                int indexNum = reference.getReferenceAttribute().getTo();
                textures.add(textures.get(indexNum));
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }
        return textures;
    }

    public void addTexture(TextureEntity textureEntity) {
        if (Objects.equals(textureEntity.getTextureAttribute().getCompress(), IndexConstant.TEXTURE_NON_ZLIB)) {
            // 压缩
            textureEntity.getTextureAttribute().setCompress(IndexConstant.TEXTURE_ZLIB);
            add(textureEntity.getTextureAttribute().toBytes(), decompressZlib(textureEntity.getTexture()));
        } else {
            add(textureEntity.getTextureAttribute().toBytes(), textureEntity.getTexture());
        }
    }

    public void addReference(ReferenceEntity referenceEntity) {
        add(referenceEntity.getReferenceAttribute().toBytes(), null);
    }

    protected void add(byte[] indexAttributes, byte[] textureData) {
        if (indexAttributes.length != TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH && indexAttributes.length != REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            throw new RuntimeException(String.format("Failed to add IMG, index attributes length %s", indexAttributes.length));
        }

        // 索引数据
        if (indexAttributes.length == TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            if (ArrayUtils.isNotEmpty(textureData)) {
                indexData = mergeByteArrays(indexData, textureData);
            } else {
                throw new RuntimeException("Texture length cannot be 0");
            }
        }

        // 索引表
        indexTable = mergeByteArrays(indexTable, indexAttributes);

        // 索引表长度
        indexTableLength = intToBytes(indexTable.length);

        // 索引总数
        indexSize = intToBytes(bytesToInt(indexSize) + 1);
    }

    public void remove(int index) {
        int indexTableRemoveOffset = 0;
        int indexDataRemoveOffset = 0;
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        int size = bytesToInt(indexSize);
        for (int i = 0; i < size; i++) {
            if (i == index) {
                // 找到要删除的元素，记录其偏移量
                indexTableRemoveOffset = indexTableOffset;
                indexDataRemoveOffset = indexDataOffset;
            }
            if (readIsTexture(indexTableOffset)) {
                // 移动偏移
                indexDataOffset += readTextureLength(indexTableOffset);
                indexTableOffset += TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            } else {
                if (i > index) {
                    ReferenceEntity reference = createReference(indexTableOffset);
                    if (reference.getReferenceAttribute().getTo() == index) {
                        // 当前帧被后续帧引用，无法删除，请先删除后续帧
                        throw new RuntimeException("The current frame is referenced by subsequent frames and cannot be deleted. Please delete subsequent frames first");
                    }
                }
                // 移动偏移
                indexTableOffset += REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH;
            }
        }

        // 从索引表里删除 & 从数据里删除
        byte[] indexTableBeforeBytes = ArrayUtils.subarray(indexTable, 0, indexTableRemoveOffset);
        byte[] indexDataBeforeBytes = ArrayUtils.subarray(indexData, 0, indexDataRemoveOffset);
        byte[] indexTableAfterBytes = null;
        byte[] indexDataAfterBytes = null;
        if (readIsTexture(indexTableRemoveOffset)) {
            indexTableAfterBytes = ArrayUtils.subarray(indexTable, indexTableRemoveOffset + TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexTable.length);
            indexDataAfterBytes = ArrayUtils.subarray(indexData, indexDataRemoveOffset + readTextureLength(indexTableRemoveOffset), indexData.length);
        } else {
            indexTableAfterBytes = ArrayUtils.subarray(indexTable, indexTableRemoveOffset + REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexTable.length);
        }
        indexTable = mergeByteArrays(indexTableBeforeBytes, indexTableAfterBytes);
        indexData = mergeByteArrays(indexDataBeforeBytes, indexDataAfterBytes);

        // 索引表长度
        indexTableLength = intToBytes(indexTable.length);

        // 总数 -1
        indexSize = intToBytes(bytesToInt(indexSize) - 1);
    }

    public byte[] build() {
        return mergeByteArrays(magicNumber, indexTableLength, imgReserve, imgVersion, indexSize, indexTable, indexData);
    }

    protected int readIndexType(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset, offset + 4));
    }

    protected boolean readIsTexture(int offset) {
        return readIndexType(offset) != IndexConstant.TYPE_REFERENCE;
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
        return ArrayUtils.subarray(indexData, indexDataOffset, indexDataOffset + length);
    }

    protected int readReferenceTo(int offset) {
        return bytesToInt(ArrayUtils.subarray(indexTable, offset + 4, offset + 8));
    }

    protected TextureEntity createTexture(int indexTableOffset, int indexDataOffset) {
        TextureEntity texture = new TextureEntity();
        TextureAttribute textureAttribute = texture.getTextureAttribute();
        textureAttribute.setType(readIndexType(indexTableOffset));
        textureAttribute.setCompress(IndexConstant.TEXTURE_NON_ZLIB);
        textureAttribute.setWidth(readTextureWidth(indexTableOffset));
        textureAttribute.setHeight(readTextureHeight(indexTableOffset));
        textureAttribute.setX(readTextureX(indexTableOffset));
        textureAttribute.setY(readTextureY(indexTableOffset));
        textureAttribute.setFrameWidth(readTextureFrameWidth(indexTableOffset));
        textureAttribute.setFrameHeight(readTextureFrameHeight(indexTableOffset));
        byte[] textureBytes = readTextureFile(indexTableOffset, indexDataOffset);
        if (readTextureZlib(indexTableOffset)) {
            // 解压
            textureBytes = decompressZlib(textureBytes);
        }
        texture.setTexture(textureBytes);
        return texture;
    }

    protected ReferenceEntity createReference(int indexTableOffset) {
        ReferenceEntity reference = new ReferenceEntity();
        reference.getReferenceAttribute().setType(readIndexType(indexTableOffset));
        reference.getReferenceAttribute().setTo(readReferenceTo(indexTableOffset));
        return reference;
    }
}
