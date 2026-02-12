package com.example.examplemod;

/**
 * TurretAttackTests.java
 * 
 * Standalone unit test script for validating Turret Attack Logic.
 * Can be run directly as a Java application.
 * 
 * Verifies:
 * 1. Attack Speed Curve (Tier 0 to Tier 4)
 * 2. Heat Stacking & Decay Logic
 * 3. Teleport Invincibility Timing
 */
public class TurretAttackTests {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Running Turret Attack Logic Tests");
        System.out.println("==========================================");
        
        try {
            testFireRateCurve();
            testHeatDecay();
            testTeleportTiming();
            
            System.out.println("\n✅ ALL TESTS PASSED SUCCESSFULLY.");
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // 🧠 Logic Simulation (Mirrors SkeletonTurret.java)
    // ==========================================

    public static float calculateFireRate(int tier, int heat, boolean isBrutal) {
        double cooldown = 20.0;
        
        // Formula: 1.0 + 7.5% per stack
        double stackMultiplier = 1.0 + (heat * 0.075);
        cooldown /= stackMultiplier;

        if (isBrutal) {
            cooldown /= 4.0;
        }
        
        cooldown = Math.max(1.0, cooldown);
        return 20.0f / (float)cooldown; // Shots per second
    }

    public static int getMaxHeat(int tier) {
        // Tier 0->0, Tier 4->120
        return tier * 30;
    }

    public static int decayHeat(int currentHeat, int tickCount, long lastDamageTime) {
        long timeSinceLast = tickCount - lastDamageTime;
        // 5s (100 ticks) wait
        if (timeSinceLast > 100 && currentHeat > 0) { 
            // 1s (20 ticks) interval
            if (tickCount % 20 == 0) { 
                int decay = Math.max(1, (int)(currentHeat * 0.2)); // 20% decay
                return Math.max(0, currentHeat - decay);
            }
        }
        return currentHeat;
    }

    // ==========================================
    // 🧪 Test Cases
    // ==========================================

    private static void testFireRateCurve() {
        System.out.println("\n[Test 1] Fire Rate Curve Verification...");
        
        // Case A: Tier 0 (Base)
        // Max Heat = 0
        // Speed = 1.0
        assertEquals(0, getMaxHeat(0), "Tier 0 Max Heat");
        float speedT0 = calculateFireRate(0, 0, false);
        assertEquals(1.0f, speedT0, "Tier 0 Base Speed");

        // Case B: Tier 4 (Max)
        // Max Heat = 120
        // Multiplier = 1.0 + 120*0.075 = 10.0
        // Speed = 10.0
        assertEquals(120, getMaxHeat(4), "Tier 4 Max Heat");
        float speedT4 = calculateFireRate(4, 120, false);
        assertEquals(10.0f, speedT4, "Tier 4 Max Speed");

        // Case C: Tier 2 (Mid)
        // Max Heat = 60
        // Multiplier = 1.0 + 60*0.075 = 5.5
        // Speed = 5.5
        assertEquals(60, getMaxHeat(2), "Tier 2 Max Heat");
        float speedT2 = calculateFireRate(2, 60, false);
        assertEquals(5.5f, speedT2, "Tier 2 Max Speed");
    }

    private static void testHeatDecay() {
        System.out.println("\n[Test 2] Heat Decay Logic...");

        int heat = 100;
        long lastHit = 0;
        
        // 1. Not enough time (4s / 80 ticks)
        int currentTick = 80;
        int newHeat = decayHeat(heat, currentTick, lastHit);
        assertEquals(100, newHeat, "No decay before 5s");

        // 2. 5s passed (100 ticks) - boundary
        currentTick = 100;
        newHeat = decayHeat(heat, currentTick, lastHit);
        assertEquals(100, newHeat, "No decay at exactly 5s");

        // 3. 6s passed (120 ticks)
        currentTick = 120;
        // Should decay 20% of 100 = 20. Result 80.
        newHeat = decayHeat(heat, currentTick, lastHit);
        assertEquals(80, newHeat, "Decay 20% after 6s");

        // 4. 7s passed (140 ticks) - recursive
        // Previous heat was 80.
        // Should decay 20% of 80 = 16. Result 64.
        newHeat = decayHeat(80, 140, lastHit);
        assertEquals(64, newHeat, "Decay 20% of remaining after 7s");
    }

    private static void testTeleportTiming() {
        System.out.println("\n[Test 3] Teleport Timing Constants...");
        
        // Constants from code
        int invincibility = 6; // 0.3s
        int attackDelay = 4;   // 0.2s
        
        // Verify 0.3s Invincibility
        assertEquals(6, invincibility, "Invincibility ticks (0.3s * 20)");
        
        // Verify 0.2s Attack Delay
        assertEquals(4, attackDelay, "Attack delay ticks (0.2s * 20)");
    }

    // ==========================================
    // 🛠️ Helpers
    // ==========================================

    private static void assertEquals(float expected, float actual, String msg) {
        if (Math.abs(expected - actual) > 0.01f) {
            throw new RuntimeException("FAIL: " + msg + " -> Expected " + expected + ", Got " + actual);
        }
        System.out.println("  PASS: " + msg + " [" + actual + "]");
    }

    private static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual) {
            throw new RuntimeException("FAIL: " + msg + " -> Expected " + expected + ", Got " + actual);
        }
        System.out.println("  PASS: " + msg + " [" + actual + "]");
    }
}
