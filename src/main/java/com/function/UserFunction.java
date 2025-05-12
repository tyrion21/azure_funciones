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
import java.util.Map;

public class UserFunction {
    private static final Logger logger = Logger.getLogger(UserFunction.class.getName());
    private final UserRepository userRepository;
    private final Gson gson;
    
    // Configuración para Event Grid
    private static final String EVENT_GRID_TOPIC_ENV = "EVENT_GRID_TOPIC_ENDPOINT";
    private static final String EVENT_GRID_KEY_ENV = "EVENT_GRID_TOPIC_KEY";
    private static final String USER_CREATED_EVENT_TYPE = "user/created";

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
        
        try {
            // Verificar conexión a la BD antes de continuar
            if (!OracleDBConnection.testConnection()) {
                logger.severe("No se pudo establecer conexión con la base de datos");
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: No se pudo conectar con la base de datos. Verifique la configuración.")
                        .build();
            }
            
            String requestBody = request.getBody().orElse("");
            if (requestBody.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Por favor proporciona un usuario para crear")
                        .build();
            }

            logger.info("Datos recibidos para creación de usuario: " + requestBody);
            User user = gson.fromJson(requestBody, User.class);
            
            if (user.getId() != null) {
                user.setId(null); // Ensure ID is null for new user
            }

            logger.info("Guardando usuario en la base de datos: " + user.getUsername());
            User savedUser = userRepository.save(user);
            logger.info("Usuario guardado con ID: " + savedUser.getId());

            // Publicar evento de usuario creado a Event Grid
            boolean eventPublished = false;
            try {
                eventPublished = publishUserCreatedEvent(savedUser);
                if (!eventPublished) {
                    logger.warning("No se pudo publicar el evento de usuario creado a Event Grid");
                }
            } catch (Exception e) {
                logger.warning("Error al publicar evento de usuario creado: " + e.getMessage());
                // Continuamos aunque falle la publicación del evento
            }

            // Preparar mensaje de respuesta
            StringBuilder responseMessage = new StringBuilder();
            responseMessage.append("Usuario creado exitosamente con ID: ").append(savedUser.getId());
            if (eventPublished) {
                responseMessage.append(". Evento publicado a Event Grid para asignación de rol.");
            } else {
                responseMessage.append(". ADVERTENCIA: No se pudo publicar el evento para asignación de rol.");
            }

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(savedUser))
                    .build();
        } catch (Exception e) {
            logger.severe("Error al crear usuario: " + e.getMessage());
            e.printStackTrace();
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
            userToUpdate.setId(userId.toString());

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
    
    /**
     * Publica un evento de usuario creado en Event Grid
     * 
     * @param user El usuario creado
     * @return true si el evento se publicó con éxito, false en caso contrario
     */
    private boolean publishUserCreatedEvent(User user) {
        Map<String, String> env = System.getenv();
        String topicEndpoint = env.getOrDefault(EVENT_GRID_TOPIC_ENV, "");
        String topicKey = env.getOrDefault(EVENT_GRID_KEY_ENV, "");
        
        if (topicEndpoint.isEmpty() || topicKey.isEmpty()) {
            logger.warning("No se encontraron las variables de entorno para Event Grid. " +
                           "Configure " + EVENT_GRID_TOPIC_ENV + " y " + EVENT_GRID_KEY_ENV);
            return false;
        }
        
        try {
            // Crear el cliente de Event Grid con la configuración
            EventGridPublisher publisher = new EventGridPublisher(topicEndpoint, topicKey);
            
            // El asunto del evento será "users/{id}"
            String subject = "users/" + user.getId();
            
            // Publicar el evento con el tipo, asunto y los datos del usuario
            boolean success = publisher.publishEvent(USER_CREATED_EVENT_TYPE, subject, user);
            
            if (success) {
                logger.info("Evento de usuario creado publicado con éxito para el usuario: " + user.getUsername());
            } else {
                logger.warning("No se pudo publicar el evento de usuario creado para: " + user.getUsername());
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Error al publicar evento de usuario creado: " + e.getMessage());
            return false;
        }
    }
}