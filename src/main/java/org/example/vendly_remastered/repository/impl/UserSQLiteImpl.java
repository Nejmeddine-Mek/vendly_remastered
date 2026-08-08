package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.security.PasswordHasher;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.service.ErrorLogsService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserSQLiteImpl implements IUserRepository {
    private final Connection connection;
    public UserSQLiteImpl(Connection connection){
        this.connection = connection;
    }
    @Override
    public boolean authenticate(String username, String password) {

        String sql = "SELECT * FROM users U " +
                "JOIN roles R ON U.role = R.role_id " +
                "WHERE U.username = ?;";
        String updateSql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?;";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setString(1,username);
            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    if(!rs.getBoolean("is_active")){
                        return false;
                    }
                    String passwordHash = rs.getString("password_hash");
                    if(PasswordHasher.checkPassword(password, passwordHash)){
                        // extract privileges from here
                        Map<String, Boolean> privileges = new HashMap<>();
                        privileges.put("can_sell", rs.getInt("can_sell") == 1);
                        privileges.put("can_see_clients", rs.getInt("can_see_clients") == 1);
                        privileges.put("can_edit_clients", rs.getInt("can_edit_clients") == 1);
                        privileges.put("can_see_products", rs.getInt("can_see_products") == 1);
                        privileges.put("can_edit_products", rs.getInt("can_edit_products") == 1);
                        privileges.put("can_see_suppliers", rs.getInt("can_see_suppliers") == 1);
                        privileges.put("can_edit_suppliers", rs.getInt("can_edit_suppliers") == 1);
                        privileges.put("can_see_history", rs.getInt("can_see_history") == 1);
                        privileges.put("can_see_dashboard", rs.getInt("can_see_dashboard") == 1);
                        privileges.put("can_see_settings", rs.getInt("can_see_settings") == 1);
                        privileges.put("can_see_users", rs.getInt("can_see_users") == 1);
                        privileges.put("can_see_purchases", rs.getInt("can_see_purchases") == 1);
                        privileges.put("can_edit_purchases", rs.getInt("can_edit_purchases") == 1);

                        //create a session now!
                        UserSession.startSession(
                                rs.getInt("userId"),
                                rs.getString("username"),
                                rs.getString("full_name"),
                                rs.getString("role_name"),
                                privileges
                        );
                        try (PreparedStatement updatePstmt = connection.prepareStatement(updateSql)) {
                            updatePstmt.setString(1, username);
                            updatePstmt.executeUpdate();
                        }catch (SQLException e) {
                            String message = "Failed to update last_login timestamp for user: " + username;
                            System.err.println(message);
                            ErrorLogsService.getInstance().log(message, e);
                            // We don't return false here because the user authenticated successfully
                            // and their session started; a logging slip shouldn't lock them out.
                        }
                        return true;
                    }
                }
            }catch(Exception e){
                String message = "CRITICAL: Error processing authentication result set for user: " + username;
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, e);
            }
        }catch (SQLException sqle){
            String message = "CRITICAL: Failed to execute authentication query for user: " + username;
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);

        }
        return false;
    }

    // I haven't used this one at all yet
    /**
     * Searches for a user by their username.
     * * @param username The username to look up.
     * @return An Optional containing the User if found, or empty otherwise.
     * @deprecated This method is being phased out in favor of lookups that explicitly
     * leverage unified cache maps or ID-based indexing. Use {@link #findById(int)} if applicable.
     */
    @Deprecated(since = "1.1", forRemoval = false)
    public Optional<User> searchByUsername(String username) {
        // 1. Updated query with the exact LEFT JOIN audit structure we built
        String sql = "SELECT U.*, R.role_name, U2.userId AS creator_id, U2.username AS creator_username FROM users U " +
                "JOIN roles R ON R.role_id = U.role " +
                "LEFT JOIN users U2 ON U.created_by = U2.userId " +
                "WHERE U.username = ?;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Safety timestamp conversions
                    java.sql.Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (createdAtTimestamp != null) ? createdAtTimestamp.toLocalDateTime() : LocalDateTime.now();

                    java.sql.Timestamp lastLoginTimestamp = rs.getTimestamp("last_login");
                    LocalDateTime lastLogin = (lastLoginTimestamp != null) ? lastLoginTimestamp.toLocalDateTime() : null;

                    // 2. Instantiate using your standard setters or matching constructor
                    User user = new User();
                    user.setUserId(rs.getInt("userId"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFullName(rs.getString("full_name"));
                    user.setIsActive(rs.getBoolean("is_active"));
                    user.setCreatedAt(createdAt);
                    user.setLastLogin(lastLogin);
                    user.setRoleId(rs.getShort("role"));
                    user.setRoleName(rs.getString("role_name"));

                    // 🎯 Track Creator mapping (Using our 0-sentinel logic)
                    int creatorId = rs.getInt("creator_id");
                    user.setCreatedBy(creatorId);
                    if (creatorId == 0) {
                        user.setCreatedByUsername("System Root");
                    } else {
                        user.setCreatedByUsername(rs.getString("creator_username"));
                    }

                    return Optional.of(user);
                }
            }
        }catch (Exception e) {
            String message = "CRITICAL: Failed to search user by username";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> usersList = new ArrayList<>();
        String sql = "SELECT U.*, R.role_name, U2.userId as creator_id, U2.username as creator_username FROM users U" +
                " JOIN roles R ON R.role_id = U.role " +
                "LEFT JOIN users U2 ON U.created_by = U2.userId";

        // Using try-with-resources to automatically close BOTH pstmt and rs
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setPasswordHash(rs.getString("password_hash"));
                user.setUserId(rs.getInt("userId"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRoleName(rs.getString("role_name"));
                user.setIsActive(rs.getBoolean("is_active"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                Timestamp timestamp = rs.getTimestamp("last_login");
                user.setLastLogin(timestamp != null ? timestamp.toLocalDateTime() : null);
                user.setRoleId(rs.getShort("role"));
                // Map the joined role name directly into your new model attribute!
                user.setRoleName(rs.getString("role_name"));

                int creatorId = rs.getInt("creator_id"); // Returns 0 natively if SQL column is NULL
                user.setCreatedBy(creatorId);

                if (creatorId == 0) {
                    user.setCreatedByUsername("System Root");
                } else {
                    user.setCreatedByUsername(rs.getString("creator_username"));
                }
                usersList.add(user);
            }

        }catch (SQLException sqle) {
            String message = "CRITICAL: Failed to retrieve all records";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return usersList;
    }

    @Override
    public boolean save(User user) {
        // 🎯 Added 'created_by' to the schema list
        String sql = """
    INSERT OR REPLACE INTO users (userId, username, password_hash, full_name, role, is_active, created_by, created_at, last_login)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // 1. Handle Primary Key Assignment (If 0, pass null so SQLite auto-increments)
            if (user.getUserId() == 0) {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(1, user.getUserId());
            }

            // 2. Set Standard String & Integer Properties
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash()); // Safe because hash is loaded in memory!
            pstmt.setString(4, user.getFullName());
            pstmt.setShort(5, user.getRoleId());

            // 3. Map Booleans
            pstmt.setBoolean(6, user.isIsActive());

            // 4. 🎯 Bind the Creator ID (If 0, pass NULL for the Root Admin node)
            if (user.getCreatedBy() == 0) {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(7, user.getCreatedBy());
            }

            // 5. Safely Convert Modern LocalDateTime Fields to SQL Timestamps
            if (user.getCreatedAt() != null) {
                pstmt.setTimestamp(8, java.sql.Timestamp.valueOf(user.getCreatedAt()));
            } else {
                pstmt.setTimestamp(8, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            }

            if (user.getLastLogin() != null) {
                pstmt.setTimestamp(9, java.sql.Timestamp.valueOf(user.getLastLogin()));
            } else {
                pstmt.setNull(9, java.sql.Types.TIMESTAMP);
            }

            // 6. Execute and confirm rows were affected
            return pstmt.executeUpdate() > 0;

        }catch (SQLException sqle) {
            String message = "CRITICAL: Failed to save record";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean delete(int userId) {
        String sql = "DELETE FROM users" +
                     " WHERE userId= ?;";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setInt(1,userId);

            return pstmt.executeUpdate() > 0;
        }catch (SQLException sqle) {
            String message = "CRITICAL: Failed to delete record";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }
        return false;
    }
}
