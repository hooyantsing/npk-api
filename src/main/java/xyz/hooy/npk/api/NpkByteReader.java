package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static xyz.hooy.npk.api.ByteUtils.*;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-02
 */
public class NpkByteReader {

    // NPK 原始文件
    private final byte[] originalNpkFile;

    // NPK 魔数
    private byte[] magicNumber;

    // IMG 总数
    private byte[] imgSize;

    // IMG 索引表
    private byte[] imgTable;

    // NPK 校验码
    private byte[] npkValidation;

    // IMG 数据
    private byte[] imgData;

    public NpkByteReader(String path) throws IOException {
        this(Paths.get(path));
    }

    public NpkByteReader(Path path) throws IOException {
        this(Files.readAllBytes(path));
    }

    public NpkByteReader(byte[] originalNpkFile) {
        this.originalNpkFile = originalNpkFile;
        this.magicNumber = ArrayUtils.subarray(originalNpkFile, 0, 16);
        this.imgSize = ArrayUtils.subarray(originalNpkFile, 16, 20);
        this.imgTable = ArrayUtils.subarray(originalNpkFile, 20, 20 + bytesToInt(imgSize) * 264);
        this.npkValidation = ArrayUtils.subarray(originalNpkFile, 20 + bytesToInt(imgSize) * 264, 52 + bytesToInt(imgSize) * 264);
        this.imgData = ArrayUtils.subarray(originalNpkFile, 52 + bytesToInt(imgSize) * 264, originalNpkFile.length);
    }

    public NpkByteReader push(byte[] img, String decryptImgName) throws NoSuchAlgorithmException {
        // 总数 +1
        imgSize = intToBytes(bytesToInt(imgSize) + 1);

        // 添加至索引表
        for (int i = 0; i < imgTable.length; i += 264) {
            byte[] oldOffsetBytes = ArrayUtils.subarray(imgTable, i, i + 4);
            byte[] newOffsetBytes = intToBytes(bytesToInt(oldOffsetBytes) + 264);
            imgTable[i] = newOffsetBytes[0];
            imgTable[i + 1] = newOffsetBytes[1];
            imgTable[i + 2] = newOffsetBytes[2];
            imgTable[i + 3] = newOffsetBytes[3];
        }
        byte[] imgOffset = intToBytes(originalNpkFile.length + 264);
        byte[] imgLength = intToBytes(img.length);
        byte[] encryptImgName = encryptImgName(stringToBytes(decryptImgName));
        imgTable = ArrayUtils.addAll(imgTable,
                ArrayUtils.addAll(imgOffset,
                        ArrayUtils.addAll(imgLength,
                                ArrayUtils.addAll(encryptImgName))));

        // 修正校验码
        int specimenLimit = new Double(Math.floor((magicNumber.length + imgSize.length + imgTable.length) / 17) * 17).intValue();
        byte[] specimenBytes = ArrayUtils.subarray(ArrayUtils.addAll(magicNumber, ArrayUtils.addAll(imgSize, imgTable)), 0, specimenLimit);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(specimenBytes);
        npkValidation = messageDigest.digest();

        // 添加至数据
        imgData = ArrayUtils.addAll(imgData, img);
        return this;
    }

    public byte[] build() {
        return ArrayUtils.addAll(magicNumber,
                ArrayUtils.addAll(imgSize,
                        ArrayUtils.addAll(imgTable,
                                ArrayUtils.addAll(npkValidation, imgData))));
    }
}
