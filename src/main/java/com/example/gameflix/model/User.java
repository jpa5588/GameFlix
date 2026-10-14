package com.example.gameflix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

//Create a table called Accounts with a unique constraint of email, no 2 emails should exist
@Entity
@Table(name="Accounts", uniqueConstraints = @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"))
public class User {
    //Primary Key Field for Accounts/Users
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;

    //Column to display the users account display name or username, required, max length ot 100
    @Column(name="display_name", nullable = false, length = 100)
    private String displayName;

    //Column to store the users account email, required, max length of 255
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    //Column to store the password, required, max length of 255
    //JsonIgnore prevents this property appearing in JSON.
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Hold the password received during registration or login.
    // Transient keeps it out of the database.
    // WRITE_ONLY lets us receive it in JSON without sending it back.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    //Column to store where the users account is active or inactive, required, max of 30
    //By default all new user accounts will be active
    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    //Column to store timestamp when the account was created, required, do not update, make column a SQL timestamp
    //Use the localdatetime class to store the date it was created
    @Column(name = "created_time", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdTime;

    //Set the creation time using the server's current local date and time.
    @PrePersist
    protected void onCreate() {this.createdTime = LocalDateTime.now();}

    //Default constructor with no arguments.
    public User() {}

    //Getters & Setters for each variable

    public Integer getAccountId() {return accountId;}

    public void setAccountId(Integer accountId) {this.accountId = accountId;}

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPassword() {return password;}

    public void setPassword(String password) {this.password = password;}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
