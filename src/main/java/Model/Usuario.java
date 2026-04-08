package Model;

// Modelo del usuario del sistema (el vendedor/administrador).
// Guarda su nombre, usuario y la ruta a su foto de perfil.
// La clave nunca se guarda en texto plano, siempre como hash SHA-256.
public class Usuario {
    private int    idVendedor;
    private String user;            // Nombre de usuario para iniciar sesión
    private String clave;           // Hash SHA-256 de la contraseña
    private String nombreCompleto;  // Nombre real que se muestra en el menú
    private String fotoPath;        // Ruta a la foto de perfil en el disco

    public Usuario() {}

    public int    getIdVendedor()              { return idVendedor; }
    public void   setIdVendedor(int id)        { this.idVendedor = id; }

    public String getUser()                    { return user; }
    public void   setUser(String user)         { this.user = user; }

    public String getClave()                   { return clave; }
    public void   setClave(String clave)       { this.clave = clave; }

    public String getNombreCompleto()                      { return nombreCompleto; }
    public void   setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getFotoPath()                { return fotoPath; }
    public void   setFotoPath(String fotoPath) { this.fotoPath = fotoPath; }

    // Devuelve el nombre que se muestra en la UI.
    // Si el usuario puso su nombre completo, lo usa. Si no, usa el username.
    public String getDisplayName() {
        return (nombreCompleto != null && !nombreCompleto.trim().isEmpty())
                ? nombreCompleto.trim() : user;
    }
}
