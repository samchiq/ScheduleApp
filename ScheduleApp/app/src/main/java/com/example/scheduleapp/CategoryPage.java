package com.example.scheduleapp;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.util.*;

public class CategoryPage extends AppCompatActivity {

    private RecyclerView recyclerCategories;
    private FloatingActionButton btnAddCategory;
    private List<Category> categoryList;
    private CategoryAdapter categoryAdapter;
    private DatabaseReference categoriesRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_page);

        recyclerCategories = findViewById(R.id.recyclerCategories);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
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
                    categoriesRef.child(category.getId()).removeValue();
                    loadCategories();
                }
            }
        });

        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        btnAddCategory.setOnClickListener(v -> showCategoryDialog(null));

        loadCategories();
    }

    private void showCategoryDialog(Category categoryToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_category);
        dialog.setCancelable(true);

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
            if (id != null) categoriesRef.child(id).setValue(category);

            dialog.dismiss();
            loadCategories();
        });

        dialog.show();
    }

    private void loadCategories() {
        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Category category = ds.getValue(Category.class);
                    if (category != null) categoryList.add(category);
                }
                categoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }
}

