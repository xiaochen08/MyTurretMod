# Death Plaque System Architecture

## Core flow
1. `LivingDropsEvent` intercepts `SkeletonTurret` death.
2. `FatalHitCount` increments per fatal damage.
3. First/second fatal hit: roll `deathPlaqueDropChance`.
4. On success, generate `death_record_card` with serialized snapshot.
5. Send owner a clickable teleport chat message.
6. Third fatal hit: no drop, send destroy confirmation.

## Serialized data scope
Included:
- UnitID, Tier, XP, KillCount, UpgradeProgress, Heat, Brutal state
- OwnerUUID, BaseName
- Equipment list
- Inventory full snapshot
- Installed upgrade modules list (slots 5-9)
- FatalHitCount and record timestamp

Excluded:
- Squad/follow-group topology metadata

## Version compatibility
- `DeathPlaqueDataCodec.CURRENT_VERSION = 3`
- Legacy normalization supports old payloads missing `FatalHitCount`/`HasTeleportModule`.

## Performance strategy
- Uses vanilla item model inheritance (`recovery_compass`) to avoid custom render overhead.
- Server GC removes expired dropped plaque entities by TTL on level tick.

## Monitoring
- Owner receives explicit death-drop and destroy notifications.
- Teleport interaction bound to `/skull tpplaque x y z`.
