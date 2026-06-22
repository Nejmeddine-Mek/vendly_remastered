package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.security.PasswordHasher;
import org.example.vendly_remastered.security.UserSession;

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
                        } catch (SQLException e) {
                            System.err.println("Failed to update last_login timestamp for user: " + username);
                            e.printStackTrace();
                            // We don't return false here because the user authenticated successfully
                            // and their session started; a logging slip shouldn't lock them out.
                        }
                        return true;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }catch (SQLException sqle){
            sqle.printStackTrace();
        }
        return false;
    }

    @Override
    public Optional<User> searchByUsername(String username) {
        // 1. Updated query to JOIN roles and fetch the literal role_name
        String sql = "SELECT U.*, R.role_name FROM users U " +
                "JOIN roles R ON R.role_id = U.role " +
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

                    // 2. Pass all 9 mapped parameters into your updated primary constructor layout!
                    User user = new User(
                            rs.getInt("userId"),
                            rs.getString("username"),
                            rs.getString("password_hash"), // Ensure this matches your DB column (e.g., password vs password_hash)
                            rs.getString("full_name"),
                            rs.getBoolean("is_active"),
                            createdAt,
                            lastLogin,
                            rs.getShort("role"),
                            rs.getString("role_name") // Passed right into the final slot 🎯
                    );
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> usersList = new ArrayList<>();
        String sql = "SELECT U.*, R.role_name FROM users U" +
                " JOIN roles R ON R.role_id = U.role";

        // Using try-with-resources to automatically close BOTH pstmt and rs
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("userId"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRoleName(rs.getString("role_name"));
                user.setIsActive(rs.getBoolean("is_active"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                Timestamp timestamp = rs.getTimestamp("last_login");
                user.setLastLogin(timestamp != null ? timestamp.toLocalDateTime() : null);

                // Map the joined role name directly into your new model attribute!
                user.setRoleName(rs.getString("role_name"));

                usersList.add(user);
            }

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return usersList;
    }

    @Override
    public boolean save(User user) {
        // SQLite upsert handles both creation and updates on primary key collisions
        String sql = """
        INSERT OR REPLACE INTO users (userId, username, password_hash, full_name, role, is_active, created_at, last_login)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
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
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getFullName());
            pstmt.setShort(5, user.getRoleId());

            // 3. Map Booleans (SQLite converts true/false to 1/0 behind the scenes)
            pstmt.setBoolean(6, user.isIsActive());

            // 4. Safely Convert Modern LocalDateTime Fields to SQL Timestamps
            if (user.getCreatedAt() != null) {
                pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(user.getCreatedAt()));
            } else {
                pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            }

            if (user.getLastLogin() != null) {
                pstmt.setTimestamp(8, java.sql.Timestamp.valueOf(user.getLastLogin()));
            } else {
                pstmt.setNull(8, java.sql.Types.TIMESTAMP);
            }

            // 5. Execute and confirm rows were affected
            return pstmt.executeUpdate() > 0;

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(int userId) {
        String sql = "DELETE FROM users" +
                     " WHERE userId= ?;";
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setInt(1,userId);

            return pstmt.executeUpdate() > 0;
        }catch(SQLException sqle){
            sqle.printStackTrace();
        }
        return false;
    }
}
