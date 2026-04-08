package dao;

import Model.Usuario;
import utilities.LicenciaManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Maneja todo lo relacionado con los usuarios del sistema.
// Autenticación, registro y actualización de perfil.
// Importante: las contraseñas siempre se guardan como hash SHA-256, nunca en texto plano.
public class UsuarioDAO {

    /**
     * Autentica al usuario comparando el hash SHA-256 de la clave ingresada
     * con el hash almacenado en la base de datos.
     * Retorna el objeto Usuario completo si las credenciales son correctas, null si no.
     */
        // Verifica las credenciales comparando el hash de la clave ingresada con el de la BD.
    // Retorna el objeto Usuario completo si es correcto, o null si no.
    public Usuario autenticar(String user, String claveTextoPlano) {
        String hashIngresado = LicenciaManager.sha256(claveTextoPlano);
        String sql = "SELECT IdVendedor, User, NombreCompleto, FotoPath FROM Vendedor WHERE User = ? AND Clave = ?";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, hashIngresado);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdVendedor(rs.getInt("IdVendedor"));
                    u.setUser(rs.getString("User"));
                    u.setNombreCompleto(rs.getString("NombreCompleto"));
                    u.setFotoPath(rs.getString("FotoPath"));
                    return u;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al autenticar: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registra un nuevo usuario con la clave ya hasheada en SHA-256.
     */
        // Registra un usuario nuevo. Hashea la clave antes de guardarla.
    public boolean registrar(String user, String claveTextoPlano, String nombreCompleto, String fotoPath) {
        String hash = LicenciaManager.sha256(claveTextoPlano);
        String sql = "INSERT INTO Vendedor (User, Clave, NombreCompleto, FotoPath) VALUES (?, ?, ?, ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.trim());
            ps.setString(2, hash);
            ps.setString(3, nombreCompleto != null ? nombreCompleto.trim() : "");
            ps.setString(4, fotoPath);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza el perfil del usuario (nombre, foto).
     * Si nuevaClave no está vacía, también cambia la contraseña.
     */
        // Actualiza el nombre y foto de perfil. Si el usuario cambió su clave, también la actualiza.
    public boolean actualizarPerfil(int idVendedor, String nombreCompleto, String fotoPath, String nuevaClave) {
        try (Connection conn = Conexion.getConnection()) {
            if (nuevaClave != null && !nuevaClave.trim().isEmpty()) {
                String sql = "UPDATE Vendedor SET NombreCompleto=?, FotoPath=?, Clave=? WHERE IdVendedor=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nombreCompleto);
                    ps.setString(2, fotoPath);
                    ps.setString(3, LicenciaManager.sha256(nuevaClave));
                    ps.setInt(4, idVendedor);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE Vendedor SET NombreCompleto=?, FotoPath=? WHERE IdVendedor=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nombreCompleto);
                    ps.setString(2, fotoPath);
                    ps.setInt(3, idVendedor);
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error al actualizar perfil: " + e.getMessage());
            return false;
        }
    }
}
