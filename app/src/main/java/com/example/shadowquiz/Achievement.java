package com.example.shadowquiz;

/**
 * Unlockable milestone achievement displayed on LeaderboardActivity and awarded after matches.
 */
public class Achievement {
    private final String code;
    private final String title;
    private final String description;
    private final int iconResId;
    private final boolean unlocked;
    private final long unlockedAt;

    public Achievement(String code, String title, String description, int iconResId,
                       boolean unlocked, long unlockedAt) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.unlocked = unlocked;
        this.unlockedAt = unlockedAt;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public long getUnlockedAt() {
        return unlockedAt;
    }
}
