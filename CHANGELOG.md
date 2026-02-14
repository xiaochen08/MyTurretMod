# Changelog

## [Unreleased]

## [1.0.1] - 2026-02-14

### Added
- Added `TurretInfoBarBuffer` to receive/store migrated prompts with sequence-aware stale packet filtering.
- Added UI mode switch in `TurretScreen` for `Traditional` and `Info Bar` display modes (default: info bar).
- Added `TurretInfoBarBufferTests` covering single prompt, batch ordering, null skeleton id, and network latency out-of-order updates.
- Added localization keys for info bar title, skeleton prefix, mode labels, and tooltips in `en_us.json` and `zh_cn.json`.

### Changed
- Migrated prompt rendering previously shown above inventory into the info bar region.
- Prepended each migrated prompt with synchronized skeleton identifier: `骷髅编号：${id}`.
- Increased info bar area and added scroll behavior to avoid overlapping other HUD elements.

### Fixed
- Repaired invalid JSON structure in `zh_cn.json` to prevent language load failures.

### Added
- **Repair Mode**: Implemented a new repair mechanic.
    - Triggered by Shift + Right-Click with Iron Ingot.
    - Displays a 30-second countdown HUD above the turret.
    - Requires 5 consecutive right-clicks within 0.4s intervals to complete.
    - Successful repair restores health, plays anvil sound, and emits happy villager particles.
    - Failure or timeout cancels the repair state.
- **HUD**: Added a dedicated repair countdown HUD (text + progress bar) in `ClientForgeEvents.java`.
- **Unit Tests**: Added `TurretRepairTests.java` to verify repair logic and attack speed intervals.
- **Multi-Shot Module System**:
    - Added installable `multi_shot_upgrade_module` item with 5 upgrade tiers and per-tier color styling.
    - Added intelligent multi-target arrow volley logic (distinct nearest enemies, 1..6 scaling by enemy count and module level cap).
    - Added anvil upgrade flow with required materials: copper block, iron block, diamond block, blaze rod, ancient debris.
    - Added crafting recipe: amethyst shard (top), bow (center), string (bottom).
    - Added standalone rule tests in `MultiShotModuleRulesTests.java`.
- **Unified Turret Upgrade Module Architecture**:
    - Integrated teleport and multi-shot into one extensible module framework (`TurretUpgradeModule` + manager + shared state).
    - Added module state sync, centralized module config, cross-module combo behavior, and unified module logging/error handling.
    - Added integration tests in `TurretModuleIntegrationTests.java`.
- **Teleport Module Tier Rework**:
    - Reworked teleport module into 5 data-driven tiers aligned with multi-shot upgrade materials/cost rules.
    - Added independent teleport + black-hole cooldown timers and tiered black-hole behavior.
    - Added server-authoritative black-hole lifecycle (delay spawn, 20-tick pull, entity cap, resistance checks, wall-clip mitigation).
    - Added gamerule `turretBlackHoleFx` for toggling black-hole client FX.

### Changed
- **Starter Wand Grant Policy**:
    - Updated login reward logic to issue 2x summon wands only on a player's first-ever world entry.
    - Added permanent account-bound marker hasReceivedStarterWands to prevent repeated grants on relogin/device change/server restart.
    - Added compatibility migration from legacy marker HasReceivedStarterKit_Final.
    - New-account notice: only newly entering accounts receive the 2-wand starter reward; accounts that already claimed before will not receive it again.

- **Interaction**: Removed the old Shift+RightClick "Follow/Guard Mode Switch" and "Repair" logic.
- **Attack Speed**: 
    - Fixed attack speed stacking to have a minimum interval of 50ms between stacks (`shootLinearArrow`).
    - Stacking now respects the tier-based caps defined in `TurretAttackSpeedCurve.xlsx`.
- **GUI**:
    - Reorganized the "Mode Switch" button in `TurretScreen.java`.
    - Moved to the top-right corner (x=230, y=60).
    - Added custom styling (semi-transparent border, hover highlight).
    - Implemented optimistic updates for instant visual feedback.

### Fixed
- Fixed an issue where attack speed could stack instantaneously without delay.
- Fixed code nesting errors in `SkeletonTurret.java`.
- Fixed a bug where `SkeletonTurret` death plaque drops could be canceled/overridden.
  - Added forced drop insertion at highest-priority `LivingDropsEvent` with uncancel/write-back safeguards.
  - Added monitor-stage repair and delayed audit fallback to ensure stable drop recovery under conflicts.
  - Added runtime validation on server start, `/reload`, and config hot reload with detailed `[ForcedPlaque]` logs.

