package com.example.scheduleapp;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onEditClick(Category category);
        void onDeleteClick(Category category);
    }

    private List<Category> categoryList;
    private OnCategoryClickListener listener;

    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        return new CategoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvTitle.setText(category.getName());
        holder.tvDescription.setText("Category");

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(category));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(category));
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        ImageButton btnEdit, btnDelete;

        public CategoryViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            btnEdit = itemView.findViewById(R.id.btnEditEvent);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
        }
    }
}
