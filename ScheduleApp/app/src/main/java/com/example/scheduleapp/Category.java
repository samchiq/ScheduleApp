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
    /** The hex color code for the category. */
    private String color;

    /**
     * Default constructor required for Firebase data mapping.
     */
    public Category() {
        this.color = "#FF1E88E5"; // Default blue
    }

    /**
     * Initializes a category with a specific identifier and name.
     */
    public Category(String id, String name) {
        this.id = id;
        this.name = name;
        this.color = "#FF1E88E5"; // Default blue
    }

    /**
     * Initializes a category with full details including color.
     */
    public Category(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
}
