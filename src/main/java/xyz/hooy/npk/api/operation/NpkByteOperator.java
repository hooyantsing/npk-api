package xyz.hooy.npk.api.operation;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import static xyz.hooy.npk.api.util.ByteUtils.*;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-03
 */
public class NpkByteOperator {

    // NPK 原始文件
    protected final byte[] originalNpkFile;

    // NPK 魔数
    protected byte[] magicNumber; // 16byte

    // IMG 总数
    protected byte[] imgSize; // 4byte

    // IMG 索引表
    protected byte[] imgTable; // 264byte * imgSize

    // NPK 校验码
    protected byte[] npkValidation; // 32byte

    // IMG 数据
    protected byte[] imgData;

    protected static final Integer IMG_TABLE_ITEM_BYTE_LENGTH = 264; // offset4 + length4 + filename256

    public NpkByteOperator(String path) throws IOException {
        this(Paths.get(path));
    }

    public NpkByteOperator(Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    public NpkByteOperator(byte[] originalNpkFile) {
        this.originalNpkFile = originalNpkFile;
        this.magicNumber = ArrayUtils.subarray(originalNpkFile, 0, 16);
        this.imgSize = ArrayUtils.subarray(originalNpkFile, 16, 20);
        this.imgTable = ArrayUtils.subarray(originalNpkFile, 20, 20 + bytesToInt(imgSize) * IMG_TABLE_ITEM_BYTE_LENGTH);
        this.npkValidation = ArrayUtils.subarray(originalNpkFile, 20 + bytesToInt(imgSize) * IMG_TABLE_ITEM_BYTE_LENGTH, 52 + bytesToInt(imgSize) * IMG_TABLE_ITEM_BYTE_LENGTH);
        this.imgData = ArrayUtils.subarray(originalNpkFile, 52 + bytesToInt(imgSize) * IMG_TABLE_ITEM_BYTE_LENGTH, originalNpkFile.length);
    }

    public void add(byte[] img, String decryptImgName) {
        // 总数 +1
        imgSize = intToBytes(bytesToInt(imgSize) + 1);

        // 添加至索引表
        refreshIndexTableOffset(imgTable, IMG_TABLE_ITEM_BYTE_LENGTH);
        byte[] imgOffset = intToBytes(52 + imgTable.length + imgData.length + IMG_TABLE_ITEM_BYTE_LENGTH);
        byte[] imgLength = intToBytes(img.length);
        byte[] encryptImgName = encryptImgName(stringToBytes(decryptImgName));
        imgTable = ArrayUtils.addAll(imgTable,
                ArrayUtils.addAll(imgOffset,
                        ArrayUtils.addAll(imgLength,
                                ArrayUtils.addAll(encryptImgName))));

        // 添加至数据
        imgData = ArrayUtils.addAll(imgData, img);
    }

    public String remove(int index) {
        // 总数 -1
        imgSize = intToBytes(bytesToInt(imgSize) - 1);

        // 从索引表删除
        int originalIndexTableLength = imgTable.length;
        int imgTableOffset = index * IMG_TABLE_ITEM_BYTE_LENGTH;
        byte[] imgTableBeforeBytes = ArrayUtils.subarray(imgTable, 0, imgTableOffset);
        byte[] imgTableRemoveBytes = ArrayUtils.subarray(imgTable, imgTableOffset, imgTableOffset + IMG_TABLE_ITEM_BYTE_LENGTH);
        byte[] imgTableAfterBytes = ArrayUtils.subarray(imgTable, imgTableOffset + IMG_TABLE_ITEM_BYTE_LENGTH, originalIndexTableLength);
        int imgDataRemoveOffset = bytesToInt(ArrayUtils.subarray(imgTableRemoveBytes, 0, 4));
        int imgDataRemoveLength = bytesToInt(ArrayUtils.subarray(imgTableRemoveBytes, 4, 8));
        String oldImgName = bytesToString(decryptImgName(ArrayUtils.subarray(imgTableRemoveBytes, 8, imgTableRemoveBytes.length)));
        refreshIndexTableOffset(imgTableBeforeBytes, -IMG_TABLE_ITEM_BYTE_LENGTH);
        refreshIndexTableOffset(imgTableAfterBytes, -(IMG_TABLE_ITEM_BYTE_LENGTH + imgDataRemoveLength));
        imgTable = ArrayUtils.addAll(imgTableBeforeBytes, imgTableAfterBytes);

        // 从数据删除
        int imgDataRemoveRelativeOffset = imgDataRemoveOffset - (52 + originalIndexTableLength);
        byte[] imgDataBeforeBytes = ArrayUtils.subarray(imgData, 0, imgDataRemoveRelativeOffset);
        byte[] imgDataAfterBytes = ArrayUtils.subarray(imgData, imgDataRemoveRelativeOffset + imgDataRemoveLength, imgData.length);
        imgData = ArrayUtils.addAll(imgDataBeforeBytes, imgDataAfterBytes);
        return oldImgName;
    }

    public void replace(int index, byte[] newImg) {
        // 修改索引表
        int imgTableOffset = index * IMG_TABLE_ITEM_BYTE_LENGTH;
        byte[] imgTableBeforeBytes = ArrayUtils.subarray(imgTable, 0, imgTableOffset);
        byte[] imgTableOldBytes = ArrayUtils.subarray(imgTable, imgTableOffset, imgTableOffset + IMG_TABLE_ITEM_BYTE_LENGTH);
        byte[] imgTableAfterBytes = ArrayUtils.subarray(imgTable, imgTableOffset + IMG_TABLE_ITEM_BYTE_LENGTH, imgTable.length);
        int imgDataOldOffset = bytesToInt(ArrayUtils.subarray(imgTableOldBytes, 0, 4));
        int imgDataOldLength = bytesToInt(ArrayUtils.subarray(imgTableOldBytes, 4, 8));
        byte[] imgDataNewLengthBytes = intToBytes(newImg.length);
        for (int i = 0; i < 4; i++) {
            imgTableOldBytes[i + 4] = imgDataNewLengthBytes[i];
        }
        refreshIndexTableOffset(imgTableAfterBytes, newImg.length - imgDataOldLength);
        imgTable = ArrayUtils.addAll(imgTableBeforeBytes, ArrayUtils.addAll(imgTableOldBytes, imgTableAfterBytes));

        // 移动数据
        int imgDataOldRelativeOffset = imgDataOldOffset - (52 + imgTable.length);
        byte[] imgDataBeforeBytes = ArrayUtils.subarray(imgData, 0, imgDataOldRelativeOffset);
        byte[] imgDataAfterBytes = ArrayUtils.subarray(imgData, imgDataOldRelativeOffset + imgDataOldLength, imgData.length);
        imgData = ArrayUtils.addAll(imgDataBeforeBytes, ArrayUtils.addAll(newImg, imgDataAfterBytes));
    }

    public String rename(int index, String newImgName) {
        // 修改索引表 IMG 文件名
        int imgTableOffset = index * IMG_TABLE_ITEM_BYTE_LENGTH + 8;
        String oldImgName = bytesToString(decryptImgName(ArrayUtils.subarray(imgTable, imgTableOffset, imgTableOffset + 256)));
        byte[] encryptImgName = encryptImgName(stringToBytes(newImgName));
        for (int i = 0; i < 256; i++) {
            imgTable[imgTableOffset + i] = encryptImgName[i];
        }
        return oldImgName;
    }

    public Map<String, byte[]> getImgs() {
        Map<String, byte[]> imgs = new LinkedHashMap<>();
        for (int i = 0; i < imgTable.length; i += IMG_TABLE_ITEM_BYTE_LENGTH) {
            int imgOffset = bytesToInt(ArrayUtils.subarray(imgTable, i, i + 4));
            int imgLength = bytesToInt(ArrayUtils.subarray(imgTable, i + 4, i + 8));
            String imgName = bytesToString(decryptImgName(ArrayUtils.subarray(imgTable, i + 8, i + IMG_TABLE_ITEM_BYTE_LENGTH)));
            int imgRelativeOffset = imgOffset - (52 + imgTable.length);
            byte[] imgBytes = ArrayUtils.subarray(imgData, imgRelativeOffset, imgRelativeOffset + imgLength);
            imgs.put(imgName, imgBytes);
        }
        return imgs;
    }

    public byte[] build() {
        // 刷新校验码
        refreshNpkValidation();
        return ArrayUtils.addAll(magicNumber,
                ArrayUtils.addAll(imgSize,
                        ArrayUtils.addAll(imgTable,
                                ArrayUtils.addAll(npkValidation, imgData))));
    }

    protected void refreshIndexTableOffset(byte[] indexTableBytes, int moveLength) {
        for (int i = 0; i < indexTableBytes.length; i += IMG_TABLE_ITEM_BYTE_LENGTH) {
            byte[] newImgOffset = intToBytes(bytesToInt(ArrayUtils.subarray(indexTableBytes, i, i + 4)) + moveLength);
            for (int j = 0; j < 4; j++) {
                indexTableBytes[i + j] = newImgOffset[j];
            }
        }
    }

    protected void refreshNpkValidation() {
        int specimenLimit = new Double(Math.floor((8 + imgTable.length) / 17) * 17).intValue();
        byte[] specimenBytes = ArrayUtils.subarray(ArrayUtils.addAll(magicNumber, ArrayUtils.addAll(imgSize, imgTable)), 0, specimenLimit);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(specimenBytes);
            npkValidation = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
