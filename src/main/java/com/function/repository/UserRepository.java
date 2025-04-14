package com.function.repository;

import com.function.model.User;
import com.function.OracleDBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USERS";

        try (Connection conn = OracleDBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = mapUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            logger.severe("Error al consultar usuarios: " + e.getMessage());
            throw e;
        }

        return users;
    }

    public Optional<User> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM USERS WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error al buscar usuario por ID: " + e.getMessage());
            throw e;
        }

        return Optional.empty();
    }

    public User save(User user) throws SQLException {
        if (user.getId() == null) {
            return insert(user);
        } else {
            return update(user);
        }
    }
   
    private User insert(User user) throws SQLException {
        String sql = "INSERT INTO USERS (USERNAME, EMAIL, FULL_NAME, PASSWORD, ACTIVE) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, new String[] { "ID" })) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getPassword());
            stmt.setBoolean(5, user.isActive());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
            }

            return user;
        } catch (SQLException e) {
            logger.severe("Error al insertar usuario: " + e.getMessage());
            throw e;
        }
    }

    

    private User update(User user) throws SQLException {
        String sql = "UPDATE USERS SET USERNAME = ?, EMAIL = ?, FULL_NAME = ?, PASSWORD = ?, ACTIVE = ? WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getPassword());
            stmt.setBoolean(5, user.isActive());
            stmt.setLong(6, user.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Actualización de usuario falló, no se encontró el ID: " + user.getId());
            }

            return user;
        } catch (SQLException e) {
            logger.severe("Error al actualizar usuario: " + e.getMessage());
            throw e;
        }
    }

    public boolean deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM USERS WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.severe("Error al eliminar usuario: " + e.getMessage());
            throw e;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setFullName(rs.getString("FULL_NAME"));
        user.setPassword(rs.getString("PASSWORD"));
        user.setActive(rs.getBoolean("ACTIVE"));
        return user;
    }
}