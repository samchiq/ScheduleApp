package com.example.scheduleapp;

/**
 * Represents a category for organizing events in the application.
 * Used to group events and display them in the category list.
 */
public class Category {
    /** Unique identifier for the category. */
    private String id;
    /** The display name of the category. */
    private String name;

    /**
     * Default constructor required for Firebase data mapping.
     */
    public Category() {}

    /**
     * Initializes a category with a specific identifier and name.
     */
    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
