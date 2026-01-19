package com.example.scheduleapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsPage extends Menu {

    private SwitchCompat switchNotifications;
    private SharedPreferences prefs;

    @Override
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
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            }
        });
    }
}