package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class PlayerManualFeatureTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Player Manual Feature Tests");
        System.out.println("================================================");
        try {
            testRegistrationAndStarterGrant();
            testBookmarkPacketRegistered();
            testManualScreenFeatures();
            testAllModItemsCoveredInManual();
            System.out.println("\nALL PLAYER MANUAL TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testRegistrationAndStarterGrant() throws Exception {
        String mod = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
        assertContains(mod, "PLAYER_MANUAL = ITEMS.register(\"player_manual\"", "manual item should be registered");
        assertContains(mod, "ensurePlayerManual(player);", "manual should be checked on player join");
        assertContains(mod, "PlayerManualItem.ensureVersion", "manual version sync logic missing");
        System.out.println("[OK] registration and auto-grant checks");
    }

    static void testBookmarkPacketRegistered() throws Exception {
        String packet = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketHandler.java"));
        assertContains(packet, "PacketManualBookmarkUpdate.class", "bookmark packet not registered");
        System.out.println("[OK] bookmark packet registration check");
    }

    static void testManualScreenFeatures() throws Exception {
        String screen = Files.readString(Paths.get("src/main/java/com/example/examplemod/PlayerManualScreen.java"));
        assertContains(screen, "searchBox", "search box missing");
        assertContains(screen, "toggleBookmark()", "bookmark toggle logic missing");
        assertContains(screen, "matchesSearch(", "search filter logic missing");
        assertContains(screen, "manual.examplemod.section.advanced", "section UI not wired");
        System.out.println("[OK] manual screen feature checks");
    }

    static void testAllModItemsCoveredInManual() throws Exception {
        String content = Files.readString(Paths.get("src/main/java/com/example/examplemod/PlayerManualContent.java"));
        assertContains(content, "\"item_wand\"", "wand entry missing");
        assertContains(content, "\"item_glitch_chip\"", "glitch chip entry missing");
        assertContains(content, "\"item_tp_module\"", "teleport module entry missing");
        assertContains(content, "\"item_multishot_module\"", "multi-shot module entry missing");
        assertContains(content, "\"item_death_plaque\"", "death plaque entry missing");
        assertContains(content, "\"item_terminal\"", "terminal entry missing");
        assertContains(content, "\"item_player_manual\"", "manual entry missing");
        System.out.println("[OK] all mod item entries covered");
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }
}
