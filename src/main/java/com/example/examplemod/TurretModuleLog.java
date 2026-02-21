package com.example.examplemod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class TurretModuleLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TurretModuleLog() {}

    public static void info(String msg, Object... args) {
        LOGGER.info("[TurretModule] " + msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn("[TurretModule] " + msg, args);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error("[TurretModule] " + msg, t);
    }
}
