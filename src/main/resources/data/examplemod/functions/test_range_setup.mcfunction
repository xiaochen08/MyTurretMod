# Clear area
fill ~ ~ ~ ~256 ~10 ~ air

# Spawn Turret at 0
summon examplemod:skeleton_turret ~ ~ ~ {RangeLevel:5,NoAI:1b,Rotation:[0f,0f],CustomName:'{"text":"Turret"}'}

# Spawn targets every 32 blocks
summon armor_stand ~32 ~ ~ {CustomName:'{"text":"32m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~64 ~ ~ {CustomName:'{"text":"64m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~96 ~ ~ {CustomName:'{"text":"96m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~128 ~ ~ {CustomName:'{"text":"128m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~160 ~ ~ {CustomName:'{"text":"160m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~192 ~ ~ {CustomName:'{"text":"192m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~224 ~ ~ {CustomName:'{"text":"224m"}',CustomNameVisible:1b,ShowArms:1b}
summon armor_stand ~256 ~ ~ {CustomName:'{"text":"256m"}',CustomNameVisible:1b,ShowArms:1b}

tellraw @a {"text":"[Test Setup] Turret and targets spawned. Range: 0-256m","color":"gold"}