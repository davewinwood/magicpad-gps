package com.coworkos.magicpadgps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (AppActions.ACTION_STOP_SHARE.equals(intent.getAction())) {
            context.stopService(new Intent(context, ShareService.class));
        } else if (AppActions.ACTION_STOP_RECEIVE.equals(intent.getAction())) {
            context.stopService(new Intent(context, ReceiveService.class));
        }
    }
}
