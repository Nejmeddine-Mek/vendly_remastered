package org.example.vendly_remastered.security;

import java.util.Collections;
import java.util.Map;

public class UserSession {

    private static UserSession instance;

    private final int userId;
    private final String username;
    private final String fullName;
    private final String roleName;
    private final Map<String, Boolean> privileges;

    // Private constructor prevents instantiation from outside
    private UserSession(int userId, String username, String fullName, String roleName, Map<String, Boolean> privileges) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.roleName = roleName;
        // Collections.unmodifiableMap ensures the UI cannot alter permissions at runtime
        this.privileges = Collections.unmodifiableMap(privileges);
    }

    /**
     * Called strictly inside UserSQLiteImpl when password validation passes.
     */
    public static synchronized void startSession(int userId, String username, String fullName, String roleName, Map<String, Boolean> privileges) {
        instance = new UserSession(userId, username, fullName, roleName, privileges);
    }

    /**
     * Used by any controller to read active user status or check permissions.
     */
    public static synchronized UserSession getInstance() {
        return instance;
    }

    /**
     * Clear context completely when the user clicks 'Logout'.
     */
    public static synchronized void cleanSession() {
        instance = null;
    }

    // Helper method to make checking view access incredibly simple
    public boolean hasPrivilege(String privilegeKey) {
        return privileges.getOrDefault(privilegeKey, false);
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRoleName() { return roleName; }
    public Map<String, Boolean> getPrivileges() { return privileges; }
}