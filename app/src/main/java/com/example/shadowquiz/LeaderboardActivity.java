package com.example.shadowquiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays Top Leaderboard (including 60s Blitz), personal Match History, and 8 Achievements from SQLite.
 */
public class LeaderboardActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SoundFX soundFX;
    private int currentUserId = -1;

    private FrameLayout btnBackLeaderboard;
    private TextView tvEmptyState;
    private TextView btnTabRank, btnTabHistory, btnTabAchievements;
    private LinearLayout layoutDifficultyFilters;
    private TextView btnFilterAll, btnFilterEasy, btnFilterMedium, btnFilterHard, btnFilterBlitz;
    private RecyclerView rvLeaderboard;

    private int activeTab = 0; // 0: Rank, 1: History, 2: Achievements
    private String selectedDifficultyFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        UiInsetsHelper.applySystemBarInsets(findViewById(R.id.headerLeaderboard), findViewById(R.id.rvLeaderboard));

        db = new DatabaseHelper(this);
        soundFX = SoundFX.getInstance(this);
        currentUserId = new SessionManager(this).getLoggedInUserId();
        activeTab = getIntent().getIntExtra(HomeActivity.EXTRA_TAB, 0);

        bindViews();
        setupListeners();
        renderActiveTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soundFX != null) {
            soundFX.setBgmDucking(false);
            soundFX.onScreenResumed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (soundFX != null) {
            soundFX.onScreenPaused();
        }
    }

    private void bindViews() {
        btnBackLeaderboard = findViewById(R.id.btnBackLeaderboard);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnTabRank = findViewById(R.id.btnTabRank);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        btnTabAchievements = findViewById(R.id.btnTabAchievements);

        layoutDifficultyFilters = findViewById(R.id.layoutDifficultyFilters);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterEasy = findViewById(R.id.btnFilterEasy);
        btnFilterMedium = findViewById(R.id.btnFilterMedium);
        btnFilterHard = findViewById(R.id.btnFilterHard);
        btnFilterBlitz = findViewById(R.id.btnFilterBlitz);

        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBackLeaderboard.setOnClickListener(v -> {
            soundFX.playTap();
            finish();
        });

        btnTabRank.setOnClickListener(v -> {
            soundFX.playTap();
            activeTab = 0;
            renderActiveTab();
        });
        btnTabHistory.setOnClickListener(v -> {
            soundFX.playTap();
            activeTab = 1;
            renderActiveTab();
        });
        btnTabAchievements.setOnClickListener(v -> {
            soundFX.playTap();
            activeTab = 2;
            renderActiveTab();
        });

        btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
        btnFilterEasy.setOnClickListener(v -> setFilter(Difficulty.EASY.getCode()));
        btnFilterMedium.setOnClickListener(v -> setFilter(Difficulty.MEDIUM.getCode()));
        btnFilterHard.setOnClickListener(v -> setFilter(Difficulty.HARD.getCode()));
        btnFilterBlitz.setOnClickListener(v -> setFilter(Difficulty.BLITZ.getCode()));
    }

    private void setFilter(String code) {
        soundFX.playTap();
        selectedDifficultyFilter = code;
        renderActiveTab();
    }

    private void renderActiveTab() {
        updateTabButtonsUI();
        updateFilterButtonsUI();

        layoutDifficultyFilters.setVisibility(activeTab == 0 ? View.VISIBLE : View.GONE);

        if (activeTab == 0) {
            List<ScoreRecord> topScores = db.getTopLeaderboard(selectedDifficultyFilter, 30);
            tvEmptyState.setVisibility(topScores.isEmpty() ? View.VISIBLE : View.GONE);
            rvLeaderboard.setAdapter(new ScoreAdapter(topScores, false));
        } else if (activeTab == 1) {
            List<ScoreRecord> history = db.getUserHistory(currentUserId, 30);
            tvEmptyState.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
            rvLeaderboard.setAdapter(new ScoreAdapter(history, true));
        } else {
            tvEmptyState.setVisibility(View.GONE);
            List<Achievement> achievements = db.getUserAchievements(currentUserId);
            rvLeaderboard.setAdapter(new AchievementAdapter(achievements));
        }
    }

    private void updateTabButtonsUI() {
        int white = ContextCompat.getColor(this, R.color.white);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);

        TextView[] tabs = {btnTabRank, btnTabHistory, btnTabAchievements};
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = (i == activeTab);
            if (selected) {
                tabs[i].setBackgroundResource(R.drawable.bg_pill_forest);
                tabs[i].setTextColor(white);
            } else {
                tabs[i].setBackground(null);
                tabs[i].setTextColor(secondary);
            }
        }
    }

    private void updateFilterButtonsUI() {
        int white = ContextCompat.getColor(this, R.color.white);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);

        styleFilterChip(btnFilterAll, "ALL".equals(selectedDifficultyFilter), white, secondary);
        styleFilterChip(btnFilterEasy, Difficulty.EASY.getCode().equals(selectedDifficultyFilter), white, secondary);
        styleFilterChip(btnFilterMedium, Difficulty.MEDIUM.getCode().equals(selectedDifficultyFilter), white, secondary);
        styleFilterChip(btnFilterHard, Difficulty.HARD.getCode().equals(selectedDifficultyFilter), white, secondary);
        styleFilterChip(btnFilterBlitz, Difficulty.BLITZ.getCode().equals(selectedDifficultyFilter), white, secondary);
    }

    private void styleFilterChip(TextView pill, boolean selected, int white, int secondary) {
        pill.setBackgroundResource(selected ? R.drawable.bg_pill_forest : R.drawable.bg_pill_oat);
        pill.setTextColor(selected ? white : secondary);
    }

    private class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ScoreVH> {
        private final List<ScoreRecord> items;
        private final boolean isHistoryMode;
        private final int[] avatars = AnimalPuzzle.getAvatarResIds();
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

        ScoreAdapter(List<ScoreRecord> items, boolean isHistoryMode) {
            this.items = new ArrayList<>(items);
            this.isHistoryMode = isHistoryMode;
        }

        @NonNull
        @Override
        public ScoreVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
            return new ScoreVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ScoreVH holder, int position) {
            ScoreRecord r = items.get(position);
            int rank = position + 1;
            holder.tvRankBadge.setText("#" + rank);

            if (!isHistoryMode && rank == 1) {
                holder.tvRankBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                holder.tvRankBadge.setTextColor(ContextCompat.getColor(LeaderboardActivity.this, R.color.amber_lip));
            } else if (!isHistoryMode && rank == 2) {
                holder.tvRankBadge.setBackgroundResource(R.drawable.bg_pill_sage);
                holder.tvRankBadge.setTextColor(ContextCompat.getColor(LeaderboardActivity.this, R.color.brand_forest));
            } else {
                holder.tvRankBadge.setBackgroundResource(R.drawable.bg_pill_oat);
                holder.tvRankBadge.setTextColor(ContextCompat.getColor(LeaderboardActivity.this, R.color.text_secondary));
            }

            int avatarIdx = Math.max(0, Math.min(r.getAvatarIndex(), avatars.length - 1));
            holder.ivAvatar.setImageResource(avatars[avatarIdx]);

            boolean isSelf = r.getUserId() == currentUserId && currentUserId > 0;
            String diffName = Difficulty.fromCode(r.getDifficulty()).getDisplayName();

            if (isHistoryMode) {
                holder.tvPlayerName.setText(diffName + " • Lv." + r.getLevelReached());
                holder.tvSubInfo.setText(sdf.format(new Date(r.getPlayedAt()))
                        + " • Combo " + r.getMaxCombo() + " • " + r.getAccuracy() + "%");
            } else {
                holder.tvPlayerName.setText(r.getDisplayName() + (isSelf ? " (Bạn)" : ""));
                holder.tvSubInfo.setText(diffName + " • Lv." + r.getLevelReached()
                        + " • Combo " + r.getMaxCombo() + " • " + r.getAccuracy() + "%");
            }

            holder.tvScore.setText(r.getScore() + " đ");
            holder.card.setBackgroundResource(
                    isSelf && !isHistoryMode ? R.drawable.bg_card_selected_sage : R.drawable.bg_card_tactile);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ScoreVH extends RecyclerView.ViewHolder {
            final LinearLayout card;
            final TextView tvRankBadge, tvPlayerName, tvSubInfo, tvScore;
            final ImageView ivAvatar;

            ScoreVH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardScoreItem);
                tvRankBadge = itemView.findViewById(R.id.tvRankBadge);
                ivAvatar = itemView.findViewById(R.id.ivItemAvatar);
                tvPlayerName = itemView.findViewById(R.id.tvItemPlayerName);
                tvSubInfo = itemView.findViewById(R.id.tvItemSubInfo);
                tvScore = itemView.findViewById(R.id.tvItemScore);
            }
        }
    }

    private class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchVH> {
        private final List<Achievement> items;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        AchievementAdapter(List<Achievement> items) {
            this.items = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public AchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
            return new AchVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AchVH holder, int position) {
            Achievement ach = items.get(position);
            holder.ivIcon.setImageResource(ach.getIconResId());
            holder.tvTitle.setText(ach.getTitle());

            if (ach.isUnlocked()) {
                holder.tvDesc.setText(ach.getDescription() + " (Mở: "
                        + sdf.format(new Date(ach.getUnlockedAt())) + ")");
                holder.flIconWrap.setBackgroundResource(R.drawable.bg_pill_amber);
                holder.llStatus.setBackgroundResource(R.drawable.bg_pill_sage);
                holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                holder.tvStatus.setText("ĐÃ ĐẠT");
                holder.tvStatus.setTextColor(ContextCompat.getColor(LeaderboardActivity.this, R.color.brand_forest));
                holder.card.setBackgroundResource(R.drawable.bg_card_selected_sage);
                holder.card.setAlpha(1f);
            } else {
                holder.tvDesc.setText(ach.getDescription());
                holder.flIconWrap.setBackgroundResource(R.drawable.bg_pill_oat);
                holder.llStatus.setBackgroundResource(R.drawable.bg_pill_oat);
                holder.ivStatusIcon.setImageResource(R.drawable.ic_lock);
                holder.tvStatus.setText("CHƯA MỞ");
                holder.tvStatus.setTextColor(ContextCompat.getColor(LeaderboardActivity.this, R.color.text_muted));
                holder.card.setBackgroundResource(R.drawable.bg_card_tactile);
                holder.card.setAlpha(0.78f);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class AchVH extends RecyclerView.ViewHolder {
            final LinearLayout card, llStatus;
            final FrameLayout flIconWrap;
            final ImageView ivIcon, ivStatusIcon;
            final TextView tvTitle, tvDesc, tvStatus;

            AchVH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardAchievementItem);
                flIconWrap = itemView.findViewById(R.id.flAchIconWrap);
                ivIcon = itemView.findViewById(R.id.ivAchIcon);
                tvTitle = itemView.findViewById(R.id.tvAchTitle);
                tvDesc = itemView.findViewById(R.id.tvAchDesc);
                llStatus = itemView.findViewById(R.id.llAchStatus);
                ivStatusIcon = itemView.findViewById(R.id.ivAchStatusIcon);
                tvStatus = itemView.findViewById(R.id.tvAchStatus);
            }
        }
    }
}
