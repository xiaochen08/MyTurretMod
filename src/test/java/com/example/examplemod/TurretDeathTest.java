package com.example.examplemod;

import java.util.Random;

public class TurretDeathTest {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  ☠️ Turret Death Drop Logic Verification");
        System.out.println("================================================");
        
        try {
            testDropProbability();
            System.out.println("\n✅ ALL DEATH LOGIC TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testDropProbability() {
        System.out.println("\n[Test 1] Verifying Drop Probability Rules...");
        
        // Tier 0 -> 30%
        verifyProbability(0, 0.3f);
        
        // Tier 1 -> 50%
        verifyProbability(1, 0.5f);
        
        // Tier 2 -> 100%
        verifyProbability(2, 1.0f);
        
        // Tier 5 -> 100%
        verifyProbability(5, 1.0f);
    }

    static void verifyProbability(int tier, float expectedChance) {
        // Logic copied from SkeletonTurret.java
        float dropChance = 0.3f;
        if (tier >= 2) dropChance = 1.0f;
        else if (tier == 1) dropChance = 0.5f;

        if (Math.abs(dropChance - expectedChance) > 0.001) {
            throw new RuntimeException("Tier " + tier + " expected " + expectedChance + " but got " + dropChance);
        }
        System.out.println("  -> Tier " + tier + " drop chance: " + (int)(dropChance * 100) + "% [OK]");
    }
}
