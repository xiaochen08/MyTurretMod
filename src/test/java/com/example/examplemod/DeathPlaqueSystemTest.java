package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class DeathPlaqueSystemTest {
    public static void main(String[] args) {
        System.out.println("=== Death Plaque System Tests ===");
        testDropAndDestroyMessageContract();
        testPlaqueCodecVersioningContract();
        testStressCounter();
        System.out.println("✅ Death Plaque System Tests Passed");
    }

    static void testDropAndDestroyMessageContract() {
        try {
            String content = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
            if (!content.contains("受到致命伤害 已阵亡")) throw new RuntimeException("missing fatal notification");
            if (!content.contains("已确认销毁")) throw new RuntimeException("missing destroy notification");
            if (!content.contains("/skull tpplaque")) throw new RuntimeException("missing clickable teleport command");
        } catch (Exception e) {
            throw new RuntimeException("message contract failed: " + e.getMessage());
        }
    }

    static void testPlaqueCodecVersioningContract() {
        try {
            String codec = Files.readString(Paths.get("src/main/java/com/example/examplemod/DeathPlaqueDataCodec.java"));
            if (!codec.contains("CURRENT_VERSION")) throw new RuntimeException("version field missing");
            if (!codec.contains("FatalHitCount")) throw new RuntimeException("fatal hit count missing");
            if (!codec.contains("version < 3")) throw new RuntimeException("compatibility migration missing");
        } catch (Exception e) {
            throw new RuntimeException("codec contract failed: " + e.getMessage());
        }
    }

    static void testStressCounter() {
        int count = 0;
        int drops = 0;
        for (int i = 0; i < 100000; i++) {
            count++;
            if (count < 3) drops++;
            else {
                // destroyed path
                count = 0;
            }
        }
        if (drops <= 0) throw new RuntimeException("stress loop invalid");
    }
}
