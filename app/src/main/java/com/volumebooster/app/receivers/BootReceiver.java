package com.volumebooster.app.receivers;

import android.content.*;
import com.volumebooster.app.services.VolumeBoosterService;
import com.volumebooster.app.services.MusicService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startService(new Intent(context, VolumeBoosterService.class));
        }
    }
}
