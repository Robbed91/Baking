package com.robbiebedford.bakebook.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.robbiebedford.bakebook.MainActivity

data class BakeTimerDefinition(
    val id: String,
    val title: String,
    val notificationId: Int,
    val requestCode: Int
)

object BakeBookTimerScheduler {
    const val CHANNEL_ID = "bakebook_alarm_timers"
    const val RUNNING_CHANNEL_ID = "bakebook_running_timers"
    private const val PREFS = "bakebook_alarm_timers"
    private const val EXTRA_TIMER_ID = "timer_id"
    private const val EXTRA_TIMER_TITLE = "timer_title"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"

    val bakeTimer = BakeTimerDefinition("bake", "Bake Countdown", 4101, 5101)
    val coolingTimer = BakeTimerDefinition("cooling", "Cooling Clock", 4102, 5102)

    fun schedule(context: Context, timer: BakeTimerDefinition, durationMillis: Long): Long {
        val triggerAt = System.currentTimeMillis() + durationMillis
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(endKey(timer), triggerAt)
            .putLong(durationKey(timer), durationMillis)
            .apply()

        val pendingIntent = alarmPendingIntent(context, timer)
        val alarmInfoIntent = PendingIntent.getActivity(
            context,
            timer.requestCode + 100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, alarmInfoIntent), pendingIntent)
        showRunningNotification(context, timer, triggerAt)
        return triggerAt
    }

    fun cancel(context: Context, timer: BakeTimerDefinition) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(endKey(timer))
            .remove(durationKey(timer))
            .apply()
        val pendingIntent = existingAlarmPendingIntent(context, timer)
        if (pendingIntent != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }
        context.getSystemService(NotificationManager::class.java).cancel(runningNotificationId(timer))
    }

    fun endAt(context: Context, timer: BakeTimerDefinition): Long {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(endKey(timer), 0L)
        return if (stored > System.currentTimeMillis()) stored else 0L
    }

    fun duration(context: Context, timer: BakeTimerDefinition): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(durationKey(timer), 0L)
    }

    fun clearFinished(context: Context, timerId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("${timerId}_end_at")
            .remove("${timerId}_duration")
            .apply()
    }

    fun createTimerChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, "BakeBook alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Baking and cooling timer alarms"
                enableVibration(true)
                setSound(alarmSound, attributes)
            }
            val runningChannel = NotificationChannel(RUNNING_CHANNEL_ID, "Running BakeBook timers", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Persistent countdowns while baking timers are running"
                setSound(null, null)
                enableVibration(false)
            }
            context.getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(channel)
                createNotificationChannel(runningChannel)
            }
        }
    }

    private fun showRunningNotification(context: Context, timer: BakeTimerDefinition, triggerAt: Long) {
        createTimerChannel(context)
        val openAppIntent = PendingIntent.getActivity(
            context,
            timer.requestCode + 200,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${timer.title} running")
            .setContentText("BakeBook will alarm when this timer finishes.")
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(triggerAt)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOnlyAlertOnce(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(runningNotificationId(timer), notification)
    }

    private fun alarmPendingIntent(context: Context, timer: BakeTimerDefinition): PendingIntent {
        val intent = Intent(context, BakeBookTimerReceiver::class.java).apply {
            putExtra(EXTRA_TIMER_ID, timer.id)
            putExtra(EXTRA_TIMER_TITLE, timer.title)
            putExtra(EXTRA_NOTIFICATION_ID, timer.notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            timer.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun existingAlarmPendingIntent(context: Context, timer: BakeTimerDefinition): PendingIntent? {
        val intent = Intent(context, BakeBookTimerReceiver::class.java).apply {
            putExtra(EXTRA_TIMER_ID, timer.id)
            putExtra(EXTRA_TIMER_TITLE, timer.title)
            putExtra(EXTRA_NOTIFICATION_ID, timer.notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            timer.requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun endKey(timer: BakeTimerDefinition) = "${timer.id}_end_at"
    private fun durationKey(timer: BakeTimerDefinition) = "${timer.id}_duration"
    private fun runningNotificationId(timer: BakeTimerDefinition) = timer.notificationId + 1000

    fun timerId(intent: Intent): String = intent.getStringExtra(EXTRA_TIMER_ID).orEmpty()
    fun timerTitle(intent: Intent): String = intent.getStringExtra(EXTRA_TIMER_TITLE) ?: "BakeBook timer"
    fun notificationId(intent: Intent): Int = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 4100)
    fun runningNotificationId(intent: Intent): Int = notificationId(intent) + 1000
}

class BakeBookTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BakeBookTimerScheduler.createTimerChannel(context)
        BakeBookTimerScheduler.clearFinished(context, BakeBookTimerScheduler.timerId(intent))
        context.getSystemService(NotificationManager::class.java)
            .cancel(BakeBookTimerScheduler.runningNotificationId(intent))

        val title = BakeBookTimerScheduler.timerTitle(intent)
        val openAppIntent = PendingIntent.getActivity(
            context,
            BakeBookTimerScheduler.notificationId(intent) + 100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notification = NotificationCompat.Builder(context, BakeBookTimerScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("BakeBook timer finished")
            .setContentText("$title is done.")
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 700, 300, 700, 300, 1000))
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(BakeBookTimerScheduler.notificationId(intent), notification)
    }
}
