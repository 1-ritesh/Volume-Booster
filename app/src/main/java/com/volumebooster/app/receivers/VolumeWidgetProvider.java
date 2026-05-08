package com.volumebooster.app.receivers;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import com.volumebooster.app.R;
import com.volumebooster.app.activities.MainActivity;
import com.volumebooster.app.services.VolumeBoosterService;

public class VolumeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_volume);

            // Open app on click
            Intent openApp = new Intent(ctx, MainActivity.class);
            PendingIntent piOpen = PendingIntent.getActivity(ctx, 0, openApp,
                    PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetRoot, piOpen);

            // Volume down
            Intent down = new Intent(VolumeBoosterService.ACTION_VOLUME_DOWN);
            down.setClass(ctx, VolumeBoosterService.class);
            PendingIntent piDown = PendingIntent.getService(ctx, 20, down,
                    PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btnWidgetDown, piDown);

            // Volume up
            Intent up = new Intent(VolumeBoosterService.ACTION_VOLUME_UP);
            up.setClass(ctx, VolumeBoosterService.class);
            PendingIntent piUp = PendingIntent.getService(ctx, 21, up,
                    PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btnWidgetUp, piUp);

            // Toggle boost
            Intent toggle = new Intent(VolumeBoosterService.ACTION_TOGGLE_BOOST);
            toggle.setClass(ctx, VolumeBoosterService.class);
            PendingIntent piToggle = PendingIntent.getService(ctx, 22, toggle,
                    PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.btnWidgetToggle, piToggle);

            mgr.updateAppWidget(id, views);
        }
    }
}
