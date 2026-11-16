package com.example.shadowquiz;

/**
 * Represents one animal entry in the user's Fieldbook collection.
 */
public class AnimalCollectionItem {
    private final AnimalPuzzle puzzle;
    private final boolean unlocked;
    private final int timesCaught;
    private final long firstCaughtAt;

    public AnimalCollectionItem(AnimalPuzzle puzzle, boolean unlocked, int timesCaught, long firstCaughtAt) {
        this.puzzle = puzzle;
        this.unlocked = unlocked;
        this.timesCaught = timesCaught;
        this.firstCaughtAt = firstCaughtAt;
    }

    public AnimalPuzzle getPuzzle() {
        return puzzle;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getTimesCaught() {
        return timesCaught;
    }

    public long getFirstCaughtAt() {
        return firstCaughtAt;
    }
}
