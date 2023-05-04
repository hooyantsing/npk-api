package xyz.hooy.npk.api;

import xyz.hooy.npk.api.model.AbstractIndex;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ApiService {

    private final String path;
    private final NpkByteOperator npkByteOperator;
    private final Map<String, ImgByteOperator> imgByteOperators = new HashMap<>();

    private ApiService(String path) throws IOException {
        this.path = path;
        this.npkByteOperator = new NpkByteOperator(path);
        for (Map.Entry<String, byte[]> img : npkByteOperator.getImgs().entrySet()) {
            imgByteOperators.put(img.getKey(), new ImgByteOperator(img.getValue()));
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

    public Map<String, List<AbstractIndex>> getIndexs() {
        Map<String, List<AbstractIndex>> indexs = new HashMap<>();
        for (Map.Entry<String, ImgByteOperator> imgByteOperatorEntry : imgByteOperators.entrySet()) {
            indexs.put(imgByteOperatorEntry.getKey(), imgByteOperatorEntry.getValue().getIndexs());
        }
        return indexs;
    }

    public List<AbstractIndex> getIndexs(String imgName) {
        ImgByteOperator imgByteOperator = imgByteOperators.get(imgName);
        if (imgByteOperator != null) {
            return imgByteOperator.getIndexs();
        }
        return null;
    }

    public AbstractIndex getIndex(String imgName, int index) {
        ImgByteOperator imgByteOperator = imgByteOperators.get(imgName);
        if (imgByteOperator != null) {
            return imgByteOperator.getIndexs().get(index);
        }
        return null;
    }
}
