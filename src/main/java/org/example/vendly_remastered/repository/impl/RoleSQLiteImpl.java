package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Role;
import org.example.vendly_remastered.repository.IRoleRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleSQLiteImpl implements IRoleRepository {
    private Connection connection;

    public RoleSQLiteImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int addRole(Role role) {
        String sql = "INSERT INTO roles (" +
                "role_name, can_sell, can_see_clients, can_edit_clients, " +
                "can_see_products, can_edit_products, can_see_suppliers, can_edit_suppliers, " +
                "can_see_history, can_see_dashboard, can_see_settings, can_see_users, " +
                "can_see_purchases, can_edit_purchases) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // 1. Bind core identity field (omitting role_id since it's auto-incremented)
            pstmt.setString(1, role.getRoleName());

            // 2. Map permissions straight from the map keys (indices shifted by 1)
            pstmt.setInt(2, role.hasPermission("can_sell") ? 1 : 0);
            pstmt.setInt(3, role.hasPermission("can_see_clients") ? 1 : 0);
            pstmt.setInt(4, role.hasPermission("can_edit_clients") ? 1 : 0);
            pstmt.setInt(5, role.hasPermission("can_see_products") ? 1 : 0);
            pstmt.setInt(6, role.hasPermission("can_edit_products") ? 1 : 0);
            pstmt.setInt(7, role.hasPermission("can_see_suppliers") ? 1 : 0);
            pstmt.setInt(8, role.hasPermission("can_edit_suppliers") ? 1 : 0);
            pstmt.setInt(9, role.hasPermission("can_see_history") ? 1 : 0);
            pstmt.setInt(10, role.hasPermission("can_see_dashboard") ? 1 : 0);
            pstmt.setInt(11, role.hasPermission("can_see_settings") ? 1 : 0);
            pstmt.setInt(12, role.hasPermission("can_see_users") ? 1 : 0);
            pstmt.setInt(13, role.hasPermission("can_see_purchases") ? 1 : 0);
            pstmt.setInt(14, role.hasPermission("can_edit_purchases") ? 1 : 0);

            // Execute transaction payload safely
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        short generatedId = rs.getShort(1);
                        role.setRoleId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (short) -1;
    }

    @Override
    public boolean saveRole(Role role) {
        String sql = "UPDATE roles SET " +
                "role_name = ?, can_sell = ?, can_see_clients = ?, can_edit_clients = ?, " +
                "can_see_products = ?, can_edit_products = ?, can_see_suppliers = ?, can_edit_suppliers = ?, " +
                "can_see_history = ?, can_see_dashboard = ?, can_see_settings = ?, can_see_users = ?, " +
                "can_see_purchases = ?, can_edit_purchases = ? " +
                "WHERE role_id = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            // 1. Update identifying name parameters
            pstmt.setString(1, role.getRoleName());

            // 2. Map structural state arrays cleanly from your permissions map
            pstmt.setInt(2, role.hasPermission("can_sell") ? 1 : 0);
            pstmt.setInt(3, role.hasPermission("can_see_clients") ? 1 : 0);
            pstmt.setInt(4, role.hasPermission("can_edit_clients") ? 1 : 0);
            pstmt.setInt(5, role.hasPermission("can_see_products") ? 1 : 0);
            pstmt.setInt(6, role.hasPermission("can_edit_products") ? 1 : 0);
            pstmt.setInt(7, role.hasPermission("can_see_suppliers") ? 1 : 0);
            pstmt.setInt(8, role.hasPermission("can_edit_suppliers") ? 1 : 0);
            pstmt.setInt(9, role.hasPermission("can_see_history") ? 1 : 0);
            pstmt.setInt(10, role.hasPermission("can_see_dashboard") ? 1 : 0);
            pstmt.setInt(11, role.hasPermission("can_see_settings") ? 1 : 0);
            pstmt.setInt(12, role.hasPermission("can_see_users") ? 1 : 0);
            pstmt.setInt(13, role.hasPermission("can_see_purchases") ? 1 : 0);
            pstmt.setInt(14, role.hasPermission("can_edit_purchases") ? 1 : 0);

            // 3. Bind the trailing primary target ID criteria key to the WHERE clause parameter
            pstmt.setShort(15, role.getRoleId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteRole(int roleId) {
        String sql = "DELETE FROM roles WHERE role_id = ?";

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
            // Bind the target primary key to execution context constraints securely
            pstmt.setInt(1, roleId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Role> findAll() {
        List<Role> rolesList = new ArrayList<>();
        String sql = "SELECT * FROM roles";

        String[] permissionColumns = {
                "can_sell", "can_see_clients", "can_edit_clients",
                "can_see_products", "can_edit_products", "can_see_suppliers",
                "can_edit_suppliers", "can_see_history", "can_see_dashboard",
                "can_see_settings", "can_see_users", "can_see_purchases",
                "can_edit_purchases"
        };

        try (PreparedStatement pstmt = this.connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Role role = new Role();
                role.setRoleId(rs.getShort("role_id"));
                role.setRoleName(rs.getString("role_name"));

                // Hydrate all granular boolean flags dynamically into the map
                for (String column : permissionColumns) {
                    role.setPermission(column, rs.getInt(column) == 1);
                }

                rolesList.add(role);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rolesList;
    }
}
