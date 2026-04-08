package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import dao.DashboardDAO;
import dao.GastosDAO;
import net.miginfocom.swing.MigLayout;
import raven.chart.data.category.DefaultCategoryDataset;
import raven.chart.line.LineChart;
import utilities.HeaderTabla;
import utilities.TabbedForm;
import utilities.TableBadgeCellRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// Panel principal del sistema — el Dashboard.
// Muestra las tarjetas de KPIs, el gráfico de ventas del mes,
// el historial de operaciones en vivo y las transacciones recientes.
public class pDashboard extends TabbedForm {

    private LineChart          lineChartVentas;

    private final DashboardDAO dao       = new DashboardDAO();
    private final GastosDAO    gastosDAO = new GastosDAO();

    private final String fechaHoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    private final String anioMes  = new SimpleDateFormat("yyyy-MM").format(new Date());

    public enum EstadoFacturaDash implements TableBadgeCellRenderer.Info {
        PAGADO("Pagado", Color.decode("#10b981")),
        POR_PAGAR("Por Pagar", Color.decode("#ef4444"));

        private final String text;
        private final Color color;
        EstadoFacturaDash(String text, Color color) { this.text = text; this.color = color; }
        @Override public String getText() { return text; }
        @Override public Color getColor() { return color; }
        @Override public Icon getIcon() { return null; }
    }

    public pDashboard() { init(); }

    @Override public void formOpen() { animar(); }

    private void init() {
        setLayout(new BorderLayout());

        JPanel contenido = new JPanel(new MigLayout(
                "wrap 2, fill, insets 25, gap 20",
                "[0::, fill, grow 40][0::, fill, grow 60]",
                "[pref!][grow, fill]"
        ));
        contenido.setOpaque(false);

        contenido.add(crearEncabezado(), "span 2, growx, gapbottom 10");

        JPanel panelIzquierdo = new JPanel(new MigLayout("wrap, fill, insets 0, gapy 20", "[fill, grow]"));
        panelIzquierdo.setOpaque(false);
        panelIzquierdo.add(crearGridKpis(), "growx");
        panelIzquierdo.add(crearListaActividadGeneral(), "grow, pushy, h 0::");

        JPanel panelDerecho = new JPanel(new MigLayout("wrap, fill, insets 0, gapy 20", "[fill, grow]"));
        panelDerecho.setOpaque(false);
        panelDerecho.add(crearGraficoVentasMes(), "growx, h 300:320:340");
        panelDerecho.add(crearTablaActividad(), "grow, pushy, h 0::");

        contenido.add(panelIzquierdo, "grow, pushy, h 0::");
        contenido.add(panelDerecho, "grow, pushy, h 0::");

        add(contenido, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::animar);
    }

    // Construye el saludo con el nombre del usuario y el botón de actualizar.
    private JPanel crearEncabezado() {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        p.setOpaque(false);

        String nombre = utilities.SesionUsuario.getInstance().getNombreDisplay();
        JLabel lblBienvenida = new JLabel("Dashboard");
        int welcomeInc = utilities.EstiloResponsivo.escala() < 0.88f ? 5 : (utilities.EstiloResponsivo.escala() < 0.94f ? 7 : 10);
        lblBienvenida.putClientProperty(FlatClientProperties.STYLE, "font:bold +" + welcomeInc + ";");

        JLabel lblFecha = new JLabel("Hola, " + nombre + ". "+ new SimpleDateFormat("EEEE, dd MMM yyyy").format(new Date()));
        lblFecha.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:+1;");

        JPanel textos = new JPanel(new MigLayout("wrap, insets 0", "[fill]"));
        textos.setOpaque(false);
        textos.add(lblBienvenida);
        textos.add(lblFecha);

        p.add(textos, "growx");
        return p;
    }

    // Las 4 tarjetas de colores: ventas totales, del mes, de hoy y stock bajo.
    private JPanel crearGridKpis() {
        JPanel grid = new JPanel(new MigLayout("wrap 2, insets 0, gap 15",
                "[0::, fill, grow][0::, fill, grow]",
                "[130::, fill, grow][130::, fill, grow]"));
        grid.setOpaque(false);

        double vTotal   = dao.ingresoTotal();
        double vMes     = dao.ingresoMes(anioMes);
        double vHoy     = dao.ingresoHoy(fechaHoy);
        int    stockB   = dao.productosStockBajo();

        grid.add(tarjetaColor("Ventas Totales", fmtDin(vTotal), "icons/caja.svg", "#56cdff"));
        grid.add(tarjetaColor("Ventas del Mes", fmtDin(vMes), "icons/calendar.svg", "#00e1b1"));
        grid.add(tarjetaColor("Ventas de Hoy", fmtDin(vHoy), "icons/ventashoy.svg", "#7e5aff"));
        grid.add(tarjetaColor("Poco Stock", stockB + " prods", "icons/box.svg", "#4e89ff"));

        return grid;
    }

