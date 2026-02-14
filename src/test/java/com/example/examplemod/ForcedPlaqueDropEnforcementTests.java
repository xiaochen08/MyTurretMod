package com.example.examplemod;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ForcedPlaqueDropEnforcementTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Forced Plaque Drop Enforcement Tests");
        System.out.println("================================================");
        try {
            testSourceHooksPresent();
            testIdempotentWriteBackSimulation();
            testConcurrentRepairSimulation();
            testReloadValidationSimulation();
            testStressSimulation();
            System.out.println("\nALL FORCED PLAQUE TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testSourceHooksPresent() throws Exception {
        String content = Files.readString(Paths.get("src/main/java/com/example/examplemod/ExampleMod.java"));
        assertContains(content, "onLivingDrops(LivingDropsEvent event)", "missing early drop hook");
        assertContains(content, "EventPriority.HIGHEST, receiveCanceled = true", "missing highest priority or receiveCanceled");
        assertContains(content, "onLivingDropsMonitor(LivingDropsEvent event)", "missing monitor write-back hook");
        assertContains(content, "onServerStarted(ServerStartedEvent event)", "missing startup runtime validation");
        assertContains(content, "onDatapackSync(OnDatapackSyncEvent event)", "missing /reload runtime validation");
        assertContains(content, "onConfigReload(ModConfigEvent.Reloading event)", "missing hot-reload runtime validation");
        assertContains(content, "processPendingPlaqueAudits(levelForAudit)", "missing tick audit repair");
        System.out.println("[OK] source-level enforcement hooks exist");
    }

    static void testIdempotentWriteBackSimulation() {
        MockForcedDropEngine engine = new MockForcedDropEngine();
        UUID turretId = UUID.randomUUID();

        boolean first = engine.forceWriteBack(turretId, 1200L);
        boolean second = engine.forceWriteBack(turretId, 1201L);
        boolean third = engine.forceWriteBack(turretId, 1202L);

        if (!first) throw new RuntimeException("first write-back should succeed");
        if (second || third) throw new RuntimeException("duplicate write-back should be deduplicated");
        if (engine.dropCount.get() != 1) throw new RuntimeException("idempotency broken: expected 1 drop");
        System.out.println("[OK] idempotent write-back simulation");
    }

    static void testConcurrentRepairSimulation() throws Exception {
        MockForcedDropEngine engine = new MockForcedDropEngine();
        UUID turretId = UUID.randomUUID();
        int workers = 16;
        CountDownLatch latch = new CountDownLatch(workers);

        for (int i = 0; i < workers; i++) {
            new Thread(() -> {
                try {
                    engine.forceWriteBack(turretId, 2200L);
                    engine.removeDropByExternalPlugin();
                    engine.forceWriteBack(turretId, 2201L);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        if (engine.dropCount.get() < 1) throw new RuntimeException("concurrent repair failed: no drop restored");
        System.out.println("[OK] concurrent override simulation");
    }

    static void testReloadValidationSimulation() {
        MockForcedDropEngine engine = new MockForcedDropEngine();
        UUID turretId = UUID.randomUUID();
        engine.guard.put(turretId, 50L);
        engine.auditQueue.add(10L);
        engine.auditQueue.add(300L);
        engine.runtimeValidate(300L);

        if (!engine.guard.isEmpty()) throw new RuntimeException("expired guard should be cleaned on reload");
        if (engine.auditQueue.size() != 1) throw new RuntimeException("stale audits should be cleaned on reload");
        System.out.println("[OK] runtime validation cleanup simulation");
    }

    static void testStressSimulation() {
        MockForcedDropEngine engine = new MockForcedDropEngine();
        UUID turretId = UUID.randomUUID();
        int repaired = 0;
        for (int i = 0; i < 100000; i++) {
            if ((i % 7) == 0) {
                engine.removeDropByExternalPlugin();
            }
            if (engine.forceWriteBack(turretId, 5000L + i)) {
                repaired++;
            }
        }
        if (engine.dropCount.get() < 1) throw new RuntimeException("stress simulation lost all drops");
        if (repaired < 1) throw new RuntimeException("stress simulation performed no repairs");
        System.out.println("[OK] high-volume stress simulation");
    }

    static void assertContains(String content, String expected, String error) {
        if (!content.contains(expected)) throw new RuntimeException(error + ": " + expected);
    }

    static class MockForcedDropEngine {
        private static final long DEDUP_WINDOW = 20L;
        final Map<UUID, Long> guard = new ConcurrentHashMap<>();
        final ConcurrentLinkedQueue<Long> auditQueue = new ConcurrentLinkedQueue<>();
        final AtomicInteger dropCount = new AtomicInteger(0);

        boolean forceWriteBack(UUID turretId, long now) {
            Long until = guard.get(turretId);
            if (until != null && until >= now && dropCount.get() > 0) {
                return false;
            }
            dropCount.set(1);
            guard.put(turretId, now + DEDUP_WINDOW);
            auditQueue.offer(now);
            return true;
        }

        void removeDropByExternalPlugin() {
            dropCount.set(0);
        }

        void runtimeValidate(long now) {
            guard.entrySet().removeIf(entry -> (entry.getValue() + DEDUP_WINDOW) < now);
            auditQueue.removeIf(createdAt -> (now - createdAt) > 200L);
        }
    }
}
