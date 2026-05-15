package com.coworkos.magicpadgps;

final class AppActions {
    static final int PORT = 42523;
    static final String CHANNEL_ID = "magicpad_gps_status";
    static final String ACTION_STATUS = "com.coworkos.magicpadgps.STATUS";
    static final String ACTION_STOP_SHARE = "com.coworkos.magicpadgps.STOP_SHARE";
    static final String ACTION_STOP_RECEIVE = "com.coworkos.magicpadgps.STOP_RECEIVE";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_HOST = "host";
    static final String EXTRA_INJECT_MOCK = "inject_mock";

    private AppActions() {
    }
}
