package com.coworkos.magicpadgps;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareService extends Service implements LocationListener {
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final List<BufferedWriter> clients = new CopyOnWriteArrayList<>();
    private LocationManager locationManager;
    private ServerSocket serverSocket;
    private volatile boolean running;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationTools.ensureChannel(this);
        locationManager = getSystemService(LocationManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && AppActions.ACTION_STOP_SHARE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(
                101,
                NotificationTools.build(
                        this,
                        "Sharing GPS",
                        "Waiting for the MagicPad on port " + AppActions.PORT,
                        AppActions.ACTION_STOP_SHARE
                )
        );

        if (!running) {
            running = true;
            startServer();
            startLocationUpdates();
        }
        StatusBus.send(this, "Sharing GPS. MagicPad can connect on port " + AppActions.PORT + ".");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (SecurityException ignored) {
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        for (BufferedWriter client : clients) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
        clients.clear();
        ioExecutor.shutdownNow();
        StatusBus.send(this, "GPS sharing stopped.");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServer() {
        ioExecutor.execute(() -> {
            try {
                serverSocket = new ServerSocket(AppActions.PORT);
                while (running) {
                    Socket socket = serverSocket.accept();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    clients.add(writer);
                    StatusBus.send(this, "MagicPad connected. Streaming GPS.");
                }
            } catch (Exception ex) {
                if (running) {
                    StatusBus.send(this, "Sharing failed: " + ex.getMessage());
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            StatusBus.send(this, "Location permission is needed before sharing can start.");
            stopSelf();
            return;
        }

        boolean requestedAnyProvider = false;
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    this
            );
            requestedAnyProvider = true;
        } catch (Exception ex) {
            StatusBus.send(this, "GPS provider is not available yet: " + ex.getMessage());
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    0f,
                    this
            );
            requestedAnyProvider = true;
        } catch (Exception ex) {
            StatusBus.send(this, "Network location provider is not available yet: " + ex.getMessage());
        }

        if (!requestedAnyProvider) {
            StatusBus.send(this, "Could not start location updates. Check Android Location settings.");
            stopSelf();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            String payload = GpsPacket.fromLocation(location) + "\n";
            int delivered = 0;
            for (BufferedWriter client : clients) {
                try {
                    client.write(payload);
                    client.flush();
                    delivered++;
                } catch (Exception ex) {
                    clients.remove(client);
                    try {
                        client.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            StatusBus.send(
                    this,
                    "Phone fix: " + fmt(location.getLatitude()) + ", " + fmt(location.getLongitude())
                            + " accuracy " + Math.round(location.hasAccuracy() ? location.getAccuracy() : 0)
                            + "m. Receivers: " + delivered + "."
            );
        } catch (JSONException ex) {
            StatusBus.send(this, "Could not encode GPS fix: " + ex.getMessage());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        StatusBus.send(this, provider + " location provider is disabled.");
    }

    private String fmt(double value) {
        return String.format(java.util.Locale.US, "%.6f", value);
    }
}
