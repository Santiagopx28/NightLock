package com.nightlock.app.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NightLockAdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "NightLockAdmin";

    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.i(TAG, "Device Admin enabled — uninstall protection active");
        Toast.makeText(context, "🔒 Protección activada", Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Message shown when user tries to revoke admin
        return "⚠️ Si desactivas NightLock, perderás la protección nocturna. " +
               "¿Estás seguro de que quieres quitarla?";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.w(TAG, "Device Admin disabled — protection removed");
        Toast.makeText(context, "Protección desactivada", Toast.LENGTH_SHORT).show();
    }
}
