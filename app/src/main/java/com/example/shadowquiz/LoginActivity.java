package com.example.shadowquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Authentication screen supporting Login, Registration with avatar picker, and 1-tap Guest play.
 */
public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager sessionManager;
    private SoundFX soundFX;

    private TextView btnModeLogin, btnModeRegister, btnSubmitAuth;
    private LinearLayout btnGuestPlay, layoutAvatarPicker;
    private TextInputLayout tilUsername, tilDisplayName, tilPassword;
    private TextInputEditText etUsername, etDisplayName, etPassword;
    private final FrameLayout[] avatarCards = new FrameLayout[6];

    private boolean isRegisterMode = false;
    private int selectedAvatarIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        int savedUserId = sessionManager.getLoggedInUserId();
        if (savedUserId > 0 && db.getUserById(savedUserId) != null) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_login);
        soundFX = SoundFX.getInstance(this);
        View root = findViewById(R.id.rootLoginContent);
        UiInsetsHelper.applySystemBarInsets(root, root);
        bindViews();
        setupListeners();
        updateAuthModeUI();
        updateAvatarSelectionUI();
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
        btnModeLogin = findViewById(R.id.btnModeLogin);
        btnModeRegister = findViewById(R.id.btnModeRegister);
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        btnGuestPlay = findViewById(R.id.btnGuestPlay);

        tilUsername = findViewById(R.id.tilUsername);
        tilDisplayName = findViewById(R.id.tilDisplayName);
        tilPassword = findViewById(R.id.tilPassword);

        etUsername = findViewById(R.id.etUsername);
        etDisplayName = findViewById(R.id.etDisplayName);
        etPassword = findViewById(R.id.etPassword);

        layoutAvatarPicker = findViewById(R.id.layoutAvatarPicker);

        avatarCards[0] = findViewById(R.id.cardAvatar0);
        avatarCards[1] = findViewById(R.id.cardAvatar1);
        avatarCards[2] = findViewById(R.id.cardAvatar2);
        avatarCards[3] = findViewById(R.id.cardAvatar3);
        avatarCards[4] = findViewById(R.id.cardAvatar4);
        avatarCards[5] = findViewById(R.id.cardAvatar5);
    }

    private void setupListeners() {
        btnModeLogin.setOnClickListener(v -> {
            soundFX.playTap();
            if (isRegisterMode) {
                isRegisterMode = false;
                clearErrors();
                updateAuthModeUI();
            }
        });

        btnModeRegister.setOnClickListener(v -> {
            soundFX.playTap();
            if (!isRegisterMode) {
                isRegisterMode = true;
                clearErrors();
                updateAuthModeUI();
            }
        });

        for (int i = 0; i < avatarCards.length; i++) {
            final int index = i;
            avatarCards[i].setOnClickListener(v -> {
                soundFX.playTap();
                selectedAvatarIndex = index;
                updateAvatarSelectionUI();
            });
        }

        btnSubmitAuth.setOnClickListener(v -> {
            soundFX.playTap();
            handleAuthSubmit();
        });

        btnGuestPlay.setOnClickListener(v -> {
            soundFX.playTap();
            User guest = db.getOrCreateGuestUser();
            if (guest != null) {
                sessionManager.saveUserSession(guest.getId());
                Toast.makeText(this, "Chào mừng " + guest.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                goToHome();
            }
        });
    }

    private void updateAuthModeUI() {
        int white = ContextCompat.getColor(this, R.color.white);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);

        if (isRegisterMode) {
            tilDisplayName.setVisibility(View.VISIBLE);
            layoutAvatarPicker.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText(R.string.btn_register);

            btnModeRegister.setBackgroundResource(R.drawable.bg_pill_forest);
            btnModeRegister.setTextColor(white);
            btnModeLogin.setBackground(null);
            btnModeLogin.setTextColor(secondary);
        } else {
            tilDisplayName.setVisibility(View.GONE);
            layoutAvatarPicker.setVisibility(View.GONE);
            btnSubmitAuth.setText(R.string.btn_login);

            btnModeLogin.setBackgroundResource(R.drawable.bg_pill_forest);
            btnModeLogin.setTextColor(white);
            btnModeRegister.setBackground(null);
            btnModeRegister.setTextColor(secondary);
        }
    }

    private void updateAvatarSelectionUI() {
        for (int i = 0; i < avatarCards.length; i++) {
            boolean selected = (i == selectedAvatarIndex);
            avatarCards[i].setBackgroundResource(selected ? R.drawable.bg_card_selected_sage : R.drawable.bg_card_tactile);
            avatarCards[i].setScaleX(selected ? 1.06f : 1.0f);
            avatarCards[i].setScaleY(selected ? 1.06f : 1.0f);
        }
    }

    private void clearErrors() {
        tilUsername.setError(null);
        tilDisplayName.setError(null);
        tilPassword.setError(null);
    }

    private void handleAuthSubmit() {
        clearErrors();

        String username = etUsername.getText() == null ? "" : etUsername.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        String displayName = etDisplayName.getText() == null ? "" : etDisplayName.getText().toString().trim();

        if (username.length() < 3) {
            tilUsername.setError("Tên đăng nhập phải từ 3 ký tự trở lên");
            return;
        }
        if (username.contains(" ")) {
            tilUsername.setError("Tên đăng nhập không được chứa khoảng trắng");
            return;
        }

        if (isRegisterMode) {
            if (displayName.length() < 2) {
                tilDisplayName.setError("Vui lòng nhập tên hiển thị (ít nhất 2 ký tự)");
                return;
            }
            if (password.length() < 4) {
                tilPassword.setError("Mật khẩu phải từ 4 ký tự trở lên");
                return;
            }

            long newUserId = db.registerUser(username, displayName, password, selectedAvatarIndex);
            if (newUserId == -1) {
                tilUsername.setError("Tên đăng nhập này đã tồn tại, hãy chọn tên khác");
                return;
            }
            sessionManager.saveUserSession((int) newUserId);
            Toast.makeText(this, "Tạo hồ sơ thành công! Tặng bạn 120 Xu khởi đầu!", Toast.LENGTH_SHORT).show();
            goToHome();
        } else {
            if (password.isEmpty()) {
                tilPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }
            User user = db.loginUser(username, password);
            if (user == null) {
                tilPassword.setError("Sai tên đăng nhập hoặc mật khẩu");
                return;
            }
            sessionManager.saveUserSession(user.getId());
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            goToHome();
        }
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
