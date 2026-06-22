package org.example.vendly_remastered.models.entity;

import java.util.HashMap;
import java.util.Map;

public class Role {
    private short roleId;
    private String roleName;

    // 🎯 All granular permissions packed into a single structure
    private Map<String, Boolean> permissions = new HashMap<>();

    public Role() {}

    public Role(short roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    // Core Getters and Setters
    public short getRoleId() { return roleId; }
    public void setRoleId(short roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public Map<String, Boolean> getPermissions() { return permissions; }
    public void setPermissions(Map<String, Boolean> permissions) { this.permissions = permissions; }

    /**
     * Helper to safely check a permission without throwing NullPointerExceptions.
     */
    public boolean hasPermission(String permissionKey) {
        return permissions.getOrDefault(permissionKey, false);
    }

    /**
     * Helper to easily add/update a permission flag.
     */
    public void setPermission(String permissionKey, boolean value) {
        this.permissions.put(permissionKey, value);
    }
}