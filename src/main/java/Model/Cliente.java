package Model;

// Modelo que representa a un cliente del negocio.
// Guarda todos los datos de contacto que necesitamos para facturar.
public class Cliente {
    private int idCliente;
    private String cedula;      // Cédula o RUC del cliente
    private String nombre;      // Nombre completo
    private String correo;      // Para enviarle las facturas por email
    private String telefono;
    private String direccion;

    public Cliente() {}

    // Constructor completo para cuando ya tenemos todos los datos listos
    public Cliente(int idCliente, String cedula, String nombre, String telefono, String correo, String direccion) {
        this.idCliente = idCliente;
        this.cedula    = cedula;
        this.nombre    = nombre;
        this.telefono  = telefono;
        this.correo    = correo;
        this.direccion = direccion;
    }

    public int    getIdCliente()           { return idCliente; }
    public void   setIdCliente(int id)     { this.idCliente = id; }

    public String getCedula()              { return cedula; }
    public void   setCedula(String v)      { this.cedula = v; }

    public String getNombre()              { return nombre; }
    public void   setNombre(String v)      { this.nombre = v; }

    public String getCorreo()              { return correo; }
    public void   setCorreo(String v)      { this.correo = v; }

    public String getTelefono()            { return telefono; }
    public void   setTelefono(String v)    { this.telefono = v; }

    public String getDireccion()           { return direccion; }
    public void   setDireccion(String v)   { this.direccion = v; }
}
