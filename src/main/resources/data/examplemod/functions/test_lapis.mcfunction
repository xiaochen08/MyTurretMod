# Turret Lapis Enchanting Test
# Description: Verifies that right-clicking a Skeleton Turret with Lapis Lazuli correctly enchants its equipment.

# 1. Spawn a fresh turret with a Diamond Sword (Unenchanted)
kill @e[type=examplemod:skeleton_turret,name=LapisTest]
summon examplemod:skeleton_turret ~ ~ ~ {CustomName:'{"text":"LapisTest"}',HandItems:[{id:"minecraft:diamond_sword",Count:1b},{}],ArmorItems:[{},{},{},{}]}

# 2. Provide resources to the tester
give @p lapis_lazuli 64
experience add @p 100 levels

# 3. Instructions
tellraw @a {"text":"=======================================","color":"gold"}
tellraw @a {"text":"[Lapis Test] Setup Complete.","color":"green"}
tellraw @a {"text":"1. Hold Lapis Lazuli (Try 1, 2, or 3+).","color":"yellow"}
tellraw @a {"text":"2. Right-click the Turret.","color":"yellow"}
tellraw @a {"text":"3. EXPECT: Diamond Sword gets enchanted.","color":"green"}
tellraw @a {"text":"4. EXPECT: XP levels consumed (1-3).","color":"green"}
tellraw @a {"text":"5. EXPECT: Particles and Sound.","color":"green"}
tellraw @a {"text":"=======================================","color":"gold"}
