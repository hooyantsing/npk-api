package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static xyz.hooy.npk.api.ByteUtils.*;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-03
 */
public class NpkByteOperator {

    // NPK 原始文件
    private final byte[] originalNpkFile;

    // NPK 魔数
    private byte[] magicNumber; // 16byte

    // IMG 总数
    private byte[] imgSize; // 4byte

    // IMG 索引表
    private byte[] imgTable; // 264byte * imgSize

    // NPK 校验码
    private byte[] npkValidation; // 32byte

    // IMG 数据
    private byte[] imgData;

    private static final Integer IMG_TABLE_ITEM_BYTE_LENGTH = 264; // offset4 + length4 + filename256

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

    public NpkByteOperator add(byte[] img, String decryptImgName) throws NoSuchAlgorithmException {
        // 总数 +1
        imgSize = intToBytes(bytesToInt(imgSize) + 1);

        // 添加至索引表
        for (int i = 0; i < imgTable.length; i += IMG_TABLE_ITEM_BYTE_LENGTH) {
            byte[] oldOffsetBytes = ArrayUtils.subarray(imgTable, i, i + 4);
            byte[] newOffsetBytes = intToBytes(bytesToInt(oldOffsetBytes) + IMG_TABLE_ITEM_BYTE_LENGTH);
            for (int j = 0; j < 4; j++) {
                imgTable[i + j] = newOffsetBytes[j];
            }
        }
        byte[] imgOffset = intToBytes(build().length + IMG_TABLE_ITEM_BYTE_LENGTH);
        byte[] imgLength = intToBytes(img.length);
        byte[] encryptImgName = encryptImgName(stringToBytes(decryptImgName));
        imgTable = ArrayUtils.addAll(imgTable,
                ArrayUtils.addAll(imgOffset,
                        ArrayUtils.addAll(imgLength,
                                ArrayUtils.addAll(encryptImgName))));

        // 刷新校验码
        refreshNpkValidation();

        // 添加至数据
        imgData = ArrayUtils.addAll(imgData, img);
        return this;
    }

    public NpkByteOperator rename(Integer index, String newImgName) throws NoSuchAlgorithmException {
        int maxImgSize = bytesToInt(imgSize);
        if (index < 0 || index > maxImgSize) {
            throw new RuntimeException(String.format("Exceeding IMG size %s, you want to visit %s", maxImgSize, index));
        }

        // 修改索引表 IMG 文件名
        int imgTableOffset = index * IMG_TABLE_ITEM_BYTE_LENGTH + 8;
        byte[] encryptImgName = encryptImgName(stringToBytes(newImgName));
        for (int i = 0; i < 256; i++) {
            imgTable[imgTableOffset + i] = encryptImgName[i];
        }

        // 刷新校验码
        refreshNpkValidation();
        return this;
    }

    public Map<String, byte[]> getImgs() {
        Map<String, byte[]> imgs = new HashMap<>();
        byte[] newImgFile = build();
        for (int i = 0; i < imgTable.length; i += IMG_TABLE_ITEM_BYTE_LENGTH) {
            int imgOffset = bytesToInt(ArrayUtils.subarray(imgTable, i, i + 4));
            int imgLength = bytesToInt(ArrayUtils.subarray(imgTable, i + 4, i + 8));
            String imgName = bytesToString(decryptImgName(ArrayUtils.subarray(imgTable, i + 8, i + IMG_TABLE_ITEM_BYTE_LENGTH)));
            byte[] imgBytes = ArrayUtils.subarray(newImgFile, imgOffset, imgOffset + imgLength);
            imgs.put(imgName, imgBytes);
        }
        return imgs;
    }

    public byte[] build() {
        return ArrayUtils.addAll(magicNumber,
                ArrayUtils.addAll(imgSize,
                        ArrayUtils.addAll(imgTable,
                                ArrayUtils.addAll(npkValidation, imgData))));
    }


    private void refreshNpkValidation() throws NoSuchAlgorithmException {
        int specimenLimit = new Double(Math.floor((magicNumber.length + imgSize.length + imgTable.length) / 17) * 17).intValue();
        byte[] specimenBytes = ArrayUtils.subarray(ArrayUtils.addAll(magicNumber, ArrayUtils.addAll(imgSize, imgTable)), 0, specimenLimit);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(specimenBytes);
        npkValidation = messageDigest.digest();
    }

    public byte[] getMagicNumber() {
        return magicNumber;
    }

    public byte[] getImgSize() {
        return imgSize;
    }

    public byte[] getImgTable() {
        return imgTable;
    }

    public byte[] getNpkValidation() {
        return npkValidation;
    }

    public byte[] getImgData() {
        return imgData;
    }
}
