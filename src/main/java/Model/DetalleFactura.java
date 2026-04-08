package Model;

// Representa una línea dentro de una factura.
// Por ejemplo: "3 unidades del producto ID 5, a $9.99 cada uno".
// Una factura puede tener varios de estos detalles.
public class DetalleFactura {
    private int    idProducto;   // Qué producto se vendió
    private int    cantidad;     // Cuántas unidades
    private double precioVenta;  // El precio al que se vendió (puede diferir del precio actual)

    public DetalleFactura(int idProducto, int cantidad, double precioVenta) {
        this.idProducto  = idProducto;
        this.cantidad    = cantidad;
        this.precioVenta = precioVenta;
    }

    public int    getIdProducto()              { return idProducto; }
    public void   setIdProducto(int v)         { this.idProducto = v; }

    public int    getCantidad()                { return cantidad; }
    public void   setCantidad(int v)           { this.cantidad = v; }

    public double getPrecioVenta()             { return precioVenta; }
    public void   setPrecioVenta(double v)     { this.precioVenta = v; }
}
