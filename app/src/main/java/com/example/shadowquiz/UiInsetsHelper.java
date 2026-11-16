package com.example.shadowquiz;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Applies status-bar top padding and navigation-bar bottom padding for Android edge-to-edge display.
 */
public final class UiInsetsHelper {

    private UiInsetsHelper() {
    }

    public static void applySystemBarInsets(View topTarget, View bottomTarget) {
        final int initTop = topTarget.getPaddingTop();
        final int initBottom = bottomTarget.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(topTarget, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            topTarget.setPadding(
                    topTarget.getPaddingLeft(),
                    initTop + bars.top,
                    topTarget.getPaddingRight(),
                    topTarget.getPaddingBottom()
            );
            bottomTarget.setPadding(
                    bottomTarget.getPaddingLeft(),
                    bottomTarget.getPaddingTop(),
                    bottomTarget.getPaddingRight(),
                    initBottom + bars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(topTarget);
    }
}
