summon examplemod:skeleton_turret ~ ~ ~ {CustomName:'{"text":"DeathTest"}',Health:20f}
tellraw @a {"text":"[Test] Turret spawned. Killing in 1 second...","color":"gold"}
schedule function examplemod:test_death_step2 20t