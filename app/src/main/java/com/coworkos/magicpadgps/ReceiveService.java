package com.coworkos.magicpadgps;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiveService extends Service {
    private static final String MOCK_PROVIDER = LocationManager.GPS_PROVIDER;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private LocationManager locationManager;
    private Socket socket;
    private volatile boolean running;
    private boolean injectMock;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationTools.ensureChannel(this);
        locationManager = getSystemService(LocationManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && AppActions.ACTION_STOP_RECEIVE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String host = intent != null ? intent.getStringExtra(AppActions.EXTRA_HOST) : null;
        injectMock = intent != null && intent.getBooleanExtra(AppActions.EXTRA_INJECT_MOCK, false);
        if (host == null || host.trim().isEmpty()) {
            StatusBus.send(this, "Phone IP address is missing.");
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(
                201,
                NotificationTools.build(
                        this,
                        "Receiving GPS",
                        "Connected location will feed Google Maps when mock mode is enabled.",
                        AppActions.ACTION_STOP_RECEIVE
                )
        );

        if (!running) {
            running = true;
            if (injectMock && !prepareMockProvider()) {
                stopSelf();
                return START_NOT_STICKY;
            }
            connect(host.trim());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
        if (injectMock) {
            try {
                locationManager.removeTestProvider(MOCK_PROVIDER);
            } catch (Exception ignored) {
            }
        }
        ioExecutor.shutdownNow();
        StatusBus.send(this, "GPS receiving stopped.");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connect(String host) {
        ioExecutor.execute(() -> {
            StatusBus.send(this, "Connecting to " + host + ":" + AppActions.PORT + "...");
            try {
                socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(host, AppActions.PORT), 10000);
                StatusBus.send(this, "TCP connected. Opening stream...");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                StatusBus.send(this, "Connected. Waiting for GPS fixes.");
                int fixCount = 0;
                String line;
                while (running && (line = reader.readLine()) != null) {
                    fixCount++;
                    GpsPacket packet;
                    try {
                        packet = GpsPacket.parse(line);
                    } catch (Exception parseEx) {
                        StatusBus.send(this, "Parse error on fix #" + fixCount + ": " + parseEx.getClass().getSimpleName() + ": " + parseEx.getMessage());
                        continue;
                    }
                    if (injectMock) {
                        inject(packet);
                    }
                    StatusBus.send(
                            this,
                            "Fix #" + fixCount + ": " + fmt(packet.latitude) + ", " + fmt(packet.longitude)
                                    + " acc " + Math.round(packet.accuracyM) + "m"
                                    + (injectMock ? " → Maps." : ".")
                    );
                }
                if (running) {
                    StatusBus.send(this, "Phone closed the connection after " + fixCount + " fixes.");
                }
            } catch (java.net.ConnectException ex) {
                if (running) {
                    StatusBus.send(this, "Could not connect: " + ex.getMessage() + " — is the phone app sharing?");
                }
            } catch (java.net.SocketTimeoutException ex) {
                if (running) {
                    StatusBus.send(this, "Connection timed out. Check phone IP and that both devices are on the same Wi-Fi.");
                }
            } catch (Exception ex) {
                if (running) {
                    StatusBus.send(this, "Receive failed (" + ex.getClass().getSimpleName() + "): " + ex.getMessage());
                }
            } finally {
                try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                stopSelf();
            }
        });
    }

    private boolean prepareMockProvider() {
        try {
            try {
                locationManager.removeTestProvider(MOCK_PROVIDER);
            } catch (Exception ignored) {
            }
            try {
                locationManager.addTestProvider(
                        MOCK_PROVIDER,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        Criteria.POWER_LOW,
                        Criteria.ACCURACY_FINE
                );
            } catch (IllegalArgumentException providerAlreadyExists) {
                // Some Android builds keep the GPS provider present and still allow
                // the selected mock app to push test locations to it.
            }
            try {
                locationManager.setTestProviderEnabled(MOCK_PROVIDER, true);
            } catch (IllegalArgumentException providerAlreadyExists) {
                // Continue; setTestProviderLocation is the important call.
            }
            return true;
        } catch (SecurityException ex) {
            StatusBus.send(
                    this,
                    "Mock location is not enabled. In Developer Options, select MagicPad GPS as the mock location app."
            );
            return false;
        } catch (Exception ex) {
            StatusBus.send(this, "Could not prepare mock location: " + ex.getMessage());
            return false;
        }
    }

    private void inject(GpsPacket packet) {
        Location location = packet.toAndroidLocation(MOCK_PROVIDER);
        try {
            locationManager.setTestProviderLocation(MOCK_PROVIDER, location);
        } catch (SecurityException ex) {
            StatusBus.send(
                    this,
                    "Android rejected mock GPS. Select MagicPad GPS as the mock location app."
            );
            stopSelf();
        } catch (Exception ex) {
            StatusBus.send(this, "Could not send fix to Google Maps: " + ex.getMessage());
        }
    }

    private String fmt(double value) {
        return String.format(java.util.Locale.US, "%.6f", value);
    }
}
