package com.volumebooster.app.services;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.*;
import android.os.*;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;
import com.volumebooster.app.R;
import com.volumebooster.app.activities.MainActivity;
import com.volumebooster.app.models.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    public static final String CHANNEL_ID     = "MusicServiceChannel";
    public static final String ACTION_PLAY    = "com.volumebooster.ACTION_PLAY";
    public static final String ACTION_PAUSE   = "com.volumebooster.ACTION_PAUSE";
    public static final String ACTION_NEXT    = "com.volumebooster.ACTION_NEXT";
    public static final String ACTION_PREV    = "com.volumebooster.ACTION_PREV";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPlaying = false;

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        loadSongs();
        initMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:  play();     break;
                case ACTION_PAUSE: pause();    break;
                case ACTION_NEXT:  next();     break;
                case ACTION_PREV:  previous(); break;
            }
        }
        return START_STICKY;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mediaPlayer.setOnCompletionListener(mp -> next());
    }

    public void loadSongs() {
        songList.clear();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null,
                MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Song song = new Song(
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    );
                    songList.add(song);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        broadcastUpdate();
    }

    public void play() {
        if (songList.isEmpty()) return;
        if (!isPlaying) {
            if (!mediaPlayer.isPlaying()) {
                try { mediaPlayer.start(); } catch (Exception ignored) {}
            }
            isPlaying = true;
            updateNotification();
            broadcastUpdate();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification();
            broadcastUpdate();
        }
    }

    public void next() {
        if (songList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % songList.size();
        playSongAt(currentIndex);
    }

    public void previous() {
        if (songList.isEmpty()) return;
        currentIndex = (currentIndex - 1 + songList.size()) % songList.size();
        playSongAt(currentIndex);
    }

    public void playSongAt(int index) {
        if (songList.isEmpty() || index < 0 || index >= songList.size()) return;
        currentIndex = index;
        Song song = songList.get(index);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            updateNotification();
            broadcastUpdate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null) mediaPlayer.seekTo(ms);
    }

    // Getters
    public boolean isPlaying()          { return isPlaying; }
    public List<Song> getSongList()     { return songList; }
    public int getCurrentPosition()     { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration()            { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }

    public String getCurrentSongTitle() {
        if (songList.isEmpty()) return null;
        return songList.get(currentIndex).getTitle();
    }

    public String getCurrentArtist() {
        if (songList.isEmpty()) return null;
        return songList.get(currentIndex).getArtist();
    }

    private void broadcastUpdate() {
        sendBroadcast(new Intent("com.volumebooster.MUSIC_UPDATE"));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void updateNotification() {
        String title  = getCurrentSongTitle();
        String artist = getCurrentArtist();

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent piOpen = PendingIntent.getActivity(this, 10, openApp, PendingIntent.FLAG_IMMUTABLE);

        Intent prevI = new Intent(ACTION_PREV); prevI.setClass(this, MusicService.class);
        Intent playI = new Intent(isPlaying ? ACTION_PAUSE : ACTION_PLAY); playI.setClass(this, MusicService.class);
        Intent nextI = new Intent(ACTION_NEXT); nextI.setClass(this, MusicService.class);

        PendingIntent piPrev = PendingIntent.getService(this, 11, prevI, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piPlay = PendingIntent.getService(this, 12, playI, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piNext = PendingIntent.getService(this, 13, nextI, PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title != null ? title : "Music Player")
                .setContentText(artist != null ? artist : "Unknown Artist")
                .setContentIntent(piOpen)
                .addAction(R.drawable.ic_skip_previous, "Prev", piPrev)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying ? "Pause" : "Play", piPlay)
                .addAction(R.drawable.ic_skip_next, "Next", piNext)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying)
                .setSilent(true)
                .build();

        try {
            startForeground(2, notif);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
    }
}
