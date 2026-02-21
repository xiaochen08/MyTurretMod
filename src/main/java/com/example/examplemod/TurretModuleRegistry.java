package com.example.examplemod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class TurretModuleRegistry {
    private static final Map<String, TurretUpgradeModule> MODULES = new LinkedHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong(0);

    static {
        registerBuiltin(new TeleportTurretUpgradeModule());
        registerBuiltin(new MultiShotTurretUpgradeModule());
    }

    private TurretModuleRegistry() {}

    public static synchronized void registerBuiltin(TurretUpgradeModule module) {
        MODULES.put(module.id(), module);
        REVISION.incrementAndGet();
    }

    public static synchronized void registerDynamic(TurretUpgradeModule module) {
        MODULES.put(module.id(), module);
        REVISION.incrementAndGet();
        TurretModuleLog.info("module dynamically registered id={}", module.id());
    }

    public static synchronized void reload(Map<String, TurretUpgradeModule> dynamicModules) {
        MODULES.clear();
        registerBuiltin(new TeleportTurretUpgradeModule());
        registerBuiltin(new MultiShotTurretUpgradeModule());
        for (TurretUpgradeModule m : dynamicModules.values()) {
            MODULES.put(m.id(), m);
        }
        REVISION.incrementAndGet();
        TurretModuleLog.info("module registry reloaded size={}", MODULES.size());
    }

    public static synchronized TurretUpgradeModule get(String moduleId) {
        return MODULES.get(moduleId);
    }

    public static synchronized Collection<TurretUpgradeModule> all() {
        return List.copyOf(MODULES.values());
    }

    public static synchronized List<String> ids() {
        return new ArrayList<>(MODULES.keySet());
    }

    public static long revision() {
        return REVISION.get();
    }
}
