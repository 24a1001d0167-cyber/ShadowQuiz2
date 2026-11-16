package com.example.shadowquiz;

/**
 * Single score record used in both Top Leaderboard and User Match History lists.
 */
public class ScoreRecord {
    private final int id;
    private final int userId;
    private final String displayName;
    private final int avatarIndex;
    private final int score;
    private final int levelReached;
    private final String difficulty;
    private final int maxCombo;
    private final int accuracy;
    private final long playedAt;

    public ScoreRecord(int id, int userId, String displayName, int avatarIndex, int score,
                       int levelReached, String difficulty, int maxCombo, int accuracy, long playedAt) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.avatarIndex = avatarIndex;
        this.score = score;
        this.levelReached = levelReached;
        this.difficulty = difficulty;
        this.maxCombo = maxCombo;
        this.accuracy = accuracy;
        this.playedAt = playedAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }

    public int getScore() {
        return score;
    }

    public int getLevelReached() {
        return levelReached;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public long getPlayedAt() {
        return playedAt;
    }
}
