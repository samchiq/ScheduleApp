package com.example.scheduleapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Helper class for managing application notifications and scheduling alarms for events.
 * Provides methods for creating notification channels and handling event-specific alerts.
 */
public class NotificationHelper {

    /** Identifier for the application's event notification channel. */
    public static final String CHANNEL_ID = "schedule_events_channel";
    /** User-visible name for the event notification channel. */
    public static final String CHANNEL_NAME = "Event Notifications";

    /**
     * Creates a notification channel for Android Oreo and above.
     * Configures the channel with high importance for timely event alerts.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled events");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Schedules a broadcast alarm to trigger a notification at a specific event time.
     * Uses AlarmManager to ensure the alert is delivered even if the device is idle.
     */
    public static void scheduleEventNotification(Context context, String eventId,
                                                 String title, long eventTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, EventNotificationReceiver.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("title", title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        eventTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        eventTime,
                        pendingIntent
                );
            }
        }
    }

    /**
     * Cancels a previously scheduled notification alarm for a specific event.
     */
    public static void cancelEventNotification(Context context, String eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, EventNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
