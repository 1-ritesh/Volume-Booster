package com.volumebooster.app.models;

public class SoundMode {
    private String name;
    private int boostLevel; // 100–200
    private int iconRes;

    public SoundMode(String name, int boostLevel, int iconRes) {
        this.name       = name;
        this.boostLevel = boostLevel;
        this.iconRes    = iconRes;
    }

    public String getName()      { return name; }
    public int getBoostLevel()   { return boostLevel; }
    public int getIconRes()      { return iconRes; }
}
