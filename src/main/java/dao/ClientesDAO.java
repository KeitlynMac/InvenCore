package dao;

import Model.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// Acceso a la tabla Clientes.
// Mismas operaciones que ProductoDAO pero para clientes.
public class ClientesDAO {

    // Devuelve todos los clientes ordenados por nombre.
    public List<Cliente> obtenerTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT IdCliente, Cedula_Ruc, Nombre_Apellido, Correo, Telefono, Direccion FROM Clientes ORDER BY Nombre_Apellido";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getInt("IdCliente"));
                c.setCedula(rs.getString("Cedula_Ruc"));
                c.setNombre(rs.getString("Nombre_Apellido"));
                c.setCorreo(rs.getString("Correo"));
                c.setTelefono(rs.getString("Telefono"));
                c.setDireccion(rs.getString("Direccion"));
                lista.add(c);
            }
        } catch (Exception e) {
            System.err.println("Error al obtener clientes: " + e.getMessage());
        }
        return lista;
    }

    // Busca clientes por cédula, nombre o correo.
    public List<Cliente> buscar(String busqueda) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT IdCliente, Cedula_Ruc, Nombre_Apellido, Correo, Telefono, Direccion " +
                "FROM Clientes WHERE Cedula_Ruc LIKE ? OR Nombre_Apellido LIKE ? ORDER BY Nombre_Apellido";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String filtro = "%" + busqueda.trim() + "%";
            ps.setString(1, filtro);
            ps.setString(2, filtro);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("IdCliente"));
                    c.setCedula(rs.getString("Cedula_Ruc"));
                    c.setNombre(rs.getString("Nombre_Apellido"));
                    c.setCorreo(rs.getString("Correo"));
                    c.setTelefono(rs.getString("Telefono"));
                    c.setDireccion(rs.getString("Direccion"));
                    lista.add(c);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar cliente: " + e.getMessage());
        }
        return lista;
    }

    // Guarda un cliente nuevo. También dispara una notificación instantánea.
    public boolean registrar(Cliente c) {
        String sql = "INSERT INTO Clientes(Cedula_Ruc, Nombre_Apellido, Correo, Telefono, Direccion, FechaCreacion) VALUES(?,?,?,?,?,datetime('now'))";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCedula());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getCorreo());
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getDireccion());
            ps.executeUpdate();

            // Notificación instantánea de nuevo cliente
            utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                "👤 Nuevo Cliente",
                "Cliente registrado: " + c.getNombre(),
                Model.Notificacion.Tipo.INFO));
            return true;
        } catch (Exception e) {
            System.err.println("Error al guardar el cliente: " + e.getMessage());
            return false;
        }
    }

    // Actualiza los datos de un cliente existente.
    public boolean editar(Cliente c) {
        String sql = "UPDATE Clientes SET Cedula_Ruc=?, Nombre_Apellido=?, Correo=?, Telefono=?, Direccion=? WHERE IdCliente=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCedula());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getCorreo());
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getDireccion());
            ps.setInt(6, c.getIdCliente());
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error al modificar el cliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un cliente tiene facturas registradas.
     * Útil para evitar eliminar clientes con historial de compras.
     */
        // Revisa si el cliente tiene facturas. Si las tiene, no se puede borrar.
    public boolean tieneFacturas(int idCliente) {
        String sql = "SELECT COUNT(*) FROM Facturas WHERE Cliente_IdCliente = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("Error verificando facturas del cliente: " + e.getMessage());
            return true; // En caso de error, no permitir borrar
        }
    }

    // Elimina un cliente. Solo funciona si no tiene facturas asociadas.
    public boolean eliminar(int id) {
        String sql = "DELETE FROM Clientes WHERE IdCliente=?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error al eliminar cliente: " + e.getMessage());
            return false;
        }
    }
}