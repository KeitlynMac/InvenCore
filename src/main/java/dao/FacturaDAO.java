package dao;

import Model.Factura;
import Model.DetalleFactura;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

// Acceso a la tabla de Facturas.
// La parte más importante es guardarFacturaCompleta(), que usa transacciones
// para que si algo falla, no queden datos a medias.
public class FacturaDAO {

        // Calcula el siguiente número de factura basándose en el último registrado.
    public String obtenerNuevaSerie() {
        String sql = "SELECT COALESCE(MAX(CAST(No_Serie AS INTEGER)), 0) FROM Facturas";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int serie = rs.next() ? rs.getInt(1) : 0;
            return String.format("%05d", serie + 1);
        } catch (Exception e) {
            return "00001";
        }
    }

        // Guarda la factura, sus detalles y actualiza el stock en una sola transacción.
    // Si algo falla en el camino, hace rollback para no dejar datos a medias.
    public boolean guardarFacturaCompleta(Factura factura) {
        Connection conn = null;
        try {
            conn = Conexion.getConnection();
            conn.setAutoCommit(false); // Inicia la transacción (Todo o Nada)

            // 1. Insertar Factura (Añadidas las 9 columnas correctas en orden)
            String sqlFactura = "INSERT INTO Facturas(No_Serie, Cliente_IdCliente, FechaVenta, Monto, Pago, Deuda, Estado, MetodoPago, Fecha_Vencimiento) VALUES(?,?,?,?,?,?,?,?,?)";
            int idFacturaGenerado = 0;

            try (PreparedStatement psFac = conn.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS)) {

                psFac.setString(1, factura.getNoSerie());

                // Si el cliente es 0 (Consumidor Final), enviamos NULL a la BD
                if (factura.getIdCliente() > 0) {
                    psFac.setInt(2, factura.getIdCliente());
                } else {
                    psFac.setNull(2, java.sql.Types.INTEGER);
                }

                psFac.setString(3, factura.getFechaVenta());
                psFac.setDouble(4, factura.getMontoTotal());
                psFac.setDouble(5, factura.getPago());             // Dinero real pagado
                psFac.setDouble(6, factura.getDeuda());            // Dinero pendiente
                psFac.setString(7, factura.getEstado());           // "Pagado" o "Por Pagar"
                psFac.setString(8, factura.getMetodoPago());       // "Efectivo", etc.
                psFac.setString(9, factura.getFechaVencimiento()); // Fecha a 10 días

                psFac.executeUpdate();

                try (ResultSet rs = psFac.getGeneratedKeys()) {
                    if (rs.next()) idFacturaGenerado = rs.getInt(1);
                }
            }

            // 2. Insertar Detalles y Actualizar Stock
            String sqlDetalle = "INSERT INTO Detalle_Factura(CantidadProductos, PrecioVenta, Factura_IdFactura, Producto_IdProducto) VALUES(?,?,?,?)";
            String sqlStock = "UPDATE Producto SET Stock = Stock - ? WHERE IdProducto = ?";

            try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle);
                 PreparedStatement psStock = conn.prepareStatement(sqlStock)) {

                for (DetalleFactura det : factura.getDetalles()) {
                    // Guardar en tabla Detalle_Factura
                    psDet.setInt(1, det.getCantidad());
                    psDet.setDouble(2, det.getPrecioVenta());
                    psDet.setInt(3, idFacturaGenerado);
                    psDet.setInt(4, det.getIdProducto());
                    psDet.executeUpdate();

                    // Restar stock en la tabla Producto
                    psStock.setInt(1, det.getCantidad());
                    psStock.setInt(2, det.getIdProducto());
                    psStock.executeUpdate();
                }
            }

            conn.commit(); // Si todo salió bien, guardamos cambios en la DB
            return true;

        } catch (Exception e) {
            System.err.println("Error en transacción de venta: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {} // Si falló, deshacemos todo
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception ex) {}
        }
    }

        // Lista las facturas con filtros opcionales por texto y fecha.
    public java.util.List<Object[]> listarFacturas(String busqueda, String fecha) {
        java.util.List<Object[]> lista = new java.util.ArrayList<>();

        // --- ACTUALIZADO: Agregamos f.MetodoPago y f.Fecha_Vencimiento al SELECT ---
        String sql = "SELECT f.No_Serie, COALESCE(c.Nombre_Apellido, 'Consumidor Final') AS Cliente, " +
                "COALESCE(c.Cedula_Ruc, '9999999999') AS Cedula, f.FechaVenta, f.Monto, f.Estado, " +
                "f.MetodoPago, f.Fecha_Vencimiento, f.Pago, f.Deuda " +
                "FROM Facturas f LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
                "WHERE 1=1 ";

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql += "AND (c.Nombre_Apellido LIKE ? OR c.Cedula_Ruc LIKE ? OR f.No_Serie LIKE ?) ";
        }
        if (fecha != null && !fecha.trim().isEmpty()) {
            sql += "AND f.FechaVenta LIKE ? ";
        }
        sql += "ORDER BY f.IdFactura DESC";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            if (busqueda != null && !busqueda.trim().isEmpty()) {
                String likeBusqueda = "%" + busqueda + "%";
                ps.setString(paramIndex++, likeBusqueda);
                ps.setString(paramIndex++, likeBusqueda);
                ps.setString(paramIndex++, likeBusqueda);
            }
            if (fecha != null && !fecha.trim().isEmpty()) {
                ps.setString(paramIndex++, fecha + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                            rs.getString("No_Serie"),           // 0
                            rs.getString("Cedula"),             // 1
                            rs.getString("Cliente"),            // 2
                            rs.getString("FechaVenta"),         // 3
                            "$" + String.format("%.2f", rs.getDouble("Monto")), // 4 total facturado
                            rs.getString("Estado"),             // 5
                            rs.getString("MetodoPago"),         // 6
                            rs.getString("Fecha_Vencimiento"),  // 7
                            "$" + String.format("%.2f", rs.getDouble("Pago")),  // 8 lo que pagó
                            "$" + String.format("%.2f", rs.getDouble("Deuda"))  // 9 deuda pendiente
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar facturas: " + e.getMessage());
        }
        return lista;
    }

    /** Marca una factura como Pagada y pone deuda en 0. */
        // Marca una factura como pagada y pone la deuda en 0.
    public boolean marcarComoPagada(String noSerie) {
        String sql = "UPDATE Facturas SET Estado='Pagado', Deuda=0, Pago=Monto WHERE No_Serie=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, noSerie);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error al marcar factura como pagada: " + e.getMessage());
            return false;
        }
    }

    /** Lista todas las facturas con estado 'Por Pagar'. */
        // Devuelve solo las facturas con estado 'Por Pagar', ordenadas por vencimiento.
    public java.util.List<Object[]> listarCuentasPorCobrar() {
        java.util.List<Object[]> lista = new java.util.ArrayList<>();
        String sql = "SELECT f.No_Serie, " +
            "COALESCE(c.Nombre_Apellido,'Consumidor Final') AS Cliente, " +
            "COALESCE(c.Cedula_Ruc,'9999999999') AS Cedula, " +
            "f.FechaVenta, " +
            "'$'||printf('%.2f',f.Monto) AS Total, " +
            "'$'||printf('%.2f',f.Pago) AS Pagado, " +
            "'$'||printf('%.2f',f.Deuda) AS Deuda, " +
            "f.Fecha_Vencimiento " +
            "FROM Facturas f " +
            "LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
            "WHERE f.Estado = 'Por Pagar' " +
            "ORDER BY f.Fecha_Vencimiento ASC";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString("No_Serie"),
                    rs.getString("Cliente"),
                    rs.getString("Cedula"),
                    rs.getString("FechaVenta"),
                    rs.getString("Total"),
                    rs.getString("Pagado"),
                    rs.getString("Deuda"),
                    rs.getString("Fecha_Vencimiento")
                });
            }
        } catch (Exception e) {
            System.err.println("Error al listar cuentas por cobrar: " + e.getMessage());
        }
        return lista;
    }

    // Registra un pago parcial o total sobre una factura con deuda.
    // Actualiza Pago y Deuda. Solo cambia Estado a "Pagado" si la deuda llega a 0.
    // También guarda el pago en la tabla PagosDeuda para tener historial de abonos.
    public boolean registrarPagoParcial(String noSerie, double montoPagado) {
        String sqlConsulta = "SELECT Pago, Deuda FROM Facturas WHERE No_Serie = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement psQ = conn.prepareStatement(sqlConsulta)) {
            psQ.setString(1, noSerie);
            ResultSet rs = psQ.executeQuery();
            if (!rs.next()) return false;

            double pagoActual = rs.getDouble("Pago");
            double deudaActual = rs.getDouble("Deuda");

            // No permitir pagar más de lo que se debe
            double pagoReal = Math.min(montoPagado, deudaActual);
            double nuevoPago  = pagoActual + pagoReal;
            double nuevaDeuda = deudaActual - pagoReal;
            String nuevoEstado = nuevaDeuda <= 0.001 ? "Pagado" : "Por Pagar";

            String sqlUpdate = "UPDATE Facturas SET Pago = ?, Deuda = ?, Estado = ? WHERE No_Serie = ?";
            try (PreparedStatement psU = conn.prepareStatement(sqlUpdate)) {
                psU.setDouble(1, nuevoPago);
                psU.setDouble(2, nuevaDeuda);
                psU.setString(3, nuevoEstado);
                psU.setString(4, noSerie);
                psU.executeUpdate();
            }

            // Guardar registro del abono en PagosDeuda
            String fechaPago = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String sqlPago = "INSERT INTO PagosDeuda(Factura_NoSerie, MontoPagado, DeudaAnterior, DeudaNueva, FechaPago) VALUES(?,?,?,?,?)";
            try (PreparedStatement psP = conn.prepareStatement(sqlPago)) {
                psP.setString(1, noSerie);
                psP.setDouble(2, pagoReal);
                psP.setDouble(3, deudaActual);
                psP.setDouble(4, nuevaDeuda);
                psP.setString(5, fechaPago);
                psP.executeUpdate();
            }

            return true;
        } catch (Exception e) {
            System.err.println("registrarPagoParcial: " + e.getMessage());
            return false;
        }
    }

    // Actualiza la ruta del recibo PDF del pago más reciente en PagosDeuda
    public void actualizarRutaRecibo(String noSerie, double montoPagado, String rutaPdf) {
        // SQLite no soporta ORDER BY ni LIMIT en UPDATE directamente — usamos subconsulta
        String sql = "UPDATE PagosDeuda SET RutaRecibo = ? WHERE IdPago = (" +
                     "SELECT IdPago FROM PagosDeuda WHERE Factura_NoSerie = ? AND RutaRecibo IS NULL " +
                     "ORDER BY IdPago DESC LIMIT 1)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rutaPdf);
            ps.setString(2, noSerie);
            ps.executeUpdate();
        } catch (Exception e) { System.err.println("actualizarRutaRecibo: " + e.getMessage()); }
    }

    // Devuelve la deuda actual de una factura
    public double obtenerDeuda(String noSerie) {
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Deuda FROM Facturas WHERE No_Serie = ?")) {
            ps.setString(1, noSerie);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) { return 0; }
    }

    // Devuelve los datos de la factura original para el recibo de pago
    public java.util.Map<String, String> obtenerDatosFactura(String noSerie) {
        java.util.Map<String, String> datos = new java.util.LinkedHashMap<>();
        String sql = "SELECT f.No_Serie, COALESCE(c.Nombre_Apellido,'Consumidor Final') AS Cliente, " +
                     "COALESCE(c.Cedula_Ruc,'9999999999') AS Cedula, " +
                     "COALESCE(c.Correo,'') AS Correo, f.Monto, f.Pago, f.Deuda, f.Estado " +
                     "FROM Facturas f LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
                     "WHERE f.No_Serie = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, noSerie);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                datos.put("noSerie",  rs.getString("No_Serie"));
                datos.put("cliente",  rs.getString("Cliente"));
                datos.put("cedula",   rs.getString("Cedula"));
                datos.put("correo",   rs.getString("Correo"));
                datos.put("monto",    String.valueOf(rs.getDouble("Monto")));
                datos.put("pago",     String.valueOf(rs.getDouble("Pago")));
                datos.put("deuda",    String.valueOf(rs.getDouble("Deuda")));
                datos.put("estado",   rs.getString("Estado"));
            }
        } catch (Exception e) { System.err.println("obtenerDatosFactura: " + e.getMessage()); }
        return datos;
    }


    // Anula una venta completamente:
    //   1. Cambia el Estado a "Anulada" y pone Monto/Pago/Deuda en 0
    //   2. Devuelve el stock de cada producto al inventario
    // Todo en una sola transacción — si algo falla, no queda a medias.
    public boolean anularVenta(String noSerie) {
        String sqlCheck = "SELECT Estado FROM Facturas WHERE No_Serie = ?";
        String sqlAnular = "UPDATE Facturas SET Estado = 'Anulada', Monto = 0, Pago = 0, " +
                           "Deuda = 0, Fecha_Vencimiento = NULL WHERE No_Serie = ?";
        String sqlDetalles = "SELECT Producto_IdProducto, CantidadProductos " +
                             "FROM Detalle_Factura df " +
                             "INNER JOIN Facturas f ON df.Factura_IdFactura = f.IdFactura " +
                             "WHERE f.No_Serie = ?";
        String sqlRestaurarStock = "UPDATE Producto SET Stock = Stock + ? WHERE IdProducto = ?";

        Connection conn = null;
        try {
            conn = Conexion.getConnection();
            conn.setAutoCommit(false);

            // Verificar que no esté ya anulada
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setString(1, noSerie);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next() && "Anulada".equals(rs.getString("Estado"))) {
                    conn.setAutoCommit(true);
                    return false; // ya estaba anulada
                }
            }

            // Restaurar el stock de cada producto vendido en esta factura
            try (PreparedStatement psDet = conn.prepareStatement(sqlDetalles)) {
                psDet.setString(1, noSerie);
                ResultSet rs = psDet.executeQuery();
                try (PreparedStatement psStock = conn.prepareStatement(sqlRestaurarStock)) {
                    while (rs.next()) {
                        psStock.setInt(1, rs.getInt("CantidadProductos"));
                        psStock.setInt(2, rs.getInt("Producto_IdProducto"));
                        psStock.executeUpdate();
                    }
                }
            }

            // Marcar la factura como Anulada y poner todos los montos en 0
            try (PreparedStatement psAnular = conn.prepareStatement(sqlAnular)) {
                psAnular.setString(1, noSerie);
                psAnular.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            System.err.println("anularVenta error: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception ignored) {}
        }
    }


}