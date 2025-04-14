package com.function;

import com.function.model.User;
import com.function.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class UserFunction {
    private static final Logger logger = Logger.getLogger(UserFunction.class.getName());
    private final UserRepository userRepository;
    private final Gson gson;

    public UserFunction() {
        this.userRepository = new UserRepository();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @FunctionName("getUserById")
    public HttpResponseMessage getUserById(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Obteniendo usuario con ID: " + id);

        try {
            Long userId = Long.parseLong(id);
            Optional<User> user = userRepository.findById(userId);

            if (user.isPresent()) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(gson.toJson(user.get()))
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario con ID " + id + " no encontrado")
                        .build();
            }
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al obtener usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuario: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("getAllUsers")
    public HttpResponseMessage getAllUsers(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Obteniendo todos los usuarios");

        try {
            List<User> users = userRepository.findAll();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(users))
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al obtener usuarios: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuarios: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("createUser")
    public HttpResponseMessage createUser(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Creando nuevo usuario");

        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un usuario para crear")
                    .build();
        }

        try {
            User user = gson.fromJson(requestBody, User.class);
            if (user.getId() != null) {
                user.setId(null); // Ensure ID is null for new user
            }

            User savedUser = userRepository.save(user);

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(savedUser))
                    .build();
        } catch (Exception e) {
            logger.severe("Error al crear usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear usuario: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("updateUser")
    public HttpResponseMessage updateUser(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.PUT }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Actualizando usuario con ID: " + id);

        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un usuario para actualizar")
                    .build();
        }

        try {
            Long userId = Long.parseLong(id);
            User userToUpdate = gson.fromJson(requestBody, User.class);
            userToUpdate.setId(userId);

            // Check if user exists
            if (!userRepository.findById(userId).isPresent()) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario con ID " + id + " no encontrado")
                        .build();
            }

            User updatedUser = userRepository.save(userToUpdate);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(updatedUser))
                    .build();
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al actualizar usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar usuario: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("deleteUser")
    public HttpResponseMessage deleteUser(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        logger.info("Eliminando usuario con ID: " + id);

        try {
            Long userId = Long.parseLong(id);
            boolean deleted = userRepository.deleteById(userId);

            if (deleted) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body("Usuario con ID " + id + " eliminado")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Usuario con ID " + id + " no encontrado")
                        .build();
            }
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("ID inválido: " + id)
                    .build();
        } catch (SQLException e) {
            logger.severe("Error al eliminar usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar usuario: " + e.getMessage())
                    .build();
        }
    }
}