    // Crea una tarjeta con fondo de color, ícono y valor numérico.
    private JPanel tarjetaColor(String titulo, String valor, String iconPath, String bgColorHex) {
        JPanel card = new JPanel(new MigLayout("fill, insets 20 16 20 16", "[grow, fill][pref!]", "[top][grow, bottom]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:16; background:" + bgColorHex + ";");

        JLabel lblTit = new JLabel(titulo);
        lblTit.putClientProperty(FlatClientProperties.STYLE, "foreground:#ffffff; font:bold +0;");

        JLabel lblVal = new JLabel(valor);
        // Tamaño del valor en la tarjeta — se reduce en pantallas pequeñas
        int fontInc = utilities.EstiloResponsivo.escala() < 0.88f ? 5 : (utilities.EstiloResponsivo.escala() < 0.94f ? 7 : 10);
        lblVal.putClientProperty(FlatClientProperties.STYLE, "foreground:#ffffff; font:bold +" + fontInc + ";");

        FlatSVGIcon svgIcon = new FlatSVGIcon(iconPath, 0.6f);
        svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
        JLabel lblIcon = new JLabel(svgIcon);

        card.add(lblTit, "cell 0 0, aligny top");
        card.add(lblIcon, "cell 1 0, aligny top, alignx right");
        card.add(lblVal, "cell 0 1 2 1");

        return card;
    }

    // ── GRÁFICO DE LÍNEAS ORIGINAL (Corregido el fondo blanco) ───────────────
    private JPanel crearGraficoVentasMes() {
        JPanel panel = new JPanel(new BorderLayout());
        // Se unificó el fondo para que no haya cajas de distinto color
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20; background:$Panel.background; border:15,20,15,20,$Component.borderColor,,20");

        lineChartVentas = new LineChart();
        lineChartVentas.setChartType(LineChart.ChartType.CURVE);

        // Forzamos la transparencia total del gráfico para evitar el cuadro blanco
        lineChartVentas.setOpaque(false);
        lineChartVentas.setBackground(new Color(0, 0, 0, 0));

        DefaultCategoryDataset<String, String> dsLinea = new DefaultCategoryDataset<>();
        SimpleDateFormat sdfDB  = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfVis = new SimpleDateFormat("dd/MM");
        List<Object[]> ventasMes = dao.ventasDelMesGrafica(anioMes);

        if (ventasMes.isEmpty()) {
            dsLinea.addValue(0, "Ventas", sdfVis.format(new Date()));
        } else {
            for (Object[] r : ventasMes) {
                try {
                    dsLinea.addValue(((Number)r[1]).doubleValue(), "Ventas", sdfVis.format(sdfDB.parse(r[0].toString())));
                } catch (Exception ex) {
                    dsLinea.addValue(((Number)r[1]).doubleValue(), "Ventas", r[0].toString());
                }
            }
        }
        lineChartVentas.setCategoryDataset(dsLinea);
        lineChartVentas.getChartColor().addColor(Color.decode("#3b82f6"));

        JLabel hLinea = new JLabel("Ventas del Mes");
        hLinea.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; border:0,0,10,0;");

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);
        pnlHead.setBackground(new Color(0, 0, 0, 0));
        pnlHead.add(hLinea, BorderLayout.WEST);

        lineChartVentas.setHeader(pnlHead);
        panel.add(lineChartVentas, BorderLayout.CENTER);

        return panel;
    }

    // Feed en vivo de operaciones usando NotificacionManager como fuente.
    // Cada nueva notificación aparece arriba automáticamente.
    private JPanel crearListaActividadGeneral() {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, insets 20", "[fill]", "[][grow, fill]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:18; background:$Panel.background; border: 1,1,1,1, $Component.borderColor, 1, 18");

        JLabel lbl = new JLabel("Historial de Operaciones");
        lbl.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        panel.add(lbl, "gapbottom 10");

        JPanel pnlLista = new JPanel(new MigLayout("wrap, fillx, insets 0, gapy 0", "[fill]"));
        pnlLista.setOpaque(false);

        JScrollPane scroll = new JScrollPane(pnlLista);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "trackArc:999; width:6; thumbInsets:2,2,2,2; background:null;");
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        panel.add(scroll, "grow, push, h 0::");

        Runnable refrescar = () -> {
            pnlLista.removeAll();

            // 1. LEER DE LA BASE DE DATOS (Para que no se borre al cerrar)
            java.util.List<Object[]> historialDB = dao.obtenerHistorialOperaciones();

            if (historialDB.isEmpty()) {
                JLabel lblVacio = new JLabel("Aún no hay operaciones registradas.");
                lblVacio.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:italic;");
                lblVacio.setHorizontalAlignment(SwingConstants.CENTER);
                pnlLista.add(lblVacio, "grow, gapy 20");
            } else {
                for (Object[] fila : historialDB) {
                    String tipo = fila[0].toString().toUpperCase();
                    String desc = fila[1].toString();
                    String valor = fila[2].toString();

                    // 2. COLORES PASTEL DIRECTOS (Para que no salgan blancos)
                    String[] estilo = switch (tipo) {
                        case "VENTA"    -> new String[]{"icons/shopping-cart.svg", "#10b981", "#d1fae5"}; // Verde
                        case "PRODUCTO" -> new String[]{"icons/box.svg",           "#f97316", "#ffedd5"}; // Naranja
                        case "GASTO"    -> new String[]{"icons/cash.svg",          "#ef4444", "#fee2e2"}; // Rojo
                        case "CLIENTE"  -> new String[]{"icons/users-plus.svg",    "#3b82f6", "#dbeafe"}; // Azul
                        default         -> new String[]{"icons/clipboard-data.svg","#009991", "#e0f7f6"}; // Teal
                    };

                    agregarItemActividad(pnlLista, fila[0].toString(), desc, valor, valor, estilo[0], estilo[1], estilo[2]);
                }
            }
            pnlLista.revalidate();
            pnlLista.repaint();
        };

