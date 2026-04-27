package com.example.scheduleapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Main dashboard of the application displaying a calendar and events for the selected day.
 * Allows users to manage events, locations, and invitations from other users.
 */
public class HomePage extends Menu {

    /** Calendar view for date selection. */
    private CalendarView calendarView;
    /** Button for adding a new event. */
    private FloatingActionButton btnAddEvent;
    /** Currently selected date in milliseconds. */
    private long selectedDate;
    /** Reference to the current user's events in Firebase. */
    private DatabaseReference eventsRef;
    /** RecyclerView for listing events on the selected date. */
    private RecyclerView recyclerEvents;
    /** List of events displayed for the current selection. */
    private List<Event> eventList;
    /** Adapter for the events RecyclerView. */
    private EventAdapter eventAdapter;

    @Override
    /**
     * Initializes the activity, sets up the menu, and loads events for the current day.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        setupMenu();

        calendarView = findViewById(R.id.calendarView);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        recyclerEvents = findViewById(R.id.recyclerEvents);

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        eventsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("user_events").child(uid);

        setupRecyclerView();

        selectedDate = Calendar.getInstance().getTimeInMillis();
        loadEventsForDay(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate = cal.getTimeInMillis();
            loadEventsForDay(selectedDate);
        });

        btnAddEvent.setOnClickListener(v -> showEventDialog(null));
    }

    /**
     * Configures the RecyclerView and its adapter for displaying events.
     * Defines actions for editing, deleting, sharing, and location management of events.
     */
    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEditClick(Event event) {
                showEventDialog(event);
            }

