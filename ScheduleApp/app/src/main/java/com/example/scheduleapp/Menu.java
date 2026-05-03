package com.example.scheduleapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Base activity class that provides a navigation drawer and common menu functionality.
 * Handles user profile loading and subscription to event invitations.
 */
public class Menu extends AppCompatActivity {

    /** Layout for the navigation drawer. */
    protected DrawerLayout drawerLayout;
    /** View for the navigation menu items. */
    protected NavigationView navigationView;
    /** Toolbar used for the action bar. */
    protected Toolbar toolbar;

    /** Currently authenticated Firebase user. */
    protected FirebaseUser currentUser;
    /** Display name of the current user. */
    protected String currentUserName;
    /** Phone number of the current user used for invitations. */
    protected String currentUserNumber;

    /** Reference to the user's invitations in Firebase. */
    protected DatabaseReference inviteRef;
    /** Listener for changes in the user's invitations. */
    protected ValueEventListener inviteListener;

    @Override
    /**
     * Standard activity lifecycle method for initialization.
     */
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        
        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();
    }

    /**
     * Checks and requests notification permission for Android 13+.
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("Menu", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                android.util.Log.d("Menu", "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    /**
     * Applies the saved theme preference (Light/Dark mode) globally.
     */
    private void applyTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isDarkMode ?
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES :
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    /**
     * Configures the toolbar, navigation drawer, and listener for menu item selections.
     * Also initializes the current user and triggers profile loading.
     */
    protected void setupMenu() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (!(this instanceof HomePage)) {
                    startActivity(new Intent(this, HomePage.class));
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_category) {
                if (!(this instanceof CategoryPage)) {
                    startActivity(new Intent(this, CategoryPage.class));
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_settings) {
                if (!(this instanceof SettingsPage)) {
                    startActivity(new Intent(this, SettingsPage.class));
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            drawerLayout.closeDrawers();
            return false;
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserProfile();
        }
    }

    /**
     * Fetches the current user's profile information from Firebase.
     * Updates the navigation header and subscribes to invitations once data is retrieved.
     */
    protected void loadUserProfile() {
        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                if (user != null) {
                    currentUserNumber = user.getNumber();
                    currentUserName = user.getName();
                }

                updateNavHeader();

                if (currentUserNumber != null && !currentUserNumber.isEmpty()) {
                    android.util.Log.d("NotificationDebug", "Subscribing to invites for: " + currentUserNumber);
                    subscribeToInvites(normalizePhone(currentUserNumber));
                } else {
                    android.util.Log.e("NotificationDebug", "currentUserNumber is null or empty for UID: " + uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("NotificationDebug", "loadUserProfile cancelled: " + error.getMessage());
            }
        });
    }

    /**
     * Updates the text in the navigation drawer header with a welcome message for the user.
     */
    protected void updateNavHeader() {
        if (navigationView != null && currentUserName != null) {
            TextView tvTitle = navigationView.getHeaderView(0).findViewById(R.id.tvNavHeaderSubtitle);
            tvTitle.setText("Welcome, "+currentUserName);
        }
    }

    /**
     * Removes all non-numeric characters from a phone number string.
     */
    protected String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^\\d]", "");
    }

    /**
     * Sets up a Firebase listener for event invitations directed at the user's phone number.
     */
    protected void subscribeToInvites(String normalizedNumber) {
        if (normalizedNumber == null || normalizedNumber.isEmpty()) {
            return;
        }

        DatabaseReference newInviteRef = FirebaseDatabase.getInstance().getReference("invites").child(normalizedNumber);
        
        // Use toString() to compare paths if getPath() is restricted
        if (inviteRef != null && inviteRef.toString().equals(newInviteRef.toString())) {
            android.util.Log.d("NotificationDebug", "Already subscribed to: " + normalizedNumber);
            return;
        }

        if (inviteRef != null && inviteListener != null) {
            try { inviteRef.removeEventListener(inviteListener); } catch (Exception ignored) {}
        }

        inviteRef = newInviteRef;

        inviteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleInvites(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("NotificationDebug", "inviteListener cancelled: " + error.getMessage());
            }
        };

        inviteRef.addValueEventListener(inviteListener);
        android.util.Log.d("NotificationDebug", "Subscribed successfully to: " + normalizedNumber);
    }

    /**
     * Hook for subclasses to handle incoming invitation data changes.
     */
    protected void handleInvites(DataSnapshot snapshot) {
        android.util.Log.d("NotificationDebug", "handleInvites triggered with " + snapshot.getChildrenCount() + " children");
        for (DataSnapshot inviteSnap : snapshot.getChildren()) {
            java.util.Map<String, Object> invite = (java.util.Map<String, Object>) inviteSnap.getValue();
            if (invite != null) {
                Object statusObj = invite.get("status");
                String status = statusObj != null ? statusObj.toString() : "";
                android.util.Log.d("NotificationDebug", "Invite status: " + status);
                if ("pending".equals(status)) {
                    String title = invite.get("title") != null ? invite.get("title").toString() : "Event";
                    String fromName = invite.get("fromName") != null ? invite.get("fromName").toString() : "Someone";
                    showInviteNotification(inviteSnap.getKey(), title, fromName);
                }
            }
        }
    }

    /**
     * Displays a system notification for an event invitation.
     */
    private void showInviteNotification(String inviteId, String title, String fromName) {
        if (inviteId == null) return;
        android.util.Log.d("NotificationDebug", "Showing notification for: " + title);

        Intent intent = new Intent(this, HomePage.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Invitation")
                .setContentText(fromName + " invited you to: " + title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        try {
            manager.notify(inviteId.hashCode(), builder.build());
            android.util.Log.d("NotificationDebug", "manager.notify called successfully");
        } catch (SecurityException e) {
            android.util.Log.e("NotificationDebug", "SecurityException in notify", e);
        } catch (Exception e) {
            android.util.Log.e("NotificationDebug", "Error in notify", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadUserProfile();
        }
    }

    @Override
    /**
     * Cleans up Firebase listeners when the activity is paused to prevent memory leaks.
     */
    protected void onPause() {
        super.onPause();
        if (inviteRef != null && inviteListener != null) {
            inviteRef.removeEventListener(inviteListener);
            inviteRef = null;
            inviteListener = null;
        }
    }
}
