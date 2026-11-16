package com.example.shadowquiz;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Core ShadowQuiz gameplay screen supporting Classic modes (EASY/MEDIUM/HARD)
 * and 60-second Time Attack (BLITZ), with tactile card states, vector hearts,
 * progressive shadow modifiers, Fieldbook unlocks, and SQLite persistence.
 */
public class MainActivity extends AppCompatActivity {

    private static final int COST_5050 = 20;
    private static final int COST_FREEZE = 20;
    private static final int COST_HINT = 15;

    private DatabaseHelper db;
    private SessionManager sessionManager;
    private SoundFX soundFX;

    private FrameLayout btnQuitGame, btnSoundToggle, vFogMask;
    private ImageView ivSoundIcon, ivMainShadow;
    private LinearLayout llHearts, llComboBadge;
    private TextView tvLevelMode, tvBlitzBanner, tvGameCoins, tvScore, tvComboText;
    private TextView tvTimeDelta, tvTimerSec, tvModifierTag, tvFeedbackBanner, tvHintText;
    private ProgressBar pbTimer;

    private final FrameLayout[] cardOptions = new FrameLayout[6];
    private final ImageView[] ivOptions = new ImageView[6];
    private ImageView[] heartViews;

    private LinearLayout btnPower5050, btnPowerFreeze, btnPowerHint;
    private TextView tvPower5050, tvPowerFreezeTitle, tvPowerFreeze, tvPowerHint;

    private Difficulty difficulty = Difficulty.EASY;
    private int currentUserId = -1;
    private int userCoins = 0;

    private final List<AnimalPuzzle> deck = new ArrayList<>();
    private final Random random = new Random();
    private AnimalPuzzle currentAnimal;
    private int deckIndex = 0;
    private int optionCount = 4;
    private int correctOptionIndex = -1;

    private int score = 0;
    private int level = 1;
    private int lives = 5;
    private int comboStreak = 0;
    private int maxCombo = 0;
    private int correctCount = 0;
    private int totalAnswered = 0;

    private boolean isInputLocked = false;
    private boolean isGameEnded = false;
    private boolean used5050ThisQuestion = false;
    private boolean usedHintThisQuestion = false;
    private boolean usedFreezeThisQuestion = false;
    private boolean free5050Used = false;
    private boolean freeFreezeUsed = false;
    private boolean freeHintUsed = false;
    private boolean isGrayscaleOptions = false;
    private boolean blitzClockInitialized = false;

    private CountDownTimer countDownTimer;
    private long totalQuestionTimeMs = 15000L;
    private long remainingTimeMs = 15000L;
    private int lastTickSecond = -1;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable flashFadeRunnable;

    private ColorMatrixColorFilter grayscaleFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UiInsetsHelper.applySystemBarInsets(findViewById(R.id.headerMain), findViewById(R.id.rootMain));

        db = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        soundFX = SoundFX.getInstance(this);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        grayscaleFilter = new ColorMatrixColorFilter(cm);

        initUserAndDifficulty();
        bindViews();
        buildHeartsRow();
        configureGridForDifficulty();
        setupListeners();

