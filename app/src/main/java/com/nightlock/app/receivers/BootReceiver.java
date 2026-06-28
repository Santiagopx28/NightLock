package com.nightlock.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nightlock.app.ui.MainActivity;
import com.nightlock.app.util.ScheduleManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "NightLockBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i(TAG, "Device booted — re-scheduling NightLock alarms");
            ScheduleManager.scheduleDaily(context);
        }
    }
}
