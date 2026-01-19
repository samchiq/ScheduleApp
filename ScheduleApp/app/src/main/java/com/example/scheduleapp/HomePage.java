package com.example.scheduleapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences; // Добавлено
import android.os.Bundle;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat; // Добавлено
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomePage extends Menu {

    private CalendarView calendarView;
    private FloatingActionButton btnAddEvent;
    private long selectedDate;
    private DatabaseReference eventsRef;
    private DatabaseReference categoriesRef;
    private RecyclerView recyclerEvents;
    private List<Event> eventList;
    private EventAdapter eventAdapter;
    private List<Category> userCategories;
    private SharedPreferences prefs; // Добавлено

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        setupMenu();

        // Инициализация SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Создаем канал уведомлений (для Android 8.0+)
        NotificationHelper.createNotificationChannel(this);


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
        categoriesRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("user_categories").child(uid);

        userCategories = new ArrayList<>();
        loadUserCategories();

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

    private void loadUserCategories() {
        categoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userCategories.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Category category = ds.getValue(Category.class);
                    if (category != null) {
                        userCategories.add(category);
                    }
                }
                if (userCategories.isEmpty()) {
                    userCategories.add(new Category("default_1", "Work"));
                    userCategories.add(new Category("default_2", "Personal"));
                    userCategories.add(new Category("default_3", "Health"));
                    userCategories.add(new Category("default_4", "Study"));
                    userCategories.add(new Category("default_5", "Other"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomePage.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList, new EventAdapter.OnEventClickListener() {
            @Override
            public void onEditClick(Event event) { showEventDialog(event); }

            @Override
            public void onDeleteClick(Event event) {
                if (event.getId() != null) {
                    // Отменяем уведомление перед удалением
                    NotificationHelper.cancelEventNotification(HomePage.this, event.getId());

                    eventsRef.child(event.getId()).removeValue();
                    loadEventsForDay(selectedDate);
                    Toast.makeText(HomePage.this, "Event deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onShareClick(Event event) { showShareDialog(event); }
        }, this);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerEvents.setAdapter(eventAdapter);
    }

    @Override
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
                        // При принятии приглашения включаем уведомления по умолчанию (true)
                        Event newEvent = new Event(
                                newEventId,
                                eventTitle,
                                "Shared",
                                System.currentTimeMillis(),
                                System.currentTimeMillis() + 60 * 60 * 1000,
                                true // notificationEnabled
                        );
                        userEventsRef.child(newEventId).setValue(newEvent);

                        // Планируем уведомление для принятого события
                        scheduleNotificationIfNeeded(newEvent);
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

    private void showShareDialog(Event event) {
        Dialog dialog = new Dialog(this);
        // Используем тему, если она настроена, или ручную прозрачность
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
        // Находим свитч для уведомлений
        SwitchCompat switchNotification = dialog.findViewById(R.id.switchNotification);

        // Создаем список названий категорий для Spinner
        List<String> categoryNames = new ArrayList<>();
        for (Category cat : userCategories) {
            categoryNames.add(cat.getName());
        }

        if (categoryNames.isEmpty()) {
            categoryNames.add("No categories");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        Calendar calStart = Calendar.getInstance();
        calStart.setTimeInMillis(selectedDate);
        calStart.set(Calendar.HOUR_OF_DAY, 9);
        calStart.set(Calendar.MINUTE, 0);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTimeInMillis(selectedDate);
        calEnd.set(Calendar.HOUR_OF_DAY, 10);
        calEnd.set(Calendar.MINUTE, 0);

        final long[] startTime = { calStart.getTimeInMillis() };
        final long[] endTime = { calEnd.getTimeInMillis() };
        final String[] eventId = {null};

        if (eventToEdit != null) {
            etTitle.setText(eventToEdit.getTitle());

            int categoryIndex = -1;
            for (int i = 0; i < categoryNames.size(); i++) {
                if (categoryNames.get(i).equals(eventToEdit.getDescription())) {
                    categoryIndex = i;
                    break;
                }
            }
            if (categoryIndex >= 0) {
                spinnerCategory.setSelection(categoryIndex);
            }

            startTime[0] = eventToEdit.getStartTime();
            endTime[0] = eventToEdit.getEndTime();
            eventId[0] = eventToEdit.getId();

            // Устанавливаем состояние свитча при редактировании
            if (switchNotification != null) {
                switchNotification.setChecked(eventToEdit.isNotificationEnabled());
            }
        }

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText("Selected date: " + sdfDate.format(new Date(selectedDate)));
        updateTimeText(tvTime, startTime[0], endTime[0]);

        btnPickStart.setOnClickListener(v -> pickTime(true, startTime, tvTime, endTime));
        btnPickEnd.setOnClickListener(v -> pickTime(false, endTime, tvTime, startTime));

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            boolean notificationEnabled = switchNotification != null && switchNotification.isChecked();

            if (title.isEmpty()) {
                etTitle.setError("Enter title");
                return;
            }

            // === ПРОВЕРКА ВРЕМЕНИ ===
            if (startTime[0] >= endTime[0]) {
                Toast.makeText(this, "Error: Start time must be before end time", Toast.LENGTH_LONG).show();
                // Можно также визуально подсветить текст с временем
                tvTime.setTextColor(android.graphics.Color.RED);
                return;
            }
            // ========================

            String id = (eventId[0] != null) ? eventId[0] : eventsRef.push().getKey();

            if (eventId[0] != null) {
                NotificationHelper.cancelEventNotification(this, id);
            }

            Event event = new Event(id, title, category, startTime[0], endTime[0], notificationEnabled);

            if (id != null) {
                eventsRef.child(id).setValue(event);
                scheduleNotificationIfNeeded(event);
            }

            dialog.dismiss();
            loadEventsForDay(selectedDate);
        });

        dialog.show();
    }

    // Метод проверки и планирования уведомлений
    private void scheduleNotificationIfNeeded(Event event) {
        boolean globalEnabled = prefs.getBoolean("notifications_enabled", true);

        if (globalEnabled && event.isNotificationEnabled()) {
            if (event.getStartTime() > System.currentTimeMillis()) {
                NotificationHelper.scheduleEventNotification(
                        this,
                        event.getId(),
                        event.getTitle(),
                        event.getStartTime()
                );
            }
        }
    }

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

            // Логика авто-сдвига:
            if (isStart) {
                // Если время начала стало больше или равно времени конца
                if (timeArray[0] >= otherTime[0]) {
                    // Устанавливаем конец на 1 час позже начала
                    otherTime[0] = timeArray[0] + (60 * 60 * 1000);
                }
            } else {
                // Если пользователь выбрал конец раньше начала - предупреждаем сразу
                if (timeArray[0] <= otherTime[0]) {
                    Toast.makeText(this, "End time set before start time!", Toast.LENGTH_SHORT).show();
                }
            }

            // Сбрасываем красный цвет, если он был установлен при ошибке
            tvTime.setTextColor(com.google.android.material.R.attr.colorOnSurface);

            updateTimeText(tvTime,
                    isStart ? timeArray[0] : otherTime[0],
                    isStart ? otherTime[0] : timeArray[0]);
        });
    }

    private void updateTimeText(TextView tv, long start, long end) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tv.setText(sdf.format(new Date(start)) + " - " + sdf.format(new Date(end)));
    }

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