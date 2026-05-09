package com.volumebooster.app.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.volumebooster.app.R;
import com.volumebooster.app.models.SoundMode;
import com.volumebooster.app.services.MusicService;
import com.volumebooster.app.services.VolumeBoosterService;
import com.volumebooster.app.utils.VolumeManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 100;

    // UI components
    private Slider volumeSlider;
    private TextView tvVolumePercent;
    private TextView tvBoostLabel;
    private Switch switchBooster;
    private ImageView ivWaveform;
    private LinearLayout llSoundModes;
    private CardView cardCurrentMode;
    private TextView tvCurrentMode;
    private ImageButton btnPrev, btnPlayPause, btnNext;
    private TextView tvSongTitle, tvArtist;
    private ImageView ivAlbumArt;
    private TextView tvBoostStatus;
    private FloatingActionButton fabMusicPlayer;

    // Services
    private VolumeBoosterService boosterService;
    private MusicService musicService;
    private boolean boosterBound = false;
    private boolean musicBound = false;

    private VolumeManager volumeManager;
    private AudioManager audioManager;
    private boolean isBoosterOn = false;
    private int currentBoostLevel = 100;
    private String currentSoundMode = "Normal";

    private final List<SoundMode> soundModes = new ArrayList<>();

    private final ServiceConnection boosterConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VolumeBoosterService.LocalBinder binder = (VolumeBoosterService.LocalBinder) service;
            boosterService = binder.getService();
            boosterBound = true;
            syncWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boosterBound = false;
        }
    };

    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicBound = true;
            updateMusicUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private final BroadcastReceiver musicUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMusicUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        volumeManager = new VolumeManager(this);

        initViews();
        initSoundModes();
        setupSoundModeButtons();
        setupVolumeSlider();
        setupBoosterSwitch();
        setupMusicControls();
        requestPermissions();
        startAndBindServices();
    }

    private void initViews() {
        volumeSlider     = findViewById(R.id.sliderVolume);
        tvVolumePercent  = findViewById(R.id.tvVolumePercent);
        tvBoostLabel     = findViewById(R.id.tvBoostLabel);
        switchBooster    = findViewById(R.id.switchBooster);
        ivWaveform       = findViewById(R.id.ivWaveform);
        llSoundModes     = findViewById(R.id.llSoundModes);
        cardCurrentMode  = findViewById(R.id.cardCurrentMode);
        tvCurrentMode    = findViewById(R.id.tvCurrentMode);
        btnPrev          = findViewById(R.id.btnPrev);
        btnPlayPause     = findViewById(R.id.btnPlayPause);
        btnNext          = findViewById(R.id.btnNext);
        tvSongTitle      = findViewById(R.id.tvSongTitle);
        tvArtist         = findViewById(R.id.tvArtist);
        ivAlbumArt       = findViewById(R.id.ivAlbumArt);
        tvBoostStatus    = findViewById(R.id.tvBoostStatus);
        fabMusicPlayer   = findViewById(R.id.fabMusicPlayer);
    }

    private void initSoundModes() {
        soundModes.add(new SoundMode("Normal",   100, R.drawable.ic_mode_normal));
        soundModes.add(new SoundMode("Music",    140, R.drawable.ic_mode_music));
        soundModes.add(new SoundMode("Movie",    160, R.drawable.ic_mode_movie));
        soundModes.add(new SoundMode("Game",     150, R.drawable.ic_mode_game));
        soundModes.add(new SoundMode("Voice",    130, R.drawable.ic_mode_voice));
        soundModes.add(new SoundMode("Outdoor",  180, R.drawable.ic_mode_outdoor));
        soundModes.add(new SoundMode("Bass",     170, R.drawable.ic_mode_bass));
        soundModes.add(new SoundMode("Max",      200, R.drawable.ic_mode_max));
    }

    private void setupSoundModeButtons() {
        llSoundModes.removeAllViews();
        for (SoundMode mode : soundModes) {
            View modeView = getLayoutInflater().inflate(R.layout.item_sound_mode, llSoundModes, false);
            ImageView icon    = modeView.findViewById(R.id.ivModeIcon);
            TextView label    = modeView.findViewById(R.id.tvModeLabel);
            CardView modeCard = modeView.findViewById(R.id.cardMode);

            icon.setImageResource(mode.getIconRes());
            label.setText(mode.getName());

            boolean isSelected = mode.getName().equals(currentSoundMode);
            modeCard.setCardBackgroundColor(isSelected
                    ? ContextCompat.getColor(this, R.color.accent_orange)
                    : ContextCompat.getColor(this, R.color.card_bg));

            modeCard.setOnClickListener(v -> selectSoundMode(mode));
            llSoundModes.addView(modeView);
        }
    }

    private void selectSoundMode(SoundMode mode) {
        currentSoundMode = mode.getName();
        currentBoostLevel = mode.getBoostLevel();

        volumeSlider.setValue(currentBoostLevel);
        tvVolumePercent.setText(currentBoostLevel + "%");
        tvCurrentMode.setText("Mode: " + mode.getName());

        applyBoost(currentBoostLevel);
        setupSoundModeButtons(); // refresh highlighting

        Toast.makeText(this, mode.getName() + " mode activated", Toast.LENGTH_SHORT).show();
    }

    private void setupVolumeSlider() {
        volumeSlider.setValueFrom(100);
        volumeSlider.setValueTo(200);
        volumeSlider.setValue(100);
        tvVolumePercent.setText("100%");

        volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int boost = (int) value;
            currentBoostLevel = boost;
            tvVolumePercent.setText(boost + "%");
            if (fromUser) {
                applyBoost(boost);
                // find closest mode name
                updateModeLabelForCustom(boost);
            }
        });
    }

    private void updateModeLabelForCustom(int boost) {
        String closest = "Custom";
        int minDiff = Integer.MAX_VALUE;
        for (SoundMode m : soundModes) {
            int diff = Math.abs(m.getBoostLevel() - boost);
            if (diff < minDiff) { minDiff = diff; closest = m.getName(); }
        }
        tvCurrentMode.setText("Mode: " + (minDiff == 0 ? closest : "Custom"));
    }

    private void setupBoosterSwitch() {
        switchBooster.setOnCheckedChangeListener((btn, isChecked) -> {
            isBoosterOn = isChecked;
            if (isChecked) {
                applyBoost(currentBoostLevel);
                tvBoostStatus.setText("Booster ON");
                tvBoostStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_orange));
                ivWaveform.startAnimation(AnimationUtils.loadAnimation(this, R.anim.wave_pulse));
            } else {
                resetBoost();
                tvBoostStatus.setText("Booster OFF");
                tvBoostStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                ivWaveform.clearAnimation();
            }
        });
    }

    private void setupMusicControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (musicBound) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    musicService.play();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (musicBound) { musicService.next(); updateMusicUI(); }
        });

        btnPrev.setOnClickListener(v -> {
            if (musicBound) { musicService.previous(); updateMusicUI(); }
        });

        fabMusicPlayer.setOnClickListener(v ->
                startActivity(new Intent(this, MusicPlayerActivity.class)));
    }

    private void applyBoost(int level) {
        if (!isBoosterOn) return;
        volumeManager.setBoostLevel(level);
        if (boosterBound) boosterService.setBoostLevel(level);
    }

    private void resetBoost() {
        volumeManager.resetBoost();
        if (boosterBound) boosterService.disableBoost();
    }

    private void syncWithService() {
        if (boosterBound) {
            isBoosterOn = boosterService.isBoosterEnabled();
            currentBoostLevel = boosterService.getCurrentBoostLevel();
            switchBooster.setChecked(isBoosterOn);
            volumeSlider.setValue(Math.max(100, Math.min(200, currentBoostLevel)));
            tvVolumePercent.setText(currentBoostLevel + "%");
        }
    }

    private void updateMusicUI() {
        if (!musicBound || musicService == null) return;
        String title = musicService.getCurrentSongTitle();
        String artist = musicService.getCurrentArtist();
        boolean playing = musicService.isPlaying();

        tvSongTitle.setText(title != null ? title : "No song playing");
        tvArtist.setText(artist != null ? artist : "Unknown artist");
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void startAndBindServices() {
        try {
            Intent boosterIntent = new Intent(this, VolumeBoosterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(boosterIntent);
            } else {
                startService(boosterIntent);
            }
            bindService(boosterIntent, boosterConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) { e.printStackTrace(); }

        try {
            Intent musicIntent = new Intent(this, MusicService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(musicIntent);
            } else {
                startService(musicIntent);
            }
            bindService(musicIntent, musicConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void requestPermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.volumebooster.MUSIC_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(musicUpdateReceiver, filter);
        }
        updateMusicUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(musicUpdateReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (boosterBound) { unbindService(boosterConnection); boosterBound = false; }
        if (musicBound)   { unbindService(musicConnection);   musicBound   = false; }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (musicBound) musicService.loadSongs();
        }
    }
}
