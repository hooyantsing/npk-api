package xyz.hooy.npkit;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Npkit {


    public static NpkWriter<ImgWriter> imgWriter() {
        return new NpkWriter<>();
    }

    public static NpkWriter<OggWriter> oggWriter() {
        return new NpkWriter<>();
    }

    public static class ImgNpkReader {

        private final Map<Integer, ImgReaderFactory> imgReaderFactories = initImgReaderFactories;

        private Map<Integer, ImgReaderFactory> initImgReaderFactories() {
            Map<Integer, ImgReaderFactory> imgReaderFactoryMap = new LinkedHashMap<>();
            imgReaderFactoryMap.put()
        }

        public ImgNpkReader setImgReaderFactory(ImgReaderFactory factory) {
            imgReaderFactories.put(factory.version(), factory);
            return this;
        }

        public List<ImgReader> read(ImageInputStream stream) throws IOException {
            int version = stream.readInt();
            ImgReaderFactory factory = imgReaderFactories.get(version);
            ImgReader imgReader = factory.newInstance();
            imgReader.read(stream);
            return Collections.emptyList();
        }
    }

    public static class NpkWriter<T extends Writer> {

        private final Map<String, T> writers = new LinkedHashMap<>();

        public NpkWriter<T> add(String name, T writer) {
            writers.put(name, writer);
            return this;
        }

        public void write(ImageOutputStream stream) throws IOException {
            for (T writer : writers.values()) {
                writer.write(stream);
            }
        }
    }
}
