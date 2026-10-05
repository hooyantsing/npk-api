package xyz.hooy.npkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * IMG 版本工厂。
 * 根据 IMG 文件的版本号创建对应的 {@link Img} 实现实例。
 * <p>
 * 支持通过 {@link #register(int, Supplier)} 注册自定义版本处理器，
 * 使得库具有良好的可扩展性。
 * </p>
 *
 * <pre>{@code
 * // 使用示例
 * Img img = ImgFactory.create(5); // 创建 Version5Img 实例
 *
 * // 注册自定义版本
 * ImgFactory.register(7, MyCustomImg::new);
 * }</pre>
 */
public final class ImgFactory {

    private static final Map<Integer, Supplier<Img>> REGISTRY = new ConcurrentHashMap<>();

    static {
        register(2, Version2Img::new);
        register(4, Version4Img::new);
        register(5, Version5Img::new);
        register(6, Version6Img::new);
    }

    private ImgFactory() {
    }

    /**
     * 注册指定版本的 IMG 构造器
     *
     * @param version  IMG 文件版本号
     * @param supplier 实例构造器
     */
    public static void register(int version, Supplier<Img> supplier) {
        REGISTRY.put(version, supplier);
    }

    /**
     * 注销指定版本的 IMG 构造器
     *
     * @param version 要注销的 IMG 版本号
     * @return 被移除的构造器，不存在时返回 null
     */
    public static Supplier<Img> unregister(int version) {
        return REGISTRY.remove(version);
    }

    /**
     * 根据版本号创建 Img 实例
     *
     * @param version IMG 文件版本号
     * @return 对应版本的 Img 实例
     * @throws NpkException 当版本号未注册时抛出
     */
    public static Img create(int version) {
        Supplier<Img> supplier = REGISTRY.get(version);
        if (supplier == null) {
            throw new NpkException("Unsupported IMG version: " + version
                    + ". Supported versions: " + REGISTRY.keySet());
        }
        return supplier.get();
    }

    /**
     * 检查指定版本是否已注册
     *
     * @param version IMG 文件版本号
     * @return 是否支持该版本
     */
    public static boolean isSupported(int version) {
        return REGISTRY.containsKey(version);
    }
}
