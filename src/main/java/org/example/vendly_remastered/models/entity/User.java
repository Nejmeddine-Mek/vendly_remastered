package org.example.vendly_remastered.models.entity;

import org.example.vendly_remastered.security.PasswordHasher;

import java.time.LocalDateTime;

public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private short roleId;
    private String roleName;

    // No-arg constructor for dynamic ResultSet mapping
    public User() {}

    // Primary constructor
    public User(int userId, String username, String passwordHash, String fullName,
                boolean isActive, LocalDateTime createdAt, LocalDateTime lastLogin, short roleId, String roleName) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.roleId = roleId;
        this.roleName = roleName;
    }

    // Secondary convenience constructor for manual insertions
    public User(String username, String passwordHash, String fullName, short roleId) {
        this(0, username, passwordHash, fullName, true, LocalDateTime.now(), null, roleId, null);
    }

    // --- Strict JavaBean Standard Getters and Setters ---

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    // Standard boolean naming convention (is... / set...)
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public short getRoleId() { return roleId; }
    public void setRoleId(short roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    public void setPassword(String text) {
        this.passwordHash = PasswordHasher.hashPassword(text);
    }
}