package com.example.examplemod;

import java.util.ArrayList;
import java.util.List;

// Mock classes for testing logic without Minecraft dependencies
class MockDamageSource {
    String msgId;
    boolean isFall;
    boolean isOutOfWorld;

    public MockDamageSource(String msgId, boolean isFall, boolean isOutOfWorld) {
        this.msgId = msgId;
        this.isFall = isFall;
        this.isOutOfWorld = isOutOfWorld;
    }

    public boolean is(String type) {
        if ("fall".equals(type)) return isFall;
        if ("outOfWorld".equals(type)) return isOutOfWorld;
        return false;
    }
}

class MockTurret {
    int id;
    double y;
    double fallDistance;
    float health;
    boolean deadOrDying;
    boolean deathRecordDropped = false;
    List<String> logs = new ArrayList<>();
    List<String> drops = new ArrayList<>();

    public MockTurret(int id) {
        this.id = id;
        this.health = 20.0f;
    }

    public boolean hasDroppedRecord() { return deathRecordDropped; }
    public void setDroppedRecord(boolean dropped) { this.deathRecordDropped = dropped; }
    
    public void log(String msg) { logs.add(msg); }
    
    // Simulate hurt logic with override
    public boolean hurt(MockDamageSource source, float amount) {
        if (health - amount <= 0.5f) {
            log("KillOverride triggered");
            if (!hasDroppedRecord()) {
                dropRecord();
                setDroppedRecord(true);
            } else {
                log("Skipped drop (idempotent)");
            }
            health = 0;
            return true;
        }
        health -= amount;
        return false;
    }

    public void dropRecord() {
        drops.add("Death Record");
        log("Dropped Death Record");
    }
}

class MockConfig {
    static boolean enableFallDeathCapture = true;
    static double fallDeathHeightThreshold = 8.0;
    static boolean enableDeathRecordDrop = true;
}

public class TurretFallDeathTest {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  ☠️ Turret Fall Death & Drop Logic Verification");
        System.out.println("================================================");
        
        try {
            testFallDeathLogic();
            testIdempotency();
            testKillOverride();
            System.out.println("\n✅ ALL FALL DEATH TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testFallDeathLogic() {
        System.out.println("\n[Test 1] Verifying Fall Death Detection...");
        
        MockTurret turret = new MockTurret(1);
        turret.fallDistance = 10.0; // > 8.0
        MockDamageSource source = new MockDamageSource("fall", true, false);
        
        // Simulate Event Handler Logic
        handleDrops(turret, source);
        
        if (!turret.logs.contains("Fall Death Detected")) {
            throw new RuntimeException("Failed to detect fall death > 8 blocks");
        }
        if (!turret.drops.contains("Death Record")) {
            throw new RuntimeException("Failed to drop record on fall death");
        }
        System.out.println("  -> Fall > 8 blocks detected & dropped [OK]");
        
        // Test Low Fall
        MockTurret turret2 = new MockTurret(2);
        turret2.fallDistance = 5.0; // < 8.0
        handleDrops(turret2, source);
        
        if (turret2.logs.contains("Fall Death Detected")) {
            throw new RuntimeException("Incorrectly detected fall death < 8 blocks");
        }
        System.out.println("  -> Fall < 8 blocks ignored [OK]");
    }

    static void testIdempotency() {
        System.out.println("\n[Test 2] Verifying Idempotency...");
        
        MockTurret turret = new MockTurret(3);
        MockDamageSource source = new MockDamageSource("generic", false, false);
        
        // First drop
        handleDrops(turret, source);
        if (turret.drops.size() != 1) throw new RuntimeException("First drop failed");
        if (!turret.hasDroppedRecord()) throw new RuntimeException("Flag not set");
        
        // Second drop (should skip)
        handleDrops(turret, source);
        if (turret.drops.size() != 1) throw new RuntimeException("Duplicate drop occurred!");
        
        System.out.println("  -> Duplicate drops prevented [OK]");
    }

    static void testKillOverride() {
        System.out.println("\n[Test 3] Verifying Kill Override (HP=1)...");
        
        MockTurret turret = new MockTurret(4);
        turret.health = 1.0f;
        MockDamageSource source = new MockDamageSource("outOfWorld", false, true);
        
        // Hurt enough to kill
        turret.hurt(source, 2.0f);
        
        if (!turret.logs.contains("KillOverride triggered")) {
            throw new RuntimeException("Kill override did not trigger");
        }
        if (turret.drops.size() != 1) {
            throw new RuntimeException("Kill override did not drop record");
        }
        if (!turret.hasDroppedRecord()) {
            throw new RuntimeException("Kill override did not set flag");
        }
        
        // Simulate subsequent death event
        handleDrops(turret, source);
        if (turret.drops.size() != 1) {
            throw new RuntimeException("Event handler dropped duplicate record after override");
        }
        
        System.out.println("  -> Kill override works & syncs with event handler [OK]");
    }

    // Replicated Logic from ExampleMod.onLivingDrops & TurretConfig
    static void handleDrops(MockTurret turret, MockDamageSource source) {
        // Idempotency Check
        if (turret.hasDroppedRecord()) {
            return;
        }

        // Fall Death Check
        if (MockConfig.enableFallDeathCapture) {
            boolean isFall = source.is("fall") || source.is("outOfWorld");
            if (isFall) {
                if (turret.fallDistance >= MockConfig.fallDeathHeightThreshold) {
                    turret.log("Fall Death Detected");
                }
            }
        }

        // Drop Logic
        if (MockConfig.enableDeathRecordDrop) {
            turret.dropRecord();
            turret.setDroppedRecord(true);
        }
    }
}
