package com.example.scheduleapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

    // Интерфейс с 4 колбэками: Edit / Delete / Share / Location
    public interface OnEventClickListener {
        void onEditClick(Event event);
        void onDeleteClick(Event event);
        void onShareClick(Event event);
        void onLocationClick(Event event);
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

        // Кнопка редактирования
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(event);
        });

        // Кнопка удаления
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(event);
        });

        // Кнопка шаринга
        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(event);
        });

        // Кнопка локации
        holder.btnLocation.setOnClickListener(v -> {
            if (event.hasLocation()) {
                // Если локация уже есть - открываем в картах
                openLocationInMaps(event);
            } else {
                // Если локации нет - открываем диалог добавления
                if (listener != null) listener.onLocationClick(event);
            }
        });

        // Меняем цвет иконки локации в зависимости от наличия адреса
        if (event.hasLocation()) {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
            // Цвет индиго для заполненной локации (можно поменять на любой)
        } else {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
            // Серый цвет для пустой локации
        }
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    // Открыть локацию в Google Maps или других картах
    private void openLocationInMaps(Event event) {
        if (!event.hasLocation()) return;

        try {
            // Создаем URI для Google Maps с координатами и меткой
            String label = Uri.encode(event.getTitle());
            String uriString = "geo:" + event.getLatitude() + "," + event.getLongitude()
                    + "?q=" + event.getLatitude() + "," + event.getLongitude()
                    + "(" + label + ")";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            intent.setPackage("com.google.android.apps.maps"); // Попытка открыть Google Maps

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // Если Google Maps нет, открываем в браузере или других картах
                intent.setPackage(null);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvCategory;
        ImageButton btnEdit, btnDelete, btnShare, btnLocation;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvTime = itemView.findViewById(R.id.tvEventTime);
            tvCategory = itemView.findViewById(R.id.tvEventCategory);
            btnEdit = itemView.findViewById(R.id.btnEditEvent);
            btnDelete = itemView.findViewById(R.id.btnDeleteEvent);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnLocation = itemView.findViewById(R.id.btnLocation);
        }
    }
}