        refrescar.run();
        utilities.NotificacionManager.getInstance().addListener(lista -> javax.swing.SwingUtilities.invokeLater(refrescar));

        return panel;
    }

    // 3. ICONOS CON TAMAÑO FIJO (Para que todos se vean iguales)
    private void agregarItemActividad(JPanel contenedor, String titulo, String desc, String fecha, String montoExtraido, String iconPath, String colorIcon, String colorBg) {
        JPanel item = new JPanel(new MigLayout("fillx, insets 10 5 10 10", "[pref!]12[grow][pref!]"));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));

        // Panel del Icono con tamaño fijo de 45x45
        JPanel pnlIcono = new JPanel(new BorderLayout());
        pnlIcono.setPreferredSize(new Dimension(50, 50));
        pnlIcono.setMinimumSize(new Dimension(50, 50));
        pnlIcono.putClientProperty(FlatClientProperties.STYLE, "arc:5; background:" + colorBg + ";");

        // Forzamos el icono a 22x22 píxeles exactos
        FlatSVGIcon svgIcon = new FlatSVGIcon(iconPath, 25, 25);
        svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(col -> Color.decode(colorIcon)));

        JLabel lblIcon = new JLabel(svgIcon);
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        pnlIcono.add(lblIcon);

        JPanel pnlTextos = new JPanel(new MigLayout("wrap, insets 0, gapy 2"));
        pnlTextos.setOpaque(false);
        JLabel lblTit = new JLabel(titulo);
        lblTit.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");

        // Ajustamos el ancho para que el texto no empuje los iconos
        JLabel lblDesc = new JLabel("<html><p style='width:200px;'>" + desc + "</p></html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:-1;");
        pnlTextos.add(lblTit);
        pnlTextos.add(lblDesc);

        JLabel lblMonto = new JLabel(montoExtraido.isEmpty() ? fecha : montoExtraido);
        lblMonto.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:" + colorIcon + ";");
        lblMonto.setHorizontalAlignment(SwingConstants.RIGHT);

        item.add(pnlIcono, "aligny center");
        item.add(pnlTextos, "growx, aligny center");
        item.add(lblMonto, "align right, wmin 80");

        contenedor.add(item);
    }

    // Tabla con las últimas 15 transacciones, con badge de estado.
    private JPanel crearTablaActividad() {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, insets 20", "[fill]", "[][grow, fill]"));
        // Se unificó el fondo
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:18; background:$Panel.background; border: 1,1,1,1, $Component.borderColor, 1, 18");

        JLabel lbl = new JLabel("Transacciones Recientes");
        lbl.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        panel.add(lbl, "gapbottom 10");

        DefaultTableModel modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) return EstadoFacturaDash.class;
                return super.getColumnClass(columnIndex);
            }
        };

        modelo.addColumn("Factura");
        modelo.addColumn("Cliente");
        modelo.addColumn("Total");
        modelo.addColumn("Estado");

        JTable tabla = new JTable(modelo);
        tabla.setFocusable(false);
        tabla.setShowGrid(false);
        // Evitamos que la tabla dibuje su propio fondo blanco
        tabla.setOpaque(false);
        ((DefaultTableCellRenderer)tabla.getDefaultRenderer(Object.class)).setOpaque(false);

        TableBadgeCellRenderer.apply(tabla, EstadoFacturaDash.class);

        tabla.getTableHeader().setDefaultRenderer(new HeaderTabla(tabla));
        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:30; font:bold +1; foreground:#9f9f9f; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background;");
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:45; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font:+1; background:null;");

        for (Object[] v : dao.actividadReciente()) {
            String estadoStr = v[3] != null ? v[3].toString() : "";
            EstadoFacturaDash estadoEnum = estadoStr.equalsIgnoreCase("Pagado") ?
                    EstadoFacturaDash.PAGADO : EstadoFacturaDash.POR_PAGAR;

            modelo.addRow(new Object[]{v[0], v[1], v[2], estadoEnum});
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "trackArc:999; width:6; thumbInsets:2,2,2,2; background:null;");
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        panel.add(scroll, "grow, push, h 0::");
        return panel;
    }

    // Arranca la animación del gráfico de líneas.
    private void animar() {
        if (lineChartVentas != null) lineChartVentas.startAnimation();
    }

    private String fmtDin(double v) { return "$" + String.format("%.2f", v); }
}