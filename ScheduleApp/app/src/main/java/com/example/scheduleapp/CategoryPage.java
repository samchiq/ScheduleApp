package com.example.scheduleapp;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.*;

public class CategoryPage extends Menu {

    private RecyclerView recyclerCategories;
    private FloatingActionButton btnAddCategory;
    private List<Category> categoryList;
    private CategoryAdapter categoryAdapter;
    private DatabaseReference categoriesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_page);

        // Настройка меню
        setupMenu();

        recyclerCategories = findViewById(R.id.recyclerCategories);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        categoriesRef = FirebaseDatabase.getInstance().getReference("user_categories").child(uid);

        categoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDeleteClick(Category category) {
                if (category.getId() != null) {
                    categoriesRef.child(category.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CategoryPage.this, "Category deleted", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CategoryPage.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        btnAddCategory.setOnClickListener(v -> showCategoryDialog(null));

        loadCategories();
    }

    @Override
    protected void setupMenu() {
        Toolbar toolbar = findViewById(R.id.toolbar_category);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout_category);
        NavigationView navigationView = findViewById(R.id.nav_view_category);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_category) {
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().clear().apply();
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    protected void updateNavHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view_category);
        if (navigationView != null && currentUserName != null) {
            TextView tvTitle = navigationView.getHeaderView(0).findViewById(R.id.tvNavHeaderSubtitle);
            tvTitle.setText("Welcome, " + currentUserName);
        }
    }

    private void showCategoryDialog(Category categoryToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_category);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etCategoryName = dialog.findViewById(R.id.etCategoryName);
        Button btnSave = dialog.findViewById(R.id.btnSaveCategory);

        if (categoryToEdit != null) {
            etCategoryName.setText(categoryToEdit.getName());
        }

        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                etCategoryName.setError("Enter category name");
                return;
            }

            String id = (categoryToEdit != null && categoryToEdit.getId() != null)
                    ? categoryToEdit.getId()
                    : categoriesRef.push().getKey();

            Category category = new Category(id, name);
            if (id != null) {
                categoriesRef.child(id).setValue(category)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CategoryPage.this,
                                    categoryToEdit != null ? "Category updated" : "Category added",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadCategories();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CategoryPage.this, "Failed to save", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }

    private void loadCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Category category = ds.getValue(Category.class);
                    if (category != null) categoryList.add(category);
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryPage.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }
}