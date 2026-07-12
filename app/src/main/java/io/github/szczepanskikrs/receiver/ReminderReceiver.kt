package io.github.szczepanskikrs.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.szczepanskikrs.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(NotificationHelper.PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(NotificationHelper.KEY_REMINDERS_ENABLED, false)
            if (enabled) {
                val hour = prefs.getInt(NotificationHelper.KEY_REMINDER_HOUR, 8)
                val minute = prefs.getInt(NotificationHelper.KEY_REMINDER_MINUTE, 0)
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        } else {
            NotificationHelper.showReminderNotification(context)
        }
    }
}
