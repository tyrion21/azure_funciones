package com.function;

import com.function.model.Role;
import com.function.repository.RoleRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class RoleFunction {
    private static final Logger logger = Logger.getLogger(RoleFunction.class.getName());
    private final RoleRepository roleRepository;
    private final Gson gson;

    public RoleFunction() {
        this.roleRepository = new RoleRepository();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @FunctionName("getRoleById")
    public HttpResponseMessage getRoleById(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Obteniendo rol con ID: " + id);

        try {
            Long roleId = Long.parseLong(id);
            Optional<Role> role = roleRepository.findById(roleId);

            if (role.isPresent()) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(role.get()))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol con ID " + id + " no encontrado")
                        .build();
            }
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al obtener rol: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener rol: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("getAllRoles")
    public HttpResponseMessage getAllRoles(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "roles") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Obteniendo todos los roles");

        try {
            List<Role> roles = roleRepository.findAll();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(roles))
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al obtener roles: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener roles: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("createRole")
    public HttpResponseMessage createRole(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "roles") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Creando nuevo rol");

        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un rol para crear")
                    .build();
        }

        try {
            Role role = gson.fromJson(requestBody, Role.class);
            if (role.getId() != null) {
                role.setId(null); // Ensure ID is null for new role
            }

            Role savedRole = roleRepository.save(role);

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(savedRole))
                    .build();
        } catch (Exception e) {
            logger.severe("Error al crear rol: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear rol: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("updateRole")
    public HttpResponseMessage updateRole(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.PUT }, authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Actualizando rol con ID: " + id);

        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un rol para actualizar")
                    .build();
        }

        try {
            Long roleId = Long.parseLong(id);
            Role roleToUpdate = gson.fromJson(requestBody, Role.class);
            roleToUpdate.setId(String.valueOf(roleId));

            // Check if role exists
            if (!roleRepository.findById(roleId).isPresent()) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol con ID " + id + " no encontrado")
                        .build();
            }

            Role updatedRole = roleRepository.save(roleToUpdate);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(updatedRole))
                    .build();
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al actualizar rol: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar rol: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("deleteRole")
    public HttpResponseMessage deleteRole(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "roles/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Eliminando rol con ID: " + id);

        try {
            Long roleId = Long.parseLong(id);
            boolean deleted = roleRepository.deleteById(roleId);

            if (deleted) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Rol con ID " + id + " eliminado")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Rol con ID " + id + " no encontrado")
                        .build();
            }
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al eliminar rol: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar rol: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("getUserRoles")
    public HttpResponseMessage getUserRoles(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{userId}/roles") HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userId,
            final ExecutionContext context) {

        logger.info("Obteniendo roles para el usuario con ID: " + userId);

        try {
            Long userIdLong = Long.parseLong(userId);
            List<Role> roles = roleRepository.findRolesByUserId(userIdLong);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(roles))
                    .build();
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID de usuario inválido: " + userId)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al obtener roles del usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener roles del usuario: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("assignRoleToUser")
    public HttpResponseMessage assignRoleToUser(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{userId}/roles/{roleId}") HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userId,
            @BindingName("roleId") String roleId,
            final ExecutionContext context) {

        logger.info("Asignando rol " + roleId + " al usuario " + userId);

        try {
            Long userIdLong = Long.parseLong(userId);
            Long roleIdLong = Long.parseLong(roleId);

            roleRepository.assignRoleToUser(userIdLong, roleIdLong);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Rol asignado correctamente al usuario")
                    .build();
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido")
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al asignar rol al usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al asignar rol al usuario: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("removeRoleFromUser")
    public HttpResponseMessage removeRoleFromUser(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{userId}/roles/{roleId}") HttpRequestMessage<Optional<String>> request,
            @BindingName("userId") String userId,
            @BindingName("roleId") String roleId,
            final ExecutionContext context) {

        logger.info("Removiendo rol " + roleId + " del usuario " + userId);

        try {
            Long userIdLong = Long.parseLong(userId);
            Long roleIdLong = Long.parseLong(roleId);

            roleRepository.removeRoleFromUser(userIdLong, roleIdLong);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Rol removido correctamente del usuario")
                    .build();
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido")
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al remover rol del usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al remover rol del usuario: " + e.getMessage())
                    .build();
        }
    }
}