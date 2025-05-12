package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.util.Optional;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Función para probar la conexión a Oracle y realizar pruebas básicas de SQL
 */
public class TestConnectionFunction {
    private static final Logger logger = Logger.getLogger(TestConnectionFunction.class.getName());

    @FunctionName("testOracleConnection")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, 
                      authLevel = AuthorizationLevel.ANONYMOUS, 
                      route = "test-connection") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger.info("Iniciando prueba de conexión a Oracle");
        StringBuilder response = new StringBuilder();
        response.append("Resultados de prueba de conexión a Oracle:\n\n");

        // Prueba 1: Verificar conexión simple
        response.append("1. Prueba de conexión básica: ");
        boolean connectionSuccess = OracleDBConnection.testConnection();
        response.append(connectionSuccess ? "EXITOSA" : "FALLIDA").append("\n\n");

        if (!connectionSuccess) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response.toString())
                    .build();
        }

        // Prueba 2: Verificar tablas
        response.append("2. Verificando tablas existentes:\n");
        try (Connection conn = OracleDBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Verificar USERS
            response.append("   - Tabla USERS: ");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM USERS")) {
                if (rs.next()) {
                    response.append("Existe (").append(rs.getInt(1)).append(" registros)\n");
                }
            } catch (SQLException e) {
                response.append("ERROR - ").append(e.getMessage()).append("\n");
            }
            
            // Verificar ROLES
            response.append("   - Tabla ROLES: ");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ROLES")) {
                if (rs.next()) {
                    response.append("Existe (").append(rs.getInt(1)).append(" registros)\n");
                }
            } catch (SQLException e) {
                response.append("ERROR - ").append(e.getMessage()).append("\n");
            }
            
            // Verificar USER_ROLES
            response.append("   - Tabla USER_ROLES: ");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM USER_ROLES")) {
                if (rs.next()) {
                    response.append("Existe (").append(rs.getInt(1)).append(" registros)\n");
                }
            } catch (SQLException e) {
                response.append("ERROR - ").append(e.getMessage()).append("\n");
            }
            
            // Prueba 3: Comprobar estructura de USER_ROLES
            response.append("\n3. Estructura de la tabla USER_ROLES:\n");
            try (ResultSet rs = stmt.executeQuery("SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'USER_ROLES'")) {
                while (rs.next()) {
                    response.append("   - ")
                            .append(rs.getString("COLUMN_NAME"))
                            .append(" (")
                            .append(rs.getString("DATA_TYPE"))
                            .append(")\n");
                }
            } catch (SQLException e) {
                response.append("ERROR - ").append(e.getMessage()).append("\n");
            }
            
            // Prueba 4: Ver variables de entorno (solo nombres para seguridad)
            response.append("\n4. Variables de entorno configuradas:\n");
            String[] envVars = {"ORACLE_TNS_NAME", "ORACLE_USER", "ORACLE_WALLET_PATH", 
                               "EVENT_GRID_TOPIC_ENDPOINT", "EVENT_GRID_TOPIC_KEY"};
            
            for (String var : envVars) {
                String value = System.getenv(var);
                response.append("   - ")
                        .append(var)
                        .append(": ")
                        .append(value != null ? "CONFIGURADA" : "NO CONFIGURADA")
                        .append("\n");
            }
            
        } catch (SQLException e) {
            logger.severe("Error durante la prueba de conexión: " + e.getMessage());
            response.append("\nError grave durante las pruebas: ").append(e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response.toString())
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body(response.toString())
                .build();
    }
}
