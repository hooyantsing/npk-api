package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultNpkAccess implements Access {

    public final static byte[] NPK_MAGIC = new byte[]{'N', 'e', 'o', 'p', 'l', 'e', 'P', 'a', 'c', 'k', '_', 'B', 'i', 'l', 'l', '\0'};

    public final static byte[] IMG_NAME_KEY = new byte[]{'p', 'u', 'c', 'h', 'i', 'k', 'o', 'n', '@', 'n', 'e', 'o', 'p', 'l', 'e', ' ', 'd', 'u', 'n', 'g', 'e', 'o', 'n', ' ', 'a', 'n', 'd', ' ', 'f', 'i', 'g', 'h', 't', 'e', 'r', ' ', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', 'D', 'N', 'F', '\0'};

    private final DefaultNpk npk;

    public DefaultNpkAccess(DefaultNpk npk) {
        this.npk = npk;
    }

    @Override
    public void read(ImageInputStream stream) throws IOException {
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        byte[] npkMagicBytes = new byte[NPK_MAGIC.length];
        stream.read(npkMagicBytes);
        verifyMagicCode(npkMagicBytes);
        int size = stream.readInt();
        stream.mark();
        stream.seek(0);
        byte[] headerBytes = new byte[NPK_MAGIC.length + 4 + size * 264];
        stream.read(headerBytes);
        byte[] verificationBytes = new byte[32];
        stream.read(verificationBytes);
        stream.reset();
        verifyVerificationCode(headerBytes, verificationBytes);
        List<Object> textures = new ArrayList<>();
        byte[] nameBytes = new byte[256];
        for (int i = 0; i < size; i++) {
            int offset = stream.readInt();
            int length = stream.readInt();
            stream.read(nameBytes);
            String name = decodeName(nameBytes);
            stream.mark();
            stream.seek(offset);
            byte[] dataBytes = new byte[length];
            stream.read(dataBytes);
            if (name.toLowerCase().endsWith(".ogg")) {
                DefaultOgg ogg = new DefaultOgg();
                ogg.setName(name);
                ogg.setRawData(dataBytes);
                textures.add(ogg);
            } else {
                try (ByteArrayInputStream imgInputStream = new ByteArrayInputStream(dataBytes);
                     MemoryCacheImageInputStream imgImageInputStream = new MemoryCacheImageInputStream(imgInputStream)) {
                    imgImageInputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    imgImageInputStream.skipBytes(ListableImgAccess.IMG_MAGIC.length + 8);
                    int version = imgImageInputStream.readInt();
                    imgImageInputStream.seek(0);
                    ListableImg img = findImg(version);
                    img.read(imgImageInputStream);
                    img.setName(name);
                    textures.add(img);
                }
            }
            stream.reset();
        }
        npk.textures = textures;
    }

    @Override
    public void write(ImageOutputStream stream) throws IOException {
        try (ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream headerImageOutputStream = new MemoryCacheImageOutputStream(headerOutputStream);
             ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream dataImageOutputStream = new MemoryCacheImageOutputStream(dataOutputStream)) {
            headerImageOutputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            dataImageOutputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            headerImageOutputStream.write(NPK_MAGIC);
            headerImageOutputStream.writeInt(npk.textures.size());
            int offset = NPK_MAGIC.length + 4 + 264 * npk.textures.size() + 32;
            for (Object texture : npk.textures) {
                String name;
                byte[] rawData;
                if (texture instanceof Ogg) {
                    Ogg ogg = (Ogg) texture;
                    name = ogg.getName();
                    rawData = ogg.getRawData();
                } else {
                    Img img = (Img) texture;
                    name = img.getName();
                    try (ByteArrayOutputStream imgOutputStream = new ByteArrayOutputStream();
                         MemoryCacheImageOutputStream imgImageOutputStream = new MemoryCacheImageOutputStream(imgOutputStream)) {
                        imgImageOutputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                        img.write(imgImageOutputStream);
                        imgImageOutputStream.flush();
                        rawData = imgOutputStream.toByteArray();
                    }
                }
                headerImageOutputStream.writeInt(offset);
                headerImageOutputStream.writeInt(rawData.length);
                headerImageOutputStream.write(encodeName(name));
                dataImageOutputStream.write(rawData);
                offset += rawData.length;
            }
            headerImageOutputStream.flush();
            dataImageOutputStream.flush();
            byte[] headerBytes = headerOutputStream.toByteArray();
            byte[] verificationBytes = compileVerificationCode(headerBytes);
            byte[] dataBytes = dataOutputStream.toByteArray();
            stream.write(headerBytes);
            stream.write(verificationBytes);
            stream.write(dataBytes);
        }
    }

    protected ListableImg findImg(int version) {
        switch (version) {
            case Version2Img.VERSION: {
                return new Version2Img();
            }
            case Version4Img.VERSION: {
                return new Version4Img();
            }
            case Version5Img.VERSION: {
                return new Version5Img();
            }
            case Version6Img.VERSION: {
                return new Version6Img();
            }
            default: {
                throw new UnsupportedOperationException("Not supported Img version: " + version);
            }
        }
    }

    private void verifyMagicCode(byte[] magicBytes) {
        if (!Arrays.equals(NPK_MAGIC, magicBytes)) {
            throw new IllegalArgumentException("Not a Npk file.");
        }
    }

    private void verifyVerificationCode(byte[] headerBytes, byte[] verificationBytes) throws IOException {
        byte[] bytes = compileVerificationCode(headerBytes);
        if (!Arrays.equals(verificationBytes, bytes)) {
            throw new IllegalArgumentException("Verify verification code failed.");
        }
    }

    private byte[] compileVerificationCode(byte[] headerBytes) throws IOException {
        if (headerBytes.length == 0) {
            return new byte[0];
        }
        byte[] specimenBytes = new byte[headerBytes.length / 17 * 17];
        System.arraycopy(headerBytes, 0, specimenBytes, 0, specimenBytes.length);
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        }
        messageDigest.update(specimenBytes);
        return messageDigest.digest();
    }

    private String decodeName(byte[] nameBytes) {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (nameBytes[i] ^ IMG_NAME_KEY[i]);
        }
        return new String(data).trim();
    }

    private byte[] encodeName(String name) {
        byte[] data = new byte[256];
        byte[] valueBytes = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(valueBytes, 0, data, 0, valueBytes.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ IMG_NAME_KEY[i]);
        }
        return data;
    }
}
