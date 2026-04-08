package Model;

import java.util.List;

// Representa una factura de venta.
// Tiene toda la información del cobro: monto, pago recibido, deuda pendiente,
// método de pago y la lista de productos vendidos (detalles).
public class Factura {
    private String noSerie;          // Número de factura (ej: 00001)
    private int    idCliente;        // 0 significa "Consumidor Final"
    private String fechaVenta;       // Fecha y hora en que se hizo la venta
    private double montoTotal;       // Total de la venta con descuentos
    private String estado;           // "Pagado" o "Por Pagar"
    private String metodoPago;       // "Efectivo", "Transferencia" o "Mixto"
    private double deuda;            // Cuánto le queda por pagar al cliente
    private String fechaVencimiento; // Fecha límite para pagar la deuda
    private double pago;             // Cuánto pagó el cliente en ese momento
    private List<DetalleFactura> detalles; // Los productos que se vendieron

    public Factura() {}

    public String getNoSerie()                   { return noSerie; }
    public void   setNoSerie(String v)           { this.noSerie = v; }

    public int    getIdCliente()                 { return idCliente; }
    public void   setIdCliente(int v)            { this.idCliente = v; }

    public String getFechaVenta()                { return fechaVenta; }
    public void   setFechaVenta(String v)        { this.fechaVenta = v; }

    public double getMontoTotal()                { return montoTotal; }
    public void   setMontoTotal(double v)        { this.montoTotal = v; }

    public String getEstado()                    { return estado; }
    public void   setEstado(String v)            { this.estado = v; }

    public String getMetodoPago()                { return metodoPago; }
    public void   setMetodoPago(String v)        { this.metodoPago = v; }

    public double getDeuda()                     { return deuda; }
    public void   setDeuda(double v)             { this.deuda = v; }

    public String getFechaVencimiento()          { return fechaVencimiento; }
    public void   setFechaVencimiento(String v)  { this.fechaVencimiento = v; }

    public double getPago()                      { return pago; }
    public void   setPago(double v)              { this.pago = v; }

    public List<DetalleFactura> getDetalles()           { return detalles; }
    public void setDetalles(List<DetalleFactura> lista) { this.detalles = lista; }
}
