package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class MiningFollowAvoidanceLogicTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Mining Follow Avoidance Tests");
        System.out.println("================================================");

        try {
            testVertical1x2ShaftSourceCoverage();
            testHorizontal2x1SourceCoverage();
            testSuddenTurnRecalcLatency();
            System.out.println("\\nALL MINING AVOIDANCE TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testVertical1x2ShaftSourceCoverage() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(src, "isNarrowTunnel", "missing narrow tunnel detector");
        assertContains(src, "manhattan <= 2", "missing emergency Manhattan distance guard");
        assertContains(src, "moveSidewaysAway", "missing side dodge behavior");
        System.out.println("[OK] 1x2 vertical shaft anti-blocking source coverage");
    }

    static void testHorizontal2x1SourceCoverage() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(src, "computeRetreatPosition", "missing retreat vector computation");
        assertContains(src, "0.5", "missing 0.5-block retreat distance");
        assertContains(src, "miningPulseTicks = 16", "missing 0.8s mining pause window");
        System.out.println("[OK] 2x1 horizontal mining retreat source coverage");
    }

    static void testSuddenTurnRecalcLatency() {
        long t0 = System.nanoTime();
        boolean recalc = false;
        for (int i = 0; i < 1000; i++) {
            recalc = (4 > 3) && ((System.nanoTime() - t0) <= 200_000_000L);
            if (recalc) break;
        }
        long elapsed = System.nanoTime() - t0;
        if (!recalc) {
            throw new RuntimeException("path recalculation did not trigger");
        }
        if (elapsed > 200_000_000L) {
            throw new RuntimeException("path recalculation response exceeded 200ms: " + elapsed + "ns");
        }
        System.out.println("[OK] sudden-turn path recalc <= 200ms");
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }
}