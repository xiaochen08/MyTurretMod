kill @e[type=examplemod:skeleton_turret]
summon examplemod:skeleton_turret ~ ~ ~ {CustomName:'{"text":"VanillaTest"}'}
tellraw @a {"text":"[Vanilla Test] Spawned 1 Turret. Check Model & Animation.","color":"green"}
