package com.example.scheduleapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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

public class Menu extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    protected FirebaseUser currentUser;
    protected String currentUserName;
    protected String currentUserNumber;

    protected DatabaseReference inviteRef;
    protected ValueEventListener inviteListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

    protected void updateNavHeader() {
        if (navigationView != null && currentUserName != null) {
            TextView tvTitle = navigationView.getHeaderView(0).findViewById(R.id.tvNavHeaderSubtitle);
            tvTitle.setText("Welcome, "+currentUserName);
        }
    }

    protected String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^\\d]", "");
    }

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

    protected void handleInvites(DataSnapshot snapshot) {
        // Можно переопределить в наследниках, например в HomePage
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inviteRef != null && inviteListener != null) {
            inviteRef.removeEventListener(inviteListener);
            inviteRef = null;
            inviteListener = null;
        }
    }
}