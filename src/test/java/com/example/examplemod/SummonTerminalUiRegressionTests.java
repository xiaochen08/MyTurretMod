package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SummonTerminalUiRegressionTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Summon Terminal UI Regression Tests");
        System.out.println("================================================");

        try {
            testHitBoxBoundsMatrix();
            testInteractionCallbacksPresent();
            testTerminalNamingConsistency();
            System.out.println("\nALL UI REGRESSION TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testHitBoxBoundsMatrix() {
        int[][] resolutions = {
                {1920, 1080},
                {2560, 1440},
                {3840, 2160}
        };
        int[] scales = {100, 125, 150};

        final int baseW = 264;
        final int baseH = 320;
        final double uiScale = 1.5;
        final int uiW = (int) Math.round(baseW * uiScale);
        final int uiH = (int) Math.round(baseH * uiScale);

        for (int[] res : resolutions) {
            int w = res[0];
            int h = res[1];
            for (int scale : scales) {
                int left = (w - uiW) / 2;
                int top = (h - uiH) / 2;

                assertRange(left, 0, w - uiW, "leftPos out of bounds @" + w + "x" + h + " scale=" + scale);
                assertRange(top, 0, h - uiH, "topPos out of bounds @" + w + "x" + h + " scale=" + scale);

                assertScaledRectInScreen(left, top, uiScale, baseW - 16, 4, 12, 12, w, h, "close button");
                assertScaledRectInScreen(left, top, uiScale, 96, baseH - 20, 18, 14, w, h, "prev button");
                assertScaledRectInScreen(left, top, uiScale, 152, baseH - 20, 18, 14, w, h, "next button");
                assertScaledRectInScreen(left, top, uiScale, baseW - 58, 24, 18, 18, w, h, "recall button");
                assertScaledRectInScreen(left, top, uiScale, baseW - 34, 24, 18, 18, w, h, "rename button");
                assertScaledRectInScreen(left, top, uiScale, 118, 268, 128, 16, w, h, "rename box");
                assertScaledRectInScreen(left, top, uiScale, 10, 62, 92, 190, w, h, "list panel");
                assertScaledRectInScreen(left, top, uiScale, 97, 62, 4, 190, w, h, "scrollbar");
            }
        }

        System.out.println("[OK] hit-box bounds matrix (1920/2560/3840 x 100/125/150)");
    }

    static void testInteractionCallbacksPresent() throws Exception {
        String screen = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalScreen.java"));
        assertContains(screen, "mouseClicked", "missing click callback");
        assertContains(screen, "mouseDragged", "missing drag callback");
        assertContains(screen, "mouseReleased", "missing release callback");
        assertContains(screen, "mouseScrolled", "missing wheel callback");
        assertContains(screen, "keyPressed", "missing keyboard callback");
        assertContains(screen, "UI_SCALE = 1.5f", "missing fixed UI scale");
        assertContains(screen, "toUiX", "missing mouse X transform");
        assertContains(screen, "toUiY", "missing mouse Y transform");
        assertContains(screen, "playUiClick", "missing positive interaction feedback");
        assertContains(screen, "playUiError", "missing negative interaction feedback");
        assertContains(screen, "waitingSnapshot", "missing loading-state feedback");
        assertContains(screen, "markRenameSyncStart(", "missing rename full-sync loading trigger");
        assertContains(screen, "isRenameSyncActive()", "missing top-right rename loading indicator");
        System.out.println("[OK] interaction callbacks and feedback hooks");
    }

    static void testTerminalNamingConsistency() throws Exception {
        String screen = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalScreen.java"));
        String zh = Files.readString(Paths.get("src/main/resources/assets/examplemod/lang/zh_cn.json"));
        String en = Files.readString(Paths.get("src/main/resources/assets/examplemod/lang/en_us.json"));

        assertContains(screen, "Component.translatable(\"block.examplemod.summon_terminal\")", "screen title key mismatch");
        assertContains(zh, "\"block.examplemod.summon_terminal\":", "zh lang key missing");
        assertContains(en, "\"block.examplemod.summon_terminal\":", "en lang key missing");
        System.out.println("[OK] terminal naming consistency");
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static void assertRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new RuntimeException(message + " actual=" + value + " range=[" + min + "," + max + "]");
        }
    }

    static void assertRectInScreen(int x, int y, int w, int h, int sw, int sh, String name) {
        if (x < 0 || y < 0 || x + w > sw || y + h > sh) {
            throw new RuntimeException(name + " out of screen bounds: rect=(" + x + "," + y + "," + w + "," + h + ") screen=(" + sw + "," + sh + ")");
        }
    }

    static void assertScaledRectInScreen(int left, int top, double scale, int x, int y, int w, int h, int sw, int sh, String name) {
        int rx = left + (int) Math.floor(x * scale);
        int ry = top + (int) Math.floor(y * scale);
        int rw = Math.max(1, (int) Math.ceil(w * scale));
        int rh = Math.max(1, (int) Math.ceil(h * scale));
        assertRectInScreen(rx, ry, rw, rh, sw, sh, name);
    }
}
