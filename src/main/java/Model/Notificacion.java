package Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Modelo de una notificación del sistema.
// Se usa para las alertas que aparecen en la campana de la barra superior.
// Puede ser de distintos tipos: ventas nuevas, stock bajo o cobros pendientes.
public class Notificacion {

    // Tipos de notificación para poder colorearlas diferente en la UI
    public enum Tipo { VENTA, STOCK, COBRO, INFO }

    private final String        titulo;
    private final String        mensaje;
    private final Tipo          tipo;
    private final LocalDateTime fecha;
    private boolean             leida;   // Si el usuario ya la vio o no

    public Notificacion(String titulo, String mensaje, Tipo tipo) {
        this.titulo  = titulo;
        this.mensaje = mensaje;
        this.tipo    = tipo;
        this.fecha   = LocalDateTime.now();
        this.leida   = false;
    }

    public String  getTitulo()   { return titulo; }
    public String  getMensaje()  { return mensaje; }
    public Tipo    getTipo()     { return tipo; }
    public boolean isLeida()     { return leida; }
    public void    marcarLeida() { this.leida = true; }

    // Formato bonito para mostrar la fecha en la UI (ej: "17/03 14:30")
    public String getFechaFormato() {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }
}
