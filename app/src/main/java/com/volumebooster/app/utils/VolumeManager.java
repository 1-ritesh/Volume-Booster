package com.volumebooster.app.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.LoudnessEnhancer;

public class VolumeManager {

    private final AudioManager audioManager;
    private LoudnessEnhancer loudnessEnhancer;
    private int originalVolume;
    private boolean loudnessActive = false;

    public VolumeManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Sets the boost level (100–200%).
     * Uses LoudnessEnhancer for amplification beyond hardware max,
     * and also sets system volume to max as baseline.
     */
    public void setBoostLevel(int percent) {
        // Push system volume to max
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume,
                AudioManager.FLAG_SHOW_UI);

        // Extra amplification via LoudnessEnhancer (0% → 0dB, 200% → +20dB)
        float gainMb = (percent - 100) * 20f; // centibels (mB)
        applyLoudnessEnhancer((int) gainMb);
    }

    public void resetBoost() {
        if (loudnessEnhancer != null) {
            loudnessEnhancer.setEnabled(false);
            loudnessEnhancer.release();
            loudnessEnhancer = null;
            loudnessActive = false;
        }
        // Restore original volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
    }

    private void applyLoudnessEnhancer(int gainMb) {
        try {
            if (loudnessEnhancer == null) {
                // audioSessionId 0 = global output mix
                loudnessEnhancer = new LoudnessEnhancer(0);
            }
            loudnessEnhancer.setTargetGain(gainMb);
            if (!loudnessEnhancer.getEnabled()) {
                loudnessEnhancer.setEnabled(true);
            }
            loudnessActive = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLoudnessActive() { return loudnessActive; }
}
