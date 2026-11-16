package com.example.shadowquiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite database manager (v2) for authentication, match history, leaderboard,
 * animal Fieldbook collection, daily check-in streaks, daily quests, and achievements.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ShadowQuiz.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_SCORE_HISTORY = "score_history";
    private static final String TABLE_USER_ACHIEVEMENTS = "user_achievements";
    private static final String TABLE_UNLOCKED_ANIMALS = "unlocked_animals";
    private static final String TABLE_DAILY_QUESTS = "daily_quests";

    private static final String GUEST_USERNAME = "khach_choi_nhanh";

    public static class GameSaveResult {
        public final boolean isNewBest;
        public final int coinsEarned;
        public final List<String> newlyUnlockedTitles;

        public GameSaveResult(boolean isNewBest, int coinsEarned, List<String> newlyUnlockedTitles) {
            this.isNewBest = isNewBest;
            this.coinsEarned = coinsEarned;
            this.newlyUnlockedTitles = newlyUnlockedTitles;
        }
    }

    private static class AchievementDef {
        final String code;
        final String title;
        final String description;
        final int iconResId;

        AchievementDef(String code, String title, String description, int iconResId) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
        }
    }

    private static final AchievementDef[] ACHIEVEMENT_DEFS = new AchievementDef[]{
            new AchievementDef("FIRST_WIN", "Bước Chân Đầu Tiên",
                    "Hoàn thành ván chơi đầu tiên với ít nhất 1 câu đúng", R.drawable.ic_medal),
            new AchievementDef("COMBO_5", "Đôi Mắt Tinh Anh",
                    "Đạt chuỗi Combo 5 câu đúng liên tiếp", R.drawable.ic_fire),
            new AchievementDef("COMBO_10", "Phản Xạ Thần Sầu",
                    "Đạt chuỗi Combo 10 câu đúng liên tiếp", R.drawable.ic_bolt),
            new AchievementDef("SCORE_300", "Kỷ Lục Gia Thảo Nguyên",
                    "Ghi từ 300 điểm trở lên trong một ván", R.drawable.ic_trophy),
            new AchievementDef("HARD_SURVIVOR", "Vượt Màn Đêm Ác Mộng",
                    "Ghi ít nhất 150 điểm ở chế độ Ác mộng (Hard)", R.drawable.ic_fire),
            new AchievementDef("BLITZ_250", "Tia Chớp 60 Giây",
                    "Ghi từ 250 điểm trở lên ở chế độ Đua 60 Giây", R.drawable.ic_bolt),
            new AchievementDef("COLLECTOR_8", "Nhà Tự Nhiên Học",
                    "Khám phá ít nhất 8/16 loài trong Sổ Tay Muôn Loài", R.drawable.ic_book_paw),
            new AchievementDef("VETERAN_10", "Nhà Thám Hiểm Kỳ Cựu",
                    "Hoàn thành tổng cộng 10 ván chơi trở lên", R.drawable.ic_trophy)
    };

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE NOT NULL, "
                + "display_name TEXT NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "avatar_index INTEGER DEFAULT 0, "
                + "coins INTEGER DEFAULT 120, "
                + "best_score INTEGER DEFAULT 0, "
                + "best_level INTEGER DEFAULT 1, "
                + "games_played INTEGER DEFAULT 0, "
                + "total_correct INTEGER DEFAULT 0, "
                + "last_checkin_date TEXT DEFAULT '', "
                + "login_streak INTEGER DEFAULT 0, "
                + "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_SCORE_HISTORY + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "score INTEGER NOT NULL, "
                + "level_reached INTEGER NOT NULL, "
                + "difficulty TEXT NOT NULL, "
                + "max_combo INTEGER NOT NULL, "
                + "correct_count INTEGER NOT NULL, "
                + "accuracy INTEGER NOT NULL, "
                + "played_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_USER_ACHIEVEMENTS + " ("
                + "user_id INTEGER NOT NULL, "
                + "achievement_code TEXT NOT NULL, "
                + "unlocked_at INTEGER NOT NULL, "
                + "PRIMARY KEY(user_id, achievement_code))");

        createV2Tables(db);
        seedDemoData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN last_checkin_date TEXT DEFAULT ''");
            } catch (Exception ignored) {
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN login_streak INTEGER DEFAULT 0");
            } catch (Exception ignored) {
            }
            createV2Tables(db);
            seedBlitzNpc(db, System.currentTimeMillis());
        }
    }

    private void createV2Tables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_UNLOCKED_ANIMALS + " ("
                + "user_id INTEGER NOT NULL, "
                + "animal_name TEXT NOT NULL, "
                + "times_caught INTEGER DEFAULT 1, "
                + "first_caught_at INTEGER NOT NULL, "
                + "PRIMARY KEY(user_id, animal_name))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DAILY_QUESTS + " ("
                + "user_id INTEGER NOT NULL, "
                + "date_key TEXT NOT NULL, "
                + "correct_count INTEGER DEFAULT 0, "
                + "combo5_count INTEGER DEFAULT 0, "
                + "special_mode_count INTEGER DEFAULT 0, "
                + "claimed_mask INTEGER DEFAULT 0, "
                + "PRIMARY KEY(user_id, date_key))");
    }

    private void seedDemoData(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        long u1 = insertSeedUser(db, "caothu_bongdem", "Cao Thủ Rừng Sâu", 2, 440, 6, 12, 85, now - 3600000L * 6);
        long u2 = insertSeedUser(db, "soc_tia_chop", "Sóc Tia Chớp", 7, 290, 4, 8, 52, now - 3600000L * 4);
        long u3 = insertSeedUser(db, "cu_thong_thai", "Cú Thông Thái", 3, 160, 3, 5, 30, now - 3600000L * 2);

        insertSeedScore(db, u1, 440, 6, Difficulty.HARD.getCode(), 9, 18, 90, now - 3600000L * 5);
        insertSeedScore(db, u2, 290, 4, Difficulty.MEDIUM.getCode(), 7, 15, 83, now - 3600000L * 3);
        insertSeedScore(db, u3, 160, 3, Difficulty.EASY.getCode(), 6, 12, 86, now - 3600000L);
        seedBlitzNpc(db, now);
    }

    private void seedBlitzNpc(SQLiteDatabase db, long now) {
        try (Cursor check = db.query(TABLE_USERS, new String[]{"id"},
                "username = ?", new String[]{"bao_dom_toc_do"}, null, null, null)) {
            if (check.moveToFirst()) {
                return;
            }
        }
        long u4 = insertSeedUser(db, "bao_dom_toc_do", "Báo Đốm Tốc Độ", 4, 340, 5, 9, 70, now - 3600000L * 2);
        insertSeedScore(db, u4, 340, 5, Difficulty.BLITZ.getCode(), 8, 22, 88, now - 1800000L);
    }

    private long insertSeedUser(SQLiteDatabase db, String username, String displayName, int avatarIndex,
                                int bestScore, int bestLevel, int gamesPlayed, int totalCorrect, long createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("display_name", displayName);
        cv.put("password_hash", hashPassword("seed_123456"));
        cv.put("avatar_index", avatarIndex);
        cv.put("coins", 200);
        cv.put("best_score", bestScore);
        cv.put("best_level", bestLevel);
        cv.put("games_played", gamesPlayed);
        cv.put("total_correct", totalCorrect);
        cv.put("last_checkin_date", "");
        cv.put("login_streak", 0);
        cv.put("created_at", createdAt);
        return db.insert(TABLE_USERS, null, cv);
    }

    private void insertSeedScore(SQLiteDatabase db, long userId, int score, int levelReached,
                                 String difficulty, int maxCombo, int correctCount, int accuracy, long playedAt) {
        if (userId <= 0) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("score", score);
        cv.put("level_reached", levelReached);
        cv.put("difficulty", difficulty);
        cv.put("max_combo", maxCombo);
        cv.put("correct_count", correctCount);
        cv.put("accuracy", accuracy);
        cv.put("played_at", playedAt);
        db.insert(TABLE_SCORE_HISTORY, null, cv);
    }

    public static String hashPassword(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return raw;
        }
    }

    private String getTodayDateKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getYesterdayDateKey() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    public long registerUser(String username, String displayName, String rawPassword, int avatarIndex) {
        String normalizedUser = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String trimmedDisplay = displayName == null ? "" : displayName.trim();
        if (normalizedUser.isEmpty() || trimmedDisplay.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        try (Cursor check = db.query(TABLE_USERS, new String[]{"id"},
                "username = ?", new String[]{normalizedUser}, null, null, null)) {
            if (check.moveToFirst()) {
                return -1;
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("username", normalizedUser);
        cv.put("display_name", trimmedDisplay);
        cv.put("password_hash", hashPassword(rawPassword));
        cv.put("avatar_index", avatarIndex);
        cv.put("coins", 120);
        cv.put("best_score", 0);
        cv.put("best_level", 1);
        cv.put("games_played", 0);
        cv.put("total_correct", 0);
        cv.put("last_checkin_date", "");
        cv.put("login_streak", 0);
        cv.put("created_at", System.currentTimeMillis());

        return db.insert(TABLE_USERS, null, cv);
    }

    public User loginUser(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return null;
        }
        String normalizedUser = username.trim().toLowerCase(Locale.ROOT);
        String hashed = hashPassword(rawPassword);

        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS, null,
                "username = ? AND password_hash = ?", new String[]{normalizedUser, hashed},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
        }
        return null;
    }

    public User getOrCreateGuestUser() {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS, null,
                "username = ?", new String[]{GUEST_USERNAME}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("username", GUEST_USERNAME);
        cv.put("display_name", "Nhà Thám Hiểm");
        cv.put("password_hash", hashPassword("guest_shadow_quiz"));
        cv.put("avatar_index", 0);
        cv.put("coins", 120);
        cv.put("best_score", 0);
        cv.put("best_level", 1);
        cv.put("games_played", 0);
        cv.put("total_correct", 0);
        cv.put("last_checkin_date", "");
        cv.put("login_streak", 0);
        cv.put("created_at", System.currentTimeMillis());

        long id = db.insert(TABLE_USERS, null, cv);
        return getUserById((int) id);
    }

    public User getUserById(int userId) {
        if (userId <= 0) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS, null,
                "id = ?", new String[]{String.valueOf(userId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToUser(cursor);
            }
        }
        return null;
    }

    public boolean updateUserProfile(int userId, String newDisplayName, int newAvatarIndex) {
        if (userId <= 0 || newDisplayName == null) {
            return false;
        }
        String trimmed = newDisplayName.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("display_name", trimmed);
        cv.put("avatar_index", newAvatarIndex);
        int rows = db.update(TABLE_USERS, cv, "id = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public boolean spendCoins(int userId, int amount) {
        if (userId <= 0 || amount <= 0) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int currentCoins;
            try (Cursor cursor = db.query(TABLE_USERS, new String[]{"coins"},
                    "id = ?", new String[]{String.valueOf(userId)}, null, null, null)) {
                if (!cursor.moveToFirst()) {
                    return false;
                }
                currentCoins = cursor.getInt(0);
            }
            if (currentCoins < amount) {
                return false;
            }
            ContentValues cv = new ContentValues();
            cv.put("coins", currentCoins - amount);
            db.update(TABLE_USERS, cv, "id = ?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Performs daily check-in for the user. Returns coins awarded, or 0 if already checked in today.
     */
    public int performDailyCheckIn(int userId) {
        if (userId <= 0) {
            return 0;
        }
        String today = getTodayDateKey();
        String yesterday = getYesterdayDateKey();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int coins = 0;
            String lastDate = "";
            int streak = 0;
            try (Cursor cursor = db.query(TABLE_USERS,
                    new String[]{"coins", "last_checkin_date", "login_streak"},
                    "id = ?", new String[]{String.valueOf(userId)}, null, null, null)) {
                if (!cursor.moveToFirst()) {
                    return 0;
                }
                coins = cursor.getInt(0);
                lastDate = cursor.getString(1) != null ? cursor.getString(1) : "";
                streak = cursor.getInt(2);
            }

            if (today.equals(lastDate)) {
                return 0;
            }

            int newStreak = yesterday.equals(lastDate) ? (streak + 1) : 1;
            int reward = 30 + Math.min(40, (newStreak - 1) * 5);

            ContentValues cv = new ContentValues();
            cv.put("coins", coins + reward);
            cv.put("last_checkin_date", today);
            cv.put("login_streak", newStreak);
            db.update(TABLE_USERS, cv, "id = ?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            return reward;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Unlocks or increments caught count for an animal in the user's Fieldbook.
     * Returns true if this is the very first discovery of this species.
     */
    public boolean unlockOrIncrementAnimal(int userId, String animalName) {
        if (userId <= 0 || animalName == null || animalName.isEmpty()) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int existingCount = -1;
            try (Cursor cursor = db.query(TABLE_UNLOCKED_ANIMALS, new String[]{"times_caught"},
                    "user_id = ? AND animal_name = ?",
                    new String[]{String.valueOf(userId), animalName}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    existingCount = cursor.getInt(0);
                }
            }

            boolean newlyUnlocked = (existingCount < 0);
            if (newlyUnlocked) {
                ContentValues cv = new ContentValues();
                cv.put("user_id", userId);
                cv.put("animal_name", animalName);
                cv.put("times_caught", 1);
                cv.put("first_caught_at", System.currentTimeMillis());
                db.insert(TABLE_UNLOCKED_ANIMALS, null, cv);

                if (getUnlockedAnimalCountInternal(db, userId) >= 8) {
                    unlockIfNew(db, userId, "COLLECTOR_8", System.currentTimeMillis(), new ArrayList<>());
                }
            } else {
                ContentValues cv = new ContentValues();
                cv.put("times_caught", existingCount + 1);
                db.update(TABLE_UNLOCKED_ANIMALS, cv,
                        "user_id = ? AND animal_name = ?",
                        new String[]{String.valueOf(userId), animalName});
            }

            db.setTransactionSuccessful();
            return newlyUnlocked;
        } finally {
            db.endTransaction();
        }
    }

    public int getUnlockedAnimalCount(int userId) {
        if (userId <= 0) {
            return 0;
        }
        return getUnlockedAnimalCountInternal(getReadableDatabase(), userId);
    }

    private int getUnlockedAnimalCountInternal(SQLiteDatabase db, int userId) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_UNLOCKED_ANIMALS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }

    public List<AnimalCollectionItem> getAnimalCollection(int userId) {
        Map<String, int[]> countAndDummy = new HashMap<>();
        Map<String, Long> timeMap = new HashMap<>();

        if (userId > 0) {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.query(TABLE_UNLOCKED_ANIMALS,
                    new String[]{"animal_name", "times_caught", "first_caught_at"},
                    "user_id = ?", new String[]{String.valueOf(userId)},
                    null, null, null)) {
                while (c.moveToNext()) {
                    String name = c.getString(0);
                    countAndDummy.put(name, new int[]{c.getInt(1)});
                    timeMap.put(name, c.getLong(2));
                }
            }
        }

        List<AnimalCollectionItem> list = new ArrayList<>();
        for (AnimalPuzzle puzzle : AnimalPuzzle.getAllAnimals()) {
            int[] cnt = countAndDummy.get(puzzle.getAnimalName());
            Long firstAt = timeMap.get(puzzle.getAnimalName());
            list.add(new AnimalCollectionItem(
                    puzzle,
                    cnt != null,
                    cnt != null ? cnt[0] : 0,
                    firstAt != null ? firstAt : 0L
            ));
        }
        return list;
    }

    public List<DailyQuest> getTodayQuests(int userId) {
        int correct = 0;
        int combo5 = 0;
        int special = 0;
        int claimedMask = 0;

        if (userId > 0) {
            String today = getTodayDateKey();
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.query(TABLE_DAILY_QUESTS,
                    new String[]{"correct_count", "combo5_count", "special_mode_count", "claimed_mask"},
                    "user_id = ? AND date_key = ?",
                    new String[]{String.valueOf(userId), today}, null, null, null)) {
                if (c.moveToFirst()) {
                    correct = c.getInt(0);
                    combo5 = c.getInt(1);
                    special = c.getInt(2);
                    claimedMask = c.getInt(3);
                }
            }
        }

        List<DailyQuest> list = new ArrayList<>();
        list.add(new DailyQuest(0, "Đoán đúng 10 con vật hôm nay",
                Math.min(10, correct), 10, 35, (claimedMask & 1) != 0));
        list.add(new DailyQuest(1, "Đạt chuỗi Combo 5 trong 1 ván",
                Math.min(1, combo5), 1, 45, (claimedMask & 2) != 0));
        list.add(new DailyQuest(2, "Hoàn thành 1 ván Đua 60s hoặc Ác mộng",
                Math.min(1, special), 1, 50, (claimedMask & 4) != 0));
        return list;
    }

    public int claimDailyQuest(int userId, int questIndex) {
        if (userId <= 0 || questIndex < 0 || questIndex > 2) {
            return 0;
        }
        String today = getTodayDateKey();
        int[] targets = {10, 1, 1};
        int[] rewards = {35, 45, 50};
        int bit = 1 << questIndex;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int[] progress = {0, 0, 0};
            int claimedMask = 0;
            try (Cursor c = db.query(TABLE_DAILY_QUESTS,
                    new String[]{"correct_count", "combo5_count", "special_mode_count", "claimed_mask"},
                    "user_id = ? AND date_key = ?",
                    new String[]{String.valueOf(userId), today}, null, null, null)) {
                if (!c.moveToFirst()) {
                    return 0;
                }
                progress[0] = c.getInt(0);
                progress[1] = c.getInt(1);
                progress[2] = c.getInt(2);
                claimedMask = c.getInt(3);
            }

            if ((claimedMask & bit) != 0 || progress[questIndex] < targets[questIndex]) {
                return 0;
            }

            ContentValues questCv = new ContentValues();
            questCv.put("claimed_mask", claimedMask | bit);
            db.update(TABLE_DAILY_QUESTS, questCv,
                    "user_id = ? AND date_key = ?", new String[]{String.valueOf(userId), today});

            db.execSQL("UPDATE " + TABLE_USERS + " SET coins = coins + ? WHERE id = ?",
                    new Object[]{rewards[questIndex], userId});
            db.setTransactionSuccessful();
            return rewards[questIndex];
        } finally {
            db.endTransaction();
        }
    }

    public GameSaveResult recordGameResult(int userId, int score, int levelReached, String difficulty,
                                           int maxCombo, int correctCount, int totalAnswered) {
        List<String> newlyUnlocked = new ArrayList<>();
        boolean isNewBest = false;
        int coinsEarned = Math.max(10, score / 8);
        int accuracy = totalAnswered > 0 ? Math.min(100, Math.round((correctCount * 100f) / totalAnswered)) : 0;
        long now = System.currentTimeMillis();
        String today = getTodayDateKey();

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            User existing = null;
            try (Cursor cursor = db.query(TABLE_USERS, null,
                    "id = ?", new String[]{String.valueOf(userId)}, null, null, null)) {
                if (cursor.moveToFirst()) {
                    existing = cursorToUser(cursor);
                }
            }

            if (existing != null) {
                isNewBest = score > existing.getBestScore();
                int newBestScore = Math.max(existing.getBestScore(), score);
                int newBestLevel = Math.max(existing.getBestLevel(), levelReached);
                int newGamesPlayed = existing.getGamesPlayed() + 1;
                int newTotalCorrect = existing.getTotalCorrect() + correctCount;
                int newCoins = existing.getCoins() + coinsEarned;

                ContentValues historyCv = new ContentValues();
                historyCv.put("user_id", userId);
                historyCv.put("score", score);
                historyCv.put("level_reached", levelReached);
                historyCv.put("difficulty", difficulty);
                historyCv.put("max_combo", maxCombo);
                historyCv.put("correct_count", correctCount);
                historyCv.put("accuracy", accuracy);
                historyCv.put("played_at", now);
                db.insert(TABLE_SCORE_HISTORY, null, historyCv);

                ContentValues userCv = new ContentValues();
                userCv.put("coins", newCoins);
                userCv.put("best_score", newBestScore);
                userCv.put("best_level", newBestLevel);
                userCv.put("games_played", newGamesPlayed);
                userCv.put("total_correct", newTotalCorrect);
                db.update(TABLE_USERS, userCv, "id = ?", new String[]{String.valueOf(userId)});

                // Update today's daily quests progress
                boolean isSpecialMode = Difficulty.HARD.getCode().equalsIgnoreCase(difficulty)
                        || Difficulty.BLITZ.getCode().equalsIgnoreCase(difficulty);
                updateDailyQuestsProgress(db, userId, today, correctCount,
                        maxCombo >= 5 ? 1 : 0, isSpecialMode ? 1 : 0);

                // Evaluate achievements
                if (correctCount >= 1) {
                    unlockIfNew(db, userId, "FIRST_WIN", now, newlyUnlocked);
                }
                if (maxCombo >= 5) {
                    unlockIfNew(db, userId, "COMBO_5", now, newlyUnlocked);
                }
                if (maxCombo >= 10) {
                    unlockIfNew(db, userId, "COMBO_10", now, newlyUnlocked);
                }
                if (score >= 300) {
                    unlockIfNew(db, userId, "SCORE_300", now, newlyUnlocked);
                }
                if (Difficulty.HARD.getCode().equalsIgnoreCase(difficulty) && score >= 150) {
                    unlockIfNew(db, userId, "HARD_SURVIVOR", now, newlyUnlocked);
                }
                if (Difficulty.BLITZ.getCode().equalsIgnoreCase(difficulty) && score >= 250) {
                    unlockIfNew(db, userId, "BLITZ_250", now, newlyUnlocked);
                }
                if (getUnlockedAnimalCountInternal(db, userId) >= 8) {
                    unlockIfNew(db, userId, "COLLECTOR_8", now, newlyUnlocked);
                }
                if (newGamesPlayed >= 10) {
                    unlockIfNew(db, userId, "VETERAN_10", now, newlyUnlocked);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new GameSaveResult(isNewBest, coinsEarned, newlyUnlocked);
    }

    private void updateDailyQuestsProgress(SQLiteDatabase db, int userId, String today,
                                           int addCorrect, int addCombo5, int addSpecial) {
        int curCorrect = 0;
        int curCombo5 = 0;
        int curSpecial = 0;
        boolean exists = false;
        try (Cursor c = db.query(TABLE_DAILY_QUESTS,
                new String[]{"correct_count", "combo5_count", "special_mode_count"},
                "user_id = ? AND date_key = ?",
                new String[]{String.valueOf(userId), today}, null, null, null)) {
            if (c.moveToFirst()) {
                exists = true;
                curCorrect = c.getInt(0);
                curCombo5 = c.getInt(1);
                curSpecial = c.getInt(2);
            }
        }

        ContentValues cv = new ContentValues();
        cv.put("correct_count", curCorrect + addCorrect);
        cv.put("combo5_count", curCombo5 + addCombo5);
        cv.put("special_mode_count", curSpecial + addSpecial);
        if (exists) {
            db.update(TABLE_DAILY_QUESTS, cv,
                    "user_id = ? AND date_key = ?", new String[]{String.valueOf(userId), today});
        } else {
            cv.put("user_id", userId);
            cv.put("date_key", today);
            cv.put("claimed_mask", 0);
            db.insert(TABLE_DAILY_QUESTS, null, cv);
        }
    }

    private void unlockIfNew(SQLiteDatabase db, int userId, String code, long unlockedAt,
                             List<String> newlyUnlockedTitles) {
        try (Cursor check = db.query(TABLE_USER_ACHIEVEMENTS, new String[]{"achievement_code"},
                "user_id = ? AND achievement_code = ?",
                new String[]{String.valueOf(userId), code}, null, null, null)) {
            if (check.moveToFirst()) {
                return;
            }
        }
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("achievement_code", code);
        cv.put("unlocked_at", unlockedAt);
        long res = db.insert(TABLE_USER_ACHIEVEMENTS, null, cv);
        if (res != -1) {
            for (AchievementDef def : ACHIEVEMENT_DEFS) {
                if (def.code.equals(code)) {
                    newlyUnlockedTitles.add(def.title);
                    break;
                }
            }
        }
    }

    public List<ScoreRecord> getTopLeaderboard(String difficultyFilter, int limit) {
        List<ScoreRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        boolean filterAll = difficultyFilter == null || "ALL".equalsIgnoreCase(difficultyFilter);
        String sql = "SELECT s.id, s.user_id, u.display_name, u.avatar_index, "
                + "s.score, s.level_reached, s.difficulty, s.max_combo, s.accuracy, s.played_at "
                + "FROM " + TABLE_SCORE_HISTORY + " s "
                + "INNER JOIN " + TABLE_USERS + " u ON u.id = s.user_id "
                + (filterAll ? "" : "WHERE s.difficulty = ? ")
                + "ORDER BY s.score DESC, s.played_at DESC "
                + "LIMIT ?";

        String[] args = filterAll
                ? new String[]{String.valueOf(limit)}
                : new String[]{difficultyFilter, String.valueOf(limit)};

        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                list.add(cursorToScoreRecord(cursor));
            }
        }
        return list;
    }

    public List<ScoreRecord> getUserHistory(int userId, int limit) {
        List<ScoreRecord> list = new ArrayList<>();
        if (userId <= 0) {
            return list;
        }
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT s.id, s.user_id, u.display_name, u.avatar_index, "
                + "s.score, s.level_reached, s.difficulty, s.max_combo, s.accuracy, s.played_at "
                + "FROM " + TABLE_SCORE_HISTORY + " s "
                + "INNER JOIN " + TABLE_USERS + " u ON u.id = s.user_id "
                + "WHERE s.user_id = ? "
                + "ORDER BY s.played_at DESC "
                + "LIMIT ?";

        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(limit)})) {
            while (cursor.moveToNext()) {
                list.add(cursorToScoreRecord(cursor));
            }
        }
        return list;
    }

    public List<Achievement> getUserAchievements(int userId) {
        Map<String, Long> unlockedMap = new HashMap<>();
        if (userId > 0) {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor cursor = db.query(TABLE_USER_ACHIEVEMENTS,
                    new String[]{"achievement_code", "unlocked_at"},
                    "user_id = ?", new String[]{String.valueOf(userId)},
                    null, null, null)) {
                while (cursor.moveToNext()) {
                    unlockedMap.put(cursor.getString(0), cursor.getLong(1));
                }
            }
        }

        List<Achievement> result = new ArrayList<>();
        for (AchievementDef def : ACHIEVEMENT_DEFS) {
            Long time = unlockedMap.get(def.code);
            result.add(new Achievement(
                    def.code,
                    def.title,
                    def.description,
                    def.iconResId,
                    time != null,
                    time != null ? time : 0L
            ));
        }
        return result;
    }

    private User cursorToUser(Cursor c) {
        String lastCheckin = "";
        int streak = 0;
        int idxCheckin = c.getColumnIndex("last_checkin_date");
        if (idxCheckin >= 0 && !c.isNull(idxCheckin)) {
            lastCheckin = c.getString(idxCheckin);
        }
        int idxStreak = c.getColumnIndex("login_streak");
        if (idxStreak >= 0 && !c.isNull(idxStreak)) {
            streak = c.getInt(idxStreak);
        }
        boolean checkedToday = getTodayDateKey().equals(lastCheckin);

        return new User(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("username")),
                c.getString(c.getColumnIndexOrThrow("display_name")),
                c.getInt(c.getColumnIndexOrThrow("avatar_index")),
                c.getInt(c.getColumnIndexOrThrow("coins")),
                c.getInt(c.getColumnIndexOrThrow("best_score")),
                c.getInt(c.getColumnIndexOrThrow("best_level")),
                c.getInt(c.getColumnIndexOrThrow("games_played")),
                c.getInt(c.getColumnIndexOrThrow("total_correct")),
                streak,
                checkedToday
        );
    }

    private ScoreRecord cursorToScoreRecord(Cursor c) {
        return new ScoreRecord(
                c.getInt(0),
                c.getInt(1),
                c.getString(2),
                c.getInt(3),
                c.getInt(4),
                c.getInt(5),
                c.getString(6),
                c.getInt(7),
                c.getInt(8),
                c.getLong(9)
        );
    }
}
