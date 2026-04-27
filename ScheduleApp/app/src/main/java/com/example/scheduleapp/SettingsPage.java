package com.example.scheduleapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Manages the application settings and user preferences.
 * Currently handles notification toggling and persists state in SharedPreferences.
 */
public class SettingsPage extends Menu {

    /** Switch component for enabling or disabling notifications. */
    private SwitchCompat switchNotifications;
    /** SharedPreferences instance for storing setting values. */
    private SharedPreferences prefs;

    @Override
    /**
     * Initializes the activity and sets up the preferences interface.
     * Configures the notification switch based on stored user settings.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_page);

        setupMenu();

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        switchNotifications = findViewById(R.id.switchNotifications);

        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            /**
             * Saves the notification preference when the switch state changes.
             */
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            }
        });
    }
}
