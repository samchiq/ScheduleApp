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
  
        holder.btnLocation.setOnClickListener(v -> {
            if (listener != null) listener.onLocationClick(event);
        });
  
        if (event.hasLocation()) {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
  
        } else {
            holder.btnLocation.setImageResource(R.drawable.ic_location);
  
        }
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
        /** TextViews for displaying event details. */
        TextView tvTitle, tvTime, tvCategory;
        /** Buttons for various event actions. */
        ImageButton btnEdit, btnDelete, btnShare, btnLocation;

        /**
         * Initializes the view references from the inflated item layout.
         */
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
