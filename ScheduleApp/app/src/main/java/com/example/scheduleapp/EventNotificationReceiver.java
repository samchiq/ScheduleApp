package com.example.scheduleapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * BroadcastReceiver that handles displaying notifications for scheduled events.
 * Triggered by AlarmManager to alert the user when an event is starting.
 */
public class EventNotificationReceiver extends BroadcastReceiver {

    @Override
    /**
     * Receives the broadcast and triggers a system notification for the event.
     * Extracts event details from the intent and configures the notification builder.
     */
    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra("eventId");
        String title = intent.getStringExtra("title");

        if (eventId == null || title == null) {
            return;
        }

        Intent activityIntent = new Intent(context, HomePage.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                eventId.hashCode(),
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Event Starting!")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        try {
            manager.notify(eventId.hashCode(), builder.build());
        } catch (SecurityException e) {
        } catch (Exception e) {
        }
    }
}
