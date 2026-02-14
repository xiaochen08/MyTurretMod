package com.example.examplemod;

public class TurretModuleIntegrationTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Turret Module Integration Tests");
        System.out.println("================================================");

        try {
            testStateSyncAndComboFlag();
            testCombatVolleyCountScenarios();
            testTeleportCooldownSynergy();
            System.out.println("\nALL TURRET MODULE INTEGRATION TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testStateSyncAndComboFlag() {
        TurretModuleState state = new TurretModuleState();
        state.setTeleportInstalled(true);
        state.setMultiShotLevel(3);

        if (!state.hasAnyModule()) throw new RuntimeException("Expected module state to be active");
        if (!state.isComboActive()) throw new RuntimeException("Expected teleport + multishot combo to be active");
        System.out.println("[OK] module state sync/combo flag");
    }

    static void testCombatVolleyCountScenarios() {
        int level = 4; // up to 5 arrows
        if (MultiShotModuleRules.computeVolleyCount(level, 1) != 1) {
            throw new RuntimeException("1 enemy should produce 1 arrow");
        }
        if (MultiShotModuleRules.computeVolleyCount(level, 2) != 2) {
            throw new RuntimeException("2 enemies should produce 2 arrows");
        }
        if (MultiShotModuleRules.computeVolleyCount(level, 10) != 5) {
            throw new RuntimeException("Enemy count should be capped by level arrow limit");
        }
        System.out.println("[OK] combat volley scaling logic");
    }

    static void testTeleportCooldownSynergy() {
        int baseCooldown = 12;
        int noCombo = TeleportTurretUpgradeModule.computeNextCooldown(baseCooldown, false, 20);
        int comboNoTick = TeleportTurretUpgradeModule.computeNextCooldown(baseCooldown, true, 19);
        int comboTick = TeleportTurretUpgradeModule.computeNextCooldown(baseCooldown, true, 20);

        if (noCombo != baseCooldown) throw new RuntimeException("Cooldown should not change without combo");
        if (comboNoTick != baseCooldown) throw new RuntimeException("Cooldown should not change off schedule");
        if (comboTick != baseCooldown - 1) throw new RuntimeException("Cooldown should reduce by 1 on combo tick");
        System.out.println("[OK] teleport + multishot cooldown synergy");
    }
}
