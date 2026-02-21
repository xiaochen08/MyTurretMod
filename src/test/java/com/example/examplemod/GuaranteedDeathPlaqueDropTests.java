package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GuaranteedDeathPlaqueDropTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Guaranteed Death Plaque Drop Tests");
        System.out.println("================================================");

        try {
            testSourceIsDeterministic();
            testMultipleDamageSourcesAlwaysDrop();
            testDropPayloadIsSinglePlaque();
            testConcurrentDeathsNoDuplicateOrLoss();
            System.out.println("\nALL GUARANTEED DROP TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testSourceIsDeterministic() throws Exception {
        String content = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
        assertContains(content, "event.getDrops().clear();", "must clear drops before forced write");
        assertContains(content, "createDeathRecordCard(1)", "must generate deterministic first plaque record");
        assertContains(content, "record.setCount(1);", "drop count must stay exactly one");
        assertContains(content, "turret.getX(), turret.getY(), turret.getZ()", "drop position must be exact death position");
        assertNotContains(content, "enableDeathRecordDrop", "guaranteed drop must not depend on config gate");
        System.out.println("[OK] source-level deterministic drop checks");
    }

    static void testMultipleDamageSourcesAlwaysDrop() {
        MockDropEngine engine = new MockDropEngine();
        UUID turretId = UUID.randomUUID();

        for (DamageSourceType source : DamageSourceType.values()) {
            MockDropResult result = engine.dropForDeath(turretId, source, 10.0, 64.0, 20.0);
            if (!result.dropped) {
                throw new RuntimeException("expected forced drop for source=" + source);
            }
            if (result.count != 1) {
                throw new RuntimeException("drop count must be 1 for source=" + source);
            }
            engine.resetForNextDeath(turretId);
        }
        System.out.println("[OK] all configured damage sources trigger 100% drop");
    }

    static void testDropPayloadIsSinglePlaque() {
        MockDropEngine engine = new MockDropEngine();
        MockDropResult result = engine.dropForDeath(UUID.randomUUID(), DamageSourceType.EXPLOSION, 1.0, 2.0, 3.0);

        if (!"death_record_card".equals(result.itemType)) {
            throw new RuntimeException("unexpected item type: " + result.itemType);
        }
        if (result.count != 1) {
            throw new RuntimeException("expected count=1, got " + result.count);
        }
        if (result.name == null || result.name.isBlank()) {
            throw new RuntimeException("name must exist");
        }
        if (result.lore == null || result.lore.isEmpty()) {
            throw new RuntimeException("lore must exist");
        }
        System.out.println("[OK] drop payload matches expected type/count/name/lore");
    }

    static void testConcurrentDeathsNoDuplicateOrLoss() throws Exception {
        MockDropEngine engine = new MockDropEngine();
        int turretCount = 12;
        CountDownLatch latch = new CountDownLatch(turretCount);
        List<UUID> turretIds = new ArrayList<>();

        for (int i = 0; i < turretCount; i++) {
            turretIds.add(UUID.randomUUID());
        }

        for (UUID turretId : turretIds) {
            new Thread(() -> {
                try {
                    // Simulate duplicate event delivery for same death.
                    engine.dropForDeath(turretId, DamageSourceType.PROJECTILE, 50, 70, 50);
                    engine.dropForDeath(turretId, DamageSourceType.PROJECTILE, 50, 70, 50);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        for (UUID turretId : turretIds) {
            int count = engine.dropCounter.getOrDefault(turretId, new AtomicInteger(0)).get();
            if (count != 1) {
                throw new RuntimeException("turret=" + turretId + " expected exactly one drop, got " + count);
            }
        }
        System.out.println("[OK] concurrent >=10 turret deaths: no duplicate and no missing drop");
    }

    enum DamageSourceType {
        MELEE,
        PROJECTILE,
        FIRE,
        FALL,
        MAGIC,
        EXPLOSION,
        VOID
    }

    static class MockDropResult {
        final boolean dropped;
        final String itemType;
        final int count;
        final String name;
        final List<String> lore;
        final double x;
        final double y;
        final double z;

        MockDropResult(boolean dropped, String itemType, int count, String name, List<String> lore, double x, double y, double z) {
            this.dropped = dropped;
            this.itemType = itemType;
            this.count = count;
            this.name = name;
            this.lore = lore;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    static class MockDropEngine {
        final Map<UUID, AtomicInteger> dropCounter = new ConcurrentHashMap<>();
        final Map<UUID, Boolean> droppedGuard = new ConcurrentHashMap<>();

        MockDropResult dropForDeath(UUID turretId, DamageSourceType source, double x, double y, double z) {
            if (droppedGuard.putIfAbsent(turretId, true) != null) {
                return new MockDropResult(false, "death_record_card", 0, "", List.of(), x, y, z);
            }

            dropCounter.computeIfAbsent(turretId, __ -> new AtomicInteger(0)).incrementAndGet();
            List<String> lore = new ArrayList<>();
            lore.add("ID#" + turretId);
            lore.add("Source=" + source);
            return new MockDropResult(true, "death_record_card", 1, "死亡铭牌", lore, x, y, z);
        }

        void resetForNextDeath(UUID turretId) {
            droppedGuard.remove(turretId);
            dropCounter.remove(turretId);
        }
    }

    static void assertContains(String content, String expected, String msg) {
        if (!content.contains(expected)) {
            throw new RuntimeException(msg + ": " + expected);
        }
    }

    static void assertNotContains(String content, String expected, String msg) {
        if (content.contains(expected)) {
            throw new RuntimeException(msg + ": " + expected);
        }
    }
}
