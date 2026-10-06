package xyz.hooy.npkapi;

/**
 * NPK 文件中的资源统一抽象。
 * NPK 文件可包含 IMG（图像）和 OGG（音频）两种资源类型，
 * 它们共享名称和原始数据等基本属性。
 */
public interface NpkResource {

    /**
     * 资源类型枚举
     */
    enum Type {
        IMG, OGG
    }

    /**
     * 获取资源类型
     */
    Type getType();

    /**
     * 获取资源在 NPK 文件中的路径名（如 character/swordman/effect.npk 内的 img 路径）
     */
    String getName();

    /**
     * 设置资源路径名
     */
    void setName(String name);

    /**
     * 获取资源的原始二进制数据（未经解码的 NPK 内部数据）
     */
    byte[] getRawData();

    /**
     * 设置资源的原始二进制数据
     */
    void setRawData(byte[] rawData);
}
