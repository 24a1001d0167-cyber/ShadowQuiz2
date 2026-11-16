package com.example.shadowquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

/**
 * Main Safari Dashboard featuring profile editing, daily check-in streak, daily missions,
 * 2x2 mode grid (including 60s Blitz), and Fieldbook / Leaderboard / Achievements shortcuts.
 */
public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY";
    public static final String EXTRA_TAB = "EXTRA_TAB";

    private DatabaseHelper db;
    private SessionManager sessionManager;
    private SoundFX soundFX;

    private LinearLayout layoutProfileEdit;
    private ImageView ivHomeAvatar, ivHomeAudioIcon;
    private TextView tvWelcomeName, tvPlayerRank, tvHomeCoins;
    private TextView tvStatBestScore, tvStatCollection, tvStatStreak, tvStatGamesSub;
    private TextView btnDailyCheckIn, btnStartGame;
    private TextView tvCollectionSub, tvAchievementProgress;
    private FrameLayout btnHomeAudio, btnLogout;

    private final TextView[] tvQuestTitles = new TextView[3];
    private final TextView[] btnQuestActions = new TextView[3];

    private LinearLayout cardDiffEasy, cardDiffMedium, cardDiffHard, cardDiffBlitz;
    private LinearLayout cardOpenCollection, cardOpenLeaderboard, cardOpenAchievements;

    private Difficulty selectedDifficulty = Difficulty.EASY;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        soundFX = SoundFX.getInstance(this);

        currentUserId = sessionManager.getLoggedInUserId();
        if (currentUserId <= 0 || db.getUserById(currentUserId) == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_home);
        UiInsetsHelper.applySystemBarInsets(findViewById(R.id.headerHome), findViewById(R.id.rootHomeContent));
        bindViews();
        setupListeners();
        updateDifficultyUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundFX != null) {
            soundFX.setBgmDucking(false);
            soundFX.onScreenResumed();
            updateAudioIcon();
        }
        refreshUserDashboard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundFX != null) {
            soundFX.onScreenPaused();
        }
    }

    private void bindViews() {
        layoutProfileEdit = findViewById(R.id.layoutProfileEdit);
        ivHomeAvatar = findViewById(R.id.ivHomeAvatar);
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        tvPlayerRank = findViewById(R.id.tvPlayerRank);
        tvHomeCoins = findViewById(R.id.tvHomeCoins);

        tvStatBestScore = findViewById(R.id.tvStatBestScore);
        tvStatCollection = findViewById(R.id.tvStatCollection);
        tvStatStreak = findViewById(R.id.tvStatStreak);
        tvStatGamesSub = findViewById(R.id.tvStatGamesSub);

        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        tvQuestTitles[0] = findViewById(R.id.tvQuestTitle0);
        tvQuestTitles[1] = findViewById(R.id.tvQuestTitle1);
        tvQuestTitles[2] = findViewById(R.id.tvQuestTitle2);
        btnQuestActions[0] = findViewById(R.id.btnQuestAction0);
        btnQuestActions[1] = findViewById(R.id.btnQuestAction1);
        btnQuestActions[2] = findViewById(R.id.btnQuestAction2);

        cardDiffEasy = findViewById(R.id.cardDiffEasy);
        cardDiffMedium = findViewById(R.id.cardDiffMedium);
        cardDiffHard = findViewById(R.id.cardDiffHard);
        cardDiffBlitz = findViewById(R.id.cardDiffBlitz);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnHomeAudio = findViewById(R.id.btnHomeAudio);
        ivHomeAudioIcon = findViewById(R.id.ivHomeAudioIcon);
        btnLogout = findViewById(R.id.btnLogout);

        cardOpenCollection = findViewById(R.id.cardOpenCollection);
        cardOpenLeaderboard = findViewById(R.id.cardOpenLeaderboard);
        cardOpenAchievements = findViewById(R.id.cardOpenAchievements);

        tvCollectionSub = findViewById(R.id.tvCollectionSub);
        tvAchievementProgress = findViewById(R.id.tvAchievementProgress);
    }

    private void setupListeners() {
        layoutProfileEdit.setOnClickListener(v -> {
            soundFX.playTap();
            showEditProfileDialog();
        });

        btnHomeAudio.setOnClickListener(v -> {
            soundFX.playTap();
            showAudioSettingsDialog();
        });

        btnDailyCheckIn.setOnClickListener(v -> {
            int awarded = db.performDailyCheckIn(currentUserId);
            if (awarded > 0) {
                soundFX.playCoin();
                Toast.makeText(this, "Điểm danh thành công! +" + awarded + " Xu!", Toast.LENGTH_SHORT).show();
                refreshUserDashboard();
            } else {
                soundFX.playTap();
                Toast.makeText(this, "Bạn đã điểm danh hôm nay rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        for (int i = 0; i < 3; i++) {
            final int qIndex = i;
            btnQuestActions[i].setOnClickListener(v -> {
                int reward = db.claimDailyQuest(currentUserId, qIndex);
                if (reward > 0) {
                    soundFX.playCoin();
                    Toast.makeText(this, "Hoàn thành nhiệm vụ! +" + reward + " Xu!", Toast.LENGTH_SHORT).show();
                    refreshUserDashboard();
                }
            });
        }

        cardDiffEasy.setOnClickListener(v -> selectDifficulty(Difficulty.EASY));
        cardDiffMedium.setOnClickListener(v -> selectDifficulty(Difficulty.MEDIUM));
        cardDiffHard.setOnClickListener(v -> selectDifficulty(Difficulty.HARD));
        cardDiffBlitz.setOnClickListener(v -> selectDifficulty(Difficulty.BLITZ));

        btnStartGame.setOnClickListener(v -> {
            soundFX.playTap();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_DIFFICULTY, selectedDifficulty.getCode());
            startActivity(intent);
        });

        cardOpenCollection.setOnClickListener(v -> {
            soundFX.playTap();
            Intent intent = new Intent(this, CollectionActivity.class);
            startActivity(intent);
        });

        cardOpenLeaderboard.setOnClickListener(v -> {
            soundFX.playTap();
            Intent intent = new Intent(this, LeaderboardActivity.class);
            intent.putExtra(EXTRA_TAB, 0);
            startActivity(intent);
        });

        cardOpenAchievements.setOnClickListener(v -> {
            soundFX.playTap();
            Intent intent = new Intent(this, LeaderboardActivity.class);
            intent.putExtra(EXTRA_TAB, 2);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            soundFX.playTap();
            sessionManager.logout();
            redirectToLogin();
        });
    }

    private void selectDifficulty(Difficulty diff) {
        soundFX.playTap();
        selectedDifficulty = diff;
        updateDifficultyUI();
    }

    private void refreshUserDashboard() {
        User user = db.getUserById(currentUserId);
        if (user == null) {
            redirectToLogin();
            return;
        }

        int[] avatars = AnimalPuzzle.getAvatarResIds();
        int idx = Math.max(0, Math.min(user.getAvatarIndex(), avatars.length - 1));
        ivHomeAvatar.setImageResource(avatars[idx]);

        tvWelcomeName.setText(user.getDisplayName());
        tvPlayerRank.setText(user.getRankTitle());
        tvHomeCoins.setText(String.valueOf(user.getCoins()));

        int totalAnimals = AnimalPuzzle.getAllAnimals().size();
        int unlockedAnimals = db.getUnlockedAnimalCount(currentUserId);

        tvStatBestScore.setText(String.valueOf(user.getBestScore()));
        tvStatCollection.setText(unlockedAnimals + "/" + totalAnimals);
        tvStatStreak.setText(user.getLoginStreak() + " ngày");
        tvStatGamesSub.setText("Điểm danh • " + user.getGamesPlayed() + " ván");
        tvCollectionSub.setText(unlockedAnimals + "/" + totalAnimals + " loài");

        // Check-in button state
        if (user.isCheckedInToday()) {
            btnDailyCheckIn.setBackgroundResource(R.drawable.bg_pill_oat);
            btnDailyCheckIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            btnDailyCheckIn.setText("Đã nhận hôm nay");
            btnDailyCheckIn.setPadding(btnDailyCheckIn.getPaddingLeft(), 0, btnDailyCheckIn.getPaddingRight(), 0);
        } else {
            btnDailyCheckIn.setBackgroundResource(R.drawable.bg_btn_terracotta_3d);
            btnDailyCheckIn.setTextColor(ContextCompat.getColor(this, R.color.white));
            btnDailyCheckIn.setText("+30 Xu Điểm danh");
        }

        // Daily quests
        List<DailyQuest> quests = db.getTodayQuests(currentUserId);
        int white = ContextCompat.getColor(this, R.color.white);
        int forest = ContextCompat.getColor(this, R.color.brand_forest);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < Math.min(3, quests.size()); i++) {
            DailyQuest q = quests.get(i);
            tvQuestTitles[i].setText(q.getTitle());
            if (q.isClaimed()) {
                btnQuestActions[i].setBackgroundResource(R.drawable.bg_pill_sage);
                btnQuestActions[i].setTextColor(forest);
                btnQuestActions[i].setText("Đã nhận");
                btnQuestActions[i].setClickable(false);
            } else if (q.isClaimable()) {
                btnQuestActions[i].setBackgroundResource(R.drawable.bg_btn_terracotta_3d);
                btnQuestActions[i].setTextColor(white);
                btnQuestActions[i].setText("Nhận +" + q.getRewardCoins());
                btnQuestActions[i].setClickable(true);
            } else {
                btnQuestActions[i].setBackgroundResource(R.drawable.bg_pill_oat);
                btnQuestActions[i].setTextColor(secondary);
                btnQuestActions[i].setText(q.getCurrent() + "/" + q.getTarget());
                btnQuestActions[i].setClickable(false);
            }
        }

        // Achievements progress
        List<Achievement> achievements = db.getUserAchievements(currentUserId);
        int unlockedCount = 0;
        for (Achievement ach : achievements) {
            if (ach.isUnlocked()) {
                unlockedCount++;
            }
        }
        tvAchievementProgress.setText("Đạt " + unlockedCount + "/" + achievements.size());
    }

    private void updateDifficultyUI() {
        cardDiffEasy.setBackgroundResource(
                selectedDifficulty == Difficulty.EASY ? R.drawable.bg_card_selected_sage : R.drawable.bg_card_tactile);
        cardDiffMedium.setBackgroundResource(
                selectedDifficulty == Difficulty.MEDIUM ? R.drawable.bg_card_selected_amber : R.drawable.bg_card_tactile);
        cardDiffHard.setBackgroundResource(
                selectedDifficulty == Difficulty.HARD ? R.drawable.bg_card_selected_wrong : R.drawable.bg_card_tactile);
        cardDiffBlitz.setBackgroundResource(
                selectedDifficulty == Difficulty.BLITZ ? R.drawable.bg_card_selected_terracotta : R.drawable.bg_card_tactile);

        btnStartGame.setText("BẮT ĐẦU KHÁM PHÁ • " + selectedDifficulty.getDisplayName().toUpperCase(Locale.ROOT));
    }

    private void showEditProfileDialog() {
        User user = db.getUserById(currentUserId);
        if (user == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilEditDisplayName);
        TextInputEditText etName = dialogView.findViewById(R.id.etEditDisplayName);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelProfile);
        TextView btnSave = dialogView.findViewById(R.id.btnSaveProfile);

        int[] avatarIds = {
                R.id.editAvatar0, R.id.editAvatar1, R.id.editAvatar2, R.id.editAvatar3,
                R.id.editAvatar4, R.id.editAvatar5, R.id.editAvatar6, R.id.editAvatar7
        };
        final FrameLayout[] avatarFrames = new FrameLayout[8];
        for (int i = 0; i < 8; i++) {
            avatarFrames[i] = dialogView.findViewById(avatarIds[i]);
        }

        final int[] chosenAvatar = {Math.max(0, Math.min(user.getAvatarIndex(), 7))};
        etName.setText(user.getDisplayName());

        Runnable refreshAvatars = () -> {
            for (int i = 0; i < 8; i++) {
                avatarFrames[i].setBackgroundResource(
                        i == chosenAvatar[0] ? R.drawable.bg_card_selected_sage : R.drawable.bg_card_tactile);
            }
        };
        refreshAvatars.run();

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            avatarFrames[i].setOnClickListener(v -> {
                chosenAvatar[0] = idx;
                refreshAvatars.run();
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (newName.length() < 2) {
                tilName.setError("Tên hiển thị phải từ 2 ký tự trở lên");
                return;
            }
            if (db.updateUserProfile(currentUserId, newName, chosenAvatar[0])) {
                Toast.makeText(this, "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshUserDashboard();
            }
        });

        dialog.show();
    }

    private void updateAudioIcon() {
        if (ivHomeAudioIcon != null && soundFX != null) {
            ivHomeAudioIcon.setImageResource(
                    soundFX.isAnyAudioEnabled() ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
        }
    }

    private void showAudioSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_audio_settings, null);
        LinearLayout rowSfx = view.findViewById(R.id.rowToggleSfx);
        LinearLayout rowMusic = view.findViewById(R.id.rowToggleMusic);
        LinearLayout rowVoice = view.findViewById(R.id.rowToggleVoice);
        TextView tvSfx = view.findViewById(R.id.tvToggleSfx);
        TextView tvMusic = view.findViewById(R.id.tvToggleMusic);
        TextView tvVoice = view.findViewById(R.id.tvToggleVoice);
        TextView btnTest = view.findViewById(R.id.btnTestAudio);
        TextView btnClose = view.findViewById(R.id.btnCloseAudio);

        Runnable syncUi = () -> {
            styleAudioPill(tvSfx, soundFX.isSoundEnabled());
            styleAudioPill(tvMusic, soundFX.isMusicEnabled());
            styleAudioPill(tvVoice, soundFX.isVoiceEnabled());
            updateAudioIcon();
        };
        syncUi.run();

        rowSfx.setOnClickListener(v -> {
            soundFX.setSoundEnabled(!soundFX.isSoundEnabled());
            syncUi.run();
        });
        rowMusic.setOnClickListener(v -> {
            soundFX.setMusicEnabled(!soundFX.isMusicEnabled());
            soundFX.playTap();
            syncUi.run();
        });
        rowVoice.setOnClickListener(v -> {
            boolean next = !soundFX.isVoiceEnabled();
            soundFX.setVoiceEnabled(next);
            soundFX.playTap();
            syncUi.run();
        });

        btnTest.setOnClickListener(v -> {
            soundFX.playCoin();
            soundFX.speakAnimalName("Xin chào nhà thám hiểm");
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> {
            soundFX.playTap();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void styleAudioPill(TextView pill, boolean enabled) {
        if (enabled) {
            pill.setBackgroundResource(R.drawable.bg_pill_forest);
            pill.setTextColor(ContextCompat.getColor(this, R.color.white));
            pill.setText("BẬT");
        } else {
            pill.setBackgroundResource(R.drawable.bg_card_tactile);
            pill.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            pill.setText("TẮT");
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
