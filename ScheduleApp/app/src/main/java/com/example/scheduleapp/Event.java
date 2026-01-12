package com.example.scheduleapp;

public class Event {
    private String id;
    private String title;
    private String description;
    private long startTime;
    private long endTime;

    public Event() { }

    public Event(String id, String title, String description, long startTime, long endTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
}
