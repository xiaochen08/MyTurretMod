summon examplemod:skeleton_turret ~ ~2 ~ {CustomName:'{"text":"RecordTest"}',PersistenceRequired:1b}
data merge entity @e[name=RecordTest,limit=1] {ForgeData:{RecordSummoned:1b},FollowMode:1b,IsSquadMember:1b}

tellraw @a {"text":"[Test] Spawned RecordTest in air. Waiting for logic...","color":"gold"}
schedule function examplemod:test_record_verify 10t