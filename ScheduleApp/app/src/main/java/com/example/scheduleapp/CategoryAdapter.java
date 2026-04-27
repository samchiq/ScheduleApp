package com.example.scheduleapp;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter class for managing and displaying a list of categories in a RecyclerView.
 * Connects the category data to the user interface and handles user interactions.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    /** Interface for handling category-related click events. */
    public interface OnCategoryClickListener {
        /**
         * Invoked when the edit action is triggered for a category.
         */
        void onEditClick(Category category);
        /**
         * Invoked when the delete action is triggered for a category.
         */
        void onDeleteClick(Category category);
    }

    /** List of category objects to be displayed. */
    private List<Category> categoryList;
    /** Listener for handling edit and delete clicks on category items. */
    private OnCategoryClickListener listener;

    /**
     * Initializes the adapter with a list of categories and a click listener.
     */
    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    /**
     * Creates a new ViewHolder by inflating the category item layout.
     */
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent, false);
        return new CategoryViewHolder(v);
    }

    @Override
    /**
     * Binds category data to the ViewHolder and sets up click listeners for actions.
     */
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategoryName.setText(category.getName());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(category);
        });
    }

    @Override
    /**
     * Returns the total number of items in the category list.
     */
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    /**
     * ViewHolder class that holds references to the views for each category item.
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        /** TextView for displaying the category name. */
        TextView tvCategoryName;
        /** Buttons for editing or deleting the category. */
        ImageButton btnEdit, btnDelete;

        /**
         * Initializes the view references from the inflated item layout.
         */
        public CategoryViewHolder(View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
