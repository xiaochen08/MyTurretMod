# Changelog

## [Unreleased]

### Added
- **Repair Mode**: Implemented a new repair mechanic.
    - Triggered by Shift + Right-Click with Iron Ingot.
    - Displays a 30-second countdown HUD above the turret.
    - Requires 5 consecutive right-clicks within 0.4s intervals to complete.
    - Successful repair restores health, plays anvil sound, and emits happy villager particles.
    - Failure or timeout cancels the repair state.
- **HUD**: Added a dedicated repair countdown HUD (text + progress bar) in `ClientForgeEvents.java`.
- **Unit Tests**: Added `TurretRepairTests.java` to verify repair logic and attack speed intervals.

### Changed
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
