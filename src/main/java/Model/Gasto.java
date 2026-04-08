package Model;

// Representa un gasto o egreso del negocio.
// Lo usamos para calcular el balance real: ingresos - gastos.
public class Gasto {
    private int    idGasto;
    private String descripcion;  // Qué fue el gasto (ej: "Pago de arriendo")
    private double monto;        // Cuánto costó
    private String categoria;    // Ej: "Arriendo", "Servicios", "Sueldos"
    private String fecha;        // Cuándo se hizo el gasto (yyyy-MM-dd)
    private String notas;        // Cualquier nota adicional (opcional)

    public Gasto() {}

    public int    getIdGasto()                 { return idGasto; }
    public void   setIdGasto(int id)           { this.idGasto = id; }

    public String getDescripcion()             { return descripcion; }
    public void   setDescripcion(String v)     { this.descripcion = v; }

    public double getMonto()                   { return monto; }
    public void   setMonto(double v)           { this.monto = v; }

    public String getCategoria()               { return categoria; }
    public void   setCategoria(String v)       { this.categoria = v; }

    public String getFecha()                   { return fecha; }
    public void   setFecha(String v)           { this.fecha = v; }

    public String getNotas()                   { return notas; }
    public void   setNotas(String v)           { this.notas = v; }
}
