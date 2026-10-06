package xyz.hooy.npkapi;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * OGG 音频资源。
 * <p>
 * NPK 文件中除了 IMG 图像资源外，还可包含 OGG 格式的音频文件。
 * OGG 资源以原始二进制形式存储，本类提供基本的资源访问能力。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * try (NpkFile npk = NpkFile.open(new File("sound.npk"))) {
 *     for (Ogg ogg : npk.getOggs()) {
 *         byte[] audioData = ogg.getData();
 *         // 写入 .ogg 文件或送入音频解码器
 *     }
 * }
 * }</pre>
 */
public class Ogg implements NpkResource {

    private String name;
    private byte[] data;

    public Ogg() {
    }

    public Ogg(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    @Override
    public Type getType() {
        return Type.OGG;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 完整的 OGG 文件字节
     */
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * 将音频数据原样写入输出流。
     *
     * @throws NpkException 未携带数据时抛出
     */
    @Override
    public void write(ImageOutputStream stream) throws IOException {
        if (data == null) {
            throw new NpkException("OGG resource has no data: " + name);
        }
        stream.write(data);
    }

    @Override
    public String toString() {
        return "Ogg{name='" + name + "', size=" + getSize() + "}";
    }

    /**
     * 获取音频数据大小（字节）
     */
    public int getSize() {
        return data != null ? data.length : 0;
    }
}