            @Override
            public void onDeleteClick(Event event) {
                if (event.getId() != null) {
                    eventsRef.child(event.getId()).removeValue();
                    loadEventsForDay(selectedDate);
                    Toast.makeText(HomePage.this, "Event deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onShareClick(Event event) {
                showShareDialog(event);
            }

            @Override
            public void onLocationClick(Event event) {
  
                if (event.hasLocation()) {
                    showLocationSpinner(event);
                } else {
  
                    showLocationDialog(event);
                }
            }
        }, this);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerEvents.setAdapter(eventAdapter);
    }
  
    /**
     * Displays a selection dialog for location-based actions.
     * Allows the user to either open the existing location in maps or change it.
     */
    private void showLocationSpinner(Event event) {
  
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location: " + event.getLocationAddress());
  
        String[] options = {"Open Location", "Change Location"};
  
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(50, 40, 50, 40);

        builder.setView(spinner);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedPosition = spinner.getSelectedItemPosition();
            if (selectedPosition == 0) {
  
                openLocationInMaps(event);
            } else if (selectedPosition == 1) {
  
                showLocationDialog(event);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
  
    /**
     * Opens the event's location coordinates in an external maps application.
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

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
  
                intent.setPackage(null);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open maps", Toast.LENGTH_SHORT).show();
        }
    }
  
    /**
     * Displays a dialog for searching and setting a location for an event.
     * Performs a mock search and allows saving coordinates to the event.
     */
    private void showLocationDialog(Event event) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_location);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etSearch = dialog.findViewById(R.id.etLocationSearch);
        Button btnSearch = dialog.findViewById(R.id.btnSearchLocation);
        TextView tvFoundAddress = dialog.findViewById(R.id.tvFoundAddress);
        TextView tvCoordinates = dialog.findViewById(R.id.tvCoordinates);
        LinearLayout layoutSearchResult = dialog.findViewById(R.id.layoutSearchResult);
        LinearLayout layoutCoordinates = dialog.findViewById(R.id.layoutCoordinates);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        Button btnSave = dialog.findViewById(R.id.btnSaveLocation);
        Button btnCancel = dialog.findViewById(R.id.btnCancelLocation);

        final Location.LocationResult[] selectedLocation = {null};
  
        if (event.hasLocation()) {
            etSearch.setText(event.getLocationAddress());
            tvFoundAddress.setText(event.getLocationAddress());
            tvCoordinates.setText(String.format(Locale.getDefault(), "%.4f, %.4f",
                    event.getLatitude(), event.getLongitude()));
            layoutSearchResult.setVisibility(View.VISIBLE);
            layoutCoordinates.setVisibility(View.VISIBLE);
            btnSave.setEnabled(true);
            selectedLocation[0] = new Location.LocationResult(
                    event.getLocationAddress(),
                    event.getLatitude(),
                    event.getLongitude()
            );
        }
  
        etSearch.setOnEditorActionListener((v, actionId, keyEvent) -> {
            btnSearch.performClick();
            return true;
        });
  
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (query.isEmpty()) {
                etSearch.setError("Enter location");
                return;
            }
  
            etSearch.clearFocus();
  
            progressBar.setVisibility(View.VISIBLE);
            layoutSearchResult.setVisibility(View.GONE);
            btnSave.setEnabled(false);
  
            new Thread(() -> {
  
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Location.LocationResult result =
                        Location.searchLocation(query);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (result != null) {
                        selectedLocation[0] = result;
                        tvFoundAddress.setText(result.fullAddress);
                        tvCoordinates.setText(String.format(Locale.getDefault(),
                                "%.4f, %.4f", result.latitude, result.longitude));
                        layoutSearchResult.setVisibility(View.VISIBLE);
                        layoutCoordinates.setVisibility(View.VISIBLE);
                        btnSave.setEnabled(true);
                    } else {
                        layoutSearchResult.setVisibility(View.VISIBLE);
                        tvFoundAddress.setText("Location not found. Try a different search.");
                        layoutCoordinates.setVisibility(View.GONE);
                        btnSave.setEnabled(false);
                    }
                });
            }).start();
        });
  
        btnSave.setOnClickListener(v -> {
            if (selectedLocation[0] != null) {
  
                event.setLocationAddress(selectedLocation[0].fullAddress);
                event.setLatitude(selectedLocation[0].latitude);
                event.setLongitude(selectedLocation[0].longitude);
  
                if (event.getId() != null) {
                    eventsRef.child(event.getId()).setValue(event)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(HomePage.this, "Location saved", Toast.LENGTH_SHORT).show();
                                loadEventsForDay(selectedDate);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(HomePage.this, "Failed to save location", Toast.LENGTH_SHORT).show();
                            });
                }

                dialog.dismiss();
            }
        });
  
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    /**
     * Scans for pending event invitations for the current user.
     */
    protected void handleInvites(DataSnapshot snapshot) {
        for (DataSnapshot inviteSnap : snapshot.getChildren()) {
            Map<String, Object> invite = (Map<String, Object>) inviteSnap.getValue();
            if (invite != null) {
                Object statusObj = invite.get("status");
                String status = statusObj != null ? statusObj.toString() : "";
                if ("pending".equals(status)) {
                    handleInviteSnapshot(inviteSnap);
                }
            }
        }
    }

    /**
     * Processes an invitation snapshot to retrieve sender information and show an alert.
     */
    private void handleInviteSnapshot(DataSnapshot inviteSnap) {
        if (inviteSnap == null) return;
        Map<String, Object> invite = (Map<String, Object>) inviteSnap.getValue();
        if (invite == null) return;

        String title = invite.get("title") != null ? invite.get("title").toString() : "Event";
        String fromUserUid = invite.get("fromUser") != null ? invite.get("fromUser").toString() : null;
        String inviteId = inviteSnap.getKey();
        String normalizedNumber = normalizePhone(currentUserNumber);

        if (fromUserUid == null || inviteId == null) return;

        DatabaseReference fromRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(fromUserUid);
        fromRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot fromSnapshot) {
                String fromName = fromSnapshot.child("name").getValue(String.class);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showInviteDialog(title, fromName != null ? fromName : "Someone", inviteId, normalizedNumber);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Displays a dialog allowing the user to accept or decline an event invitation.
     * On acceptance, the event is added to the user's schedule.
     */
    private void showInviteDialog(String eventTitle, String fromName, String inviteId, String normalizedNumber) {
        new AlertDialog.Builder(this)
                .setTitle("Event Invitation")
                .setMessage(fromName + " invited you to the event:\n\n" + eventTitle)
                .setCancelable(false)
                .setPositiveButton("Accept", (dialog, which) -> {
                    String uid = currentUser.getUid();
                    DatabaseReference userEventsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("user_events").child(uid);

                    String newEventId = userEventsRef.push().getKey();
                    if (newEventId != null) {
                        Event newEvent = new Event(
                                newEventId,
                                eventTitle,
                                "Shared",
                                System.currentTimeMillis(),
                                System.currentTimeMillis() + 60 * 60 * 1000
                        );
                        userEventsRef.child(newEventId).setValue(newEvent);
                    }

                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("invites")
                            .child(normalizedNumber)
                            .child(inviteId)
                            .removeValue();

                    Toast.makeText(HomePage.this, "Event saved", Toast.LENGTH_SHORT).show();
                    loadEventsForDay(selectedDate);
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("invites")
                            .child(normalizedNumber)
                            .child(inviteId)
                            .removeValue();

                    Toast.makeText(HomePage.this, "Invitation declined", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /**
     * Displays a dialog for sharing an event with another user via their phone number.
     */
    private void showShareDialog(Event event) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_share_event);
        dialog.setCancelable(true);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etPhone = dialog.findViewById(R.id.etPhoneNumber);
        Button btnSend = dialog.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Enter phone number");
                return;
            }
            sendInvite(event, phone);
            dialog.dismiss();
            Toast.makeText(this, "Invitation sent to " + phone, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    /**
     * Sends an event invitation to the specified phone number after verifying user existence.
     */
    private void sendInvite(Event event, String phoneNumber) {
        final String normalizedPhone = normalizePhone(phoneNumber);
        if (normalizedPhone.isEmpty()) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference usersRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String targetUserKey = null;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String storedNumber = userSnap.child("number").getValue(String.class);
                    if (storedNumber != null && normalizePhone(storedNumber).equals(normalizedPhone)) {
                        targetUserKey = userSnap.getKey();
                        break;
                    }
                }

                if (targetUserKey == null) {
                    Toast.makeText(HomePage.this, "User with this number not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference invitesRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("invites").child(normalizedPhone);
                String inviteId = invitesRef.push().getKey();
                if (inviteId == null) {
                    Toast.makeText(HomePage.this, "Failed to create invite", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> inviteData = new HashMap<>();
                inviteData.put("eventId", event.getId());
                inviteData.put("title", event.getTitle());
                inviteData.put("fromUser", currentUser.getUid());
                inviteData.put("status", "pending");

                invitesRef.child(inviteId).setValue(inviteData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(HomePage.this, "Invitation sent!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(HomePage.this, "Failed to send invite", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Displays a dialog for creating or editing an event.
     * Manages input for event title, category selection, and time intervals.
     */
    private void showEventDialog(Event eventToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.create_event);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etTitle = dialog.findViewById(R.id.etEventTitle);
        Spinner spinnerCategory = dialog.findViewById(R.id.spinnerCategory);
        TextView tvDate = dialog.findViewById(R.id.tvSelectedDate);
        TextView tvTime = dialog.findViewById(R.id.tvSelectedTime);
        Button btnPickStart = dialog.findViewById(R.id.btnPickStartTime);
        Button btnPickEnd = dialog.findViewById(R.id.btnPickEndTime);
        Button btnSave = dialog.findViewById(R.id.btnSaveEvent);

        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(selectedDate);
        calStart.set(Calendar.HOUR_OF_DAY, 9);
        calStart.set(Calendar.MINUTE, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(selectedDate);
        calEnd.set(Calendar.HOUR_OF_DAY, 10);
        calEnd.set(Calendar.MINUTE, 0);

        final long[] startTime = { calStart.getTimeInMillis() };
        final long[] endTime   = { calEnd.getTimeInMillis() };
        final String[] eventId = { null };

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText("Selected date: " + sdfDate.format(new Date(selectedDate)));
        updateTimeText(tvTime, startTime[0], endTime[0]);

        btnPickStart.setOnClickListener(v -> pickTime(true,  startTime, tvTime, endTime));
        btnPickEnd.setOnClickListener(v   -> pickTime(false, endTime,   tvTime, startTime));
  
        String uid = currentUser.getUid();
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("user_categories").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> categoryNames = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Category cat = ds.getValue(Category.class);
                            if (cat != null && cat.getName() != null) {
                                categoryNames.add(cat.getName());
                            }
                        }
  
                        if (categoryNames.isEmpty()) {
                            categoryNames.add("General");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                HomePage.this,
                                android.R.layout.simple_spinner_item,
                                categoryNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(adapter);
  
                        if (eventToEdit != null) {
                            etTitle.setText(eventToEdit.getTitle());
                            int pos = adapter.getPosition(eventToEdit.getDescription());
                            if (pos >= 0) spinnerCategory.setSelection(pos);
                            startTime[0] = eventToEdit.getStartTime();
                            endTime[0]   = eventToEdit.getEndTime();
                            eventId[0]   = eventToEdit.getId();
                            updateTimeText(tvTime, startTime[0], endTime[0]);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomePage.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
                    }
                });

        btnSave.setOnClickListener(v -> {
            String title    = etTitle.getText().toString().trim();
            Object selected = spinnerCategory.getSelectedItem();
            String category = selected != null ? selected.toString() : "General";

            if (title.isEmpty()) {
                etTitle.setError("Enter title");
                return;
            }

            String id = (eventId[0] != null) ? eventId[0] : eventsRef.push().getKey();
            Event event = new Event(id, title, category, startTime[0], endTime[0]);

            if (eventToEdit != null && eventToEdit.hasLocation()) {
                event.setLocationAddress(eventToEdit.getLocationAddress());
                event.setLatitude(eventToEdit.getLatitude());
                event.setLongitude(eventToEdit.getLongitude());
            }

            if (id != null) eventsRef.child(id).setValue(event);

            dialog.dismiss();
            loadEventsForDay(selectedDate);
        });

        dialog.show();
    }

    /**
     * Opens a time picker dialog to select either start or end time for an event.
     */
    private void pickTime(boolean isStart, long[] timeArray, TextView tvTime, long[] otherTime) {
        com.google.android.material.timepicker.MaterialTimePicker picker =
                new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                        .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                        .setHour(12)
                        .setMinute(0)
                        .setTitleText("Select time")
                        .build();
        picker.show(getSupportFragmentManager(), "TIME_PICKER");
        picker.addOnPositiveButtonClickListener(dialogTime -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, picker.getHour());
            cal.set(Calendar.MINUTE, picker.getMinute());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(selectedDate);
            cal.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR));
            cal.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH));

            timeArray[0] = cal.getTimeInMillis();
            updateTimeText(tvTime,
                    isStart ? timeArray[0] : otherTime[0],
                    isStart ? otherTime[0] : timeArray[0]);
        });
    }

    /**
     * Updates the time range display in the user interface.
     */
    private void updateTimeText(TextView tv, long start, long end) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tv.setText(sdf.format(new Date(start)) + " - " + sdf.format(new Date(end)));
    }

    /**
     * Fetches events for the specified date from Firebase and updates the display list.
     */
    private void loadEventsForDay(long dateMillis) {
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTimeInMillis(dateMillis);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        eventsRef.orderByChild("startTime")
                .startAt(startOfDay.getTimeInMillis())
                .endAt(endOfDay.getTimeInMillis())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        eventList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Event event = ds.getValue(Event.class);
                            if (event != null) eventList.add(event);
                        }
                        eventAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }
}
