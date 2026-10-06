package xyz.hooy.npkapi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * 二进制流读写处理器接口。
 * 实现此接口的类可以从 {@link ImageInputStream} 读取数据，
 * 或将自身状态写入 {@link ImageOutputStream}。
 */
public interface Processor {

    /**
     * 从输入流中读取并解析数据
     *
     * @param stream 小端序图像输入流
     * @throws IOException 读取异常
     */
    void read(ImageInputStream stream) throws IOException;

    /**
     * 将当前状态写入输出流
     *
     * @param stream 小端序图像输出流
     * @throws IOException 写入异常
     */
    void write(ImageOutputStream stream) throws IOException;
}
