package com.volumebooster.app.services;

import android.app.*;
import android.content.Intent;
import android.media.AudioManager;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.volumebooster.app.R;
import com.volumebooster.app.activities.MainActivity;
import com.volumebooster.app.utils.VolumeManager;

public class VolumeBoosterService extends Service {

    public static final String CHANNEL_ID = "VolumeBoosterChannel";
    public static final String ACTION_VOLUME_UP     = "com.volumebooster.ACTION_VOLUME_UP";
    public static final String ACTION_VOLUME_DOWN   = "com.volumebooster.ACTION_VOLUME_DOWN";
    public static final String ACTION_TOGGLE_BOOST  = "com.volumebooster.ACTION_TOGGLE_BOOST";

    private final IBinder binder = new LocalBinder();
    private VolumeManager volumeManager;
    private AudioManager audioManager;
    private boolean boosterEnabled = false;
    private int currentBoostLevel = 100;

    public class LocalBinder extends Binder {
        public VolumeBoosterService getService() { return VolumeBoosterService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        volumeManager = new VolumeManager(this);
        audioManager  = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
        try {
            startForeground(1, buildNotification());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_VOLUME_UP:
                    adjustSystemVolume(true);
                    break;
                case ACTION_VOLUME_DOWN:
                    adjustSystemVolume(false);
                    break;
                case ACTION_TOGGLE_BOOST:
                    toggleBoost();
                    break;
            }
            updateNotification();
        }
        return START_STICKY;
    }

    private void adjustSystemVolume(boolean up) {
        if (up) {
            currentBoostLevel = Math.min(200, currentBoostLevel + 10);
        } else {
            currentBoostLevel = Math.max(100, currentBoostLevel - 10);
        }
        if (boosterEnabled) {
            volumeManager.setBoostLevel(currentBoostLevel);
        }
        broadcastUpdate();
    }

    private void toggleBoost() {
        boosterEnabled = !boosterEnabled;
        if (boosterEnabled) {
            volumeManager.setBoostLevel(currentBoostLevel);
        } else {
            volumeManager.resetBoost();
        }
        broadcastUpdate();
    }

    private void broadcastUpdate() {
        Intent i = new Intent("com.volumebooster.BOOSTER_UPDATE");
        i.putExtra("boostLevel", currentBoostLevel);
        i.putExtra("enabled", boosterEnabled);
        sendBroadcast(i);
    }

    public void setBoostLevel(int level) {
        currentBoostLevel = level;
        if (boosterEnabled) volumeManager.setBoostLevel(level);
        updateNotification();
    }

    public void disableBoost() {
        boosterEnabled = false;
        volumeManager.resetBoost();
        updateNotification();
    }

    public boolean isBoosterEnabled()    { return boosterEnabled; }
    public int getCurrentBoostLevel()    { return currentBoostLevel; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Volume Booster", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Volume booster controls");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent piOpen = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE);

        // Volume down action
        Intent downIntent = new Intent(ACTION_VOLUME_DOWN);
        downIntent.setPackage(getPackageName());
        PendingIntent piDown = PendingIntent.getBroadcast(this, 1, downIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // Toggle boost action
        Intent toggleIntent = new Intent(ACTION_TOGGLE_BOOST);
        toggleIntent.setPackage(getPackageName());
        PendingIntent piToggle = PendingIntent.getBroadcast(this, 2, toggleIntent,
                PendingIntent.FLAG_IMMUTABLE);

        // Volume up action
        Intent upIntent = new Intent(ACTION_VOLUME_UP);
        upIntent.setPackage(getPackageName());
        PendingIntent piUp = PendingIntent.getBroadcast(this, 3, upIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_volume_booster)
                .setContentTitle("Volume Booster")
                .setContentText(boosterEnabled
                        ? "Boost: " + currentBoostLevel + "% — Active"
                        : "Booster is OFF")
                .setContentIntent(piOpen)
                .addAction(R.drawable.ic_volume_down, "-10%", piDown)
                .addAction(boosterEnabled ? R.drawable.ic_boost_on : R.drawable.ic_boost_off,
                        boosterEnabled ? "ON" : "OFF", piToggle)
                .addAction(R.drawable.ic_volume_up, "+10%", piUp)
                .setOngoing(true)
                .setSilent(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1, buildNotification());
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        volumeManager.resetBoost();
    }
}
