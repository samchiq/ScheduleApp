package com.example.scheduleapp;

public class Event {
    private String id;
    private String title;
    private String description;
    private long startTime;
    private long endTime;

    // Новые поля для локации
    private String locationAddress;
    private double latitude;
    private double longitude;

    public Event() { }

    public Event(String id, String title, String description, long startTime, long endTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationAddress = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    // Конструктор с локацией
    public Event(String id, String title, String description, long startTime, long endTime,
                 String locationAddress, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationAddress = locationAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Геттеры
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getLocationAddress() { return locationAddress != null ? locationAddress : ""; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // Сеттеры
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Проверка наличия локации
    public boolean hasLocation() {
        return locationAddress != null && !locationAddress.isEmpty();
    }
}