package com.example.shadowquiz;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores active user session ID and persistent audio preferences (SFX, BGM, Vietnamese voice).
 */
public class SessionManager {
    private static final String PREF_NAME = "shadow_quiz_prefs";
    private static final String KEY_USER_ID = "logged_in_user_id";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserSession(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public int getLoggedInUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public void logout() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isMusicEnabled() {
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public void setMusicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();
    }

    public boolean isVoiceEnabled() {
        return prefs.getBoolean(KEY_VOICE_ENABLED, true);
    }

    public void setVoiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
    }
}
