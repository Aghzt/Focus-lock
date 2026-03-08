package com.focuslock

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.focuslock.databinding.ActivityLockBinding

class LockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
    }

    private lateinit var binding: ActivityLockBinding
    private var countDownTimer: CountDownTimer? = null
    private var totalSeconds: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tampilkan di atas lock screen, nyalakan layar
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        totalSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 3600)

        // Simpan waktu selesai ke SharedPreferences
        val endTime = System.currentTimeMillis() + (totalSeconds * 1000L)
        getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).edit()
            .putLong("session_end_time", endTime)
            .apply()

        // Aktifkan Screen Pinning (kiosk mode) agar user tidak bisa keluar
        startLockTask()

        startCountdown(totalSeconds)
        setupEmergencyButton()
    }

    private fun startCountdown(seconds: Int) {
        updateTimerDisplay(seconds)

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                updateTimerDisplay(secondsLeft)
                updateProgressBar(secondsLeft)
            }

            override fun onFinish() {
                onSessionComplete()
            }
        }.start()
    }

    private fun updateTimerDisplay(secondsLeft: Int) {
        val hours = secondsLeft / 3600
        val minutes = (secondsLeft % 3600) / 60
        val seconds = secondsLeft % 60

        binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        // Update label motivasi berdasarkan sisa waktu
        binding.tvMotivation.text = when {
            secondsLeft > totalSeconds * 0.75 -> "🎯 Fokus! Kamu baru mulai."
            secondsLeft > totalSeconds * 0.50 -> "💪 Bagus! Sudah setengah jalan."
            secondsLeft > totalSeconds * 0.25 -> "🔥 Hampir sampai! Jangan menyerah."
            else -> "⚡ Sedikit lagi! Kamu bisa!"
        }
    }

    private fun updateProgressBar(secondsLeft: Int) {
        val progress = ((totalSeconds - secondsLeft).toFloat() / totalSeconds * 100).toInt()
        binding.progressBar.progress = progress
    }

    private fun setupEmergencyButton() {
        // Tombol darurat yang butuh konfirmasi 5 detik tahan
        var pressStartTime = 0L

        binding.btnEmergency.setOnLongClickListener {
            pressStartTime = System.currentTimeMillis()
            true
        }

        binding.btnEmergency.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    pressStartTime = System.currentTimeMillis()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val holdDuration = System.currentTimeMillis() - pressStartTime
                    if (holdDuration >= 5000) {
                        // Tahan 5 detik = darurat, bisa keluar
                        showEmergencyExit()
                    } else {
                        binding.tvEmergencyHint.text =
                            "Tahan ${5 - (holdDuration / 1000).toInt()} detik lagi untuk keluar darurat"
                    }
                }
            }
            false
        }
    }

    private fun showEmergencyExit() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Keluar Darurat")
            .setMessage("Apakah kamu benar-benar perlu keluar? Sesi fokusmu akan berakhir.")
            .setPositiveButton("Ya, keluar darurat") { _, _ ->
                endSession()
            }
            .setNegativeButton("Tidak, lanjutkan") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun onSessionComplete() {
        // Matikan DND
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (e: SecurityException) { /* ignore */ }

        // Hentikan screen pinning
        stopLockTask()

        // Hapus data sesi
        getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).edit()
            .remove("session_end_time")
            .apply()

        // Tampilkan layar selesai
        binding.tvTimer.text = "✅ SELESAI!"
        binding.tvMotivation.text = "Luar biasa! Kamu berhasil fokus penuh."
        binding.progressBar.progress = 100
        binding.btnEmergency.isEnabled = false

        // Kembali ke MainActivity setelah 3 detik
        binding.root.postDelayed({
            stopFocusService()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }, 3000)
    }

    private fun endSession() {
        countDownTimer?.cancel()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } catch (e: SecurityException) { /* ignore */ }

        stopLockTask()

        getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).edit()
            .remove("session_end_time")
            .apply()

        stopFocusService()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun stopFocusService() {
        stopService(Intent(this, FocusService::class.java))
    }

    // Blokir tombol Back dan Recent
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true // Abaikan semua tombol navigasi
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {
        // Blokir tombol back
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // Cegah minimize saat terjadi perubahan konfigurasi
    override fun onPause() {
        super.onPause()
        // Jika sesi masih berjalan, paksa balik ke LockActivity
        val prefs = getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("session_end_time", 0L)
        if (endTime > System.currentTimeMillis()) {
            val intent = Intent(this, LockActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }
}
