package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// Consultas de solo lectura para el Dashboard.
// No modifica datos — solo lee para mostrar estadísticas y actividad.
public class DashboardDAO {

    private Connection getConnection() throws java.sql.SQLException {
        return Conexion.getConnection();
    }

    // Cuenta el total de clientes registrados.
    public int totalClientes() { return contar("SELECT COUNT(*) FROM Clientes"); }

    // Cuenta productos con 3 o menos unidades — para la alerta de stock bajo.
    public int productosStockBajo() { return contar("SELECT COUNT(*) FROM Producto WHERE Stock <= 3"); }

    // Usa SUM(Pago) no SUM(Monto) — así mostramos lo realmente cobrado, no lo facturado con deuda
    public double ingresoHoy(String fechaHoy)   { return sumar("SELECT COALESCE(SUM(Pago),0) FROM Facturas WHERE FechaVenta LIKE ?", fechaHoy + "%"); }
    public double ingresoMes(String anioMes)     { return sumar("SELECT COALESCE(SUM(Pago),0) FROM Facturas WHERE FechaVenta LIKE ?", anioMes + "%"); }
    public double ingresoTotal()                 { return sumar("SELECT COALESCE(SUM(Pago),0) FROM Facturas", null); }

    // Ventas del mes agrupadas por día — para el gráfico de líneas del dashboard.
    public List<Object[]> ventasDelMesGrafica(String anioMes) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT substr(FechaVenta,1,10), SUM(Monto) FROM Facturas WHERE FechaVenta LIKE ? GROUP BY substr(FechaVenta,1,10) ORDER BY FechaVenta";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, anioMes + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(new Object[]{rs.getString(1), rs.getDouble(2)});
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // Últimas transacciones para la tabla "Transacciones Recientes".
    // COALESCE en No_Serie por si alguna factura antigua no tiene número asignado.
    public List<Object[]> actividadReciente() {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT COALESCE(f.No_Serie, '#' || CAST(f.IdFactura AS TEXT)) AS NumFac, " +
                     "COALESCE(c.Nombre_Apellido, 'Consumidor Final'), " +
                     "'$' || printf('%.2f', f.Monto), f.Estado " +
                     "FROM Facturas f LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
                     "ORDER BY f.IdFactura DESC LIMIT 15";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Object[]{ rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // Historial mezclado de todas las operaciones del negocio:
    // ventas, gastos, nuevos clientes y nuevos productos.
    // Usa FechaCreacion (si existe) para clientes y productos, o los ordena por ID como proxy.
    public List<Object[]> obtenerHistorialOperaciones() {
        List<Object[]> historial = new ArrayList<>();
        String sql = """
            SELECT 'Venta' AS Tipo,
                   'Factura #' || COALESCE(f.No_Serie, CAST(f.IdFactura AS TEXT)) ||
                   ' - ' || COALESCE(c.Nombre_Apellido, 'Consumidor Final') AS Descripcion,
                   '+ $' || printf('%.2f', f.Pago) AS Valor,
                   COALESCE(f.FechaVenta, '1970-01-01') AS FechaOrden
            FROM Facturas f
            LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente

            UNION ALL

            SELECT 'Gasto' AS Tipo,
                   COALESCE(Descripcion, 'Gasto') AS Descripcion,
                   '- $' || printf('%.2f', Monto) AS Valor,
                   COALESCE(Fecha, '1970-01-01') AS FechaOrden
            FROM Gastos

            UNION ALL

            SELECT 'Cliente' AS Tipo,
                   'Nuevo cliente: ' || COALESCE(Nombre_Apellido, 'Sin nombre') AS Descripcion,
                   '' AS Valor,
                   COALESCE(FechaCreacion, '1970-01-01 00:00:0' || IdCliente) AS FechaOrden
            FROM Clientes

            UNION ALL

            SELECT 'Producto' AS Tipo,
                   'Producto: ' || COALESCE(Nombre, 'Sin nombre') || ' (Stock: ' || COALESCE(CAST(Stock AS TEXT), '0') || ')' AS Descripcion,
                   '$' || printf('%.2f', COALESCE(Precio, 0)) AS Valor,
                   COALESCE(FechaCreacion, '1970-01-01 00:00:0' || IdProducto) AS FechaOrden
            FROM Producto

            ORDER BY FechaOrden DESC
            LIMIT 40
            """;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                historial.add(new Object[]{ rs.getString("Tipo"), rs.getString("Descripcion"), rs.getString("Valor") });
            }
        } catch (Exception e) {
            System.err.println("Error historial operaciones: " + e.getMessage());
        }
        return historial;
    }

    // Top 5 productos más vendidos — para el gráfico de barras.
    public List<Object[]> productosPopulares() {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT p.Nombre, SUM(d.CantidadProductos) as Vendidos " +
                     "FROM Detalle_Factura d INNER JOIN Producto p ON d.Producto_IdProducto = p.IdProducto " +
                     "GROUP BY p.IdProducto ORDER BY Vendidos DESC LIMIT 5";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Object[]{rs.getString(1), rs.getInt(2)});
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Object[]> metodosPagoHoy(String fechaHoy) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT MetodoPago, COUNT(*) FROM Facturas WHERE FechaVenta LIKE ? GROUP BY MetodoPago";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fechaHoy + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(new Object[]{rs.getString(1), rs.getInt(2)});
        } catch (Exception e) {
            // Fallback: agrupar por estado si MetodoPago no existe
            String sql2 = "SELECT Estado, COUNT(*) FROM Facturas WHERE FechaVenta LIKE ? GROUP BY Estado";
            try (Connection c2 = getConnection(); PreparedStatement ps2 = c2.prepareStatement(sql2)) {
                ps2.setString(1, fechaHoy + "%");
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) lista.add(new Object[]{rs2.getString(1), rs2.getInt(2)});
            } catch (Exception e2) {}
        }
        return lista;
    }

    // Helper: ejecuta un COUNT(*) y devuelve el resultado.
    private int contar(String sql) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    // Helper: ejecuta un SUM() con un parámetro opcional.
    private double sumar(String sql, String param) {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (Exception e) { return 0.0; }
    }
}
