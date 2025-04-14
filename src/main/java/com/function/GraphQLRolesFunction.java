package com.function;

import com.function.graphql.GraphQLProvider;
import com.function.graphql.GraphQLRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

public class GraphQLRolesFunction {
    private static final Logger logger = Logger.getLogger(GraphQLRolesFunction.class.getName());
    private final GraphQLProvider graphQLProvider;
    private final Gson gson;

    public GraphQLRolesFunction() {
        this.graphQLProvider = new GraphQLProvider();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @FunctionName("graphql-roles")
    public HttpResponseMessage execute(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, 
                         route = "graphql/roles") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Processing GraphQL Roles request");
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un cuerpo de solicitud GraphQL válido")
                    .build();
        }

        try {
            GraphQLRequest graphQLRequest = gson.fromJson(requestBody, GraphQLRequest.class);
            
            // Validar que la consulta sea sobre roles
            String query = graphQLRequest.getQuery();
            if (query == null || query.trim().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("La consulta GraphQL no puede estar vacía")
                        .build();
            }
            
            // Este endpoint es específico para roles, podríamos agregar validación adicional aquí
            // para asegurar que solo se ejecutan queries relacionadas con roles
            
            Map<String, Object> variables = graphQLRequest.getVariables() != null 
                ? graphQLRequest.getVariables() 
                : new HashMap<>();
            
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(graphQLRequest.getOperationName())
                .variables(variables)
                .build();
            
            ExecutionResult executionResult = graphQLProvider.getGraphQL().execute(executionInput);
            
            Map<String, Object> responseMap = new HashMap<>();
            if (!executionResult.getErrors().isEmpty()) {
                responseMap.put("errors", executionResult.getErrors());
            }
            responseMap.put("data", executionResult.getData());
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(responseMap))
                    .build();
        } catch (Exception e) {
            logger.severe("Error procesando solicitud GraphQL de roles: " + e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Error interno del servidor: " + e.getMessage());
            errorResponse.put("errors", new Object[]{error});
            
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(gson.toJson(errorResponse))
                    .build();
        }
    }
}