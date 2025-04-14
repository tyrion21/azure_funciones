package com.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class OracleDBConnection {
    private static final Logger logger = Logger.getLogger(OracleDBConnection.class.getName());

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn != null && conn.isValid(5);
            logger.info("Conexión a Oracle exitosa: " + isValid);
            return isValid;
        } catch (SQLException e) {
            logger.severe("Error al conectar a Oracle: " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            logger.severe("Error de configuración: " + e.getMessage());
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        String tnsName = System.getenv("ORACLE_TNS_NAME");
        String user = System.getenv("ORACLE_USER");
        String password = System.getenv("ORACLE_PASSWORD");
        String walletPath = System.getenv("ORACLE_WALLET_PATH");

        // Validate required environment variables
        if (tnsName == null || tnsName.isEmpty()) {
            String msg = "TNS_NAME environment variable is not set";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        if (user == null || user.isEmpty()) {
            String msg = "ORACLE_USER environment variable is not set";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        if (password == null || password.isEmpty()) {
            String msg = "ORACLE_PASSWORD environment variable is not set";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        if (walletPath == null || walletPath.isEmpty()) {
            String msg = "ORACLE_WALLET_PATH environment variable is not set";
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        String url = "jdbc:oracle:thin:@" + tnsName + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.net.ssl_version", "1.2");
        props.setProperty("oracle.net.wallet_location",
                "(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=" + walletPath + ")))");

        logger.info("Intentando conectar con oracle...");
        logger.info("TNS: " + tnsName);
        logger.info("User: " + user);
        logger.info("Ruta Wallet: " + walletPath);

        try {
            Connection conn = DriverManager.getConnection(url, props);
            logger.info("Conexión a Oracle exitosa");
            return conn;
        } catch (SQLException e) {
            logger.severe("Error al conectar a Oracle: " + e.getMessage());
            throw e;
        } finally {
            logger.info("Intento de conexión a Oracle finalizado");
        }
    }
}
