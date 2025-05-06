package com.function;

import com.function.model.User;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.logging.Logger;

public class EventGridConsumerFunction {
    private static final Logger logger = Logger.getLogger(EventGridConsumerFunction.class.getName());
    private final Gson gson = new Gson();

    @FunctionName("processUserEvents")
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

            // Procesar el evento basado en su tipo
            if ("user/created".equals(eventData.eventType)) {
                // Los datos están en formato JSON, convertirlos a objeto User
                User user = gson.fromJson(eventData.data.toString(), User.class);

                logger.info("Procesando evento de creación de usuario para: " + user.getUsername());

                // AQUÍ VA LA LÓGICA DE NEGOCIO PARA MANEJAR LA CREACIÓN
                // Por ejemplo: enviar email de bienvenida, crear recursos relacionados, etc.

                logger.info("Notificación de usuario creado procesada con éxito");
            } else if ("user/updated".equals(eventData.eventType)) {
                // Los datos están en formato JSON, convertirlos a objeto User
                User user = gson.fromJson(eventData.data.toString(), User.class);

                logger.info("Procesando evento de actualización de usuario para: " + user.getUsername());

                // Lógica para usuario actualizado

                logger.info("Evento de usuario actualizado procesado con éxito");
            } else {
                logger.info("Recibido evento de tipo: " + eventData.eventType + " (no procesado)");
            }
        } catch (Exception e) {
            logger.severe("Error al procesar evento: " + e.getMessage());
            // En producción, considera utilizar un patrón de reintentos o cola de mensajes
            // muertos
            // en lugar de solo relanzar la excepción
            throw new RuntimeException("Error al procesar evento", e);
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