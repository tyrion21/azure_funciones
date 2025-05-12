package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.util.Optional;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Función para probar la publicación de eventos a Event Grid
 */
public class TestEventGridFunction {
    private static final Logger logger = Logger.getLogger(TestEventGridFunction.class.getName());
    
    // Configuración para Event Grid (igual que en UserFunction)
    private static final String EVENT_GRID_TOPIC_ENV = "EVENT_GRID_TOPIC_ENDPOINT";
    private static final String EVENT_GRID_KEY_ENV = "EVENT_GRID_TOPIC_KEY";
    private static final String TEST_EVENT_TYPE = "test/connection";

    @FunctionName("testPublishEvent")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, 
                      authLevel = AuthorizationLevel.ANONYMOUS, 
                      route = "test-event-grid") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Iniciando prueba de publicación a Event Grid");
        StringBuilder response = new StringBuilder();
        
        // Obtener configuración de Event Grid
        Map<String, String> env = System.getenv();
        String topicEndpoint = env.getOrDefault(EVENT_GRID_TOPIC_ENV, "");
        String topicKey = env.getOrDefault(EVENT_GRID_KEY_ENV, "");
        
        response.append("Prueba de publicación a Event Grid:\n\n");
        
        // Verificar configuración
        response.append("1. Verificación de configuración:\n");
        response.append("   - EVENT_GRID_TOPIC_ENDPOINT: ");
        response.append(topicEndpoint.isEmpty() ? "NO CONFIGURADO" : "CONFIGURADO").append("\n");
        
        response.append("   - EVENT_GRID_TOPIC_KEY: ");
        response.append(topicKey.isEmpty() ? "NO CONFIGURADO" : "CONFIGURADO").append("\n\n");
        
        if (topicEndpoint.isEmpty() || topicKey.isEmpty()) {
            response.append("ERROR: Variables de entorno de Event Grid no están configuradas correctamente.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(response.toString())
                    .build();
        }
        
        // Datos de prueba
        TestData testData = new TestData();
        testData.id = "test-id-123";
        testData.message = "Mensaje de prueba";
        testData.timestamp = System.currentTimeMillis();
        
        // Intentar publicar evento
        response.append("2. Intento de publicación:\n");
        try {
            EventGridPublisher publisher = new EventGridPublisher(topicEndpoint, topicKey);
            boolean success = publisher.publishEvent(TEST_EVENT_TYPE, "test/event", testData);
            
            if (success) {
                response.append("   EXITOSO: Evento publicado correctamente a Event Grid");
            } else {
                response.append("   FALLIDO: No se pudo publicar el evento a Event Grid");
            }
        } catch (Exception e) {
            logger.severe("Error al publicar evento de prueba: " + e.getMessage());
            response.append("   ERROR: ").append(e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response.toString())
                    .build();
        }
        
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body(response.toString())
                .build();
    }
    
    // Clase para datos de prueba
    private static class TestData {
        public String id;
        public String message;
        public long timestamp;
    }
}
