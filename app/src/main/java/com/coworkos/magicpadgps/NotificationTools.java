package com.coworkos.magicpadgps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

final class NotificationTools {
    private NotificationTools() {
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                AppActions.CHANNEL_ID,
                "GPS sharing status",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows when MagicPad GPS is sharing or receiving location.");
        manager.createNotificationChannel(channel);
    }

    static Notification build(Context context, String title, String text, String stopAction) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                1,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(context, stopAction);
        stopIntent.setClass(context, StopReceiver.class);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, AppActions.CHANNEL_ID)
                : new Notification.Builder(context);

        return builder
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build();
    }
}
