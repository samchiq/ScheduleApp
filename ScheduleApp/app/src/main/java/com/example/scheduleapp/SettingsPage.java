package com.example.scheduleapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    private MaterialButton btnEditName, btnEditPhone, btnEditEmail, btnEditPassword, btnDeleteAccount;

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

        btnEditName = findViewById(R.id.btnEditName);
        btnEditPhone = findViewById(R.id.btnEditPhone);
        btnEditEmail = findViewById(R.id.btnEditEmail);
        btnEditPassword = findViewById(R.id.btnEditPassword);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

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

        btnEditName.setOnClickListener(v -> showEditNameDialog());
        btnEditPhone.setOnClickListener(v -> showEditPhoneDialog());
        btnEditEmail.setOnClickListener(v -> showReAuthDialog("email"));
        btnEditPassword.setOnClickListener(v -> showReAuthDialog("password"));
        btnDeleteAccount.setOnClickListener(v -> showReAuthDialog("delete"));
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new name");
        input.setText(currentUserName);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Name")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateName(String newName) {
        if (currentUser == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        userRef.child("name").setValue(newName).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
                loadUserProfile(); // Refresh data in Menu
            } else {
                Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditPhoneDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new phone number");
        input.setText(currentUserNumber);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Phone Number")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPhone = input.getText().toString().trim();
                    if (newPhone.matches("\\+?\\d{9,15}")) {
                        updatePhone(newPhone);
                    } else {
                        Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePhone(String newPhone) {
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        String oldPhoneNormalized = normalizePhone(currentUserNumber);
        String newPhoneNormalized = normalizePhone(newPhone);

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        
        db.child("users").child(uid).child("number").setValue(newPhone).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update mapping
                if (!oldPhoneNormalized.isEmpty()) {
                    db.child("phone_to_uid").child(oldPhoneNormalized).removeValue();
                }
                if (!newPhoneNormalized.isEmpty()) {
                    db.child("phone_to_uid").child(newPhoneNormalized).setValue(uid);
                }
                Toast.makeText(this, "Phone number updated", Toast.LENGTH_SHORT).show();
                loadUserProfile();
            } else {
                Toast.makeText(this, "Failed to update phone", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReAuthDialog(String action) {
        EditText input = new EditText(this);
        input.setHint("Enter current password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Re-authentication Required")
                .setMessage("Please enter your current password to continue.")
                .setView(container)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!password.isEmpty()) {
                        reAuthenticateAndProceed(password, action);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reAuthenticateAndProceed(String password, String action) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                switch (action) {
                    case "email":
                        showEditEmailDialog(password);
                        break;
                    case "password":
                        showEditPasswordDialog(password);
                        break;
                    case "delete":
                        showDeleteConfirmDialog(password);
                        break;
                }
            } else {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditEmailDialog(String currentPassword) {
        EditText input = new EditText(this);
        input.setHint("Enter new email");
        input.setText(currentUser.getEmail());

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Email")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newEmail = input.getText().toString().trim();
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                        updateEmail(newEmail);
                    } else {
                        Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmail(String newEmail) {
        currentUser.updateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
                userRef.child("email").setValue(newEmail);
                
                prefs.edit().putString("user_email", newEmail).apply();
                Toast.makeText(this, "Email updated", Toast.LENGTH_SHORT).show();
                loadUserProfile();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Failed to update email: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditPasswordDialog(String currentPassword) {
        EditText input = new EditText(this);
        input.setHint("Enter new password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        FrameLayout container = new FrameLayout(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Password")
                .setView(container)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPassword = input.getText().toString();
                    if (newPassword.length() >= 6) {
                        updatePassword(newPassword);
                    } else {
                        Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String newPassword) {
        currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
                userRef.child("password").setValue(newPassword);
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmDialog(String currentPassword) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        String uid = currentUser.getUid();
        String phoneNormalized = normalizePhone(currentUserNumber);
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // 1. Delete mapping
        if (!phoneNormalized.isEmpty()) {
            db.child("phone_to_uid").child(phoneNormalized).removeValue();
        }

        // 2. Delete user data
        db.child("users").child(uid).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 3. Delete auth account
                currentUser.delete().addOnCompleteListener(authDeleteTask -> {
                    if (authDeleteTask.isSuccessful()) {
                        prefs.edit().clear().apply();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete account from Auth", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
