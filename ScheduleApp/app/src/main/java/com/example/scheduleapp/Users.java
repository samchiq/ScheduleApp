package com.example.scheduleapp;

/**
 * Represents a user profile in the system.
 * Stores account details such as name, email, phone number, and password for authentication.
 */
public class Users {
    /** The user's registered email address. */
    private String email;
    /** The user's registered phone number. */
    private String number;
    /** The user's full display name. */
    private String name;
    /** The user's account password. */
    private String password;

    /**
     * Default constructor required for Firebase data mapping.
     */
    public Users() {
    }

    /**
     * Initializes a user profile with full account details.
     */
    public Users(String email, String number, String name, String password) {
        this.email = email;
        this.number = number;
        this.name = name;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
