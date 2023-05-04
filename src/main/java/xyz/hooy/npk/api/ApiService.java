package xyz.hooy.npk.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiService {

    private final String path;
    private final NpkByteOperator npkByteOperator;
    private final List<ImgByteOperator> imgByteOperators = new ArrayList<>();

    public ApiService(String path) throws IOException {
        this.path = path;
        this.npkByteOperator = new NpkByteOperator(path);
        for (byte[] imgBytes : npkByteOperator.getImgs().values()) {
            this.imgByteOperators.add(new ImgByteOperator(imgBytes));
        }
    }

    public Map<String, byte[]> getImgs() {
        return npkByteOperator.getImgs();
    }

}
