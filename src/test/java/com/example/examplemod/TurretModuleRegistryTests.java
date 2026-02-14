package com.example.examplemod;

import java.util.Map;

public class TurretModuleRegistryTests {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Turret Module Registry Tests");
        System.out.println("================================================");

        try {
            testBuiltinRegistryState();
            testDynamicRegistrationAndRevisionBump();
            testReloadReplacesDynamicModules();
            System.out.println("\nALL TURRET MODULE REGISTRY TESTS PASSED.");
        } catch (Exception e) {
            System.err.println("\nTEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testBuiltinRegistryState() {
        if (!TurretModuleRegistry.ids().contains("teleport")) {
            throw new RuntimeException("builtin teleport module not found");
        }
        if (!TurretModuleRegistry.ids().contains("multi_shot")) {
            throw new RuntimeException("builtin multi_shot module not found");
        }
        if (TurretModuleRegistry.get("teleport") == null || TurretModuleRegistry.get("multi_shot") == null) {
            throw new RuntimeException("builtin module lookup returned null");
        }
        System.out.println("[OK] builtin module registry state");
    }

    static void testDynamicRegistrationAndRevisionBump() {
        long before = TurretModuleRegistry.revision();
        TurretUpgradeModule module = new DummyModule("dummy_runtime");
        TurretModuleRegistry.registerDynamic(module);

        if (TurretModuleRegistry.get("dummy_runtime") == null) {
            throw new RuntimeException("dynamic module registration failed");
        }
        if (TurretModuleRegistry.revision() <= before) {
            throw new RuntimeException("registry revision should increase after dynamic registration");
        }
        System.out.println("[OK] dynamic registration + revision bump");
    }

    static void testReloadReplacesDynamicModules() {
        TurretModuleRegistry.reload(Map.of("dummy_reload", new DummyModule("dummy_reload")));
        if (TurretModuleRegistry.get("dummy_reload") == null) {
            throw new RuntimeException("dynamic module missing after reload");
        }
        if (TurretModuleRegistry.get("teleport") == null || TurretModuleRegistry.get("multi_shot") == null) {
            throw new RuntimeException("builtin modules must be present after reload");
        }
        if (TurretModuleRegistry.get("dummy_runtime") != null) {
            throw new RuntimeException("stale dynamic module should be replaced during reload");
        }
        System.out.println("[OK] registry reload behavior");
    }

    static final class DummyModule implements TurretUpgradeModule {
        private final String id;

        DummyModule(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public TurretModuleMetadata metadata() {
            return new TurretModuleMetadata(id, "Dummy", "Runtime test module", "test_icon", TurretModuleRarity.COMMON);
        }

        @Override
        public void refreshState(SkeletonTurret turret, TurretModuleState state) {
            // no-op
        }
    }
}
