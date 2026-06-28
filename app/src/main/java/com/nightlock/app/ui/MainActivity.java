package com.nightlock.app.ui;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nightlock.app.R;
import com.nightlock.app.receivers.NightLockAdminReceiver;
import com.nightlock.app.service.NightLockVpnService;
import com.nightlock.app.util.ScheduleManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_VPN   = 1;
    private static final int REQUEST_ADMIN = 2;

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    private TextView tvStatus;
    private TextView tvCountdown;
    private TextView tvScheduleInfo;
    private Button   btnToggle;
    private View     lockOverlay;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dpm            = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, NightLockAdminReceiver.class);

        tvStatus       = findViewById(R.id.tvStatus);
        tvCountdown    = findViewById(R.id.tvCountdown);
        tvScheduleInfo = findViewById(R.id.tvScheduleInfo);
        btnToggle      = findViewById(R.id.btnToggle);
        lockOverlay    = findViewById(R.id.lockOverlay);

        tvScheduleInfo.setText("Bloqueo automático: 10:30 PM → 5:00 AM · Todos los días");

        btnToggle.setOnClickListener(v -> handleToggle());

        // Request Device Admin if not already granted
        if (!dpm.isAdminActive(adminComponent)) {
            requestDeviceAdmin();
        }

        // Request VPN permission if needed
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, REQUEST_VPN);
        } else {
            // VPN ready — schedule daily alarms
            ScheduleManager.scheduleDaily(this);
            // If we're already in block window, start VPN immediately
            if (ScheduleManager.isInBlockWindow()) {
                startBlocking();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    // ─── UI Update ────────────────────────────────────────────────────────────

    private void updateUi() {
        boolean inWindow = ScheduleManager.isInBlockWindow();

        if (inWindow) {
            // LOCKED STATE — hide manual toggle
            tvStatus.setText("🔒 WiFi bloqueado");
            tvStatus.setTextColor(getColor(R.color.red_blocked));
            btnToggle.setVisibility(View.GONE);
            lockOverlay.setVisibility(View.VISIBLE);
            startCountdownToUnlock();
        } else {
            // OPEN STATE — show toggle
            tvStatus.setText("✅ WiFi disponible");
            tvStatus.setTextColor(getColor(R.color.green_active));
            btnToggle.setVisibility(View.VISIBLE);
            lockOverlay.setVisibility(View.GONE);
            tvCountdown.setText("Próximo bloqueo: 10:30 PM");
            if (countDownTimer != null) countDownTimer.cancel();
        }
    }

    private void startCountdownToUnlock() {
        Calendar unlock = Calendar.getInstance();
        int currentHour = unlock.get(Calendar.HOUR_OF_DAY);

        // If past midnight (0–4), unlock is today at 5am
        // If before midnight (23), unlock is tomorrow at 5am
        if (currentHour >= 23) {
            unlock.add(Calendar.DAY_OF_YEAR, 1);
        }
        unlock.set(Calendar.HOUR_OF_DAY, 5);
        unlock.set(Calendar.MINUTE, 0);
        unlock.set(Calendar.SECOND, 0);
        unlock.set(Calendar.MILLISECOND, 0);

        long millisLeft = unlock.getTimeInMillis() - System.currentTimeMillis();
        if (millisLeft < 0) millisLeft += 24 * 60 * 60 * 1000L;

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millisLeft, 1000) {
            @Override
            public void onTick(long ms) {
                long h = TimeUnit.MILLISECONDS.toHours(ms);
                long m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
                long s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
                tvCountdown.setText(String.format("Se desbloquea en %02d:%02d:%02d", h, m, s));
            }

            @Override
            public void onFinish() {
                updateUi();
            }
        }.start();
    }

    // ─── Toggle Logic ─────────────────────────────────────────────────────────

    private void handleToggle() {
        // Double-check: if somehow in block window, reject
        if (ScheduleManager.isInBlockWindow()) {
            Toast.makeText(this,
                    "🔒 No puedes desactivar NightLock en este horario",
                    Toast.LENGTH_LONG).show();
            updateUi();
            return;
        }

        // Outside block window — allow manual toggle for testing
        boolean vpnRunning = NightLockVpnService.isRunning;
        if (vpnRunning) {
            stopBlocking();
        } else {
            startBlocking();
        }
    }

    // ─── VPN Control ─────────────────────────────────────────────────────────

    private void startBlocking() {
        Intent i = new Intent(this, NightLockVpnService.class);
        i.setAction(NightLockVpnService.ACTION_START);
        startForegroundService(i);
        NightLockVpnService.isRunning = true;
        updateUi();
    }

    private void stopBlocking() {
        Intent i = new Intent(this, NightLockVpnService.class);
        i.setAction(NightLockVpnService.ACTION_STOP);
        startForegroundService(i);
        NightLockVpnService.isRunning = false;
        updateUi();
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    private void requestDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "NightLock necesita este permiso para evitar que la app sea desinstalada " +
                "durante el horario de bloqueo nocturno.");
        startActivityForResult(intent, REQUEST_ADMIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            ScheduleManager.scheduleDaily(this);
            if (ScheduleManager.isInBlockWindow()) {
                startBlocking();
            }
        }

        if (requestCode == REQUEST_ADMIN) {
            if (dpm.isAdminActive(adminComponent)) {
                Toast.makeText(this, "✅ Protección contra desinstalación activada",
                        Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Protección recomendada")
                        .setMessage("Sin este permiso, podrías desinstalar la app durante el bloqueo. " +
                                    "¿Deseas activar la protección?")
                        .setPositiveButton("Activar", (d, w) -> requestDeviceAdmin())
                        .setNegativeButton("Omitir", null)
                        .show();
            }
        }
    }
}
