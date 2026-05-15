package com.coworkos.magicpadgps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 41;

    private TextView statusText;
    private TextView shareInfoText;
    private EditText hostInput;
    private CheckBox injectMockCheck;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(AppActions.EXTRA_MESSAGE);
            if (message != null) {
                statusText.setText(message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationTools.ensureChannel(this);
        setContentView(buildContent());
        updateShareInfo();
        requestCorePermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AppActions.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(statusReceiver);
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("MagicPad GPS");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF12312A);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Use the phone as the GPS source and the MagicPad as the receiver. First prototype uses manual IP connection so Google Maps can be tested quickly.");
        intro.setTextSize(16);
        intro.setTextColor(0xFF34423F);
        intro.setPadding(0, dp(8), 0, dp(18));
        root.addView(intro);

        statusText = sectionText("Idle");
        statusText.setTextSize(16);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(statusText);

        addDivider(root);
        addShareSection(root);
        addDivider(root);
        addReceiveSection(root);
        addDivider(root);
        addSetupSection(root);

        return scrollView;
    }

    private void addShareSection(LinearLayout root) {
        root.addView(sectionTitle("Phone: Share GPS"));

        shareInfoText = sectionText("");
        root.addView(shareInfoText);

        Button start = primaryButton("Start sharing from this device");
        start.setOnClickListener(v -> {
            requestCorePermissions();
            Intent intent = new Intent(this, ShareService.class);
            startForegroundServiceCompat(intent);
        });
        root.addView(start);

        Button stop = secondaryButton("Stop sharing");
        stop.setOnClickListener(v -> stopService(new Intent(this, ShareService.class)));
        root.addView(stop);
    }

    private void addReceiveSection(LinearLayout root) {
        root.addView(sectionTitle("MagicPad: Receive GPS"));

        TextView help = sectionText("Enter the phone IP shown above on the phone, then connect. Turn on mock location when you want Google Maps to use the shared GPS.");
        root.addView(help);

        hostInput = new EditText(this);
        hostInput.setHint("Phone IP address, e.g. 192.168.1.42");
        hostInput.setSingleLine(true);
        hostInput.setInputType(InputType.TYPE_CLASS_TEXT);
        hostInput.setPadding(0, dp(10), 0, dp(10));
        root.addView(hostInput);

        injectMockCheck = new CheckBox(this);
        injectMockCheck.setText("Use as tablet location for Google Maps");
        injectMockCheck.setTextSize(16);
        injectMockCheck.setPadding(0, dp(6), 0, dp(6));
        root.addView(injectMockCheck);

        Button connect = primaryButton("Connect to phone");
        connect.setOnClickListener(v -> {
            String host = hostInput.getText().toString().trim();
            if (host.isEmpty()) {
                statusText.setText("Enter the phone IP address first.");
                return;
            }
            requestCorePermissions();
            Intent intent = new Intent(this, ReceiveService.class);
            intent.putExtra(AppActions.EXTRA_HOST, host);
            intent.putExtra(AppActions.EXTRA_INJECT_MOCK, injectMockCheck.isChecked());
            startForegroundServiceCompat(intent);
        });
        root.addView(connect);

        Button stop = secondaryButton("Stop receiving");
        stop.setOnClickListener(v -> stopService(new Intent(this, ReceiveService.class)));
        root.addView(stop);
    }

    private void addSetupSection(LinearLayout root) {
        root.addView(sectionTitle("Google Maps setup"));

        TextView steps = sectionText(
                "On the MagicPad, enable Developer Options and choose MagicPad GPS as the mock location app. Then start Receive GPS and open Google Maps."
        );
        root.addView(steps);

        Button devOptions = secondaryButton("Open Developer Options");
        devOptions.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception ex) {
                statusText.setText("Could not open Developer Options from this device.");
            }
        });
        root.addView(devOptions);
    }

    private void updateShareInfo() {
        List<String> addresses = localIpAddresses();
        if (addresses.isEmpty()) {
            shareInfoText.setText("Connect to Wi-Fi, then start sharing. Port: " + AppActions.PORT);
        } else {
            shareInfoText.setText("Phone IP: " + addresses.get(0) + "\nPort: " + AppActions.PORT);
        }
    }

    private void requestCorePermissions() {
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST);
        }
    }

    private void startForegroundServiceCompat(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private List<String> localIpAddresses() {
        try {
            ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
            // Iterate only Wi-Fi networks so we never surface a cellular (10.x) address.
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    continue;
                }
                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                if (linkProperties == null) {
                    continue;
                }
                List<String> addresses = new ArrayList<>();
                for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                    java.net.InetAddress addr = linkAddress.getAddress();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        addresses.add(addr.getHostAddress());
                    }
                }
                if (!addresses.isEmpty()) {
                    return addresses;
                }
            }
            return Collections.emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(0xFF12312A);
        view.setPadding(0, dp(4), 0, dp(8));
        return view;
    }

    private TextView sectionText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(0xFF34423F);
        view.setLineSpacing(2, 1.05f);
        view.setPadding(0, dp(4), 0, dp(8));
        return view;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = primaryButton(text);
        button.setTextColor(0xFF0B8063);
        return button;
    }

    private void addDivider(LinearLayout root) {
        TextView spacer = new TextView(this);
        spacer.setText("");
        spacer.setHeight(dp(18));
        root.addView(spacer);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
