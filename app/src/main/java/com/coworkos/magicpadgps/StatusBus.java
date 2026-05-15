package com.coworkos.magicpadgps;

import android.content.Context;
import android.content.Intent;

final class StatusBus {
    private StatusBus() {
    }

    static void send(Context context, String message) {
        Intent intent = new Intent(AppActions.ACTION_STATUS);
        intent.setPackage(context.getPackageName());
        intent.putExtra(AppActions.EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }
}
