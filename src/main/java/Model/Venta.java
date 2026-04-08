package Model;

/**
 * Representa un resumen de venta para uso en reportes y el Dashboard.
 * Para la lógica transaccional completa se usa Factura + DetalleFactura.
 */
public class Venta {
    private int idFactura;
    private String noSerie;
    private String nombreCliente;
    private String fechaVenta;
    private double monto;
    private String estado;
    private String metodoPago;

    public Venta() {}

    public Venta(int idFactura, String noSerie, String nombreCliente,
                 String fechaVenta, double monto, String estado, String metodoPago) {
        this.idFactura     = idFactura;
        this.noSerie       = noSerie;
        this.nombreCliente = nombreCliente;
        this.fechaVenta    = fechaVenta;
        this.monto         = monto;
        this.estado        = estado;
        this.metodoPago    = metodoPago;
    }

    public int    getIdFactura()             { return idFactura; }
    public void   setIdFactura(int v)        { this.idFactura = v; }
    public String getNoSerie()               { return noSerie; }
    public void   setNoSerie(String v)       { this.noSerie = v; }
    public String getNombreCliente()         { return nombreCliente; }
    public void   setNombreCliente(String v) { this.nombreCliente = v; }
    public String getFechaVenta()            { return fechaVenta; }
    public void   setFechaVenta(String v)    { this.fechaVenta = v; }
    public double getMonto()                 { return monto; }
    public void   setMonto(double v)         { this.monto = v; }
    public String getEstado()                { return estado; }
    public void   setEstado(String v)        { this.estado = v; }
    public String getMetodoPago()            { return metodoPago; }
    public void   setMetodoPago(String v)    { this.metodoPago = v; }

    // Representación en texto de la venta — útil para depurar.
    @Override
    public String toString() {
        return "Venta{" + noSerie + ", " + nombreCliente + ", $" + monto + ", " + estado + "}";
    }
}
