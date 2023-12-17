package xyz.hooy.npk.api.coder;

import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npk.api.constant.ImgVersions;
import xyz.hooy.npk.api.entity.ImgEntity;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NpkCoder {

    public static Charset Encoding = StandardCharsets.UTF_8;

    public static final String NPK_FlAG = "NeoplePack_Bill";

    public static final String IMG_FLAG = "Neople Img File";

    public static final String IMAGE_FLAG = "Neople Image File";

    public static final String IMAGE_DIR = "ImagePacks2";

    public static final String SOUND_DIR = "SoundPacks";

    public static final String KEY_HEADER = "puchikon@neople dungeon and fighter ";

    public static byte[] Key = ("puchikon@neople dungeon and fighter " +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNFDNFDNFDNFDNFDNFDNFDNF" +
            "DNFDNFDNF\0").getBytes(Encoding);

    public static List<ImgEntity> readNpk(ByteBuffer buffer) {
        return readNpk(buffer, null);
    }

    public static List<ImgEntity> readNpk(ByteBuffer buffer, String file) {
        List<ImgEntity> imgEntities = new ArrayList<>();
        String flag = StringUtils.toEncodedString(buffer.get(new byte[NPK_FlAG.length()]).array(), Encoding);
        if (StringUtils.equals(NPK_FlAG, flag)) {
            // 当文件是NPK时
            buffer.position(0);
            imgEntities.addAll(readInfo(buffer));
            if (!imgEntities.isEmpty()) {
                buffer.position(32);
            }
        } else {
            ImgEntity imgEntity = new ImgEntity();
            if (Objects.nonNull(file)) {
                String suffix = StringUtils.substringAfterLast(file, ".");
                imgEntity.setPath(suffix);
            }
            imgEntities.add(imgEntity);
        }
        for (int i = 0; i < imgEntities.size(); i++) {
            int length = i < imgEntities.size() - 1 ? imgEntities.get(i + 1).getOffset() : buffer.array().length;
            readImg(buffer, imgEntities.get(i), length);
        }
        return imgEntities;
    }

    public static List<ImgEntity> readInfo(ByteBuffer buffer) {
        String flag = StringUtils.toEncodedString(buffer.get(new byte[NPK_FlAG.length()]).array(), Encoding);
        List<ImgEntity> imgEntities = new ArrayList<>();
        if (!StringUtils.equals(NPK_FlAG, flag)) {
            return imgEntities;
        }
        int count = buffer.getInt();
        for (int i = 0; i < count; i++) {
            ImgEntity imgEntity = new ImgEntity();
            imgEntity.setOffset(buffer.getInt());
            imgEntity.setLength(buffer.getInt());
            String path = StringUtils.toEncodedString(buffer.get(new byte[Key.length]).array(), Encoding);
            imgEntity.setPath(path);
            imgEntities.add(imgEntity);
        }
        return imgEntities;
    }

    public static void readImg(ByteBuffer buffer, ImgEntity imgEntity, long length) {
        buffer.position(imgEntity.getOffset());
        String albumFlag = StringUtils.toEncodedString(buffer.get(new byte[IMG_FLAG.length()]).array(), Encoding);
        if (StringUtils.equals(IMG_FLAG, albumFlag)) {
            imgEntity.setIndexLength(buffer.getLong());
            imgEntity.setImgVersion(ImgVersions.valueOf(buffer.getInt()));
            imgEntity.setCount(buffer.getInt());
            imgEntity.initHandle(buffer);
        } else {
            if (StringUtils.equals(IMAGE_FLAG, albumFlag)) {
                imgEntity.setImgVersion(ImgVersions.VERSION_1);
            } else {
                if (length < 0) {
                    length = buffer.array().length;
                }
                imgEntity.setImgVersion(ImgVersions.OTHER);
                buffer.position(imgEntity.getOffset());
                if (StringUtils.endsWith(StringUtils.lowerCase(imgEntity.getName()), "ogg")) {
                    imgEntity.setImgVersion(ImgVersions.OTHER);
                    imgEntity.setIndexLength(length - buffer.position());
                }
            }
            imgEntity.initHandle(buffer);
        }
    }
}