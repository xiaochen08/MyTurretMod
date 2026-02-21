# Turret Upgrade Module System

## Overview
This document describes the unified upgrade-module architecture for `SkeletonTurret`.
`Teleport` and `Multi-Shot` are now integrated under one extensible module system.

## Core Interfaces
- `TurretUpgradeModule`:
  - `metadata()`
  - `activate(SkeletonTurret, TurretModuleState)`
  - `deactivate(SkeletonTurret, TurretModuleState)`
  - `getCooldown(SkeletonTurret, TurretModuleState)`
  - `refreshState(SkeletonTurret, TurretModuleState)`
  - `onTick(SkeletonTurret, TurretModuleState)`
  - `onRangedAttack(SkeletonTurret, LivingEntity, int, TurretModuleState)`
- `TurretModuleMetadata`:
  - `moduleId`, `name`, `description`, `icon`, `rarity`
- `TurretModuleRegistry`:
  - Built-in + dynamic registration (`registerBuiltin`, `registerDynamic`)
  - Hot reload entrypoint (`reload(Map<String, TurretUpgradeModule>)`)
  - Registry revision (`revision()`) for runtime cache invalidation
- `TurretModuleState`:
  - Shared state container for module communication.
  - Current fields: `teleportInstalled`, `multiShotLevel`.
  - Derived state: `isComboActive()`.
- `TurretUpgradeModuleManager`:
  - Dependency-injected module orchestrator.
  - Pulls modules from registry by default (`createDefault()`).
  - Auto refreshes module list when registry revision changes.
  - Handles refresh/tick/attack hooks with centralized error handling.

## Built-in Modules
- `TeleportTurretUpgradeModule`
  - Detects teleport module installation from turret upgrade slots (5-9).
  - Applies combo synergy tick when multi-shot is also enabled.
- `MultiShotTurretUpgradeModule`
  - Detects highest installed multi-shot module level from slots (5-9).
  - On ranged attack, dispatches extra arrows to distinct nearby enemies.
  - Arrow count scales by `min(enemyCount, levelLimit)`.

## Configuration
All module-system parameters are centralized in `TurretConfig.Common`:
- `UpgradeModuleSystem.moduleStateResyncIntervalTicks`
- `UpgradeModuleSystem.multiShotRange`
- `UpgradeModuleSystem.multiShotDamageScalePerAttackDamage`
- `UpgradeModuleSystem.multiShotSpeedScalePerAttackSpeed`
- `UpgradeModuleSystem.moduleVerboseLog`

Teleport cooldown settings remain in:
- `TeleportModule.teleportCooldownBase`
- `TeleportModule.teleportCooldownReductionPerTier`
- `TeleportModule.teleportCooldownMin`

Teleport tier data (`TeleportModuleRules`) is code-driven with per-tier fields:
- `levelId`
- `upgradeMaterials`
- `nameSuffix`
- `teleportCooldownSeconds`
- `blackHoleRange`
- `blackHoleCooldownSeconds`
- `friendlyFilterEnabled`

Tier design:
- L1: blink only, 10s cooldown.
- L2: blink only, 8s cooldown.
- L3: 6s cooldown + black hole radius 6, black hole cooldown 15s.
- L4: 3s cooldown + black hole radius 15, black hole cooldown 12s.
- L5: 1s cooldown + black hole radius 30, black hole cooldown 6s, frame-sliced pull.

FX gamerule:
- `/gamerule turretBlackHoleFx true|false`
- Controls black-hole visual/audio feedback without disabling server pull logic.

## Installation & Usage
1. Craft `multi_shot_upgrade_module` (amethyst shard / bow / string, vertical pattern).
2. Upgrade module level through anvil using mapped materials.
3. Install modules by:
   - Right-clicking turret with module item, or
   - Inserting module item into turret upgrade slots (5-9).
4. Turret AI will:
   - Use teleport ability if teleport module exists and cooldown is ready.
   - Fire multi-shot volley if multi-shot module exists.

## Error Handling and Logging
- Unified logger: `TurretModuleLog`.
- Per-module lifecycle hooks are wrapped in try/catch inside manager.
- One module failure does not stop other modules.

## Redundant/Extensible Design (Future Modules)
To add new modules:
1. Implement `TurretUpgradeModule`.
2. Define metadata with a unique `moduleId`.
3. Register module in `TurretModuleRegistry` (builtin or dynamic).
4. Add state fields to `TurretModuleState` if cross-module data is needed.
5. Add config entries in `TurretConfig` for tunable behavior.

The manager + shared state approach keeps module contracts stable and supports future expansion without breaking existing modules.
