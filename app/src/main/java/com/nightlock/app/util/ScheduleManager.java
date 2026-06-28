package com.nightlock.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nightlock.app.receivers.ScheduleReceiver;

import java.util.Calendar;

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";

    // Block starts at 22:30 (10:30 PM)
    public static final int BLOCK_START_HOUR   = 22;
    public static final int BLOCK_START_MINUTE = 30;

    // Block ends at 05:00 (5:00 AM)
    public static final int BLOCK_STOP_HOUR   = 5;
    public static final int BLOCK_STOP_MINUTE = 0;

    private static final int REQUEST_START = 100;
    private static final int REQUEST_STOP  = 101;

    /**
     * Schedules daily alarms for 11pm (start) and 5am (stop).
     * Uses ALLOW_WHILE_IDLE to fire even in Doze mode.
     */
    public static void scheduleDaily(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        scheduleAlarm(context, am,
                BLOCK_START_HOUR, BLOCK_START_MINUTE,
                ScheduleReceiver.ACTION_BLOCK_START, REQUEST_START);

        scheduleAlarm(context, am,
                BLOCK_STOP_HOUR, BLOCK_STOP_MINUTE,
                ScheduleReceiver.ACTION_BLOCK_STOP, REQUEST_STOP);

        Log.i(TAG, "Daily schedule set: BLOCK 23:00 → 05:00");
    }

    public static void cancelSchedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        cancelAlarm(context, am, ScheduleReceiver.ACTION_BLOCK_START, REQUEST_START);
        cancelAlarm(context, am, ScheduleReceiver.ACTION_BLOCK_STOP,  REQUEST_STOP);

        Log.i(TAG, "Schedule cancelled");
    }

    private static void scheduleAlarm(Context context, AlarmManager am,
                                      int hour, int minute,
                                      String action, int requestCode) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.setAction(action);

        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Repeats every 24 hours (86_400_000 ms)
        am.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pi
        );

        Log.i(TAG, "Alarm set for " + hour + ":" + String.format("%02d", minute)
                + " action=" + action);
    }

    private static void cancelAlarm(Context context, AlarmManager am,
                                    String action, int requestCode) {
        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
    }

    /**
     * Returns true if current time is within the block window (11pm–5am).
     */
    public static boolean isInBlockWindow() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        // Block window: 22:30 to 04:59
        int minute = now.get(Calendar.MINUTE);
        if (hour == BLOCK_START_HOUR) return minute >= BLOCK_START_MINUTE;
        return hour > BLOCK_START_HOUR || hour < BLOCK_STOP_HOUR;
    }
}
