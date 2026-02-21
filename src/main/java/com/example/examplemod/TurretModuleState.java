package com.example.examplemod;

public class TurretModuleState {
    private boolean teleportInstalled;
    private int teleportLevel;
    private int multiShotLevel;

    public void reset() {
        this.teleportInstalled = false;
        this.teleportLevel = 0;
        this.multiShotLevel = 0;
    }

    public boolean isTeleportInstalled() {
        return teleportInstalled;
    }

    public void setTeleportInstalled(boolean teleportInstalled) {
        this.teleportInstalled = teleportInstalled;
    }

    public int getTeleportLevel() {
        return teleportLevel;
    }

    public void setTeleportLevel(int teleportLevel) {
        this.teleportLevel = TeleportModuleRules.clampLevel(teleportLevel);
        this.teleportInstalled = this.teleportLevel > 0;
    }

    public int getMultiShotLevel() {
        return multiShotLevel;
    }

    public void setMultiShotLevel(int multiShotLevel) {
        this.multiShotLevel = MultiShotModuleRules.clampLevel(multiShotLevel);
    }

    public boolean hasAnyModule() {
        return teleportLevel > 0 || multiShotLevel > 0;
    }

    public boolean isComboActive() {
        return teleportLevel > 0 && multiShotLevel > 0;
    }
}
