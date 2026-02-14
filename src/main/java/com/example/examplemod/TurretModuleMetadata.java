package com.example.examplemod;

import java.util.List;

public record TurretModuleMetadata(
        String moduleId,
        String name,
        String description,
        String icon,
        TurretModuleRarity rarity,
        List<String> lore
) {
    public TurretModuleMetadata(String moduleId, String name, String description, String icon, TurretModuleRarity rarity) {
        this(moduleId, name, description, icon, rarity, List.of());
    }
}
