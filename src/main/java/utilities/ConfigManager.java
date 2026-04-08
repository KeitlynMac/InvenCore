package utilities;

import dao.Conexion;

import java.sql.*;

// Gestiona la configuración del sistema almacenada en la tabla Configuracion de la BD.
// Los datos del negocio y el correo se guardan ahí para que los backups los incluyan.
// La contraseña de correo se guarda como hash SHA-256, nunca en texto plano.
public class ConfigManager {

    // ── Lectura ──────────────────────────────────────────────────────────────

    public static String get(String clave, String defaultValue) {
        String sql = "SELECT Valor FROM Configuracion WHERE Clave = ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String val = rs.getString("Valor");
                return (val != null && !val.isEmpty()) ? val : defaultValue;
            }
        } catch (Exception e) {
            System.err.println("ConfigManager.get: " + e.getMessage());
        }
        return defaultValue;
    }

    // ── Escritura ─────────────────────────────────────────────────────────────

    public static void set(String clave, String valor) {
        String sql = "INSERT OR REPLACE INTO Configuracion(Clave, Valor) VALUES(?, ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor != null ? valor : "");
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("ConfigManager.set: " + e.getMessage());
        }
    }

    // Guarda un valor en la tabla Configuracion (INSERT OR REPLACE).
    public static void setMultiple(java.util.Map<String, String> valores) {
        String sql = "INSERT OR REPLACE INTO Configuracion(Clave, Valor) VALUES(?, ?)";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (java.util.Map.Entry<String, String> e : valores.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue() != null ? e.getValue() : "");
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.err.println("ConfigManager.setMultiple: " + e.getMessage());
        }
    }

    // ── Contraseña de correo — siempre hasheada ───────────────────────────────

    // Guarda la clave de correo como hash SHA-256. Nunca en texto plano.
    public static void setEmailClave(String claveTextoPlano) {
        String hash = LicenciaManager.sha256(claveTextoPlano);
        set("email.clave.hash", hash);
        // Guardamos también el texto plano de forma temporal SOLO para envío SMTP.
        // En un sistema real usaríamos cifrado reversible; aquí usamos ofuscación Base64.
        set("email.clave.smtp", java.util.Base64.getEncoder().encodeToString(
            claveTextoPlano.getBytes()));
    }

    // Devuelve la clave SMTP para el envío de correos (decodificada de Base64).
    // Nota: esto es ofuscación, no cifrado fuerte. Para producción usar AES.
    public static String getEmailClaveSmtp() {
        String encoded = get("email.clave.smtp", "");
        if (encoded.isEmpty()) return "";
        try {
            return new String(java.util.Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return "";
        }
    }

    // Verifica si la clave ingresada coincide con el hash guardado
    public static boolean verificarEmailClave(String claveTextoPlano) {
        String hashGuardado = get("email.clave.hash", "");
        return !hashGuardado.isEmpty() && hashGuardado.equals(LicenciaManager.sha256(claveTextoPlano));
    }
}
