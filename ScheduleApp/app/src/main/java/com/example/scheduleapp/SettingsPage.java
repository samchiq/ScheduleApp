package com.example.scheduleapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Manages the application settings and user preferences.
 * Currently handles notification toggling and persists state in SharedPreferences.
 */
public class SettingsPage extends Menu {

    /** Switch component for enabling or disabling notifications. */
    private SwitchCompat switchNotifications;
    /** Switch component for toggling dark mode. */
    private SwitchCompat switchDarkMode;
    /** SharedPreferences instance for storing setting values. */
    private SharedPreferences prefs;

    @Override
    /**
     * Initializes the activity and sets up the preferences interface.
     * Configures the notification and theme switches based on stored user settings.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_page);

        setupMenu();

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only respond to user-initiated changes
                prefs.edit().putBoolean("dark_mode", isChecked).apply();
                int targetMode = isChecked ?
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
            }
        });
    }
}
