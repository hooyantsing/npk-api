package xyz.hooy.npkapi;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * NPK 文件中的资源统一抽象。
 * <p>
 * NPK 文件可包含 IMG（图像）和 OGG（音频）两种资源类型，
 * 它们共享名称属性，并各自负责将自身内容序列化到小端序输出流。
 * </p>
 * <p>
 * 资源以内存模型为单一事实来源：保存 NPK 时一律调用 {@link #write(ImageOutputStream)}
 * 从模型重新序列化，不缓存、不回放旧字节，从根本上避免
 * 「修改被缓存字节静默覆盖」一类失效问题。
 * </p>
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
     * 将资源的完整内容序列化写入输出流（小端序）。
     *
     * @param stream 小端序图像输出流
     * @throws IOException  写入异常
     * @throws NpkException 资源缺少序列化所需数据时抛出
     */
    void write(ImageOutputStream stream) throws IOException;
}
