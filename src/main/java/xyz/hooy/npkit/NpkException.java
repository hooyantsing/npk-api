package xyz.hooy.npkit;

/**
 * NPK 文件格式统一异常。
 * 当 NPK/IMG 文件解析、校验或写入过程中出现格式相关错误时抛出。
 */
public class NpkException extends RuntimeException {

    public NpkException(String message) {
        super(message);
    }

    public NpkException(String message, Throwable cause) {
        super(message, cause);
    }
}
