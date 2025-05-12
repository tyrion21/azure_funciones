package com.function;

import com.function.model.User;
import com.function.model.Role;
import com.function.repository.UserRepository;
import com.function.repository.RoleRepository;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.logging.Logger;
import java.util.List;
import java.util.Optional;

public class EventGridConsumerFunction {
    private static final Logger logger = Logger.getLogger(EventGridConsumerFunction.class.getName());
    private final Gson gson = new Gson();
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    // Constante para el rol por defecto
    private static final String DEFAULT_ROLE = "USER";
    
    public EventGridConsumerFunction() {
        // Inicializar repositorios
        this.userRepository = new UserRepository();
        this.roleRepository = new RoleRepository();
    }    @FunctionName("processUserEvents")
    public void run(
            @EventGridTrigger(name = "event") String eventGridEvent,
            final ExecutionContext context) {

        logger.info("Función de trigger de Event Grid procesó un evento: " + eventGridEvent);

        try {
            // El EventGridEvent se recibe como una cadena JSON
            EventGridEventData eventData = gson.fromJson(eventGridEvent, EventGridEventData.class);

            // Registrar los detalles del evento
            logger.info("ID de Evento: " + eventData.id);
            logger.info("Tipo de Evento: " + eventData.eventType);
            logger.info("Asunto del Evento: " + eventData.subject);
            logger.info("Tiempo del Evento: " + eventData.eventTime);
            logger.info("Datos del Evento: " + (eventData.data != null ? eventData.data.toString() : "null"));

            // Procesar el evento basado en su tipo
            if ("user/created".equals(eventData.eventType)) {
                // Los datos están en formato JSON, convertirlos a objeto User
                if (eventData.data == null) {
                    logger.severe("Datos del evento user/created son nulos");
                    return;
                }
                
                User user = gson.fromJson(eventData.data.toString(), User.class);
                
                if (user == null || user.getId() == null || user.getUsername() == null) {
                    logger.severe("Datos de usuario inválidos en el evento: " + eventData.data.toString());
                    return;
                }

                logger.info("Procesando evento de creación de usuario para: " + user.getUsername() + " con ID: " + user.getId());

                // Asignar rol por defecto al usuario creado
                if (!OracleDBConnection.testConnection()) {
                    logger.severe("No se pudo establecer conexión con la base de datos");
                    return;
                }
                
                assignDefaultRoleToUser(user);

                logger.info("Notificación de usuario creado procesada con éxito");
            } else if ("user/updated".equals(eventData.eventType)) {
                // Los datos están en formato JSON, convertirlos a objeto User
                User user = gson.fromJson(eventData.data.toString(), User.class);

                logger.info("Procesando evento de actualización de usuario para: " + user.getUsername());

                // Lógica para usuario actualizado

                logger.info("Evento de usuario actualizado procesado con éxito");
            } else if ("role/deleted".equals(eventData.eventType)) {
                // Los datos están en formato JSON, convertirlos a objeto Role
                Role deletedRole = gson.fromJson(eventData.data.toString(), Role.class);
                
                logger.info("Procesando evento de eliminación de rol: " + deletedRole.getName());
                
                // Remover el rol eliminado de todos los usuarios afectados
                removeRoleFromAffectedUsers(deletedRole);
                
                logger.info("Rol eliminado y usuarios actualizados con éxito");
            } else {
                logger.info("Recibido evento de tipo: " + eventData.eventType + " (no procesado)");
            }
        } catch (Exception e) {
            logger.severe("Error al procesar evento: " + e.getMessage());
            e.printStackTrace();
            // En producción, considera utilizar un patrón de reintentos o cola de mensajes
            // muertos en lugar de solo relanzar la excepción
            throw new RuntimeException("Error al procesar evento", e);
        }
    }

