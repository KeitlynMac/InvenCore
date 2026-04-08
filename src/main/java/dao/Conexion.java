package dao;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.Connection;
import java.sql.SQLException;

// Maneja el pool de conexiones a la base de datos SQLite.
// Uso un pool (DBCP2) para que no tengamos que abrir y cerrar conexiones a mano
// cada vez. Mucho más eficiente y evita problemas de archivos bloqueados.
// Para usarlo: try (Connection conn = Conexion.getConnection()) { ... }
public class Conexion {

    private static final BasicDataSource dataSource;

    static {
        // 1. Definimos la ruta segura en la carpeta del usuario (Documentos/Perfil)
        String userHome = System.getProperty("user.home");
        String carpetaDestino = userHome + java.io.File.separator + "Ivencore";

        // 2. Nos aseguramos de que la carpeta exista ANTES de conectar
        java.io.File directorio = new java.io.File(carpetaDestino);
        if (!directorio.exists()) {
            directorio.mkdirs(); // Crea la carpeta si es la primera vez que se abre el programa
        }

        // 3. Definimos la ruta final del archivo .db
        String dbPath = carpetaDestino + java.io.File.separator + "BaseNegocio.db";

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        // WAL mode permite lecturas concurrentes en SQLite
        dataSource.setUrl("jdbc:sqlite:" + dbPath + "?journal_mode=WAL&busy_timeout=5000");
        dataSource.setInitialSize(1);
        dataSource.setMaxTotal(5);
        dataSource.setMaxIdle(3);
        dataSource.setMinIdle(1);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setMaxWaitMillis(10_000);
    }

    /**
     * Obtiene una conexión del pool. Debe cerrarse con try-with-resources
     * para que sea devuelta al pool automáticamente.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * @deprecated Usar getConnection() con try-with-resources en su lugar.
     * Mantenido por compatibilidad con código existente.
     */
    @Deprecated
    public Connection establecerConexion() {
        try {
            return getConnection();
        } catch (SQLException e) {
            System.err.println("Error al obtener conexión del pool: " + e.getMessage());
            return null;
        }
    }

    /**
     * @deprecated No-op: las conexiones se devuelven al pool automáticamente
     * cuando se cierran via try-with-resources. Mantenido por compatibilidad.
     */
    @Deprecated
    public void cerrarConexion() {
        // No-op intencional: el pool gestiona el ciclo de vida de las conexiones.
    }

    /** Cierra el pool de conexiones. Llamar antes de restaurar un backup. */
    public static void cerrarPool() {
        try { if (Conexion.dataSource != null && !Conexion.dataSource.isClosed()) Conexion.dataSource.close(); }
        catch (Exception e) { System.err.println("Error cerrando pool: " + e.getMessage()); }
    }
}

