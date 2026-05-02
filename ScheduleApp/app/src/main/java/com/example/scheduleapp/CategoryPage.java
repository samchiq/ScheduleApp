package com.example.scheduleapp;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
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
    /** Listener for category changes in Firebase. */
    private ValueEventListener categoriesListener;
    /** TextView shown when there are no categories to display. */
    private TextView tvEmptyState;

    /** Predefined colors for categories. */
    private final String[] categoryColors = {
            "#FF1E88E5", // Blue
            "#FF00897B", // Teal
            "#FF3949AB", // Indigo
            "#FFE53935", // Red
            "#FFFB8C00", // Orange
            "#FF8E24AA", // Purple
            "#FF7CB342", // Light Green
            "#FF00ACC1"  // Cyan
    };

    private String selectedColor = categoryColors[0];

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
        tvEmptyState = findViewById(R.id.tvEmptyState);

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
        LinearLayout layoutColorPicker = dialog.findViewById(R.id.layoutColorPicker);
        Button btnSave = dialog.findViewById(R.id.btnSaveCategory);

        if (categoryToEdit != null) {
            etCategoryName.setText(categoryToEdit.getName());
            selectedColor = categoryToEdit.getColor();
        } else {
            selectedColor = categoryColors[0];
        }

        setupColorPicker(layoutColorPicker);

        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                etCategoryName.setError("Enter category name");
                return;
            }

            String id = (categoryToEdit != null && categoryToEdit.getId() != null)
                    ? categoryToEdit.getId()
                    : categoriesRef.push().getKey();

            Category category = new Category(id, name, selectedColor);
            if (id != null) {
                categoriesRef.child(id).setValue(category)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CategoryPage.this,
                                    categoryToEdit != null ? "Category updated" : "Category added",
                                    Toast.LENGTH_SHORT).show();
                            
                            if (categoryToEdit != null && !categoryToEdit.getColor().equals(selectedColor)) {
                                updateEventsColor(categoryToEdit.getName(), selectedColor);
                            }

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
     * Dynamically creates color selection circles in the dialog.
     */
    private void setupColorPicker(LinearLayout layout) {
        layout.removeAllViews();
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (String colorHex : categoryColors) {
            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor(colorHex));
            
            if (colorHex.equals(selectedColor)) {
                shape.setStroke(6, Color.BLACK);
            }

            colorView.setBackground(shape);
            colorView.setOnClickListener(v -> {
                selectedColor = colorHex;
                setupColorPicker(layout);
            });

            layout.addView(colorView);
        }
    }

    /**
     * Updates the color of all events associated with a category when the category color changes.
     */
    private void updateEventsColor(String categoryName, String newColor) {
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("user_events").child(currentUser.getUid());
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Event event = ds.getValue(Event.class);
                    if (event != null && categoryName.equals(event.getDescription())) {
                        ds.getRef().child("color").setValue(newColor);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Loads categories from the Firebase Realtime Database.
     * Updates the category list and notifies the adapter when data changes.
     */
    private void loadCategories() {
        if (categoriesRef == null) return;
        
        if (categoriesListener != null) {
            categoriesRef.removeEventListener(categoriesListener);
        }

        categoriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Category category = ds.getValue(Category.class);
                    if (category != null) categoryList.add(category);
                }

                if (categoryList.isEmpty()) {
                    recyclerCategories.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerCategories.setVisibility(View.VISIBLE);
                    tvEmptyState.setVisibility(View.GONE);
                }

                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Don't show toast if it's a permission error due to logout
                if (error.getCode() != DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(CategoryPage.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                }
            }
        };

        categoriesRef.addValueEventListener(categoriesListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (categoriesRef != null && categoriesListener != null) {
            categoriesRef.removeEventListener(categoriesListener);
        }
    }
}
