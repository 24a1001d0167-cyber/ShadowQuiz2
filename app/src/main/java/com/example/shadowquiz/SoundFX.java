package com.example.shadowquiz;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * App-wide game audio engine combining low-latency SoundPool PCM WAV effects,
 * seamless looping Safari BGM (MediaPlayer), and optional Vietnamese TTS voiceover.
 */
public class SoundFX {

    private static SoundFX instance;

    private final Context appContext;
    private final SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Integer> loadedIds = Collections.synchronizedSet(new HashSet<>());

    private SoundPool soundPool;
    private int sfxTapId;
    private int sfxCorrectId;
    private int sfxWrongId;
    private int sfxLevelUpId;
    private int sfxPowerUpId;
    private int sfxCoinId;
    private int sfxTickId;
    private int sfxGameOverId;

    private MediaPlayer bgmPlayer;
    private int resumedScreens = 0;
    private boolean inMatchDucking = false;
    private final Runnable pauseBgmRunnable = () -> {
        if (resumedScreens <= 0) {
            pauseBgmInternal();
        }
    };

    private TextToSpeech tts;
    private boolean ttsReady = false;

    public static synchronized SoundFX getInstance(Context context) {
        if (instance == null) {
            instance = new SoundFX(context.getApplicationContext());
        }
        return instance;
    }

    private SoundFX(Context appContext) {
        this.appContext = appContext;
        this.sessionManager = new SessionManager(appContext);
        initSoundPool();
        initTts();
    }

    private void initSoundPool() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attrs)
                    .build();
            soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
                if (status == 0) {
                    loadedIds.add(sampleId);
                }
            });

            sfxTapId = soundPool.load(appContext, R.raw.sfx_tap, 1);
            sfxCorrectId = soundPool.load(appContext, R.raw.sfx_correct, 1);
            sfxWrongId = soundPool.load(appContext, R.raw.sfx_wrong, 1);
            sfxLevelUpId = soundPool.load(appContext, R.raw.sfx_level_up, 1);
            sfxPowerUpId = soundPool.load(appContext, R.raw.sfx_powerup, 1);
            sfxCoinId = soundPool.load(appContext, R.raw.sfx_coin, 1);
            sfxTickId = soundPool.load(appContext, R.raw.sfx_tick, 1);
            sfxGameOverId = soundPool.load(appContext, R.raw.sfx_gameover, 1);
        } catch (RuntimeException ignored) {
            soundPool = null;
        }
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    try {
                        int res = tts.setLanguage(new Locale("vi", "VN"));
                        ttsReady = res != TextToSpeech.LANG_MISSING_DATA
                                && res != TextToSpeech.LANG_NOT_SUPPORTED;
                        if (ttsReady) {
                            tts.setSpeechRate(1.05f);
                        }
                    } catch (RuntimeException ignored) {
                        ttsReady = false;
                    }
                }
            });
        } catch (RuntimeException ignored) {
            tts = null;
            ttsReady = false;
        }
    }

    public boolean isSoundEnabled() {
        return sessionManager.isSoundEnabled();
    }

    public void setSoundEnabled(boolean enabled) {
        sessionManager.setSoundEnabled(enabled);
        if (enabled) {
            playTap();
        } else if (tts != null) {
            try {
                tts.stop();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public boolean isMusicEnabled() {
        return sessionManager.isMusicEnabled();
    }

    public void setMusicEnabled(boolean enabled) {
        sessionManager.setMusicEnabled(enabled);
        if (enabled && resumedScreens > 0) {
            startBgmInternal();
        } else {
            pauseBgmInternal();
        }
    }

    public boolean isVoiceEnabled() {
        return sessionManager.isVoiceEnabled();
    }

    public void setVoiceEnabled(boolean enabled) {
        sessionManager.setVoiceEnabled(enabled);
        if (!enabled && tts != null) {
            try {
                tts.stop();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public boolean isAnyAudioEnabled() {
        return isSoundEnabled() || isMusicEnabled();
    }

    public void setBgmDucking(boolean inMatch) {
        this.inMatchDucking = inMatch;
        applyBgmVolume();
    }

    public void onScreenResumed() {
        resumedScreens++;
        mainHandler.removeCallbacks(pauseBgmRunnable);
        if (sessionManager.isMusicEnabled()) {
            startBgmInternal();
        }
    }

    public void onScreenPaused() {
        resumedScreens = Math.max(0, resumedScreens - 1);
        mainHandler.removeCallbacks(pauseBgmRunnable);
        mainHandler.postDelayed(pauseBgmRunnable, 450L);
    }

    private void ensureBgmPlayer() {
        if (bgmPlayer != null) {
            return;
        }
        try {
            AudioAttributes musicAttrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            bgmPlayer = MediaPlayer.create(appContext, R.raw.bgm_safari);
            if (bgmPlayer != null) {
                bgmPlayer.setAudioAttributes(musicAttrs);
                bgmPlayer.setLooping(true);
                applyBgmVolume();
            }
        } catch (RuntimeException ignored) {
            bgmPlayer = null;
        }
    }

    private void applyBgmVolume() {
        if (bgmPlayer != null) {
            try {
                float vol = inMatchDucking ? 0.15f : 0.24f;
                bgmPlayer.setVolume(vol, vol);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void startBgmInternal() {
        ensureBgmPlayer();
        if (bgmPlayer != null) {
            try {
                applyBgmVolume();
                if (!bgmPlayer.isPlaying()) {
                    bgmPlayer.start();
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void pauseBgmInternal() {
        if (bgmPlayer != null) {
            try {
                if (bgmPlayer.isPlaying()) {
                    bgmPlayer.pause();
                }
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public void playTap() {
        playSound(sfxTapId, 0.55f, 1.0f, true);
    }

    public void playCoin() {
        playSound(sfxCoinId, 0.85f, 1.0f, true);
    }

    public void playCorrect(int comboStreak) {
        float rate = Math.min(1.30f, 1.0f + Math.max(0, comboStreak - 1) * 0.04f);
        playSound(sfxCorrectId, 0.88f, rate, true);
    }

    public void playWrong() {
        playSound(sfxWrongId, 0.85f, 1.0f, true);
    }

    public void playLevelUp() {
        playSound(sfxLevelUpId, 0.92f, 1.0f, true);
    }

    public void playPowerUp() {
        playSound(sfxPowerUpId, 0.85f, 1.0f, true);
    }

    public void playTick() {
        playSound(sfxTickId, 0.60f, 1.0f, false);
    }

    public void playGameOver() {
        playSound(sfxGameOverId, 0.90f, 1.0f, true);
    }

    public void speakAnimalName(String text) {
        if (!ttsReady || tts == null || text == null || text.isEmpty()) {
            return;
        }
        if (!sessionManager.isVoiceEnabled() || !sessionManager.isSoundEnabled()) {
            return;
        }
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shadowquiz_tts");
        } catch (RuntimeException ignored) {
        }
    }

    private void playSound(int soundId, float volume, float rate, boolean allowRetry) {
        if (!sessionManager.isSoundEnabled() || soundPool == null || soundId == 0) {
            return;
        }
        if (!loadedIds.contains(soundId)) {
            if (allowRetry) {
                mainHandler.postDelayed(() -> playSound(soundId, volume, rate, false), 90L);
            }
            return;
        }
        try {
            soundPool.play(soundId, volume, volume, 1, 0, rate);
        } catch (RuntimeException ignored) {
        }
    }
}
