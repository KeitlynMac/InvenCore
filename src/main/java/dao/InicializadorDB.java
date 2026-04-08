package dao;

import java.sql.Connection;
import java.sql.Statement;

// Se encarga de crear las tablas de la base de datos la primera vez que se ejecuta el programa.
// También hace migraciones seguras (agrega columnas nuevas sin romper bases de datos existentes).
public class InicializadorDB {

        // Crea todas las tablas si no existen.
    // Las tablas de Producto y Vendedor tienen migraciones al final para bases de datos viejas.
    public void inicializarBaseDeDatos() {

        // Vendedor ahora guarda: usuario, hash SHA-256 de la clave, nombre completo y ruta de foto
        String sqlVendedor = """
            CREATE TABLE IF NOT EXISTS Vendedor (
                IdVendedor     INTEGER PRIMARY KEY AUTOINCREMENT,
                User           TEXT NOT NULL UNIQUE,
                Clave          TEXT NOT NULL,
                NombreCompleto TEXT,
                FotoPath       TEXT
            );
            """;

        String sqlClientes = """
            CREATE TABLE IF NOT EXISTS Clientes (
                IdCliente      INTEGER PRIMARY KEY AUTOINCREMENT,
                Cedula_Ruc     TEXT NOT NULL UNIQUE,
                Nombre_Apellido TEXT NOT NULL,
                Correo         TEXT,
                Telefono       TEXT,
                Direccion      TEXT
            );
            """;

        String sqlProducto = """
            CREATE TABLE IF NOT EXISTS Producto (
                IdProducto INTEGER PRIMARY KEY AUTOINCREMENT,
                Codigo     TEXT NOT NULL UNIQUE,
                Nombre     TEXT NOT NULL,
                Precio     REAL NOT NULL,
                Stock      INTEGER NOT NULL,
                Categoria  TEXT
            );
            """;

        String sqlFacturas = "CREATE TABLE IF NOT EXISTS Facturas ("
                + "IdFactura          INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "No_Serie           TEXT, "
                + "Cliente_IdCliente  INTEGER, "
                + "FechaVenta         TEXT, "
                + "Monto              REAL, "
                + "Pago               REAL, "
                + "Deuda              REAL, "
                + "Estado             TEXT, "
                + "MetodoPago         TEXT, "
                + "Fecha_Vencimiento  TEXT, "
                + "FOREIGN KEY(Cliente_IdCliente) REFERENCES Clientes(IdCliente)"
                + ");";

        String sqlDetalleFactura = """
            CREATE TABLE IF NOT EXISTS Detalle_Factura (
                IdDetalle            INTEGER PRIMARY KEY AUTOINCREMENT,
                CantidadProductos    INTEGER NOT NULL,
                PrecioVenta          REAL NOT NULL,
                Factura_IdFactura    INTEGER,
                Producto_IdProducto  INTEGER,
                FOREIGN KEY(Factura_IdFactura)   REFERENCES Facturas(IdFactura),
                FOREIGN KEY(Producto_IdProducto) REFERENCES Producto(IdProducto)
            );
            """;

        // Tabla de Gastos / Egresos del negocio
        String sqlGastos = """
            CREATE TABLE IF NOT EXISTS Gastos (
                IdGasto     INTEGER PRIMARY KEY AUTOINCREMENT,
                Descripcion TEXT NOT NULL,
                Monto       REAL NOT NULL,
                Categoria   TEXT,
                Fecha       TEXT NOT NULL,
                Notas       TEXT
            );
            """;

        try (Connection conn = Conexion.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlVendedor);
            stmt.execute(sqlClientes);
            stmt.execute(sqlProducto);
            stmt.execute(sqlFacturas);
            stmt.execute(sqlDetalleFactura);
            stmt.execute(sqlGastos);

                    // Migraciones seguras: agrega columnas nuevas a bases de datos existentes.: añadir columnas nuevas si la BD ya existía (usuarios existentes)
            ejecutarSilencioso(stmt, "ALTER TABLE Vendedor ADD COLUMN NombreCompleto TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Vendedor ADD COLUMN FotoPath TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Producto ADD COLUMN Categoria TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Producto ADD COLUMN Descripcion TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Producto ADD COLUMN ImagenPath TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Producto ADD COLUMN FechaVencimiento TEXT");
            // FechaCreacion: para trackear cuándo se registró cada producto y cliente en el historial
            ejecutarSilencioso(stmt, "ALTER TABLE Producto ADD COLUMN FechaCreacion TEXT");
            ejecutarSilencioso(stmt, "ALTER TABLE Clientes ADD COLUMN FechaCreacion TEXT");

            // Tabla de pagos parciales de deuda — registra cada abono con su recibo PDF
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS PagosDeuda (
                    IdPago          INTEGER PRIMARY KEY AUTOINCREMENT,
                    Factura_NoSerie TEXT NOT NULL,
                    MontoPagado     REAL NOT NULL,
                    DeudaAnterior   REAL NOT NULL,
                    DeudaNueva      REAL NOT NULL,
                    FechaPago       TEXT NOT NULL,
                    RutaRecibo      TEXT
                )
            """);

            // Tabla de configuración del sistema (datos del negocio + correo)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Configuracion (
                    Clave TEXT PRIMARY KEY,
                    Valor TEXT
                )
            """);

            // Insertar valores por defecto solo si no existen aún
            String[] defaults = {
                "negocio.nombre", "Tu Negocio",
                "negocio.ruc", "0000000000001",
                "negocio.direccion", "Dirección del negocio",
                "negocio.telefono", "0999999999",
                "negocio.email", "negocio@correo.com",
                "email.remitente", "",
                "email.clave.hash", ""
            };
            for (int i = 0; i < defaults.length; i += 2) {
                stmt.execute("INSERT OR IGNORE INTO Configuracion(Clave,Valor) VALUES('" +
                    defaults[i] + "','" + defaults[i+1] + "')");
            }

            System.out.println("Base de datos inicializada/migrada correctamente.");

        } catch (Exception e) {
            System.err.println("Error crítico inicializando la base de datos: " + e.getMessage());
        }
    }

    /** Ejecuta un SQL ignorando errores de "columna ya existe" (para migraciones). */
    private void ejecutarSilencioso(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception ignored) {
            // Columna ya existe — es esperado en bases de datos ya existentes
        }
    }
}
