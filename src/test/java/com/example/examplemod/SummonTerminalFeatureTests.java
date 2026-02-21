package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SummonTerminalFeatureTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Summon Terminal Feature Tests");
        System.out.println("================================================");

        try {
            testMissingTeleportModulePromptRemoved();
            testRenamePersistencePath();
            testRenameTriggersFullSync();
            testRenameValidationAndAckPath();
            testManualRenameLockAgainstCardOverride();
            testTerminalListUsesBaseName();
            testSummonBindsOwnerImmediately();
            testRecallCoordinateMath();
            testConcurrentRenameRollbackModel();
            System.out.println("\nALL SUMMON TERMINAL TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testMissingTeleportModulePromptRemoved() throws Exception {
        String content = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketSummonTerminalAction.java"));
        assertNotContains(content, "displayClientMessage(", "missing-module actionbar prompt should be removed");
        assertNotContains(content, "\u672A\u5B89\u88C5\u4F20\u9001\u6A21\u5757", "missing-module literal should be removed");
        System.out.println("[OK] missing-module actionbar prompt removed check");
    }

    static void testRenamePersistencePath() throws Exception {
        String action = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketSummonTerminalAction.java"));
        String turret = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));

        assertContains(action, "applyPlayerBaseName", "terminal rename should route to turret authoritative rename API");
        assertContains(action, "findOwnedTurretByUuid", "rename target must be resolved by UUID");
        assertContains(turret, "CustomBaseName", "custom name persistence key missing");
        System.out.println("[OK] rename persistence source path check");
    }

    static void testRenameTriggersFullSync() throws Exception {
        String action = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketSummonTerminalAction.java"));
        String screen = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalScreen.java"));
        String cache = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalClientCache.java"));

        assertContains(action, "pushFullSync(", "rename should trigger full sync");
        assertContains(action, "buildAllEntries(player)", "full sync should enumerate all owned turrets");
        assertContains(action, "new PacketSummonTerminalSnapshot(snapshot)", "full sync should push snapshot after deltas");
        assertContains(action, "broadcastDeltaToRelatedClients", "rename should broadcast delta to related viewers");
        assertContains(screen, "markRenameSyncStart(", "client should show rename syncing feedback");
        assertContains(cache, "isRenameSyncActive()", "cache should expose rename sync state");
        System.out.println("[OK] rename full-sync source path check");
    }

    static void testRenameValidationAndAckPath() throws Exception {
        String action = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketSummonTerminalAction.java"));
        String cache = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalClientCache.java"));
        String packet = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketSummonTerminalRenameResult.java"));
        String handler = Files.readString(Paths.get("src/main/java/com/example/examplemod/PacketHandler.java"));

        assertContains(action, "sanitizeBaseNameInput", "server must sanitize rename payload");
        assertContains(action, "sendRenameResult", "server must ACK rename request");
        assertContains(action, "requestId", "rename ACK should carry request id");
        assertContains(cache, "handleRenameResult", "client cache must process rename ACK");
        assertContains(cache, "consumeRenameErrorKey", "client cache must expose rollback error");
        assertContains(packet, "handleRenameResult", "rename result packet must update client cache");
        assertContains(handler, "PacketSummonTerminalRenameResult", "packet handler must register rename result packet");
        System.out.println("[OK] rename validation + ACK source path check");
    }

    static void testManualRenameLockAgainstCardOverride() throws Exception {
        String turret = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        assertContains(turret, "PLAYER_NAME_LOCK_TAG", "manual-name lock tag missing");
        assertContains(turret, "applyPlayerBaseName", "player rename API missing");
        assertContains(turret, "applyBaseNameFromIdCard", "id-card rename path missing");
        assertContains(turret, "restoreDefaultBaseNameFromIdCardRule", "id-card default restore path missing");
        assertContains(turret, "if (isPlayerNameLocked())", "manual name lock guard missing");
        System.out.println("[OK] manual rename lock coverage check");
    }

    static void testConcurrentRenameRollbackModel() {
        RenameStateMachine model = new RenameStateMachine("Alpha");

        int req1 = model.requestRename("Bravo"); // older request
        int req2 = model.requestRename("Charlie"); // newer request
        model.onAck(req1, false, "rename_failed"); // stale failure should not rollback newer optimistic name
        assertEquals("Charlie", model.visibleName, "stale failure must not rollback newer rename");

        model.onAck(req2, false, "rename_failed");
        assertEquals("Bravo", model.visibleName, "latest failure should rollback to previous stable name");
        assertEquals("rename_failed", model.lastErrorKey, "latest failure should expose error key");

        int req3 = model.requestRename("Delta");
        model.onAck(req3, true, "");
        assertEquals("Delta", model.visibleName, "success should commit renamed value");
        System.out.println("[OK] concurrent rename rollback model");
    }

    static final class RenameStateMachine {
        String visibleName;
        String stableName;
        String lastErrorKey = "";
        int seq;
        int pendingReq = -1;
        String rollbackName;

        RenameStateMachine(String initialName) {
            this.visibleName = initialName;
            this.stableName = initialName;
        }

        int requestRename(String newName) {
            rollbackName = visibleName;
            visibleName = newName;
            pendingReq = ++seq;
            return pendingReq;
        }

        void onAck(int reqId, boolean success, String errorKey) {
            if (reqId < pendingReq) {
                return;
            }
            if (success) {
                stableName = visibleName;
                return;
            }
            visibleName = rollbackName;
            stableName = rollbackName;
            lastErrorKey = errorKey;
        }
    }

    static void testTerminalListUsesBaseName() throws Exception {
        String screen = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalScreen.java"));
        assertContains(screen, "e.baseName()", "terminal list should render renamed base name");
        System.out.println("[OK] terminal list name binding check");
    }

    static void testSummonBindsOwnerImmediately() throws Exception {
        String item = Files.readString(Paths.get("src/main/java/com/example/examplemod/TurretItem.java"));
        assertContains(item, "turret.setOwner(context.getPlayer())", "summoned turret should bind owner immediately");
        System.out.println("[OK] summon owner binding check");
    }

    static void testRecallCoordinateMath() {
        try {
            String math = Files.readString(Paths.get("src/main/java/com/example/examplemod/SummonTerminalRecallMath.java"));
            assertContains(math, "radius = 2.0", "recall radius should be fixed at 2.0m");
            assertContains(math, "Math.cos(angle) * radius", "X coordinate should use cosine projection");
            assertContains(math, "Math.sin(angle) * radius", "Z coordinate should use sine projection");
            System.out.println("[OK] recall coordinate math check");
        } catch (Exception e) {
            throw new RuntimeException("failed to validate recall math source: " + e.getMessage(), e);
        }
    }

    static void assertContains(String content, String expected, String message) {
        if (!content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static void assertNotContains(String content, String expected, String message) {
        if (content.contains(expected)) {
            throw new RuntimeException(message + ": " + expected);
        }
    }

    static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new RuntimeException(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
