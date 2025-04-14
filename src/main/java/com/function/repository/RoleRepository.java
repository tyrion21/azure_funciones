package com.function.repository;

import com.function.model.Role;
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

public class RoleRepository {
    private static final Logger logger = Logger.getLogger(RoleRepository.class.getName());

    public List<Role> findAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM ROLES";

        try (Connection conn = OracleDBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Role role = mapRole(rs);
                roles.add(role);
            }
        } catch (SQLException e) {
            logger.severe("Error al consultar roles: " + e.getMessage());
            throw e;
        }

        return roles;
    }

    public Optional<Role> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM ROLES WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRole(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error al buscar rol por ID: " + e.getMessage());
            throw e;
        }

        return Optional.empty();
    }

    public Role save(Role role) throws SQLException {
        if (role.getId() == null) {
            return insert(role);
        } else {
            return update(role);
        }
    }
    private Role insert(Role role) throws SQLException {
        String sql = "INSERT INTO ROLES (NAME, DESCRIPTION) VALUES (?, ?) RETURNING ID INTO ?";

        try (Connection conn = OracleDBConnection.getConnection();
                CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.registerOutParameter(3, java.sql.Types.NUMERIC);

            stmt.executeUpdate();
            role.setId(stmt.getLong(3));

            return role;
        } catch (SQLException e) {
            logger.severe("Error al insertar rol: " + e.getMessage());
            throw e;
        }
    }

    private Role update(Role role) throws SQLException {
        String sql = "UPDATE ROLES SET NAME = ?, DESCRIPTION = ? WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.setLong(3, role.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Actualización de rol falló, no se encontró el ID: " + role.getId());
            }

            return role;
        } catch (SQLException e) {
            logger.severe("Error al actualizar rol: " + e.getMessage());
            throw e;
        }
    }

    public boolean deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM ROLES WHERE ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.severe("Error al eliminar rol: " + e.getMessage());
            throw e;
        }
    }

    public List<Role> findRolesByUserId(Long userId) throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT r.* FROM ROLES r JOIN USER_ROLES ur ON r.ID = ur.ROLE_ID WHERE ur.USER_ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapRole(rs));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error al consultar roles por usuario: " + e.getMessage());
            throw e;
        }

        return roles;
    }

    public void assignRoleToUser(Long userId, Long roleId) throws SQLException {
        String sql = "INSERT INTO USER_ROLES (USER_ID, ROLE_ID) VALUES (?, ?)";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, roleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error al asignar rol a usuario: " + e.getMessage());
            throw e;
        }
    }

    public void removeRoleFromUser(Long userId, Long roleId) throws SQLException {
        String sql = "DELETE FROM USER_ROLES WHERE USER_ID = ? AND ROLE_ID = ?";

        try (Connection conn = OracleDBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, roleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error al remover rol de usuario: " + e.getMessage());
            throw e;
        }
    }

    private Role mapRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getLong("ID"));
        role.setName(rs.getString("NAME"));
        role.setDescription(rs.getString("DESCRIPTION"));
        return role;
    }
}