package xyz.hooy.npk.api.test;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import xyz.hooy.npk.api.NpkByteOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class NpkByteOperatorTest {

    @Ignore
    @Test
    void add() throws IOException, NoSuchAlgorithmException {
        byte[] img = Files.readAllBytes(Paths.get("/Users/hooy/Project/npk-api/input/modred.img"));
        NpkByteOperator npkByteOperator = new NpkByteOperator(Paths.get("/Users/hooy/Project/npk-api/input/sprite_common.NPK"));
        byte[] build = npkByteOperator.add(img, "img/hooy/modred.img").build();
        Files.write(Paths.get("/Users/hooy/Project/npk-api/output/NpkByteOperator-add-test.npk"), build);
    }
}
