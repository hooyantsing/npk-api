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
        if (indexAttributes.length != TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH && indexAttributes.length != REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            throw new RuntimeException(String.format("Failed to add IMG, index attributes length %s", indexAttributes.length));
        }

        // 索引数据
        if (indexAttributes.length == TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH) {
            if (ArrayUtils.isNotEmpty(textureData)) {
                indexData = ArrayUtils.addAll(indexData, textureData);
            } else {
                throw new RuntimeException("Texture length cannot be 0");
            }
        }

        // 索引表
        indexTable = ArrayUtils.addAll(indexTable, indexAttributes);

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
                    Reference reference = createReference(indexTableOffset);
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
        byte[] indexTableAfterBytes;
        byte[] indexDataAfterBytes;
        if (readIsTexture(indexTableRemoveOffset)) {
            indexTableAfterBytes = ArrayUtils.subarray(indexTable, indexTableRemoveOffset + TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexTable.length);
            indexDataAfterBytes = ArrayUtils.subarray(indexData, indexDataRemoveOffset + TEXTURE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexData.length);
        } else {
            indexTableAfterBytes = ArrayUtils.subarray(indexTable, indexTableRemoveOffset + REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexTable.length);
            indexDataAfterBytes = ArrayUtils.subarray(indexData, indexDataRemoveOffset + REFERENCE_INDEX_TABLE_ITEM_BYTE_LENGTH, indexData.length);
        }
        indexTable = ArrayUtils.addAll(indexTableBeforeBytes, indexTableAfterBytes);
        indexData = ArrayUtils.addAll(indexDataBeforeBytes, indexDataAfterBytes);

        // 索引表长度
        indexTableLength = intToBytes(indexTable.length);

        // 总数 -1
        indexSize = intToBytes(bytesToInt(indexSize) - 1);
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
        int indexTableOffset = 0;
        int indexDataOffset = 0;
        int size = bytesToInt(indexSize);
        for (int i = 0; i < size; i++) {
            if (readIsTexture(indexTableOffset)) {
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