    /**
     * Asigna el rol por defecto a un usuario recién creado
     */
    private void assignDefaultRoleToUser(User user) {
        try {
            logger.info("Asignando rol por defecto '" + DEFAULT_ROLE + "' al usuario: " + user.getUsername());
            
            // Obtener el rol por defecto desde la base de datos
            Optional<Role> defaultRoleOpt = roleRepository.findByName(DEFAULT_ROLE);
            
            if (defaultRoleOpt.isPresent()) {
                Role defaultRole = defaultRoleOpt.get();
                logger.info("Rol por defecto encontrado: " + defaultRole.getId() + " - " + defaultRole.getName());
                
                // Asignar el rol al usuario
                roleRepository.assignRoleToUser(Long.parseLong(user.getId()), Long.parseLong(defaultRole.getId()));
                
                // Actualizar el objeto de usuario en memoria para reflejar el cambio
                user.addRole(defaultRole);
                
                logger.info("Rol por defecto asignado correctamente al usuario: " + user.getUsername());
            } else {
                logger.warning("No se encontró el rol por defecto '" + DEFAULT_ROLE + "' en la base de datos");
                
                // Crear rol por defecto si no existe
                Role newDefaultRole = new Role();
                newDefaultRole.setName(DEFAULT_ROLE);
                newDefaultRole.setDescription("Rol por defecto para nuevos usuarios");
                
                Role savedRole = roleRepository.save(newDefaultRole);
                logger.info("Rol por defecto creado con ID: " + savedRole.getId());
                
                // Asignar el rol al usuario
                roleRepository.assignRoleToUser(Long.parseLong(user.getId()), Long.parseLong(savedRole.getId()));
                
                // Actualizar el objeto de usuario en memoria
                user.addRole(savedRole);
                
                logger.info("Nuevo rol por defecto creado y asignado al usuario: " + user.getUsername());
            }
            
        } catch (Exception e) {
            logger.severe("Error al asignar rol por defecto: " + e.getMessage());
            throw new RuntimeException("Error al asignar rol por defecto", e);
        }
    }
    
    /**
     * Remueve el rol eliminado de todos los usuarios afectados
     */
    private void removeRoleFromAffectedUsers(Role deletedRole) {
        try {
            logger.info("Buscando usuarios con el rol eliminado: " + deletedRole.getName());
            
            // Consultar todos los usuarios que tienen el rol eliminado
            List<User> affectedUsers = userRepository.findUsersByRoleId(Long.parseLong(deletedRole.getId()));
            logger.info("Se encontraron " + affectedUsers.size() + " usuarios afectados por la eliminación del rol: " + deletedRole.getName());
            
            // Actualizar cada usuario eliminando la referencia al rol
            for (User user : affectedUsers) {
                // Eliminar la relación en la base de datos
                roleRepository.removeRoleFromUser(Long.parseLong(user.getId()), Long.parseLong(deletedRole.getId()));
                
                // Actualizar el objeto en memoria
                user.removeRole(deletedRole);
                
                logger.info("Rol eliminado del usuario: " + user.getUsername() + " (ID: " + user.getId() + ")");
                
                // Si el usuario se queda sin roles, podría asignarle otro rol por defecto
                if (user.getRoles() == null || user.getRoles().isEmpty()) {
                    logger.info("El usuario " + user.getUsername() + " se ha quedado sin roles. Asignando rol por defecto alternativo.");
                    
                    // Podríamos asignar un rol alternativo aquí, como GUEST
                    // O simplemente llamar al método que asigna el rol por defecto
                    assignDefaultRoleToUser(user);
                }
            }
            
            logger.info("Todos los usuarios afectados han sido actualizados correctamente");
            
        } catch (Exception e) {
            logger.severe("Error al actualizar usuarios afectados: " + e.getMessage());
            throw new RuntimeException("Error al actualizar usuarios después de eliminar rol", e);
        }
    }

    // Clase auxiliar para deserializar eventos de Event Grid
    private static class EventGridEventData {
        public String id;
        public String eventType;
        public String subject;
        public String eventTime;
        public Object data;
        public String dataVersion;
        public String metadataVersion;
        public String topic;
    }
}