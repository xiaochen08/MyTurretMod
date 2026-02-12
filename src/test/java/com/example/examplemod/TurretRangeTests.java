package com.example.examplemod;

import java.util.HashMap;
import java.util.Map;

/**
 * TurretRangeTests.java
 * 
 * Standalone unit test script for validating Turret Range and Teleport Logic.
 * Can be run directly as a Java application.
 * 
 * Verifies:
 * 1. Range Calculation (Level 1 to 5)
 * 2. Teleport Cooldown Scaling (Tier 0 to Tier 5)
 * 3. Range Configuration Consistency
 */
public class TurretRangeTests {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Running Turret Range & Teleport Tests");
        System.out.println("==========================================");
        
        try {
            testRangeCalculation();
            testTeleportCooldownScaling();
            testTierToRangeMapping(); // New test
            
            System.out.println("\n✅ ALL TESTS PASSED SUCCESSFULLY.");
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // 🧠 Logic Simulation
    // ==========================================

    // Mocking RANGE_CONFIG from SkeletonTurret
    private static final Map<Integer, Double> MOCK_RANGE_CONFIG = new HashMap<>();
    static {
        MOCK_RANGE_CONFIG.put(1, 20.0);
        MOCK_RANGE_CONFIG.put(2, 32.0);
        MOCK_RANGE_CONFIG.put(3, 64.0);
        MOCK_RANGE_CONFIG.put(4, 128.0);
        MOCK_RANGE_CONFIG.put(5, 256.0);
    }

    public static double getAttackRange(int level) {
        if (!MOCK_RANGE_CONFIG.containsKey(level)) {
            return 20.0; // Default
        }
        return MOCK_RANGE_CONFIG.get(level);
    }

    // Mocking Teleport Cooldown Logic
    public static int getMaxTeleportCooldown(int tier) {
        // Default values from TurretConfig
        int base = 60; // 3 seconds
        int reduction = 10; // 0.5s per tier
        int min = 10; // 0.5s minimum

        return Math.max(min, base - (tier * reduction));
    }

    // Logic: Range Level = Tier + 1
    public static int getRangeLevelFromTier(int tier) {
        return tier + 1;
    }

    // ==========================================
    // 🧪 Test Cases
    // ==========================================

    private static void testRangeCalculation() {
        System.out.println("\n[Test 1] Range Calculation Verification...");
        
        // Level 1: 20m
        assertEquals(20.0, getAttackRange(1), "Level 1 Range");
        
        // Level 2: 32m
        assertEquals(32.0, getAttackRange(2), "Level 2 Range");
        
        // Level 3: 64m
        assertEquals(64.0, getAttackRange(3), "Level 3 Range");
        
        // Level 4: 128m
        assertEquals(128.0, getAttackRange(4), "Level 4 Range");
        
        // Level 5: 256m
        assertEquals(256.0, getAttackRange(5), "Level 5 Range");
        
        // Level 0 (Invalid -> Default)
        assertEquals(20.0, getAttackRange(0), "Level 0 (Invalid) Range");
    }

    private static void testTeleportCooldownScaling() {
        System.out.println("\n[Test 2] Teleport Cooldown Scaling Verification...");
        
        // Tier 0: Base 60 ticks (3s)
        assertEquals(60, getMaxTeleportCooldown(0), "Tier 0 Cooldown");
        
        // Tier 1: 50 ticks (2.5s)
        assertEquals(50, getMaxTeleportCooldown(1), "Tier 1 Cooldown");
        
        // Tier 2: 40 ticks (2s)
        assertEquals(40, getMaxTeleportCooldown(2), "Tier 2 Cooldown");
        
        // Tier 3: 30 ticks (1.5s)
        assertEquals(30, getMaxTeleportCooldown(3), "Tier 3 Cooldown");
        
        // Tier 4: 20 ticks (1s)
        assertEquals(20, getMaxTeleportCooldown(4), "Tier 4 Cooldown");
        
        // Tier 5: 10 ticks (0.5s) - Min cap check
        assertEquals(10, getMaxTeleportCooldown(5), "Tier 5 Cooldown");
        
        // Tier 6: 10 ticks (0.5s) - Should not go below min
        assertEquals(10, getMaxTeleportCooldown(6), "Tier 6 (Overcap) Cooldown");
    }

    // Helper
    private static void testTierToRangeMapping() {
        System.out.println("\n[Test 3] Tier -> Range Level Mapping Verification...");
        
        assertEquals(1, getRangeLevelFromTier(0), "Tier 0 -> Level 1");
        assertEquals(2, getRangeLevelFromTier(1), "Tier 1 -> Level 2");
        assertEquals(3, getRangeLevelFromTier(2), "Tier 2 -> Level 3");
        assertEquals(4, getRangeLevelFromTier(3), "Tier 3 -> Level 4");
        assertEquals(5, getRangeLevelFromTier(4), "Tier 4 -> Level 5");
    }

    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new RuntimeException(message + " Failed. Expected: " + expected + ", Got: " + actual);
        }
        System.out.println("  ✅ " + message + " Passed");
    }
    
    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new RuntimeException(message + " Failed. Expected: " + expected + ", Got: " + actual);
        }
        System.out.println("  ✅ " + message + " Passed");
    }
}
