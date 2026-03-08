package com.focuslock

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.focuslock.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        setupTimerPicker()
        setupStartButton()
    }

    private fun setupTimerPicker() {
        // NumberPicker untuk jam (0-5)
        binding.pickerHours.apply {
            minValue = 0
            maxValue = 5
            value = 0
            displayedValues = arrayOf("0j", "1j", "2j", "3j", "4j", "5j")
        }

        // NumberPicker untuk menit (0-55, step 5)
        binding.pickerMinutes.apply {
            minValue = 0
            maxValue = 11
            value = 2
            displayedValues = arrayOf("0m", "5m", "10m", "15m", "20m", "25m", "30m", "35m", "40m", "45m", "50m", "55m")
        }
    }

    private fun setupStartButton() {
        binding.btnStart.setOnClickListener {
            val hours = binding.pickerHours.value
            val minuteIndex = binding.pickerMinutes.value
            val minutes = minuteIndex * 5

            val totalSeconds = (hours * 3600) + (minutes * 60)

            if (totalSeconds == 0) {
                Toast.makeText(this, "Pilih durasi minimal 5 menit!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cek izin Do Not Disturb
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                requestDndPermission()
                return@setOnClickListener
            }

            startFocusSession(totalSeconds)
        }
    }

    private fun requestDndPermission() {
        Toast.makeText(
            this,
            "Izinkan akses Do Not Disturb untuk membisukan notifikasi",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun startFocusSession(totalSeconds: Int) {
        // Aktifkan Do Not Disturb (bisukan semua notifikasi)
        try {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_NONE
            )
        } catch (e: SecurityException) {
            Toast.makeText(this, "Gagal mengaktifkan mode senyap", Toast.LENGTH_SHORT).show()
        }

        // Jalankan ForegroundService agar timer tetap berjalan
        val serviceIntent = Intent(this, FocusService::class.java).apply {
            putExtra(FocusService.EXTRA_DURATION_SECONDS, totalSeconds)
        }
        startForegroundService(serviceIntent)

        // Buka LockActivity
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_DURATION_SECONDS, totalSeconds)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(lockIntent)
    }

    override fun onResume() {
        super.onResume()
        // Jika sesi sedang berjalan, kembali ke LockActivity
        val prefs = getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("session_end_time", 0L)
        if (endTime > System.currentTimeMillis()) {
            val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
            val lockIntent = Intent(this, LockActivity::class.java).apply {
                putExtra(LockActivity.EXTRA_DURATION_SECONDS, remaining)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(lockIntent)
        }
    }
}
