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

public class GraphQLQueryFunction {
    private static final Logger logger = Logger.getLogger(GraphQLQueryFunction.class.getName());
    private final GraphQLProvider graphQLProvider;
    private final Gson gson;

    public GraphQLQueryFunction() {
        this.graphQLProvider = new GraphQLProvider();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @FunctionName("graphql")
    public HttpResponseMessage execute(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, 
                         route = "graphql") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        logger.info("Processing GraphQL request");
        
        String requestBody = request.getBody().orElse("");
        if (requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Por favor proporciona un cuerpo de solicitud GraphQL válido")
                    .build();
        }

        try {
            GraphQLRequest graphQLRequest = gson.fromJson(requestBody, GraphQLRequest.class);
            
            if (graphQLRequest.getQuery() == null || graphQLRequest.getQuery().trim().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("La consulta GraphQL no puede estar vacía")
                        .build();
            }
            
            Map<String, Object> variables = graphQLRequest.getVariables() != null 
                ? graphQLRequest.getVariables() 
                : new HashMap<>();
            
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(graphQLRequest.getQuery())
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
            logger.severe("Error procesando solicitud GraphQL: " + e.getMessage());
            
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