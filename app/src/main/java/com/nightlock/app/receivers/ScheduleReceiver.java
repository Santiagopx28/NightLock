package com.nightlock.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import com.nightlock.app.service.NightLockVpnService;

public class ScheduleReceiver extends BroadcastReceiver {

    private static final String TAG = "NightLockSchedule";

    public static final String ACTION_BLOCK_START = "com.nightlock.ACTION_BLOCK_START";
    public static final String ACTION_BLOCK_STOP  = "com.nightlock.ACTION_BLOCK_STOP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        switch (intent.getAction()) {
            case ACTION_BLOCK_START:
                Log.i(TAG, "Schedule trigger: START block (11:00 PM)");
                startVpnBlocking(context);
                break;

            case ACTION_BLOCK_STOP:
                Log.i(TAG, "Schedule trigger: STOP block (5:00 AM)");
                stopVpnBlocking(context);
                break;
        }
    }

    private void startVpnBlocking(Context context) {
        // Check VPN permission first — if not granted, the MainActivity handles it
        Intent prepare = VpnService.prepare(context);
        if (prepare != null) {
            // VPN not yet authorized — can't start from background without user
            Log.w(TAG, "VPN not prepared yet. User must open app once first.");
            return;
        }

        Intent vpnIntent = new Intent(context, NightLockVpnService.class);
        vpnIntent.setAction(NightLockVpnService.ACTION_START);
        context.startForegroundService(vpnIntent);
    }

    private void stopVpnBlocking(Context context) {
        Intent vpnIntent = new Intent(context, NightLockVpnService.class);
        vpnIntent.setAction(NightLockVpnService.ACTION_STOP);
        context.startForegroundService(vpnIntent);
    }
}
