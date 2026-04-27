package com.example.scheduleapp;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;
import java.util.*;

/**
 * Manages the category management interface, allowing users to view, add, and edit categories.
 * Handles Firebase synchronization for the user's category data.
 */
public class CategoryPage extends Menu {

    /** RecyclerView to display the list of categories. */
    private RecyclerView recyclerCategories;
    /** Button for adding a new category. */
    private FloatingActionButton btnAddCategory;
    /** List of category objects fetched from the database. */
    private List<Category> categoryList;
    /** Adapter for the category RecyclerView. */
    private CategoryAdapter categoryAdapter;
    /** Reference to the current user's categories in Firebase. */
    private DatabaseReference categoriesRef;

    @Override
    /**
     * Initializes the activity and sets up the user interface components.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_page);
  
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

    /**
     * Displays a dialog for adding or editing a category.
     * Sets up the dialog UI and handles the save button click for both operations.
     */
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

    /**
     * Loads categories from the Firebase Realtime Database.
     * Updates the category list and notifies the adapter when data changes.
     */
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
