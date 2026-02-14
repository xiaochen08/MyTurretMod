package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;

public class StarterWandGrantTests {
    private static final String STARTER_WAND_FLAG = "hasReceivedStarterWands";
    private static final String LEGACY_STARTER_KIT_FLAG = "HasReceivedStarterKit_Final";

    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Starter Wand Grant Logic Tests");
        System.out.println("================================================");

        try {
            testFirstJoinWhenFlagMissing();
            testRepeatJoinIsIdempotent();
            testExistingFlagSkipsGrant();
            testLegacyFlagMigrationWithMissingAndExistingLegacyFlag();
            System.out.println("\nALL STARTER WAND TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testFirstJoinWhenFlagMissing() {
        Map<String, Boolean> persisted = new HashMap<>();
        boolean shouldGrant = markStarterWandsIfFirstJoin(persisted);
        if (!shouldGrant) throw new RuntimeException("Expected first join to grant starter wands");
        if (!persisted.getOrDefault(STARTER_WAND_FLAG, false)) {
            throw new RuntimeException("Starter wand flag should be set after first grant");
        }
        System.out.println("[OK] first join grants and sets flag");
    }

    static void testRepeatJoinIsIdempotent() {
        Map<String, Boolean> persisted = new HashMap<>();
        boolean first = markStarterWandsIfFirstJoin(persisted);
        boolean second = markStarterWandsIfFirstJoin(persisted);
        if (!first) throw new RuntimeException("First call should grant");
        if (second) throw new RuntimeException("Second call should not grant (must be idempotent)");
        System.out.println("[OK] repeated join does not duplicate grant");
    }

    static void testExistingFlagSkipsGrant() {
        Map<String, Boolean> persisted = new HashMap<>();
        persisted.put(STARTER_WAND_FLAG, true);
        boolean shouldGrant = markStarterWandsIfFirstJoin(persisted);
        if (shouldGrant) throw new RuntimeException("Existing flag must skip grant");
        System.out.println("[OK] existing flag skips grant");
    }

    static void testLegacyFlagMigrationWithMissingAndExistingLegacyFlag() {
        Map<String, Boolean> playerData = new HashMap<>();
        Map<String, Boolean> persisted = new HashMap<>();

        if (migrateLegacyStarterKitFlag(playerData, persisted)) {
            throw new RuntimeException("Migration should not run when legacy flag is missing");
        }

        playerData.put(LEGACY_STARTER_KIT_FLAG, true);
        boolean migrated = migrateLegacyStarterKitFlag(playerData, persisted);
        if (!migrated) throw new RuntimeException("Legacy flag should migrate");
        if (!persisted.getOrDefault(STARTER_WAND_FLAG, false)) {
            throw new RuntimeException("Migration should set new starter wand flag");
        }
        if (playerData.containsKey(LEGACY_STARTER_KIT_FLAG)) {
            throw new RuntimeException("Migration should remove legacy flag");
        }
        System.out.println("[OK] legacy flag migration works for missing/existing cases");
    }

    // Mirrors ExampleMod.markStarterWandsIfFirstJoin
    static boolean markStarterWandsIfFirstJoin(Map<String, Boolean> persistedData) {
        if (persistedData.getOrDefault(STARTER_WAND_FLAG, false)) {
            return false;
        }
        persistedData.put(STARTER_WAND_FLAG, true);
        return true;
    }

    // Mirrors ExampleMod.migrateLegacyStarterKitFlag
    static boolean migrateLegacyStarterKitFlag(Map<String, Boolean> playerData, Map<String, Boolean> persistedData) {
        if (!playerData.getOrDefault(LEGACY_STARTER_KIT_FLAG, false)) {
            return false;
        }
        persistedData.put(STARTER_WAND_FLAG, true);
        playerData.remove(LEGACY_STARTER_KIT_FLAG);
        return true;
    }
}
