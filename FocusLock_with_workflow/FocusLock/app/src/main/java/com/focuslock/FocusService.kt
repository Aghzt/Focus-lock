package com.focuslock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder

class FocusService : Service() {

    companion object {
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        private const val CHANNEL_ID = "focus_lock_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var countDownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationSeconds = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 3600) ?: 3600

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Sesi fokus aktif...", durationSeconds))

        startTimer(durationSeconds)

        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Int) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val hours = secondsLeft / 3600
                val minutes = (secondsLeft % 3600) / 60
                val seconds = secondsLeft % 60
                val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                updateNotification("⏳ Fokus: $timeStr tersisa", secondsLeft)
            }

            override fun onFinish() {
                updateNotification("✅ Sesi fokus selesai! Hebat!", 0)
                // Matikan DND
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                try {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                } catch (e: SecurityException) { /* ignore */ }

                stopSelf()
            }
        }.start()
    }

    private fun buildNotification(text: String, secondsLeft: Int): Notification {
        // Intent untuk membuka kembali LockActivity jika user geser notifikasi
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_DURATION_SECONDS, secondsLeft)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 FocusLock - Mode Fokus")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)         // Notifikasi tidak bisa digeser
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String, secondsLeft: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, secondsLeft))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Lock Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Menjaga sesi fokus tetap berjalan"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
