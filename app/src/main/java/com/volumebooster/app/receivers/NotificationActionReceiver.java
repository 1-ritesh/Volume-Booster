package com.volumebooster.app.receivers;

import android.content.*;
import com.volumebooster.app.services.MusicService;
import com.volumebooster.app.services.VolumeBoosterService;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Intent serviceIntent;
        switch (action) {
            case VolumeBoosterService.ACTION_VOLUME_UP:
            case VolumeBoosterService.ACTION_VOLUME_DOWN:
            case VolumeBoosterService.ACTION_TOGGLE_BOOST:
                serviceIntent = new Intent(context, VolumeBoosterService.class);
                serviceIntent.setAction(action);
                context.startService(serviceIntent);
                break;
            case MusicService.ACTION_PLAY:
            case MusicService.ACTION_PAUSE:
            case MusicService.ACTION_NEXT:
            case MusicService.ACTION_PREV:
                serviceIntent = new Intent(context, MusicService.class);
                serviceIntent.setAction(action);
                context.startService(serviceIntent);
                break;
        }
    }
}
