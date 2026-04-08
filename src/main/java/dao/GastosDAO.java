package dao;
import Model.Notificacion;

import Model.Gasto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Maneja los gastos y egresos del negocio.
// Lo uso para calcular el balance real en el dashboard: ventas - gastos.
public class GastosDAO {

    // Devuelve todos los gastos ordenados por fecha descendente.
    public List<Gasto> obtenerTodos() {
        List<Gasto> lista = new ArrayList<>();
        String sql = "SELECT IdGasto, Descripcion, Monto, Categoria, Fecha, Notas FROM Gastos ORDER BY Fecha DESC";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { System.err.println("Error al obtener gastos: " + e.getMessage()); }
        return lista;
    }

    // Filtra gastos por texto (descripción/categoría) y/o mes (yyyy-MM).
    public List<Gasto> buscar(String texto, String mes) {
        List<Gasto> lista = new ArrayList<>();
        String sql = "SELECT IdGasto, Descripcion, Monto, Categoria, Fecha, Notas FROM Gastos WHERE 1=1";
        if (texto != null && !texto.trim().isEmpty()) sql += " AND (Descripcion LIKE ? OR Categoria LIKE ?)";
        if (mes   != null && !mes.trim().isEmpty())   sql += " AND Fecha LIKE ?";
        sql += " ORDER BY Fecha DESC";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            if (texto != null && !texto.trim().isEmpty()) {
                ps.setString(i++, "%" + texto.trim() + "%");
                ps.setString(i++, "%" + texto.trim() + "%");
            }
            if (mes != null && !mes.trim().isEmpty()) ps.setString(i, mes + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (Exception e) { System.err.println("Error al buscar gastos: " + e.getMessage()); }
        return lista;
    }

    // Registra un gasto nuevo. Dispara notificación instantánea al guardar.
    public boolean registrar(Gasto g) {
        String sql = "INSERT INTO Gastos(Descripcion, Monto, Categoria, Fecha, Notas) VALUES(?,?,?,?,?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getDescripcion());
            ps.setDouble(2, g.getMonto());
            ps.setString(3, g.getCategoria());
            ps.setString(4, g.getFecha());
            ps.setString(5, g.getNotas());
            ps.executeUpdate();
            utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                "Gasto Registrado",
                g.getDescripcion() + " — $" + String.format("%.2f", g.getMonto()),
                Model.Notificacion.Tipo.COBRO));
            return true;
        } catch (Exception e) { System.err.println("Error al registrar gasto: " + e.getMessage()); return false; }
    }

    // Actualiza un gasto existente.
    public boolean editar(Gasto g) {
        String sql = "UPDATE Gastos SET Descripcion=?, Monto=?, Categoria=?, Fecha=?, Notas=? WHERE IdGasto=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getDescripcion());
            ps.setDouble(2, g.getMonto());
            ps.setString(3, g.getCategoria());
            ps.setString(4, g.getFecha());
            ps.setString(5, g.getNotas());
            ps.setInt(6, g.getIdGasto());
            ps.executeUpdate();
            return true;
        } catch (Exception e) { System.err.println("Error al editar gasto: " + e.getMessage()); return false; }
    }

    // Elimina un gasto por su ID.
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Gastos WHERE IdGasto=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate(); return true;
        } catch (Exception e) { System.err.println("Error al eliminar gasto: " + e.getMessage()); return false; }
    }

        // Total de gastos en un mes específico (formato 'yyyy-MM').
    public double totalMes(String mes) {
        String sql = "SELECT COALESCE(SUM(Monto),0) FROM Gastos WHERE Fecha LIKE ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mes + "%");
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (Exception e) { return 0; }
    }

        // Total de gastos del día de hoy.
    public double totalHoy(String hoy) {
        String sql = "SELECT COALESCE(SUM(Monto),0) FROM Gastos WHERE Fecha LIKE ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hoy + "%");
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (Exception e) { return 0; }
    }

    private Gasto mapear(ResultSet rs) throws SQLException {
        Gasto g = new Gasto();
        g.setIdGasto(rs.getInt("IdGasto"));
        g.setDescripcion(rs.getString("Descripcion"));
        g.setMonto(rs.getDouble("Monto"));
        g.setCategoria(rs.getString("Categoria"));
        g.setFecha(rs.getString("Fecha"));
        g.setNotas(rs.getString("Notas"));
        return g;
    }
}
