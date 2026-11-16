package com.example.shadowquiz;

/**
 * Daily mission state backed by SQLite daily_quests table.
 */
public class DailyQuest {
    private final int index;
    private final String title;
    private final int current;
    private final int target;
    private final int rewardCoins;
    private final boolean claimed;

    public DailyQuest(int index, String title, int current, int target, int rewardCoins, boolean claimed) {
        this.index = index;
        this.title = title;
        this.current = current;
        this.target = target;
        this.rewardCoins = rewardCoins;
        this.claimed = claimed;
    }

    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    public int getCurrent() {
        return current;
    }

    public int getTarget() {
        return target;
    }

    public int getRewardCoins() {
        return rewardCoins;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public boolean isClaimable() {
        return current >= target && !claimed;
    }
}
