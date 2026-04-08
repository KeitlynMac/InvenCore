package utilities;

import Model.Usuario;

/**
 * Singleton que almacena el usuario actualmente autenticado.
 * Se llena en frLogin después de autenticar correctamente y
 * se usa en MyDrawer, pPerfil y cualquier parte que necesite
 * saber quién está logueado.
 */
// Singleton que guarda al usuario que está logueado en este momento.
// Así cualquier parte del programa puede saber quién está usando el sistema
// sin tener que pasar el usuario como parámetro por todas partes.
public class SesionUsuario {

    private static SesionUsuario instancia;
    private Usuario usuarioActual;

    private SesionUsuario() {}

    public static SesionUsuario getInstance() {
        if (instancia == null) instancia = new SesionUsuario();
        return instancia;
    }

        // Llama esto justo después de que el usuario inicia sesión correctamente.
    public void iniciar(Usuario usuario) {
        this.usuarioActual = usuario;
    }

        // Llama esto cuando el usuario cierra sesión. Limpia el estado.
    public void cerrar() {
        this.usuarioActual = null;
    }

    public Usuario getUsuario() {
        return usuarioActual;
    }

    /** Nombre visible en UI. Nunca retorna null. */
    public String getNombreDisplay() {
        if (usuarioActual == null) return "Usuario";
        return usuarioActual.getDisplayName();
    }

    /** Ruta de foto de perfil, o null si no tiene. */
    public String getFotoPath() {
        if (usuarioActual == null) return null;
        return usuarioActual.getFotoPath();
    }

    public int getIdUsuario() {
        if (usuarioActual == null) return -1;
        return usuarioActual.getIdVendedor();
    }
}
