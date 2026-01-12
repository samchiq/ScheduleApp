package com.example.scheduleapp;

public class Users {
    private String email;
    private String number;
    private String name;
    private String password;

    public Users() {
    }

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
