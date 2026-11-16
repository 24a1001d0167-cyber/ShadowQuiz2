package com.example.shadowquiz;

/**
 * Supported difficulty and game modes controlling option count, timer, lives, and score multiplier.
 */
public enum Difficulty {
    EASY("EASY", "Dễ", 4, 15000L, 5, 1.0f),
    MEDIUM("MEDIUM", "Trung bình", 6, 11000L, 3, 1.5f),
    HARD("HARD", "Ác mộng", 6, 8000L, 3, 2.5f),
    BLITZ("BLITZ", "Đua 60 Giây", 6, 60000L, 99, 2.0f);

    private final String code;
    private final String displayName;
    private final int optionCount;
    private final long baseTimeMs;
    private final int initialLives;
    private final float scoreMultiplier;

    Difficulty(String code, String displayName, int optionCount, long baseTimeMs,
               int initialLives, float scoreMultiplier) {
        this.code = code;
        this.displayName = displayName;
        this.optionCount = optionCount;
        this.baseTimeMs = baseTimeMs;
        this.initialLives = initialLives;
        this.scoreMultiplier = scoreMultiplier;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOptionCount() {
        return optionCount;
    }

    public long getBaseTimeMs() {
        return baseTimeMs;
    }

    public int getInitialLives() {
        return initialLives;
    }

    public float getScoreMultiplier() {
        return scoreMultiplier;
    }

    public boolean isBlitz() {
        return this == BLITZ;
    }

    public static Difficulty fromCode(String code) {
        if (code == null) {
            return EASY;
        }
        for (Difficulty diff : values()) {
            if (diff.code.equalsIgnoreCase(code)) {
                return diff;
            }
        }
        return EASY;
    }
}
