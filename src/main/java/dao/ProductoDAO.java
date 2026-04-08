package dao;

import Model.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Acceso a la tabla Producto.
// Operaciones básicas: buscar, registrar, editar, eliminar.
// La búsqueda filtra por nombre, código Y categoría al mismo tiempo.
public class ProductoDAO {

    // Devuelve todos los productos sin filtro.
    public List<Producto> obtenerTodos() {
        return buscar("");
    }

        // Busca productos por nombre, código o categoría. Si el texto está vacío, devuelve todos.
    public List<Producto> buscar(String busqueda) {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT IdProducto,Codigo,Nombre,Precio,Stock,Categoria,Descripcion,ImagenPath,FechaVencimiento " +
                     "FROM Producto WHERE Nombre LIKE ? OR Codigo LIKE ? OR Categoria LIKE ? ORDER BY Nombre";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String f = "%" + (busqueda != null ? busqueda.trim() : "") + "%";
            ps.setString(1, f); ps.setString(2, f); ps.setString(3, f);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(mapear(rs)); }
        } catch (Exception e) { System.err.println("Error al buscar producto: " + e.getMessage()); }
        return lista;
    }

    // Guarda un producto nuevo y genera notificación instantánea.
    public boolean registrar(Producto p) {
        String sql = "INSERT INTO Producto(Codigo,Nombre,Precio,Stock,Categoria,Descripcion,ImagenPath,FechaVencimiento,FechaCreacion) " +
                "VALUES(?,?,?,?,?,?,?,?,datetime('now'))";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setDouble(3, p.getPrecio());
            ps.setInt(4, p.getStock());
            ps.setString(5, p.getCategoria());
            ps.setString(6, p.getDescripcion());
            ps.setString(7, p.getImagenPath());
            ps.setString(8, p.getFechaVencimiento());
            ps.executeUpdate();

            // Cambia esa línea por esta:
            utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                    "Nuevo Producto",
                    "Producto registrado: " + p.getNombre(),
                    Model.Notificacion.Tipo.STOCK)); // Ahora el Dashboard verá "STOCK" y lo pondrá naranja

            return true;

        } catch (Exception e) {
            System.err.println("Error al guardar producto: " + e.getMessage());
            return false;
        }
    }

        // Actualiza todos los campos de un producto existente.
    public boolean editar(Producto p) {
        String sql = "UPDATE Producto SET Codigo=?,Nombre=?,Precio=?,Stock=?,Categoria=?," +
                     "Descripcion=?,ImagenPath=?,FechaVencimiento=? WHERE IdProducto=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setDouble(3, p.getPrecio());
            ps.setInt(4, p.getStock());
            ps.setString(5, p.getCategoria());
            ps.setString(6, p.getDescripcion());
            ps.setString(7, p.getImagenPath());
            ps.setString(8, p.getFechaVencimiento());
            ps.setInt(9, p.getIdProducto());
            ps.executeUpdate();
            return true;
        } catch (Exception e) { System.err.println("Error al modificar producto: " + e.getMessage()); return false; }
    }

        // Elimina un producto. Antes de llamar esto, verifica con tieneVentas().
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Producto WHERE IdProducto=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate(); return true;
        } catch (Exception e) { System.err.println("Error al eliminar: " + e.getMessage()); return false; }
    }

        // Verifica si el producto aparece en alguna factura. Si es así, no lo podemos borrar.
    public boolean tieneVentas(int idProducto) {
        String sql = "SELECT COUNT(*) FROM Detalle_Factura WHERE Producto_IdProducto=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (Exception e) { return true; }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("IdProducto"));
        p.setCodigo(rs.getString("Codigo"));
        p.setNombre(rs.getString("Nombre"));
        p.setPrecio(rs.getDouble("Precio"));
        p.setStock(rs.getInt("Stock"));
        p.setCategoria(rs.getString("Categoria"));
        p.setDescripcion(rs.getString("Descripcion"));
        p.setImagenPath(rs.getString("ImagenPath"));
        p.setFechaVencimiento(rs.getString("FechaVencimiento"));
        return p;
    }
}
