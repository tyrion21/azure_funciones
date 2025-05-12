package com.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class OracleDBConnection {
    private static final Logger logger = Logger.getLogger(OracleDBConnection.class.getName());

    public static boolean testConnection() {
        try {
            logger.info("Iniciando prueba de conexión a Oracle...");
            
            Connection conn = getConnection();
            if (conn != null) {
                boolean isValid = conn.isValid(5);
                logger.info("Conexión a Oracle exitosa: " + isValid);
                conn.close();
                return isValid;
            } else {
                logger.severe("La conexión a Oracle es nula");
                return false;
            }
        } catch (SQLException e) {
            logger.severe("Error SQL al conectar a Oracle: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e) {
            logger.severe("Error de configuración: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            logger.severe("Error inesperado al conectar a Oracle: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        String tnsName = System.getenv("ORACLE_TNS_NAME");
        String user = System.getenv("ORACLE_USER");
        String password = System.getenv("ORACLE_PASSWORD");
        String walletPath = System.getenv("ORACLE_WALLET_PATH");

        // Validar variables de entorno requeridas con logging detallado
        logger.info("Validando variables de entorno para conexión Oracle:");
        logger.info("TNS_NAME: " + (tnsName != null ? tnsName : "NO CONFIGURADO"));
        logger.info("ORACLE_USER: " + (user != null ? user : "NO CONFIGURADO"));
        // No mostrar contraseña por seguridad
        logger.info("ORACLE_PASSWORD: " + (password != null ? "CONFIGURADO" : "NO CONFIGURADO"));
        logger.info("ORACLE_WALLET_PATH: " + (walletPath != null ? walletPath : "NO CONFIGURADO"));

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
        
        // Verificar que la carpeta del wallet existe
        java.io.File walletDir = new java.io.File(walletPath);
        if (!walletDir.exists() || !walletDir.isDirectory()) {
            String msg = "Oracle Wallet directory does not exist: " + walletPath;
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }
        
        // Listar archivos en la carpeta del wallet para verificar contenido
        logger.info("Contenido de la carpeta del wallet:");
        String[] walletFiles = walletDir.list();
        if (walletFiles != null) {
            for (String file : walletFiles) {
                logger.info(" - " + file);
            }
        } else {
            logger.warning("No se pudieron listar archivos en la carpeta del wallet");
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
        logger.info("URL: " + url);
        logger.info("Ruta Wallet: " + walletPath);

        try {
            // Primero, verificamos que el driver de Oracle esté cargado
            try {
                Class.forName("oracle.jdbc.OracleDriver");
                logger.info("Driver Oracle cargado correctamente");
            } catch (ClassNotFoundException e) {
                logger.severe("No se pudo cargar el driver Oracle: " + e.getMessage());
                throw new SQLException("Error al cargar el driver Oracle: " + e.getMessage(), e);
            }
            
            Connection conn = DriverManager.getConnection(url, props);
            logger.info("Conexión a Oracle exitosa");
            return conn;
        } catch (SQLException e) {
            logger.severe("Error al conectar a Oracle: " + e.getMessage());
            if (e.getMessage().contains("ClassNotFoundException")) {
                logger.severe("Posible problema con el driver Oracle: asegúrese de que ojdbc8.jar esté en el classpath");
            } else if (e.getMessage().contains("wallet")) {
                logger.severe("Posible problema con Oracle Wallet: verifique la ruta y los archivos del wallet");
            }
            throw e;
        } finally {
            logger.info("Intento de conexión a Oracle finalizado");
        }
    }
}
