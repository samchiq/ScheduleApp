package com.example.scheduleapp;

/**
 * Represents a scheduled event in the application.
 * Stores details such as title, time, and optional location information.
 */
public class Event {
    /** Unique identifier for the event. */
    private String id;
    /** The title or name of the event. */
    private String title;
    /** A detailed description of the event. */
    private String description;
    /** The starting time of the event in milliseconds. */
    private long startTime;
    /** The ending time of the event in milliseconds. */
    private long endTime;
  
    /** The physical address or name of the event location. */
    private String locationAddress;
    /** The latitude coordinate for the event location. */
    private double latitude;
    /** The longitude coordinate for the event location. */
    private double longitude;

    /**
     * Default constructor required for Firebase data mapping.
     */
    public Event() { }

    /**
     * Initializes an event with basic details.
     */
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
  
    /**
     * Initializes an event with full details including location coordinates.
     */
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
  
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getLocationAddress() { return locationAddress != null ? locationAddress : ""; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
  
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
  
    /**
     * Checks if the event has a valid location address associated with it.
     */
    public boolean hasLocation() {
        return locationAddress != null && !locationAddress.isEmpty();
    }
}
