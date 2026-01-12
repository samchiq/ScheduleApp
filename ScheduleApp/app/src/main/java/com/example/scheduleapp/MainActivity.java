package com.example.scheduleapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText etMail, etPassword;
    Button btnLogin, btnRegister;

    Dialog d;
    Button btnSave;
    EditText etRegName, etRegNumber, etRegMail, etRegPassword;

    FirebaseAuth myAuth;
    DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверка авторизации
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        if (isLoggedIn) {
            startActivity(new Intent(this, HomePage.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        myAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        etMail = findViewById(R.id.etmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == btnLogin) {
            login();
        }

        if (view == btnRegister) {
            d = new Dialog(this);
            d.setContentView(R.layout.register);
            d.setTitle("Register");
            d.setCancelable(true);

            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            etRegName = d.findViewById(R.id.etRegName);
            etRegNumber = d.findViewById(R.id.etRegNumber);
            etRegMail = d.findViewById(R.id.etRegMail);
            etRegPassword = d.findViewById(R.id.etRegPassword);
            btnSave = d.findViewById(R.id.btnSave);

            btnSave.setOnClickListener(this);
            d.show();
        }

        if (view == btnSave) {
            register();
        }
    }

    private void login() {
        String email = etMail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        myAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences.Editor editor = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit();
                        editor.putBoolean("is_logged_in", true);
                        editor.putString("user_email", email);
                        editor.apply();

                        Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, HomePage.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void register() {
        String name = etRegName.getText().toString().trim();
        String phone = etRegNumber.getText().toString().trim();
        String email = etRegMail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegMail.setError("Invalid email");
            return;
        }

        if (!phone.matches("\\+?\\d{9,15}")) {
            etRegNumber.setError("Invalid phone number");
            return;
        }

        myAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = myAuth.getCurrentUser().getUid();

                        // Исправленный порядок параметров
                        Users newUser = new Users(email, phone, name, password);

                        usersRef.child(userId).setValue(newUser)
                                .addOnCompleteListener(saveTask -> {
                                    if (saveTask.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                        if (d != null && d.isShowing()) d.dismiss();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Failed to save user data: " + saveTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(MainActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
