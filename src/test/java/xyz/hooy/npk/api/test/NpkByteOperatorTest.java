package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.NpkByteOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

@Ignore
public class NpkByteOperatorTest {

    private static final String INPUT_PATH = "/Users/hooy/Project/npk-loader/input/";
    private static final String OUTPUT_PATH = "/Users/hooy/Project/npk-api/output/";

    @Test
    void add() throws IOException, NoSuchAlgorithmException {
        byte[] img = Files.readAllBytes(Paths.get(INPUT_PATH + "modred.img"));
        NpkByteOperator npkByteOperator = new NpkByteOperator(Paths.get(INPUT_PATH + "sprite_common.NPK"));
        byte[] build = npkByteOperator.add(img, "img/hooy/modred.img").build();
        Files.write(Paths.get(OUTPUT_PATH + "NpkByteOperator-add-test.npk"), build);
    }
}
