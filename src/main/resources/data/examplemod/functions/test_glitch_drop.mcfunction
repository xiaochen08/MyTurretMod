# Test script for Glitch Chip drop fix
# Spawns multiple turrets to increase chance of natural glitch (Blue Screen)
# Usage: Run this function, wait ~3-5 seconds.
# If a turret emits smoke/error sound, it is glitching.
# Verify that upon death (self-destruct or player kill), it drops EXACTLY ONE Glitch Chip.
# Verify that non-glitching turrets do NOT drop the chip.

summon examplemod:skeleton_turret ~ ~ ~ {CustomName:'{"text":"GlitchTest1"}'}
summon examplemod:skeleton_turret ~ ~ ~2 {CustomName:'{"text":"GlitchTest2"}'}
summon examplemod:skeleton_turret ~ ~ ~-2 {CustomName:'{"text":"GlitchTest3"}'}
summon examplemod:skeleton_turret ~2 ~ ~ {CustomName:'{"text":"GlitchTest4"}'}
summon examplemod:skeleton_turret ~-2 ~ ~ {CustomName:'{"text":"GlitchTest5"}'}

tellraw @a {"text":"[Test] Spawned 5 turrets. Watch for Blue Screen (smoke/error sound).","color":"gold"}
tellraw @a {"text":"[Test] If one glitches, let it explode or kill it. Verify only ONE chip drops.","color":"gold"}
tellraw @a {"text":"[Test] Kill normal turrets to verify they do NOT drop the chip.","color":"yellow"}
