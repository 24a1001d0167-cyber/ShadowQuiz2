package com.example.shadowquiz;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Fieldbook Collection ("Sổ Tay Muôn Loài") showing discovered vs. silhouette-locked animals
 * and detailed biological fun facts with interactive Shadow-to-Color preview.
 */
public class CollectionActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SoundFX soundFX;
    private int currentUserId = -1;

    private TextView tvCollectionCountPill;
    private ProgressBar pbCollection;
    private RecyclerView rvCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        UiInsetsHelper.applySystemBarInsets(findViewById(R.id.headerCollection), findViewById(R.id.rvCollection));

        db = new DatabaseHelper(this);
        soundFX = SoundFX.getInstance(this);
        currentUserId = new SessionManager(this).getLoggedInUserId();

        FrameLayout btnBack = findViewById(R.id.btnBackCollection);
        tvCollectionCountPill = findViewById(R.id.tvCollectionCountPill);
        pbCollection = findViewById(R.id.pbCollection);
        rvCollection = findViewById(R.id.rvCollection);

        btnBack.setOnClickListener(v -> {
            soundFX.playTap();
            finish();
        });
        rvCollection.setLayoutManager(new GridLayoutManager(this, 2));

        loadCollection();
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

    private void loadCollection() {
        List<AnimalCollectionItem> items = db.getAnimalCollection(currentUserId);
        int unlocked = 0;
        for (AnimalCollectionItem item : items) {
            if (item.isUnlocked()) {
                unlocked++;
            }
        }
        tvCollectionCountPill.setText("Đã mở " + unlocked + "/" + items.size());
        pbCollection.setMax(items.size());
        pbCollection.setProgress(unlocked);
        rvCollection.setAdapter(new CollectionAdapter(items));
    }

    private void showAnimalDetailDialog(AnimalCollectionItem item) {
        AnimalPuzzle puzzle = item.getPuzzle();
        soundFX.playCorrect(1);
        soundFX.speakAnimalName(puzzle.getAnimalName());
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_animal_detail, null);

        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvCaught = view.findViewById(R.id.tvDetailCaughtCount);
        ImageView ivAnimal = view.findViewById(R.id.ivDetailAnimal);
        TextView tvName = view.findViewById(R.id.tvDetailName);
        TextView tvHint = view.findViewById(R.id.tvDetailHint);
        TextView tvFunFact = view.findViewById(R.id.tvDetailFunFact);
        TextView btnToggle = view.findViewById(R.id.btnToggleShadowPreview);
        TextView btnClose = view.findViewById(R.id.btnCloseDetail);

        tvCategory.setText(puzzle.getCategory());
        tvCaught.setText("Đã gặp " + item.getTimesCaught() + " lần");
        tvName.setText(puzzle.getAnimalName());
        tvHint.setText("Đặc điểm: " + puzzle.getHint());
        tvFunFact.setText("Sự thật thú vị: " + puzzle.getFunFact());

        ivAnimal.setImageResource(puzzle.getImageResId());
        if (ivAnimal.getDrawable() != null) {
            ivAnimal.getDrawable().mutate();
        }
        ivAnimal.clearColorFilter();

        final boolean[] showingShadow = {false};
        int shadowColor = ContextCompat.getColor(this, R.color.shadow_ink);

        btnToggle.setOnClickListener(v -> {
            soundFX.playTap();
            showingShadow[0] = !showingShadow[0];
            if (ivAnimal.getDrawable() != null) {
                ivAnimal.getDrawable().mutate();
            }
            if (showingShadow[0]) {
                ivAnimal.setColorFilter(shadowColor, PorterDuff.Mode.SRC_IN);
                btnToggle.setText("Xem ảnh màu");
            } else {
                ivAnimal.clearColorFilter();
                btnToggle.setText("Xem bóng đen");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.VH> {
        private final List<AnimalCollectionItem> items;

        CollectionAdapter(List<AnimalCollectionItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection_animal, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AnimalCollectionItem item = items.get(position);
            AnimalPuzzle puzzle = item.getPuzzle();

            holder.ivAnimal.setImageResource(puzzle.getImageResId());
            if (holder.ivAnimal.getDrawable() != null) {
                holder.ivAnimal.getDrawable().mutate();
            }

            if (item.isUnlocked()) {
                holder.ivAnimal.clearColorFilter();
                holder.ivAnimal.setAlpha(1f);
                holder.tvName.setText(puzzle.getAnimalName());
                holder.tvName.setTextColor(ContextCompat.getColor(CollectionActivity.this, R.color.text_primary));
                holder.tvSub.setText(puzzle.getCategory() + " • " + item.getTimesCaught() + " lần");
                holder.tvSub.setBackgroundResource(R.drawable.bg_pill_sage);
                holder.tvSub.setTextColor(ContextCompat.getColor(CollectionActivity.this, R.color.brand_forest));
                holder.card.setOnClickListener(v -> showAnimalDetailDialog(item));
            } else {
                holder.ivAnimal.setColorFilter(ContextCompat.getColor(CollectionActivity.this, R.color.shadow_ink),
                        PorterDuff.Mode.SRC_IN);
                holder.ivAnimal.setAlpha(0.72f);
                holder.tvName.setText("???");
                holder.tvName.setTextColor(ContextCompat.getColor(CollectionActivity.this, R.color.text_muted));
                holder.tvSub.setText("Chưa mở • " + puzzle.getCategory());
                holder.tvSub.setBackgroundResource(R.drawable.bg_pill_oat);
                holder.tvSub.setTextColor(ContextCompat.getColor(CollectionActivity.this, R.color.text_secondary));
                holder.card.setOnClickListener(v -> Toast.makeText(
                        CollectionActivity.this,
                        "Hãy đoán đúng loài vật thuộc nhóm " + puzzle.getCategory() + " để mở khóa!",
                        Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final LinearLayout card;
            final ImageView ivAnimal;
            final TextView tvName, tvSub;

            VH(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardCollectionItem);
                ivAnimal = itemView.findViewById(R.id.ivCollectionAnimal);
                tvName = itemView.findViewById(R.id.tvCollectionName);
                tvSub = itemView.findViewById(R.id.tvCollectionSub);
            }
        }
    }
}
