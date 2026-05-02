package com.example.scheduleapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

/**
 * Adapter for managing and displaying a list of events in a RecyclerView.
 * Handles event data binding and user interactions such as editing, deleting, and sharing.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /** Interface for handling event-related click actions. */
    public interface OnEventClickListener {
        /** Invoked when the edit action is triggered for an event. */
        void onEditClick(Event event);
        /** Invoked when the delete action is triggered for an event. */
        void onDeleteClick(Event event);
        /** Invoked when the share action is triggered for an event. */
        void onShareClick(Event event);
        /** Invoked when the location action is triggered for an event. */
        void onLocationClick(Event event);
    }

    /** List of event objects to be displayed. */
    private final List<Event> events;
    /** Listener for handling event actions. */
    private final OnEventClickListener listener;
    /** Context used for accessing resources and starting activities. */
    private final Context context;

    /**
     * Initializes the adapter with a list of events and a click listener.
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener, Context context) {
        this.events = events;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    /**
     * Creates a new ViewHolder by inflating the event item layout.
     */
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    /**
     * Binds event data to the ViewHolder and configures click listeners for actions.
     */
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        String timeRange = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(event.getStartTime())) + " - " +
                new SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(new Date(event.getEndTime()));

        holder.tvTitle.setText(event.getTitle());
        holder.tvTime.setText(timeRange);
        
        String category = event.getDescription();
        if (category == null || category.isEmpty()) {
            holder.tvCategory.setVisibility(View.GONE);
        } else {
            holder.tvCategory.setVisibility(View.VISIBLE);
            holder.tvCategory.setText("Category: " + category);
        }

        if (event.getColor() != null) {
            try {
                int color = Color.parseColor(event.getColor());
                holder.cardView.setCardBackgroundColor(color);

                // Adjust text and icon colors based on background luminance
                int textColor = isColorDark(color) ? Color.WHITE : Color.BLACK;
                int subTextColor = isColorDark(color) ? Color.argb(178, 255, 255, 255) : Color.argb(178, 0, 0, 0);

                holder.tvTitle.setTextColor(textColor);
                holder.tvTime.setTextColor(subTextColor);
                
                holder.btnEdit.setColorFilter(textColor);
                holder.btnShare.setColorFilter(textColor);
                holder.btnLocation.setColorFilter(textColor);
                holder.btnDelete.setColorFilter(textColor);
                
                // Keep category tag style or adjust it
                if (isColorDark(color)) {
                    holder.tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.argb(50, 255, 255, 255)));
                    holder.tvCategory.setTextColor(Color.WHITE);
                } else {
                    holder.tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.argb(50, 0, 0, 0)));
                    holder.tvCategory.setTextColor(Color.BLACK);
                }
            } catch (Exception e) {
                // Fallback
            }
        }
  
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(event);
        });
  
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(event);
        });
  
        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(event);
        });
  
        holder.btnLocation.setOnClickListener(v -> {
            if (listener != null) listener.onLocationClick(event);
        });
  
        if (event.hasLocation()) {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
  
        } else {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
  
        }
    }

    /**
     * Determines if a color is dark based on its luminance.
     */
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    @Override
    /**
     * Returns the total number of items in the event list.
     */
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }
  
    /**
     * Opens the location of an event in the Google Maps application.
     * Uses geo coordinates and event title for the map marker.
     */
    private void openLocationInMaps(Event event) {
        if (!event.hasLocation()) return;

        try {
  
            String label = Uri.encode(event.getTitle());
            String uriString = "geo:" + event.getLatitude() + "," + event.getLongitude()
                    + "?q=" + event.getLatitude() + "," + event.getLongitude()
                    + "(" + label + ")";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            intent.setPackage("com.google.android.apps.maps"); 

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
  
                intent.setPackage(null);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ViewHolder class that holds references to the views for each event item.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        /** CardView for background color. */
        com.google.android.material.card.MaterialCardView cardView;
        /** TextViews for displaying event details. */
        TextView tvTitle, tvTime, tvCategory;
        /** Buttons for various event actions. */
        ImageButton btnEdit, btnDelete, btnShare, btnLocation;

        /**
         * Initializes the view references from the inflated item layout.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (com.google.android.material.card.MaterialCardView) itemView;
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
