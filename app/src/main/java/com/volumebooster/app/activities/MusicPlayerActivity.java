package com.volumebooster.app.activities;

import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.slider.Slider;
import com.volumebooster.app.R;
import com.volumebooster.app.adapters.SongAdapter;
import com.volumebooster.app.models.Song;
import com.volumebooster.app.services.MusicService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private ImageButton btnBack, btnPlayPause, btnNext, btnPrev;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private Slider seekBar;
    private RecyclerView rvPlaylist;
    private SongAdapter songAdapter;
    private ImageView ivAlbumArt;

    private MusicService musicService;
    private boolean isBound = false;

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            loadPlaylist();
            updateUI();
            startProgressUpdater();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initViews();
        setupControls();
        bindService(new Intent(this, MusicService.class), connection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        btnBack      = findViewById(R.id.btnBack);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext      = findViewById(R.id.btnNext);
        btnPrev      = findViewById(R.id.btnPrev);
        tvTitle      = findViewById(R.id.tvSongTitle);
        tvArtist     = findViewById(R.id.tvArtist);
        tvCurrentTime= findViewById(R.id.tvCurrentTime);
        tvTotalTime  = findViewById(R.id.tvTotalTime);
        seekBar      = findViewById(R.id.seekBar);
        rvPlaylist   = findViewById(R.id.rvPlaylist);
        ivAlbumArt   = findViewById(R.id.ivAlbumArt);

        rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (!isBound) return;
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.play();
            }
            updateUI();
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) { musicService.next(); updateUI(); }
        });

        btnPrev.setOnClickListener(v -> {
            if (isBound) { musicService.previous(); updateUI(); }
        });

        seekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && isBound) {
                musicService.seekTo((int) value);
            }
        });
    }

    private void loadPlaylist() {
        if (!isBound) return;
        List<Song> songs = musicService.getSongList();
        songAdapter = new SongAdapter(songs, position -> {
            musicService.playSongAt(position);
            updateUI();
        });
        rvPlaylist.setAdapter(songAdapter);
    }

    private void updateUI() {
        if (!isBound || musicService == null) return;
        String title  = musicService.getCurrentSongTitle();
        String artist = musicService.getCurrentArtist();
        boolean playing = musicService.isPlaying();
        int duration = musicService.getDuration();

        tvTitle.setText(title != null ? title : "No song");
        tvArtist.setText(artist != null ? artist : "Unknown");
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);

        if (duration > 0) {
            seekBar.setValueTo(duration);
            tvTotalTime.setText(formatTime(duration));
        }
    }

    private void startProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null && musicService.isPlaying()) {
                    int pos = musicService.getCurrentPosition();
                    int dur = musicService.getDuration();
                    if (dur > 0) {
                        seekBar.setValueTo(dur);
                        seekBar.setValue(Math.min(pos, dur));
                        tvCurrentTime.setText(formatTime(pos));
                    }
                }
                progressHandler.postDelayed(this, 500);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private String formatTime(int ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.volumebooster.MUSIC_UPDATE");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
        if (isBound) { unbindService(connection); isBound = false; }
    }
}
