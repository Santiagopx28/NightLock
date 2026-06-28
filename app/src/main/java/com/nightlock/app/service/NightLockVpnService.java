package com.nightlock.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.nightlock.app.R;
import com.nightlock.app.ui.MainActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class NightLockVpnService extends VpnService {

    private static final String TAG = "NightLockVPN";
    private static final String CHANNEL_ID = "nightlock_channel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "com.nightlock.VPN_START";
    public static final String ACTION_STOP  = "com.nightlock.VPN_STOP";

    private ParcelFileDescriptor vpnInterface;
    private Thread packetThread;
    private volatile boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startVpn();
        } else if (ACTION_STOP.equals(action)) {
            stopVpn();
            stopSelf();
        }
        return START_STICKY;
    }

    private void startVpn() {
        if (running) return;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        try {
            Builder builder = new Builder();
            builder.setSession("NightLock")
                    .addAddress("10.99.99.1", 32)
                    .addRoute("0.0.0.0", 0)           // capture ALL traffic
                    .addDnsServer("127.0.0.1")
                    .setBlocking(true)
                    .setMtu(1500);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface");
                return;
            }

            running = true;
            packetThread = new Thread(this::drainPackets, "NightLockPacketDrain");
            packetThread.start();

            Log.i(TAG, "VPN started — WiFi traffic blocked");

        } catch (Exception e) {
            Log.e(TAG, "Error starting VPN: " + e.getMessage());
        }
    }

    /**
     * Reads packets from the VPN tunnel and simply discards them.
     * This effectively blocks all network traffic routed through WiFi.
     */
    private void drainPackets() {
        ByteBuffer buffer = ByteBuffer.allocate(32767);
        java.io.FileInputStream in = new java.io.FileInputStream(vpnInterface.getFileDescriptor());

        while (running) {
            try {
                buffer.clear();
                int length = in.read(buffer.array());
                if (length > 0) {
                    // Packet received — just discard it (block)
                    // No forwarding = no internet access
                }
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Packet read error: " + e.getMessage());
                }
                break;
            }
        }
    }

    private void stopVpn() {
        running = false;
        if (packetThread != null) {
            packetThread.interrupt();
        }
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing VPN: " + e.getMessage());
        }
        stopForeground(true);
        Log.i(TAG, "VPN stopped — WiFi unblocked");
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "NightLock activo",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("WiFi bloqueado por NightLock");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔒 NightLock activo")
                .setContentText("WiFi bloqueado hasta las 5:00 AM")
                .setSmallIcon(R.drawable.ic_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // ─── Static helpers ───────────────────────────────────────────────────────

    public static boolean isRunning = false;
}
