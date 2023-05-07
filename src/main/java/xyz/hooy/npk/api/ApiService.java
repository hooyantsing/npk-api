package xyz.hooy.npk.api;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import xyz.hooy.npk.api.model.AbstractIndex;
import xyz.hooy.npk.api.model.Texture;
import xyz.hooy.npk.api.operation.ImgByteOperator;
import xyz.hooy.npk.api.operation.NpkByteOperator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ApiService {

    private final String path;
    private final NpkByteOperator npkByteOperator;
    private final Map<String, ImgByteOperator> imgByteOperators = new LinkedHashMap<>();
    private final Map<String, Boolean> imgByteOperatorsChange = new LinkedHashMap<>();

    private ApiService(String path) throws IOException {
        this.path = path;
        this.npkByteOperator = new NpkByteOperator(path);
        for (Map.Entry<String, byte[]> img : npkByteOperator.getImgs().entrySet()) {
            putImgByteOperator(img.getKey(), img.getValue());
        }
    }

    public static ApiService newInstance(String path) throws IOException {
        return new ApiService(path);
    }

    public Map<String, byte[]> getImgs() {
        return npkByteOperator.getImgs();
    }

    public byte[] getImg(String imgName) {
        return npkByteOperator.getImgs().get(imgName);
    }

    public byte[] getImg(int imgIndex) {
        String imgName = imgByteOperatorsIndexToName(imgIndex);
        return getImg(imgName);
    }

    public ApiService addImg(byte[] img, String imgName) {
        if (ArrayUtils.isNotEmpty(img) && StringUtils.isNotBlank(imgName)) {
            npkByteOperator.add(img, imgName);
            putImgByteOperator(imgName, img);
        }
        return this;
    }

    public ApiService removeImg(int imgIndex) {
        String name = imgByteOperatorsIndexToName(imgIndex);
        npkByteOperator.remove(imgIndex);
        removeImgByteOperator(name);
        return this;
    }

    public ApiService renameImg(int oldImgIndex, String newImgName) {
        String oldImgName = imgByteOperatorsIndexToName(oldImgIndex);
        if (!StringUtils.equals(oldImgName, newImgName)) {
            npkByteOperator.rename(oldImgIndex, newImgName);
            putImgByteOperator(newImgName, imgByteOperators.get(oldImgName));
            removeImgByteOperator(oldImgName);
        }
        return this;
    }

    public Map<String, List<AbstractIndex>> getIndexs() {
        Map<String, List<AbstractIndex>> indexs = new LinkedHashMap<>();
        for (Map.Entry<String, ImgByteOperator> imgByteOperatorEntry : imgByteOperators.entrySet()) {
            indexs.put(imgByteOperatorEntry.getKey(), imgByteOperatorEntry.getValue().getIndexs());
        }
        return indexs;
    }

    public List<AbstractIndex> getIndexs(int imgIndex) {
        String name = imgByteOperatorsIndexToName(imgIndex);
        ImgByteOperator imgByteOperator = imgByteOperators.get(name);
        return imgByteOperator.getIndexs();
    }

    public AbstractIndex getIndex(int imgIndex, int index) {
        return getIndexs(imgIndex).get(index);
    }

    public Map<String, List<Texture>> transferTextures() {
        Map<String, List<Texture>> textures = new LinkedHashMap<>();
        for (Map.Entry<String, ImgByteOperator> imgByteOperatorEntry : imgByteOperators.entrySet()) {
            textures.put(imgByteOperatorEntry.getKey(), imgByteOperatorEntry.getValue().transferTextures());
        }
        return textures;
    }

    public List<Texture> transferTextures(int imgIndex) {
        String name = imgByteOperatorsIndexToName(imgIndex);
        ImgByteOperator imgByteOperator = imgByteOperators.get(name);
        return imgByteOperator.transferTextures();
    }

    public Texture transferTexture(int imgIndex, int index) {
        return transferTextures(imgIndex).get(index);
    }

    public ApiService addTexture() {
        return this;
    }

    public ApiService removeIndex(int imgIndex, int index) {
        String name = imgByteOperatorsIndexToName(imgIndex);
        ImgByteOperator imgByteOperator = imgByteOperators.get(name);
        imgByteOperator.remove(index);
        imgByteOperatorsChange.put(name, true);
        return this;
    }

    public byte[] build() {
        int i = 0;
        for (Map.Entry<String, ImgByteOperator> entry : imgByteOperators.entrySet()) {
            if (imgByteOperatorsChange.get(entry.getKey())) {
                byte[] newImgBytes = entry.getValue().build();
                npkByteOperator.replace(i, newImgBytes);
            }
            i++;
        }
        return npkByteOperator.build();
    }

    protected String imgByteOperatorsIndexToName(int index) {
        if (0 <= index && index < imgByteOperators.size()) {
            int i = 0;
            for (Map.Entry<String, ImgByteOperator> img : imgByteOperators.entrySet()) {
                if (i == index) {
                    return img.getKey();
                }
                i++;
            }
        }
        throw new RuntimeException("Out of imgByteOperators range");
    }

    protected int imgByteOperatorsNameToIndex(String name) {
        if (StringUtils.isNotBlank(name)) {
            int i = 0;
            for (Map.Entry<String, ImgByteOperator> img : imgByteOperators.entrySet()) {
                if (StringUtils.equals(img.getKey(), name)) {
                    return i;
                }
                i++;
            }
        }
        throw new RuntimeException("Out of imgByteOperators range");
    }

    protected void putImgByteOperator(String imgName, byte[] img) {
        putImgByteOperator(imgName, new ImgByteOperator(img));
    }

    protected void putImgByteOperator(String imgName, ImgByteOperator imgByteOperator) {
        imgByteOperators.put(imgName, imgByteOperator);
        imgByteOperatorsChange.put(imgName, false);
    }

    protected void removeImgByteOperator(String imgName) {
        imgByteOperators.remove(imgName);
        imgByteOperatorsChange.remove(imgName);
    }
}
