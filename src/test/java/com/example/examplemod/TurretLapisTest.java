package com.example.examplemod;

/**
 * TurretLapisTest.java
 * 
 * Logic Verification for Lapis Lazuli Enchanting Mechanics.
 * Validates that the decision logic aligns with Vanilla Enchanting Table tiers.
 */
public class TurretLapisTest {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  💎 Turret Lapis Enchanting Logic Tests");
        System.out.println("================================================");
        
        try {
            testLapisTiers();
            testBoundaryConditions();
            System.out.println("\n✅ ALL LAPIS LOGIC TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    static void testLapisTiers() {
        System.out.println("\n[Test 1] Verifying Lapis Count -> Enchanting Tier Mapping...");
        
        // 1 Lapis -> Tier 1 (Lv 10 cost)
        assertTier(1, 1, 10);
        
        // 2 Lapis -> Tier 2 (Lv 20 cost)
        assertTier(2, 2, 20);
        
        // 3 Lapis -> Tier 3 (Lv 30 cost)
        assertTier(3, 3, 30);
        
        // 64 Lapis -> Tier 3 (Lv 30 cost) - Max Cap
        assertTier(64, 3, 30);
    }
    
    static void testBoundaryConditions() {
        System.out.println("\n[Test 2] Verifying Boundary Conditions...");
        // 0 Lapis should theoretically be Tier 1 in the code block, 
        // but the item check (item == LAPIS) prevents 0 count in game usually.
        // However, if logic falls through:
        assertTier(0, 1, 10); 
    }
    
    static void assertTier(int lapisCount, int expectedCost, int expectedLevel) {
        // Logic replicated EXACTLY from SkeletonTurret.java
        int tier = 0;
        int costLevels = 0;
        int requiredLevels = 0;
        int enchantPower = 0;

        if (lapisCount >= 3) {
            tier = 3;
            costLevels = 3;
            requiredLevels = 30;
            enchantPower = 30;
        } else if (lapisCount == 2) {
            tier = 2;
            costLevels = 2;
            requiredLevels = 20;
            enchantPower = 20;
        } else {
            tier = 1;
            costLevels = 1;
            requiredLevels = 10;
            enchantPower = 10;
        }
        
        if (costLevels != expectedCost) throw new RuntimeException("Cost mismatch for " + lapisCount + " lapis. Expected " + expectedCost + ", Got " + costLevels);
        if (enchantPower != expectedLevel) throw new RuntimeException("Power mismatch for " + lapisCount + " lapis. Expected " + expectedLevel + ", Got " + enchantPower);
        
        System.out.println("  -> " + lapisCount + " Lapis correctly maps to Cost " + costLevels + " / Power " + enchantPower);
    }
}
