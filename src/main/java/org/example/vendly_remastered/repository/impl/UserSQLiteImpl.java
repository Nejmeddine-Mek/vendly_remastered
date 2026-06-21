package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.security.PasswordHasher;
import org.example.vendly_remastered.security.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        String sql = "SELECT * FROM users WHERE username = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Extract timestamps safely, handling potential nulls for last_login
                    java.sql.Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = (createdAtTimestamp != null) ? createdAtTimestamp.toLocalDateTime() : LocalDateTime.now();

                    java.sql.Timestamp lastLoginTimestamp = rs.getTimestamp("last_login");
                    LocalDateTime lastLogin = (lastLoginTimestamp != null) ? lastLoginTimestamp.toLocalDateTime() : null;

                    User user = new User(
                            rs.getInt("userId"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getBoolean("is_active"), // JDBC converts 1/0 to true/false automatically!
                            createdAt,
                            lastLogin,
                            rs.getShort("role")
                    );
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
