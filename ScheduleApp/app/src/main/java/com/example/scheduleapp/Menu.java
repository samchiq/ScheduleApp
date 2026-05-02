package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
    private void loadUserProfile() {
        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserNumber = snapshot.child("number").getValue(String.class);
                currentUserName = snapshot.child("name").getValue(String.class);

                updateNavHeader();

                if (currentUserNumber != null && !currentUserNumber.isEmpty()) {
                    subscribeToInvites(normalizePhone(currentUserNumber));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
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
        if (normalizedNumber == null || normalizedNumber.isEmpty()) return;

        if (inviteRef != null && inviteListener != null) {
            try { inviteRef.removeEventListener(inviteListener); } catch (Exception ignored) {}
        }

        inviteRef = FirebaseDatabase.getInstance().getReference("invites").child(normalizedNumber);

        inviteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleInvites(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        inviteRef.addValueEventListener(inviteListener);
    }

    /**
     * Hook for subclasses to handle incoming invitation data changes.
     */
    protected void handleInvites(DataSnapshot snapshot) {
  
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
