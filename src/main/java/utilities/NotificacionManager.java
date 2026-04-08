package utilities;

import Model.Notificacion;
import Model.Notificacion.Tipo;
import dao.Conexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Singleton que gestiona el ciclo de vida de las notificaciones en memoria.
 * Genera alertas automáticas escaneando la base de datos periódicamente:
 *  - Facturas por cobrar vencidas o próximas a vencer
 *  - Productos con stock bajo (≤ 3 unidades)
 *  - Registra nuevas ventas del día
 *
 * Uso:
 *   NotificacionManager.getInstance().addListener(lista -> actualizarUI(lista));
 *   NotificacionManager.getInstance().iniciarMonitor();  // en frPrincipal
 */
// Maneja el sistema de notificaciones del programa.
// Escanea la base de datos cada 60 segundos buscando:
//   - Productos con stock bajo o agotado
//   - Facturas vencidas o próximas a vencer
//   - Ventas nuevas del día
// Las notificaciones se muestran en la campana de la barra superior.
public class NotificacionManager {

    private static NotificacionManager instance;
    private final List<Notificacion>        lista   = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<Notificacion>>> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService        scheduler;

    // Para evitar duplicar alertas de stock/cobro en la misma sesión
    private final Set<String> alertasEmitidas = Collections.synchronizedSet(new HashSet<>());

    private NotificacionManager() {}

    public static NotificacionManager getInstance() {
        if (instance == null) instance = new NotificacionManager();
        return instance;
    }

    // ── Listeners ────────────────────────────────────────────────────────────
    public void addListener(Consumer<List<Notificacion>> l) { listeners.add(l); }

