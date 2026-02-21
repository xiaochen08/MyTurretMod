package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportGatewayEnforcementTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Teleport Gateway Enforcement Tests");
        System.out.println("================================================");
        try {
            testCooldownRejectSimulation();
            testDamageTriggeredBlackHoleContract();
            testBlackHoleEntityCoverageContract();
            System.out.println("\nALL TELEPORT GATEWAY TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testCooldownRejectSimulation() {
        MockCooldownGate gate = new MockCooldownGate(40);
        UUID id = UUID.randomUUID();
        if (!gate.allow(id, 100)) throw new RuntimeException("first teleport should pass");
        gate.mark(id, 100);
        if (gate.allow(id, 120)) throw new RuntimeException("cooldown period should block teleport");
        if (!gate.allow(id, 141)) throw new RuntimeException("cooldown expiry should allow teleport again");
        System.out.println("[OK] cooldown reject simulation");
    }

    static void testDamageTriggeredBlackHoleContract() throws Exception {
        String turret = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        String emergencyGoal = Files.readString(Paths.get("src/main/java/com/example/examplemod/TurretEmergencyTeleportGoal.java"));
        if (!turret.contains("TeleportRequestSource.TURRET_DAMAGE_REACTION, true")) {
            throw new RuntimeException("damage-triggered teleport must pass damageTriggered=true");
        }
        if (!turret.contains("this.onTeleportCompleted(fromPos, damageTriggered);")) {
            throw new RuntimeException("teleport completion should route through explicit trigger flag");
        }
        if (!emergencyGoal.contains("turret.onTeleportCompleted(startPos, false);")) {
            throw new RuntimeException("non-damage emergency teleport must not trigger black-hole");
        }
        System.out.println("[OK] damage-triggered black-hole contract");
    }

    static void testBlackHoleEntityCoverageContract() throws Exception {
        String turret = Files.readString(Paths.get("src/main/java/com/example/examplemod/SkeletonTurret.java"));
        if (turret.contains("WitherBoss") || turret.contains("EnderDragon")) {
            throw new RuntimeException("boss hard exclusion should be removed");
        }
        if (turret.contains("instanceof Monster || entity instanceof Enemy")) {
            throw new RuntimeException("black-hole should not be limited to monster/enemy only");
        }
        if (!turret.contains("private boolean canBePulledByBlackHole")) {
            throw new RuntimeException("black-hole filter method missing");
        }
        System.out.println("[OK] black-hole entity coverage contract");
    }

    static class MockCooldownGate {
        private final int cooldownTicks;
        private final Map<UUID, Long> until = new HashMap<>();

        MockCooldownGate(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
        }

        boolean allow(UUID id, long now) {
            return now >= until.getOrDefault(id, 0L);
        }

        void mark(UUID id, long now) {
            until.put(id, now + cooldownTicks);
        }
    }
}