        prepareDeck(null);
        updateTopHud();
        updatePowerUpLabels();
        loadNextPuzzle();
    }

    private void initUserAndDifficulty() {
        currentUserId = sessionManager.getLoggedInUserId();
        User user = db.getUserById(currentUserId);
        if (user == null) {
            user = db.getOrCreateGuestUser();
            if (user != null) {
                currentUserId = user.getId();
                sessionManager.saveUserSession(currentUserId);
            }
        }
        userCoins = user != null ? user.getCoins() : 120;

        String diffCode = getIntent().getStringExtra(HomeActivity.EXTRA_DIFFICULTY);
        difficulty = Difficulty.fromCode(diffCode);
        optionCount = difficulty.getOptionCount();
        lives = difficulty.getInitialLives();
        if (difficulty.isBlitz()) {
            totalQuestionTimeMs = difficulty.getBaseTimeMs();
            remainingTimeMs = difficulty.getBaseTimeMs();
        }
    }

    private void bindViews() {
        btnQuitGame = findViewById(R.id.btnQuitGame);
        tvLevelMode = findViewById(R.id.tvLevelMode);
        llHearts = findViewById(R.id.llHearts);
        tvBlitzBanner = findViewById(R.id.tvBlitzBanner);
        tvGameCoins = findViewById(R.id.tvGameCoins);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);
        ivSoundIcon = findViewById(R.id.ivSoundIcon);

        tvScore = findViewById(R.id.tvScore);
        llComboBadge = findViewById(R.id.llComboBadge);
        tvComboText = findViewById(R.id.tvComboText);
        tvTimeDelta = findViewById(R.id.tvTimeDelta);
        tvTimerSec = findViewById(R.id.tvTimerSec);
        pbTimer = findViewById(R.id.pbTimer);

        ivMainShadow = findViewById(R.id.ivMainShadow);
        vFogMask = findViewById(R.id.vFogMask);
        tvModifierTag = findViewById(R.id.tvModifierTag);
        tvFeedbackBanner = findViewById(R.id.tvFeedbackBanner);
        tvHintText = findViewById(R.id.tvHintText);

        cardOptions[0] = findViewById(R.id.cardOption0);
        cardOptions[1] = findViewById(R.id.cardOption1);
        cardOptions[2] = findViewById(R.id.cardOption2);
        cardOptions[3] = findViewById(R.id.cardOption3);
        cardOptions[4] = findViewById(R.id.cardOption4);
        cardOptions[5] = findViewById(R.id.cardOption5);

        ivOptions[0] = findViewById(R.id.ivOption0);
        ivOptions[1] = findViewById(R.id.ivOption1);
        ivOptions[2] = findViewById(R.id.ivOption2);
        ivOptions[3] = findViewById(R.id.ivOption3);
        ivOptions[4] = findViewById(R.id.ivOption4);
        ivOptions[5] = findViewById(R.id.ivOption5);

        btnPower5050 = findViewById(R.id.btnPower5050);
        btnPowerFreeze = findViewById(R.id.btnPowerFreeze);
        btnPowerHint = findViewById(R.id.btnPowerHint);
        tvPower5050 = findViewById(R.id.tvPower5050);
        tvPowerFreezeTitle = findViewById(R.id.tvPowerFreezeTitle);
        tvPowerFreeze = findViewById(R.id.tvPowerFreeze);
        tvPowerHint = findViewById(R.id.tvPowerHint);

        ivSoundIcon.setImageResource(soundFX.isAnyAudioEnabled() ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
        tvPowerFreezeTitle.setText(difficulty.isBlitz() ? "+8s Rõ Bóng" : "+6s Rõ Bóng");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundFX != null) {
            soundFX.setBgmDucking(true);
            soundFX.onScreenResumed();
            ivSoundIcon.setImageResource(soundFX.isAnyAudioEnabled() ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundFX != null) {
            soundFX.onScreenPaused();
        }
    }

    private void buildHeartsRow() {
        llHearts.removeAllViews();
        if (difficulty.isBlitz()) {
            llHearts.setVisibility(View.GONE);
            tvBlitzBanner.setVisibility(View.VISIBLE);
            heartViews = new ImageView[0];
            return;
        }

        llHearts.setVisibility(View.VISIBLE);
        tvBlitzBanner.setVisibility(View.GONE);

        int count = difficulty.getInitialLives();
        heartViews = new ImageView[count];
        float density = getResources().getDisplayMetrics().density;
        int sizePx = Math.round(density * 18f);
        int marginPx = Math.round(density * 2.5f);

        for (int i = 0; i < count; i++) {
            ImageView heart = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(marginPx, 0, marginPx, 0);
            heart.setLayoutParams(lp);
            heart.setImageResource(R.drawable.ic_heart_full);
            heartViews[i] = heart;
            llHearts.addView(heart);
        }
    }

    private void configureGridForDifficulty() {
        float density = getResources().getDisplayMetrics().density;
        int imgHeightPx = Math.round(density * (optionCount == 4 ? 112f : 84f));

        for (int i = 0; i < 6; i++) {
            boolean active = i < optionCount;
            cardOptions[i].setVisibility(active ? View.VISIBLE : View.GONE);
            ViewGroup.LayoutParams lp = ivOptions[i].getLayoutParams();
            lp.height = imgHeightPx;
            ivOptions[i].setLayoutParams(lp);
        }
    }

    private void setupListeners() {
        btnQuitGame.setOnClickListener(v -> confirmExit());

        btnSoundToggle.setOnClickListener(v -> {
            boolean newState = !soundFX.isAnyAudioEnabled();
            soundFX.setSoundEnabled(newState);
            soundFX.setMusicEnabled(newState);
            ivSoundIcon.setImageResource(newState ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
            Toast.makeText(this, newState ? "Đã bật âm thanh" : "Đã tắt âm thanh", Toast.LENGTH_SHORT).show();
        });

        for (int i = 0; i < optionCount; i++) {
            final int chosenIdx = i;
            cardOptions[i].setOnClickListener(v -> handleOptionSelected(chosenIdx));
        }

        btnPower5050.setOnClickListener(v -> usePower5050());
        btnPowerFreeze.setOnClickListener(v -> usePowerFreeze());
        btnPowerHint.setOnClickListener(v -> usePowerHint());
    }

    private void confirmExit() {
        if (totalAnswered == 0 || isGameEnded) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Thoát ván đấu?")
                .setMessage("Tiến trình ván hiện tại sẽ không được tính nếu bạn rời khỏi ngay bây giờ.")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Chơi tiếp", null)
                .show();
    }

    private void prepareDeck(AnimalPuzzle avoidFirst) {
        deck.clear();
        deck.addAll(AnimalPuzzle.getAllAnimals());
        Collections.shuffle(deck, random);
        if (avoidFirst != null && deck.size() > 1
                && deck.get(0).getAnimalName().equals(avoidFirst.getAnimalName())) {
            Collections.swap(deck, 0, deck.size() - 1);
        }
        deckIndex = 0;
    }

    private void loadNextPuzzle() {
        if (isFinishing() || isDestroyed() || isGameEnded) {
            return;
        }

        cancelFlashFade();
        if (!difficulty.isBlitz()) {
            cancelTimerOnly();
        }
        isInputLocked = false;
        used5050ThisQuestion = false;
        usedHintThisQuestion = false;
        usedFreezeThisQuestion = false;

        if (deckIndex >= deck.size()) {
            prepareDeck(currentAnimal);
        }
        currentAnimal = deck.get(deckIndex++);

        tvFeedbackBanner.setVisibility(View.GONE);
        vFogMask.setVisibility(View.GONE);
        tvModifierTag.setVisibility(View.GONE);
        tvHintText.setText(R.string.prompt_guess_shadow);
        tvHintText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        ivMainShadow.animate().cancel();
        ivMainShadow.setAlpha(1f);
        ivMainShadow.setRotation(0f);
        ivMainShadow.setScaleX(1f);
        ivMainShadow.setScaleY(1f);
        ivMainShadow.setImageResource(currentAnimal.getImageResId());
        if (ivMainShadow.getDrawable() != null) {
            ivMainShadow.getDrawable().mutate();
        }
        ivMainShadow.setColorFilter(ContextCompat.getColor(this, R.color.shadow_ink), PorterDuff.Mode.SRC_IN);

        applyShadowModifiers();

        List<AnimalPuzzle> options = new ArrayList<>();
        options.add(currentAnimal);
        List<AnimalPuzzle> pool = new ArrayList<>(AnimalPuzzle.getAllAnimals());
        pool.removeIf(a -> a.getAnimalName().equals(currentAnimal.getAnimalName()));
        Collections.shuffle(pool, random);
        for (int i = 0; i < optionCount - 1; i++) {
            options.add(pool.get(i));
        }
        Collections.shuffle(options, random);

        for (int i = 0; i < optionCount; i++) {
            AnimalPuzzle opt = options.get(i);
            cardOptions[i].setAlpha(1f);
            cardOptions[i].setClickable(true);
            cardOptions[i].setTranslationX(0f);
            cardOptions[i].setBackgroundResource(R.drawable.bg_card_tactile);

            ivOptions[i].setImageResource(opt.getImageResId());
            if (ivOptions[i].getDrawable() != null) {
                ivOptions[i].getDrawable().mutate();
            }
            if (isGrayscaleOptions) {
                ivOptions[i].setColorFilter(grayscaleFilter);
            } else {
                ivOptions[i].clearColorFilter();
            }

            if (opt.getAnimalName().equals(currentAnimal.getAnimalName())) {
                correctOptionIndex = i;
            }
        }

        if (difficulty.isBlitz()) {
            if (!blitzClockInitialized) {
                blitzClockInitialized = true;
                startCountdown(remainingTimeMs);
            } else if (countDownTimer == null && remainingTimeMs > 0) {
                startCountdown(remainingTimeMs);
            }
        } else {
            totalQuestionTimeMs = Math.max(4800L, difficulty.getBaseTimeMs() - (level - 1) * 600L);
            startCountdown(totalQuestionTimeMs);
        }
        updateTopHud();
    }

    private void applyShadowModifiers() {
        isGrayscaleOptions = false;
        List<String> tags = new ArrayList<>();

        if (difficulty == Difficulty.EASY) {
            if (level >= 3 && random.nextFloat() < 0.45f) {
                ivMainShadow.setScaleX(-1f);
                tags.add("Lật gương");
            }
        } else if (difficulty == Difficulty.MEDIUM) {
            int maxAngle = level >= 2 ? 75 : 35;
            float angle = (random.nextInt(maxAngle * 2 + 1) - maxAngle);
            if (Math.abs(angle) < 12) {
                angle = angle >= 0 ? 22f : -22f;
            }
            ivMainShadow.setRotation(angle);
            tags.add("Bóng xoay");

            if (random.nextBoolean()) {
                ivMainShadow.setScaleX(-1f);
            }
            if (level >= 2 && random.nextFloat() < 0.38f) {
                vFogMask.setVisibility(View.VISIBLE);
                tags.add("Sương mù");
            }
        } else if (difficulty == Difficulty.BLITZ) {
            if (level >= 2) {
                float angle = (random.nextInt(91) - 45);
                ivMainShadow.setRotation(angle);
                tags.add("Bóng xoay");
            }
            if (level >= 2 && random.nextBoolean()) {
                ivMainShadow.setScaleX(-1f);
            }
            if (level >= 3 && random.nextFloat() < 0.35f) {
                vFogMask.setVisibility(View.VISIBLE);
                tags.add("Sương mù");
            }
        } else {
            // HARD ("Ác mộng")
            float angle = 30f + random.nextInt(301);
            ivMainShadow.setRotation(angle);
            if (random.nextFloat() < 0.6f) {
                ivMainShadow.setScaleX(-1f);
            }
            tags.add("Xoay loạn");

            boolean fogActive = random.nextFloat() < 0.45f;
            if (fogActive) {
                vFogMask.setVisibility(View.VISIBLE);
                tags.add("Sương mù");
            }

            boolean flashActive = !fogActive || random.nextFloat() < 0.45f;
            if (flashActive) {
                tags.add("Chớp bóng");
                flashFadeRunnable = () -> {
                    if (!isInputLocked && !isFinishing() && !isDestroyed()) {
                        ivMainShadow.animate().alpha(0.08f).setDuration(450).start();
                    }
                };
                uiHandler.postDelayed(flashFadeRunnable, 1350L);
            }

            if (level >= 3) {
                isGrayscaleOptions = true;
                tags.add("Trắng đen");
            }
        }

        if (!tags.isEmpty()) {
            tvModifierTag.setText(String.join(" • ", tags));
            tvModifierTag.setVisibility(View.VISIBLE);
        }
    }

    private void startCountdown(long durationMs) {
        cancelTimerOnly();
        remainingTimeMs = durationMs;
        lastTickSecond = -1;
        countDownTimer = new CountDownTimer(durationMs, 50L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeMs = millisUntilFinished;
                int progress = (int) Math.max(0, Math.min(1000,
                        (millisUntilFinished * 1000L) / Math.max(1L, totalQuestionTimeMs)));
                pbTimer.setProgress(progress);
                tvTimerSec.setText(String.format(Locale.US, "%.1fs", millisUntilFinished / 1000f));

                int secLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                if (secLeft <= 3 && secLeft >= 1 && secLeft != lastTickSecond) {
                    lastTickSecond = secLeft;
                    soundFX.playTick();
                }
            }

            @Override
            public void onFinish() {
                remainingTimeMs = 0L;
                pbTimer.setProgress(0);
                tvTimerSec.setText("0.0s");
                handleTimeout();
            }
        }.start();
    }

    private void handleOptionSelected(int chosenIndex) {
        if (isInputLocked || isGameEnded) {
            return;
        }
        isInputLocked = true;
        cancelFlashFade();
        cancelTimerOnly();
        totalAnswered++;

        clearGrayscaleOnOptions();

        if (chosenIndex == correctOptionIndex) {
            correctCount++;
            comboStreak++;
            if (comboStreak > maxCombo) {
                maxCombo = comboStreak;
            }

            boolean newlyUnlocked = db.unlockOrIncrementAnimal(currentUserId, currentAnimal.getAnimalName());

            int comboMultiplier = getComboMultiplier(comboStreak);
            int speedBonus = difficulty.isBlitz()
                    ? 4
                    : Math.round(((float) remainingTimeMs / Math.max(1L, totalQuestionTimeMs)) * 8f);
            int gained = Math.round((10 + speedBonus) * comboMultiplier * difficulty.getScoreMultiplier());
            score += gained;

            boolean leveledUp = false;
            if (correctCount % 5 == 0) {
                level++;
                leveledUp = true;
            }

            if (difficulty.isBlitz()) {
                remainingTimeMs = Math.min(75000L, remainingTimeMs + 1500L);
                showTimeDelta("+1.5s", R.color.emerald_correct);
            }

            String feedback = currentAnimal.getAnimalName() + " (+" + gained + "đ)"
                    + (newlyUnlocked ? " • Mở Sổ tay!" : "");
            revealAnswerStage(true, chosenIndex, feedback);
            updateTopHud();

            if (leveledUp) {
                soundFX.playLevelUp();
                Toast.makeText(this, "LÊN LEVEL " + level + "!", Toast.LENGTH_SHORT).show();
            } else {
                soundFX.playCorrect(comboStreak);
            }
            if (!difficulty.isBlitz()) {
                soundFX.speakAnimalName(currentAnimal.getAnimalName());
            }

            long delayMs = difficulty.isBlitz() ? 450L : 950L;
            uiHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && !isGameEnded) {
                    if (difficulty.isBlitz()) {
                        startCountdown(remainingTimeMs);
                    }
                    loadNextPuzzle();
                }
            }, delayMs);

        } else {
            comboStreak = 0;
            soundFX.playWrong();
            shakeView(cardOptions[chosenIndex]);

            if (difficulty.isBlitz()) {
                remainingTimeMs = Math.max(0L, remainingTimeMs - 3000L);
                showTimeDelta("-3.0s", R.color.coral_wrong);
                revealAnswerStage(false, chosenIndex,
                        "Sai rồi! Đáp án: " + currentAnimal.getAnimalName() + " (-3s)");
                updateTopHud();

                uiHandler.postDelayed(() -> {
                    if (isFinishing() || isDestroyed() || isGameEnded) {
                        return;
                    }
                    if (remainingTimeMs <= 0L) {
                        triggerGameOver();
                    } else {
                        startCountdown(remainingTimeMs);
                        loadNextPuzzle();
                    }
                }, 700L);
            } else {
                lives--;
                revealAnswerStage(false, chosenIndex,
                        "Sai rồi! Đáp án: " + currentAnimal.getAnimalName() + " (-1 mạng)");
                updateTopHud();

                uiHandler.postDelayed(() -> {
                    if (isFinishing() || isDestroyed() || isGameEnded) {
                        return;
                    }
                    if (lives <= 0) {
                        triggerGameOver();
                    } else {
                        loadNextPuzzle();
                    }
                }, 1150L);
            }
        }
    }

    private void handleTimeout() {
        if (isInputLocked || isFinishing() || isDestroyed() || isGameEnded) {
            return;
        }
        isInputLocked = true;
        cancelFlashFade();
        cancelTimerOnly();
        clearGrayscaleOnOptions();
        soundFX.playWrong();

        if (difficulty.isBlitz()) {
            revealAnswerStage(false, -1, "Hết 60 giây! Đáp án: " + currentAnimal.getAnimalName());
            updateTopHud();
            uiHandler.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed() && !isGameEnded) {
                    triggerGameOver();
                }
            }, 850L);
            return;
        }

        totalAnswered++;
        lives--;
        comboStreak = 0;
        revealAnswerStage(false, -1,
                "Hết giờ! Đáp án: " + currentAnimal.getAnimalName() + " (-1 mạng)");
        updateTopHud();

        uiHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed() || isGameEnded) {
                return;
            }
            if (lives <= 0) {
                triggerGameOver();
            } else {
                loadNextPuzzle();
            }
        }, 1200L);
    }

    private void showTimeDelta(String text, int colorResId) {
        tvTimeDelta.setText(text);
        tvTimeDelta.setTextColor(ContextCompat.getColor(this, colorResId));
        tvTimeDelta.setVisibility(View.VISIBLE);
        tvTimeDelta.setAlpha(1f);
        tvTimeDelta.animate().alpha(0f).setDuration(750).withEndAction(() -> {
            if (!isFinishing() && !isDestroyed()) {
                tvTimeDelta.setVisibility(View.GONE);
            }
        }).start();
    }

    private void revealAnswerStage(boolean isCorrect, int chosenIndex, String bannerMessage) {
        ivMainShadow.animate().cancel();
        ivMainShadow.setAlpha(1f);
        ivMainShadow.setRotation(0f);
        ivMainShadow.setScaleX(0.85f);
        ivMainShadow.setScaleY(0.85f);
        if (ivMainShadow.getDrawable() != null) {
            ivMainShadow.getDrawable().mutate();
        }
        ivMainShadow.clearColorFilter();
        ivMainShadow.animate().scaleX(1f).scaleY(1f).setDuration(220).start();

        vFogMask.setVisibility(View.GONE);

        if (correctOptionIndex >= 0 && correctOptionIndex < optionCount) {
            cardOptions[correctOptionIndex].setBackgroundResource(R.drawable.bg_card_selected_sage);
        }
        if (!isCorrect && chosenIndex >= 0 && chosenIndex < optionCount) {
            cardOptions[chosenIndex].setBackgroundResource(R.drawable.bg_card_selected_wrong);
        }

        tvFeedbackBanner.setText(bannerMessage);
        tvFeedbackBanner.setBackgroundResource(isCorrect ? R.drawable.bg_pill_sage : R.drawable.bg_pill_amber);
        tvFeedbackBanner.setTextColor(ContextCompat.getColor(this,
                isCorrect ? R.color.brand_forest : R.color.coral_wrong));
        tvFeedbackBanner.setVisibility(View.VISIBLE);
    }

    private void clearGrayscaleOnOptions() {
        for (int i = 0; i < optionCount; i++) {
            if (ivOptions[i].getDrawable() != null) {
                ivOptions[i].getDrawable().mutate();
            }
            ivOptions[i].clearColorFilter();
        }
    }

    private void shakeView(View target) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationX",
                0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f);
        animator.setDuration(360);
        animator.start();
    }

    private int getComboMultiplier(int streak) {
        if (streak >= 10) return 4;
        if (streak >= 6) return 3;
        if (streak >= 3) return 2;
        return 1;
    }

    private void updateTopHud() {
        tvLevelMode.setText("Lv." + level + " • " + difficulty.getDisplayName());
        tvScore.setText(score + " đ");
        tvGameCoins.setText(String.valueOf(userCoins));

        if (!difficulty.isBlitz() && heartViews != null) {
            for (int i = 0; i < heartViews.length; i++) {
                heartViews[i].setImageResource(i < lives ? R.drawable.ic_heart_full : R.drawable.ic_heart_empty);
            }
        }

        if (comboStreak >= 2) {
            int mult = getComboMultiplier(comboStreak);
            tvComboText.setText("COMBO " + comboStreak + (mult > 1 ? " x" + mult : ""));
            llComboBadge.setVisibility(View.VISIBLE);
            llComboBadge.setScaleX(0.86f);
            llComboBadge.setScaleY(0.86f);
            llComboBadge.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        } else {
            llComboBadge.setVisibility(View.INVISIBLE);
        }
    }

    private void updatePowerUpLabels() {
        tvPower5050.setText(free5050Used ? COST_5050 + " Xu" : "Miễn phí");
        tvPowerFreeze.setText(freeFreezeUsed ? COST_FREEZE + " Xu" : "Miễn phí");
        tvPowerHint.setText(freeHintUsed ? COST_HINT + " Xu" : "Miễn phí");
    }

    private boolean consumePowerUpCost(boolean freeAlreadyUsed, int cost) {
        if (!freeAlreadyUsed) {
            return true;
        }
        if (userCoins < cost) {
            Toast.makeText(this, "Không đủ xu! Cần " + cost + " Xu", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (db.spendCoins(currentUserId, cost)) {
            userCoins -= cost;
            updateTopHud();
            return true;
        }
        return false;
    }

    private void usePower5050() {
        if (isInputLocked || used5050ThisQuestion || isGameEnded) {
            return;
        }
        if (!consumePowerUpCost(free5050Used, COST_5050)) {
            return;
        }
        free5050Used = true;
        used5050ThisQuestion = true;
        soundFX.playPowerUp();

        List<Integer> wrongIndices = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            if (i != correctOptionIndex) {
                wrongIndices.add(i);
            }
        }
        Collections.shuffle(wrongIndices, random);
        int toRemove = Math.max(1, optionCount / 2);
        for (int i = 0; i < Math.min(toRemove, wrongIndices.size()); i++) {
            int idx = wrongIndices.get(i);
            cardOptions[idx].setAlpha(0.22f);
            cardOptions[idx].setClickable(false);
        }
        updatePowerUpLabels();
    }

    private void usePowerFreeze() {
        if (isInputLocked || usedFreezeThisQuestion || isGameEnded) {
            return;
        }
        if (!consumePowerUpCost(freeFreezeUsed, COST_FREEZE)) {
            return;
        }
        freeFreezeUsed = true;
        usedFreezeThisQuestion = true;
        soundFX.playPowerUp();

        cancelFlashFade();
        ivMainShadow.animate().cancel();
        ivMainShadow.setAlpha(1f);
        ivMainShadow.setRotation(0f);
        ivMainShadow.setScaleX(1f);
        vFogMask.setVisibility(View.GONE);
        tvModifierTag.setText("Đã làm rõ bóng");
        tvModifierTag.setVisibility(View.VISIBLE);

        long bonus = difficulty.isBlitz() ? 8000L : 6000L;
        long newRemaining = Math.min(75000L, remainingTimeMs + bonus);
        totalQuestionTimeMs = Math.max(totalQuestionTimeMs, newRemaining);
        startCountdown(newRemaining);
        updatePowerUpLabels();
    }

    private void usePowerHint() {
        if (isInputLocked || usedHintThisQuestion || currentAnimal == null || isGameEnded) {
            return;
        }
        if (!consumePowerUpCost(freeHintUsed, COST_HINT)) {
            return;
        }
        freeHintUsed = true;
        usedHintThisQuestion = true;
        soundFX.playPowerUp();

        tvHintText.setText("Gợi ý (" + currentAnimal.getCategory() + "): " + currentAnimal.getHint());
        tvHintText.setTextColor(ContextCompat.getColor(this, R.color.accent_terracotta));
        updatePowerUpLabels();
    }

    private void triggerGameOver() {
        if (isGameEnded) {
            return;
        }
        isGameEnded = true;
        cancelFlashFade();
        cancelTimerOnly();
        soundFX.playGameOver();

        DatabaseHelper.GameSaveResult saveResult = db.recordGameResult(
                currentUserId, score, level, difficulty.getCode(),
                maxCombo, correctCount, totalAnswered
        );

        int accuracy = totalAnswered > 0 ? Math.round((correctCount * 100f) / totalAnswered) : 0;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_over, null);
        TextView tvNewBest = dialogView.findViewById(R.id.tvDialogNewBest);
        TextView tvFinalScore = dialogView.findViewById(R.id.tvDialogFinalScore);
        TextView tvDlgLevel = dialogView.findViewById(R.id.tvDialogLevel);
        TextView tvDlgCombo = dialogView.findViewById(R.id.tvDialogCombo);
        TextView tvDlgAcc = dialogView.findViewById(R.id.tvDialogAccuracy);
        TextView tvDlgCoins = dialogView.findViewById(R.id.tvDialogCoinsEarned);
        TextView tvDlgAch = dialogView.findViewById(R.id.tvDialogAchievements);

        TextView btnReplay = dialogView.findViewById(R.id.btnDialogReplay);
        TextView btnLeaderboard = dialogView.findViewById(R.id.btnDialogLeaderboard);
        TextView btnHome = dialogView.findViewById(R.id.btnDialogHome);

        tvNewBest.setVisibility(saveResult.isNewBest ? View.VISIBLE : View.GONE);
        tvFinalScore.setText(score + " điểm");
        tvDlgLevel.setText("Lv." + level);
        tvDlgCombo.setText(String.valueOf(maxCombo));
        tvDlgAcc.setText(accuracy + "%");
        tvDlgCoins.setText("+" + saveResult.coinsEarned + " Xu");

        if (!saveResult.newlyUnlockedTitles.isEmpty()) {
            tvDlgAch.setText("Danh hiệu mới:\n" + String.join(" • ", saveResult.newlyUnlockedTitles));
            tvDlgAch.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnReplay.setOnClickListener(v -> {
            dialog.dismiss();
            Intent restart = new Intent(this, MainActivity.class);
            restart.putExtra(HomeActivity.EXTRA_DIFFICULTY, difficulty.getCode());
            startActivity(restart);
            finish();
        });

        btnLeaderboard.setOnClickListener(v -> {
            dialog.dismiss();
            Intent lb = new Intent(this, LeaderboardActivity.class);
            lb.putExtra(HomeActivity.EXTRA_TAB, 0);
            startActivity(lb);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private void cancelTimerOnly() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void cancelFlashFade() {
        if (flashFadeRunnable != null) {
            uiHandler.removeCallbacks(flashFadeRunnable);
            flashFadeRunnable = null;
        }
        ivMainShadow.animate().cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimerOnly();
        cancelFlashFade();
        uiHandler.removeCallbacksAndMessages(null);
    }
}
