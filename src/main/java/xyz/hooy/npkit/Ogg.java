package xyz.hooy.npkit;

/**
 * OGG 音频资源。
 * <p>
 * NPK 文件中除了 IMG 图像资源外，还可包含 OGG 格式的音频文件。
 * OGG 资源以原始二进制形式存储，本类提供基本的资源访问能力。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * NpkFile npk = NpkFile.open(new File("sound.npk"));
 * for (Ogg ogg : npk.getOggs()) {
 *     byte[] audioData = ogg.getRawData();
 *     // 写入 .ogg 文件或送入音频解码器
 * }
 * }</pre>
 */
public class Ogg implements NpkResource {

    private String name;
    private byte[] rawData;

    public Ogg() {
    }

    public Ogg(String name, byte[] rawData) {
        this.name = name;
        this.rawData = rawData;
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

    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    @Override
    public String toString() {
        return "Ogg{name='" + name + "', size=" + getSize() + "}";
    }

    /**
     * 获取音频数据大小（字节）
     */
    public int getSize() {
        return rawData != null ? rawData.length : 0;
    }
}
