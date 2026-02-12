# Test Setup for Enchantment
# 1. Summon a dummy turret with unenchanted gear
summon examplemod:skeleton_turret ~ ~ ~ {Tags:["test_dummy"],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{},{id:"minecraft:iron_chestplate",Count:1b},{}]}

# 2. Clear player inventory and XP
clear @p
experience set @p 0 levels

# 3. Give test materials
give @p minecraft:lapis_lazuli 64
give @p minecraft:stick{display:{Name:'{"text":"Reset XP"}'}} 1

tellraw @p ["",{"text":"[Test Setup Complete]","color":"green"},{"text":"\n1. Try clicking with Lapis (Should fail - No XP)","color":"yellow"},{"text":"\n2. Run '/xp add @p 30 levels'","color":"aqua"},{"text":"\n3. Click with 1 Lapis (Should cost 1 level)","color":"yellow"},{"text":"\n4. Click with 3 Lapis (Should cost 3 levels)","color":"yellow"}]