    private void notificarListeners() {
        List<Notificacion> snapshot = Collections.unmodifiableList(new ArrayList<>(lista));
        // Siempre en el EDT para que la UI pueda actualizarse sin problemas
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            listeners.forEach(l -> l.accept(snapshot));
        } else {
            javax.swing.SwingUtilities.invokeLater(() ->
                listeners.forEach(l -> l.accept(snapshot)));
        }
    }

    // ── API pública ───────────────────────────────────────────────────────────
    /** Agrega una notificación manual (ej: al completar una venta). */
        // Agrega una notificación manual, por ejemplo cuando se completa una venta.
    public void agregar(Notificacion n) {
        lista.add(0, n);      // más recientes primero
        if (lista.size() > 50) lista.remove(lista.size() - 1); // máx 50
        notificarListeners();
    }

    public List<Notificacion> getLista() { return Collections.unmodifiableList(lista); }

    public long contarNoLeidas() { return lista.stream().filter(n -> !n.isLeida()).count(); }

    // Marca todas las notificaciones como leídas (el badge de la campana vuelve a 0).
    public void marcarTodasLeidas() {
        lista.forEach(Notificacion::marcarLeida);
        notificarListeners();
    }

    // ── Monitor automático ────────────────────────────────────────────────────
    /** Inicia el scanner periódico. Llamar una sola vez desde frPrincipal. */
        // Arranca el hilo de fondo que escanea la BD cada 60 segundos.
    // Solo lo llamo una vez al abrir la ventana principal.
    public void iniciarMonitor() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NotificacionMonitor");
            t.setDaemon(true);
            return t;
        });
        // Primera ejecución inmediata, luego cada 60 segundos
        // Primera ejecución a los 2 segundos, luego cada 30 segundos
        scheduler.scheduleAtFixedRate(this::escanear, 2, 30, TimeUnit.SECONDS);
    }

    public void detenerMonitor() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    // ── Scanner ───────────────────────────────────────────────────────────────
        // El escaneo principal: stock bajo, cobros pendientes y ventas nuevas.
    private void escanear() {
        try {
            escanearStockBajo();
            escanearCuentasCobrar();
            escanearVentasRecientes();
        } catch (Exception e) {
            System.err.println("NotificacionManager: error en escaneo: " + e.getMessage());
        }
    }

    // El ciclo principal del monitor: revisa stock, cobros y ventas recientes.
    private void escanearStockBajo() {
        String sql = "SELECT Nombre, Stock FROM Producto WHERE Stock <= 3 ORDER BY Stock ASC";
        try (Connection c = Conexion.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nombre = rs.getString("Nombre");
                int    stock  = rs.getInt("Stock");
                String key    = "stock:" + nombre;
                if (!alertasEmitidas.contains(key)) {
                    alertasEmitidas.add(key);
                    String msg = stock == 0
                        ? "'" + nombre + "' está AGOTADO."
                        : "'" + nombre + "' tiene solo " + stock + " unidades.";
                    lista.add(0, new Notificacion("⚠ Stock Bajo", msg, Tipo.STOCK));
                }
            }
        } catch (Exception e) { System.err.println("escanearStock: " + e.getMessage()); }
        // Limpiar claves de stock para re-evaluar en el próximo ciclo (el stock pudo reponerse)
        alertasEmitidas.removeIf(k -> k.startsWith("stock:"));
    }

    // El ciclo principal del monitor: revisa stock, cobros y ventas recientes.
    private void escanearCuentasCobrar() {
        String hoy       = LocalDate.now().toString();
        String en3dias   = LocalDate.now().plusDays(3).toString();
        String sql = "SELECT f.No_Serie, COALESCE(c.Nombre_Apellido,'Consumidor Final') AS Cliente, " +
                     "f.Deuda, f.Fecha_Vencimiento " +
                     "FROM Facturas f " +
                     "LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
                     "WHERE f.Estado='Por Pagar' AND f.Fecha_Vencimiento IS NOT NULL " +
                     "AND f.Fecha_Vencimiento <= ?";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, en3dias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String serie = rs.getString("No_Serie");
                    String key   = "cobro:" + serie;
                    if (!alertasEmitidas.contains(key)) {
                        alertasEmitidas.add(key);
                        String cliente = rs.getString("Cliente");
                        String deuda   = String.format("$%.2f", rs.getDouble("Deuda"));
                        String venc    = rs.getString("Fecha_Vencimiento");
                        boolean vencida = venc != null && venc.compareTo(hoy) < 0;
                        String titulo  = vencida ? "🔴 Factura Vencida" : "🟡 Vence Pronto";
                        String msg     = "Factura #" + serie + " – " + cliente +
                                         " | Deuda: " + deuda +
                                         (vencida ? " (VENCIDA)" : " | Vence: " + venc);
                        lista.add(0, new Notificacion(titulo, msg, Tipo.COBRO));
                    }
                }
            }
        } catch (Exception e) { System.err.println("escanearCobros: " + e.getMessage()); }
    }

    // El ciclo principal del monitor: revisa stock, cobros y ventas recientes.
    private void escanearVentasRecientes() {
        // Notificar las últimas 3 ventas del día que aún no se alertaron
        String hoy = LocalDate.now().toString();
        String sql = "SELECT f.No_Serie, COALESCE(c.Nombre_Apellido,'Consumidor Final') AS Cliente, " +
                     "f.Monto, f.FechaVenta " +
                     "FROM Facturas f " +
                     "LEFT JOIN Clientes c ON f.Cliente_IdCliente = c.IdCliente " +
                     "WHERE f.FechaVenta LIKE ? ORDER BY f.IdFactura DESC LIMIT 5";
        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hoy + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String serie = rs.getString("No_Serie");
                    String key   = "venta:" + serie;
                    if (!alertasEmitidas.contains(key)) {
                        alertasEmitidas.add(key);
                        String cliente = rs.getString("Cliente");
                        String monto   = String.format("$%.2f", rs.getDouble("Monto"));
                        lista.add(0, new Notificacion(
                            "✅ Nueva Venta",
                            "Factura #" + serie + " – " + cliente + " por " + monto,
                            Tipo.VENTA));
                    }
                }
            }
        } catch (Exception e) { System.err.println("escanearVentas: " + e.getMessage()); }
    }
}
