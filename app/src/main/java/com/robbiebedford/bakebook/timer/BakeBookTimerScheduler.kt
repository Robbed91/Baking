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
    val type: String,
    val title: String,
    val notificationId: Int,
    val requestCode: Int
)

object BakeBookTimerScheduler {
    const val CHANNEL_ID = "bakebook_alarm_timers"
    const val RUNNING_CHANNEL_ID = "bakebook_running_timers"
    private const val PREFS = "bakebook_alarm_timers"
    private const val EXTRA_TIMER_ID = "timer_id"
    private const val EXTRA_TIMER_TYPE = "timer_type"
    private const val EXTRA_TIMER_TITLE = "timer_title"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun newTimer(type: String, title: String): BakeTimerDefinition {
        val id = "${type}_${System.currentTimeMillis()}"
        return timerDefinition(id, type, title)
    }

    fun savedTimers(context: Context, type: String): List<BakeTimerDefinition> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(timersKey(type), emptySet()).orEmpty()
            .sorted()
            .map { id ->
                timerDefinition(
                    id = id,
                    type = type,
                    title = prefs.getString(titleKey(id), null) ?: defaultTitle(type)
                )
            }
            .filter { endAt(context, it) > 0L }
    }

    fun schedule(context: Context, timer: BakeTimerDefinition, durationMillis: Long): Long {
        val triggerAt = System.currentTimeMillis() + durationMillis
        registerTimer(context, timer)
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
        unregisterTimer(context, timer)
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
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val type = prefs.getString(typeKey(timerId), null) ?: inferType(timerId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("${timerId}_end_at")
            .remove("${timerId}_duration")
            .remove(titleKey(timerId))
            .remove(typeKey(timerId))
            .apply()
        removeTimerId(context, type, timerId)
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
            putExtra(EXTRA_TIMER_TYPE, timer.type)
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
            putExtra(EXTRA_TIMER_TYPE, timer.type)
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
    private fun titleKey(id: String) = "${id}_title"
    private fun typeKey(id: String) = "${id}_type"
    private fun timersKey(type: String) = "${type}_timer_ids"
    private fun runningNotificationId(timer: BakeTimerDefinition) = timer.notificationId + 1000

    fun timerId(intent: Intent): String = intent.getStringExtra(EXTRA_TIMER_ID).orEmpty()
    fun timerType(intent: Intent): String = intent.getStringExtra(EXTRA_TIMER_TYPE).orEmpty()
    fun timerTitle(intent: Intent): String = intent.getStringExtra(EXTRA_TIMER_TITLE) ?: "BakeBook timer"
    fun notificationId(intent: Intent): Int = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 4100)
    fun runningNotificationId(intent: Intent): Int = notificationId(intent) + 1000

    private fun timerDefinition(id: String, type: String, title: String): BakeTimerDefinition {
        val hash = id.hashCode() and Int.MAX_VALUE
        return BakeTimerDefinition(
            id = id,
            type = type,
            title = title,
            notificationId = 4100 + (hash % 100_000),
            requestCode = 5100 + (hash % 100_000)
        )
    }

    private fun registerTimer(context: Context, timer: BakeTimerDefinition) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(timersKey(timer.type), emptySet()).orEmpty().toMutableSet()
        ids.add(timer.id)
        prefs.edit()
            .putStringSet(timersKey(timer.type), ids)
            .putString(titleKey(timer.id), timer.title)
            .putString(typeKey(timer.id), timer.type)
            .apply()
    }

    private fun unregisterTimer(context: Context, timer: BakeTimerDefinition) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(titleKey(timer.id))
            .remove(typeKey(timer.id))
            .apply()
        removeTimerId(context, timer.type, timer.id)
    }

    private fun removeTimerId(context: Context, type: String, timerId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(timersKey(type), emptySet()).orEmpty().toMutableSet()
        ids.remove(timerId)
        prefs.edit().putStringSet(timersKey(type), ids).apply()
    }

    private fun defaultTitle(type: String): String = if (type == "cooling") "Cooling Clock" else "Bake Countdown"

    private fun inferType(timerId: String): String = when {
        timerId.startsWith("cooling") -> "cooling"
        else -> "bake"
    }
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
