package com.example.scheduleapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    // Интерфейс с 3 колбэками: Edit / Delete / Share
    public interface OnEventClickListener {
        void onEditClick(Event event);
        void onDeleteClick(Event event);
        void onShareClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;
    private final Context context;

    public EventAdapter(List<Event> events, OnEventClickListener listener, Context context) {
        this.events = events;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        String timeRange = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(event.getStartTime())) + " - " +
                new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(event.getEndTime()));

        holder.tvTitle.setText(event.getTitle());
        holder.tvTime.setText(timeRange);
        holder.tvCategory.setText("Category: " + event.getDescription());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(event);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(event);
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(event);
        });
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvCategory;
        ImageButton btnEdit, btnDelete, btnShare;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvTime = itemView.findViewById(R.id.tvEventTime);
            tvCategory = itemView.findViewById(R.id.tvEventCategory);
            btnEdit = itemView.findViewById(R.id.btnEditEvent);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}
