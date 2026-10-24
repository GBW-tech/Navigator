package com.nav.navigator.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.nav.navigator.MainActivity;
import com.nav.navigator.core.Protocol;

public class StreamingService extends Service {
    private static final String CHANNEL_ID = "stream_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        Protocol.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.klustrkast.nav.ENABLE".equals(intent.getAction())) {
            createNotificationChannel();
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Navigator")
                    .setContentText("Connected to TVS RTX 300 Bike")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                    .build();
            startForeground(1, notification);
        } else if (intent != null && "com.klustrkast.nav.DISABLE".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bike Connection Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
