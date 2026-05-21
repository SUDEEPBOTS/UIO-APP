package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class TrainReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val trainNo = intent.getStringExtra("trainNo") ?: "Train Alert"
            val trainName = intent.getStringExtra("trainName") ?: "Running Status Update"
            val stationName = intent.getStringExtra("stationName") ?: "Upcoming Station"
            val delayMinutes = intent.getIntExtra("delayMinutes", 0)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "train_reminders_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Train Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies users about train arrivals, delays, and schedule alarms."
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val delayText = if (delayMinutes > 0) "is running late by $delayMinutes mins" else "is arriving On Time"
            val contentText = "$trainNo - $trainName $delayText at $stationName!"

            // Use standard system info drawable or launcher. Under Jetpack Compose templates,android.R.drawable.ic_dialog_info is safe.
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("uio train Reminder 🚉")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
