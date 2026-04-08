package utilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


public class LicenciaManager {

    private static final String HASH_CLAVE_SECRETA =
            "78bfdfa34e0407e65488b59e71ac58e571e8230d18f728f2ade4ee22827d5fce";

    // 1. Definimos la misma ruta maestra segura de la base de datos
    private static final String RUTA_CARPETA = System.getProperty("user.home") + File.separator + "Ivencore";

    // 2. El archivo de activación vivirá dentro de esa carpeta
    private static final String ARCHIVO_ACTIVACION = RUTA_CARPETA + File.separator + "activacion.sys";


        // El programa está activado si el archivo activacion.sys existe en el directorio.
    public static boolean estaActivado() {
        return new File(ARCHIVO_ACTIVACION).exists();
    }

        // Compara el hash de la clave ingresada. Si coincide, crea el archivo de activación.
    public static boolean activar(String claveIngresada) {
        if (claveIngresada == null || claveIngresada.trim().isEmpty()) return false;

        String hashIngresado = sha256(claveIngresada.trim());

        if (HASH_CLAVE_SECRETA.equalsIgnoreCase(hashIngresado)) {
            try {
                // Nos aseguramos de que la carpeta maestra exista antes de crear el archivo
                File carpeta = new File(RUTA_CARPETA);
                if (!carpeta.exists()) {
                    carpeta.mkdirs();
                }
                new File(ARCHIVO_ACTIVACION).createNewFile();
                return true;
            } catch (Exception e) {
                System.err.println("LicenciaManager: No se pudo crear el archivo de activación: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /** Calcula el hash SHA-256 de un texto y lo retorna como hex en minúsculas. */
        // Calcula el hash SHA-256 de un texto. Lo uso para proteger claves y contraseñas.
    public static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
