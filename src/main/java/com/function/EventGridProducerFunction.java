package com.function;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.function.model.User;
import com.google.gson.Gson;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;
import java.util.logging.Logger;

public class EventGridProducerFunction {
    private static final Logger logger = Logger.getLogger(EventGridProducerFunction.class.getName());
    private final Gson gson = new Gson();


    private static final String HARDCODED_TOPIC_ENDPOINT = "https://topicusuariosroles.eastus2-1.eventgrid.azure.net/api/events";
    private static final String HARDCODED_TOPIC_KEY = "6CEDn1lUeB6Ta2lyCqWpJYDjSnjsxLwWTGbtNXcT55jIZcB1VFjrJQQJ99BDACHYHv6XJ3w3AAABAZEGXz4o";

    @FunctionName("publishUserCreatedEvent")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION, route = "users/create") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Recibida solicitud para crear un usuario y publicar evento");

        // Intentar obtener las variables de entorno primero
        String topicEndpoint = System.getenv("EVENT_GRID_ENDPOINT");
        String topicKey = System.getenv("EVENT_GRID_KEY");
        
        // Si no se encuentran, usar los valores hardcodeados
        if (topicEndpoint == null || topicEndpoint.isEmpty()) {
            logger.warning("Usando endpoint hardcodeado ya que EVENT_GRID_ENDPOINT no está configurado");
            topicEndpoint = HARDCODED_TOPIC_ENDPOINT;
        }
        
        if (topicKey == null || topicKey.isEmpty()) {
            logger.warning("Usando clave hardcodeada ya que EVENT_GRID_KEY no está configurado");
            topicKey = HARDCODED_TOPIC_KEY;
        }
        
        logger.info("Usando endpoint: " + topicEndpoint);
        // NO registrar la clave por seguridad
        
        // Analizar cuerpo de la solicitud
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un objeto usuario en el cuerpo de la solicitud")
                    .build();
        }

        try {
            // Crear usuario a partir del cuerpo de la solicitud
            User user = gson.fromJson(requestBody, User.class);

            // AQUÍ IRÍA LA LÓGICA PARA CREAR EL USUARIO EN LA BASE DE DATOS
            // ... código para guardar el usuario ...

            logger.info("Usuario creado con éxito, publicando evento a Event Grid");

            // Crear cliente publicador de Event Grid
            EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                    .endpoint(topicEndpoint)
                    .credential(new AzureKeyCredential(topicKey))
                    .buildEventGridEventPublisherClient();
            
            // Crear el evento de usuario creado
            EventGridEvent event = new EventGridEvent(
                    "user/created", // Tipo de evento
                    "UserService", // Asunto
                    BinaryData.fromObject(user), // Datos convertidos a formato BinaryData
                    "1.0" // Versión de datos
            );

            // Publicar el evento
            client.sendEvent(event);

            logger.info("Evento de usuario creado publicado con éxito en Event Grid");

            return request.createResponseBuilder(HttpStatus.CREATED)
                    .body("Usuario creado, rol asignado y evento publicado con éxito")
                    .build();
        } catch (Exception e) {
            logger.severe("Error al crear usuario o publicar evento: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage())
                    .build();
        }
    }
}