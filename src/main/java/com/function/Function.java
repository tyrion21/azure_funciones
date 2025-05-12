package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import java.util.Optional;  

public class Function {
    @FunctionName("testBasicOracleConnection")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS, route = "test-basic-connection") HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {
        
        context.getLogger().info("Ejecutando la función de prueba de conexión a Oracle");

        boolean conectado = OracleDBConnection.testConnection();

        String mensaje = conectado ? "Conexión exitosa a la base de datos Oracle." : "Errors al conectar a la base de datos Oracle.";
        context.getLogger().info(mensaje);

        return request.createResponseBuilder(HttpStatus.OK).body(mensaje).build();
    }
}