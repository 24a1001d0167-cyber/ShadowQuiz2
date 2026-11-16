package com.example.shadowquiz;

/**
 * Persistent player account model backed by SQLite users table.
 */
public class User {
    private final int id;
    private final String username;
    private final String displayName;
    private final int avatarIndex;
    private final int coins;
    private final int bestScore;
    private final int bestLevel;
    private final int gamesPlayed;
    private final int totalCorrect;
    private final int loginStreak;
    private final boolean checkedInToday;

    public User(int id, String username, String displayName, int avatarIndex,
                int coins, int bestScore, int bestLevel, int gamesPlayed,
                int totalCorrect, int loginStreak, boolean checkedInToday) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarIndex = avatarIndex;
        this.coins = coins;
        this.bestScore = bestScore;
        this.bestLevel = bestLevel;
        this.gamesPlayed = gamesPlayed;
        this.totalCorrect = totalCorrect;
        this.loginStreak = loginStreak;
        this.checkedInToday = checkedInToday;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }

    public int getCoins() {
        return coins;
    }

    public int getBestScore() {
        return bestScore;
    }

    public int getBestLevel() {
        return bestLevel;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public int getLoginStreak() {
        return loginStreak;
    }

    public boolean isCheckedInToday() {
        return checkedInToday;
    }

    public String getRankTitle() {
        if (bestScore >= 500) {
            return "Huyền Thoại Rừng Già";
        } else if (bestScore >= 250) {
            return "Nhà Tự Nhiên Học";
        } else if (bestScore >= 100) {
            return "Thợ Săn Tinh Mắt";
        }
        return "Tân Binh Thảo Nguyên";
    }
}
