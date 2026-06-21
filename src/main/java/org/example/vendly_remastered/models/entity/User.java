package org.example.vendly_remastered.models.entity;

import java.time.LocalDateTime;

public class User {

    private final int userId;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLogin;
    private final short roleId;

    // Primary constructor for mapping straight out of your ResultSet
    public User(int userId, String username, String passwordHash, String fullName,
                boolean isActive, LocalDateTime createdAt, LocalDateTime lastLogin, short roleId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.roleId = roleId;
    }

    // Secondary convenience constructor (useful for inserts when database handles defaults)
    public User(String username, String passwordHash, String fullName, short roleId) {
        this(0, username, passwordHash, fullName, true, LocalDateTime.now(), null, roleId);
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public short getRoleId() { return roleId; }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", isActive=" + isActive +
                ", roleId=" + roleId +
                '}';
    }
}