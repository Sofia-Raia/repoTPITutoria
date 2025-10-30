package Main;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import Config.DatabaseConnection;


public class TestConexion {
    public static void main(String[] args) {
        System.out.println("--- PRUEBA DE CONEXIÓN JDBC ---");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ Conexión exitosa a la base de datos.");

                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("Usuario conectado: " + metaData.getUserName());
                System.out.println("Base de datos: " + conn.getCatalog());
                System.out.println("URL: " + metaData.getURL());
                System.out.println("Driver: " + metaData.getDriverName() + " v" + metaData.getDriverVersion());
            } else {
                // Este bloque es teóricamente inalcanzable si DatabaseConnection
                // funciona correctamente, pero se mantiene por seguridad.
                System.out.println("❌ No se pudo establecer la conexión.");
            }
        } catch (SQLException e) {
            System.err.println("\n❌ Error al conectar a la base de datos. Verifique el estado de MySQL y las credenciales.");
            System.err.println("Mensaje de error: " + e.getMessage());
            // e.printStackTrace(); // Se omite el stack trace para un mensaje de error más limpio en la consola.
        }
        System.out.println("--- FIN DE PRUEBA ---");
    }
}
