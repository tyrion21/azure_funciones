package com.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Utilidad para publicar eventos a Azure Event Grid
 */
public class EventGridPublisher {
    private static final Logger logger = Logger.getLogger(EventGridPublisher.class.getName());
    private final String topicEndpoint;
    private final String topicKey;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Constructor con los datos de conexión al topic de Event Grid
     * 
     * @param topicEndpoint El endpoint del topic de Event Grid
     * @param topicKey      La clave de acceso al topic
     */
    public EventGridPublisher(String topicEndpoint, String topicKey) {
        this.topicEndpoint = topicEndpoint;
        this.topicKey = topicKey;
    }

    /**
     * Publica un evento en Event Grid
     * 
     * @param eventType  El tipo de evento (ej: user/created)
     * @param subject    El asunto del evento (ej: users/123)
     * @param data       El objeto de datos que se serializará a JSON
     * @return           true si el evento se publicó con éxito, false en caso contrario
     */    public boolean publishEvent(String eventType, String subject, Object data) {
        logger.info("Intentando publicar evento: " + eventType + ", subject: " + subject);

        try {
            // Crear el array de eventos (Event Grid acepta arrays de eventos)
            JsonObject[] events = new JsonObject[1];
            JsonObject event = new JsonObject();
            
            // Crear el evento con los campos requeridos por Event Grid
            String eventId = UUID.randomUUID().toString();
            event.addProperty("id", eventId);
            event.addProperty("eventType", eventType);
            event.addProperty("subject", subject);
            event.addProperty("eventTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            event.addProperty("dataVersion", "1.0");
            event.add("data", gson.toJsonTree(data));
            events[0] = event;
            
            // Convertir el array de eventos a JSON
            String jsonEvents = gson.toJson(events);
            logger.info("Evento JSON: " + jsonEvents);
            
            // Verificar que el endpoint esté correctamente formateado
            if (!topicEndpoint.startsWith("https://")) {
                logger.severe("URL de Event Grid inválida: " + topicEndpoint);
                return false;
            }
            
            logger.info("Enviando petición HTTP POST a: " + topicEndpoint);
            
            // Crear la petición HTTP POST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(topicEndpoint))
                    .header("Content-Type", "application/json")
                    .header("aeg-sas-key", topicKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonEvents))
                    .build();
            
            // Enviar la petición y obtener la respuesta
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Verificar si la petición fue exitosa (código 200)
            boolean success = (response.statusCode() >= 200 && response.statusCode() < 300);
            
            if (success) {
                logger.info("Evento ID " + eventId + " publicado con éxito. Código: " + response.statusCode());
            } else {
                logger.severe("Error al publicar evento. Código: " + response.statusCode() + " Respuesta: " + response.body());
            }
            
            return success;
        } catch (IOException | InterruptedException e) {
            logger.severe("Error al publicar evento: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            logger.severe("Error inesperado al publicar evento: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
