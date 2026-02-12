# Test New Record System

# 1. Summon a turret with specific ID and Level
summon examplemod:skeleton_turret ~ ~ ~ {ForgeData:{UnitId:777,RangeLevel:5},CustomName:'{"text":"TargetTurret"}'}
tellraw @a {"text":"[Test] Spawned Turret 777 (Lv.5)","color":"gold"}

# 2. Give player a simulated record card (since we can't easily simulate death drop pick up in function without complex logic)
# We simulate the ITEM that would be dropped.
give @p examplemod:death_record_card{SavedUnitId:777,SavedRangeLevel:5,display:{Name:'{"text":"(编号777 记录卡)"}'}}
tellraw @a {"text":"[Test] Gave player simulated Record Card (ID:777, Lv.5)","color":"gold"}

# 3. Instructions
tellraw @a {"text":"[Instruction] Please use the card on the ground.","color":"yellow"}
tellraw @a {"text":"[Instruction] Verify:","color":"yellow"}
tellraw @a {"text":"  1. New turret has ID 777","color":"yellow"}
tellraw @a {"text":"  2. New turret has Range Level 5","color":"yellow"}
tellraw @a {"text":"  3. New turret has FULL Health (not old health)","color":"yellow"}
tellraw @a {"text":"  4. New turret has NO Inventory (clean slate)","color":"yellow"}
