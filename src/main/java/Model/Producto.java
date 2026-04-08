package Model;

// Representa un producto del inventario.
// Tiene todos los campos que necesita un producto real: precio, stock, categoría,
// y también cosas opcionales como imagen, descripción y fecha de vencimiento.
public class Producto {
    private int    idProducto;
    private String codigo;           // Código único del producto (ej: P-001)
    private String nombre;
    private double precio;
    private int    stock;            // Cuántas unidades quedan
    private String categoria;        // Ej: "Alimentos", "Tecnología"
    private String descripcion;      // Descripción opcional del producto
    private String imagenPath;       // Ruta a la imagen en el disco local
    private String fechaVencimiento; // Opcional, para productos perecederos

    public Producto() {}

    public int    getIdProducto()              { return idProducto; }
    public void   setIdProducto(int id)        { this.idProducto = id; }

    public String getCodigo()                  { return codigo; }
    public void   setCodigo(String v)          { this.codigo = v; }

    public String getNombre()                  { return nombre; }
    public void   setNombre(String v)          { this.nombre = v; }

    public double getPrecio()                  { return precio; }
    public void   setPrecio(double v)          { this.precio = v; }

    public int    getStock()                   { return stock; }
    public void   setStock(int v)              { this.stock = v; }

    public String getCategoria()               { return categoria; }
    public void   setCategoria(String v)       { this.categoria = v; }

    public String getDescripcion()             { return descripcion; }
    public void   setDescripcion(String v)     { this.descripcion = v; }

    public String getImagenPath()              { return imagenPath; }
    public void   setImagenPath(String v)      { this.imagenPath = v; }

    public String getFechaVencimiento()        { return fechaVencimiento; }
    public void   setFechaVencimiento(String v){ this.fechaVencimiento = v; }
}
