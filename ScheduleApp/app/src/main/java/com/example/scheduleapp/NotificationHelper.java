package com.example.scheduleapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Helper class for managing application notifications and scheduling alarms for events.
 * Provides methods for creating notification channels and handling event-specific alerts.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

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
            Log.d(TAG, "Creating notification channel: " + CHANNEL_NAME);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for scheduled events");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created successfully");
            } else {
                Log.e(TAG, "Failed to get NotificationManager to create channel");
            }
        }
    }

    /**
     * Schedules a broadcast alarm to trigger a notification at a specific event time.
     * Uses AlarmManager to ensure the alert is delivered even if the device is idle.
     */
    public static void scheduleEventNotification(Context context, String eventId,
                                                 String title, long eventTime) {
        if (eventId == null || title == null) {
            Log.w(TAG, "Cannot schedule notification: missing ID or title");
            return;
        }

        if (eventTime < System.currentTimeMillis()) {
            Log.i(TAG, "Not scheduling notification for '" + title + "' (time is in the past)");
            return;
        }

        Log.d(TAG, "Scheduling notification for: " + title + " at " + eventTime + " (eventId=" + eventId + ")");

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "Scheduling exact alarm for API 31+");
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            eventTime,
                            pendingIntent
                    );
                } else {
                    Log.w(TAG, "Permission missing for exact alarms, falling back to inexact");
                    // Fallback to inexact alarm if permission is missing to avoid SecurityException
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            eventTime,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "Scheduling exact alarm for API 23-30");
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        eventTime,
                        pendingIntent
                );
            } else {
                Log.d(TAG, "Scheduling exact alarm for below API 23");
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        eventTime,
                        pendingIntent
                );
            }
        } else {
            Log.e(TAG, "AlarmManager is null, cannot schedule notification");
        }
    }

    /**
     * Cancels a previously scheduled notification alarm for a specific event.
     */
    public static void cancelEventNotification(Context context, String eventId) {
        if (eventId == null) return;
        Log.d(TAG, "Canceling notification for eventId: " + eventId);

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
            Log.d(TAG, "Notification alarm canceled");
        }
    }
}
