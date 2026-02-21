# Skeleton Turret Death Plaque Force-Drop Guide

## Overview
- Goal: ensure `SkeletonTurret` death always produces a death plaque drop, even if other listeners cancel/modify drops.
- Scope: startup, `/reload`, config hot reload, and runtime combat events.

## Config Fields
- `Loot.deathPlaqueDropChance`
  - Legacy probability field retained for compatibility.
  - Force-drop pipeline ignores this value for `SkeletonTurret` death enforcement (always enforced).
- `Loot.enableDeathPlaqueGc`
  - Controls garbage collection of dropped plaque entities after TTL.
- `Loot.deathPlaqueItemTtlSeconds`
  - Controls plaque item lifetime before cleanup.

## Force Enforcement Flow
1. `LivingDropsEvent` at `HIGHEST` (`receiveCanceled = true`) inserts forced plaque into drop list.
2. If event is already canceled, logic uncancels it and logs forced repair.
3. `LivingDropsEvent` at `MONITOR` performs write-back verification:
   - if canceled again or plaque missing, force list repair + direct entity spawn.
4. Deferred audit queue validates next ticks:
   - if no matching plaque entity is found nearby, direct spawn fallback is executed.
5. Runtime validation runs on:
   - server started,
   - datapack sync (`/reload` path),
   - mod config reloading.

## Logging
- Prefix: `[ForcedPlaque]`
- Key channels:
  - `[Repair]`: immediate forced write-back/uncancel actions.
  - `[AuditRepair]`: delayed audit fallback spawn.
  - `[RuntimeCheck]`: startup/reload/hot-reload validation and cleanup.

## Conflict Troubleshooting
1. Confirm highest-priority handler and monitor handler are both registered in `ExampleMod`.
2. Search logs for `[ForcedPlaque][Repair]` and `[ForcedPlaque][AuditRepair]`.
3. If third-party plugin keeps removing drop entities:
   - verify audit fallback logs are present;
   - inspect plugin load order and event hooks touching `LivingDropsEvent`.
4. If no logs appear after turret death:
   - verify mod is loaded and `MinecraftForge.EVENT_BUS.register(this)` executed;
   - verify affected entity is `SkeletonTurret`.

## Rollback Plan
1. Revert `ExampleMod.java` force-drop handler additions (`onLivingDropsMonitor`, runtime validation hooks, audit logic).
2. Remove forced-drop test script `src/test/java/com/example/examplemod/ForcedPlaqueDropEnforcementTests.java`.
3. Remove this guide file.
4. Re-run build and baseline death-plaque tests to confirm old behavior is restored.
