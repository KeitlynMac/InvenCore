package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.alerts.MessageAlerts;
import raven.modal.component.DropShadowBorder;
import raven.toast.Notifications;
import services.ReporteService;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

// Panel para generar reportes Excel.
// Cada tarjeta dispara la generación de un tipo de reporte y lo abre automáticamente.
public class pReportes extends TabbedForm {

    public pReportes() { init(); }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 24", "[fill, grow]", "[][grow]"));

        // ── Encabezado ────────────────────────────────────────────────────────
        JPanel encabezado = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        encabezado.setOpaque(false);

        JLabel lblTitulo = new JLabel("REPORTES");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lblTitulo.setBorder(new EmptyBorder(0, 6, 4, 0));

        JLabel lblDesc = new JLabel("Genera reportes en Excel (.xlsx) y ábrelos directamente desde aquí.");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:+1;");

        JLabel lblCarpeta = new JLabel("Se guardan en: User / Ivencore / Reportes");
        lblCarpeta.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:-1;");
        lblCarpeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblCarpeta.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                abrirCarpetaReportes();
            }
        });

        JPanel textos = new JPanel(new MigLayout("wrap, insets 0", "[fill]"));
        textos.setOpaque(false);
        textos.add(lblTitulo);
        textos.add(lblDesc);
        textos.add(lblCarpeta, "gaptop 2");

        JButton btnAbrirCarpeta = new JButton("Abrir Carpeta");
        btnAbrirCarpeta.setIcon(new FlatSVGIcon("icons/search.svg", 0.28f));
        btnAbrirCarpeta.putClientProperty(FlatClientProperties.STYLE,
            "arc:14; background:null; foreground:$Label.disabledForeground; font:bold;" +
            "borderWidth:1; borderColor:$Component.borderColor; margin:6,14,6,14;");
        btnAbrirCarpeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAbrirCarpeta.addActionListener(e -> abrirCarpetaReportes());

        encabezado.add(textos, "growx");
        encabezado.add(btnAbrirCarpeta, "align right bottom");
        add(encabezado);

        // ── Grid de tarjetas de reportes — responsivo: wrap cuando no caben ───
        // Con "wrap" en el layout y ancho mínimo en cada celda,
        // las tarjetas se bajan automáticamente si la pantalla es pequeña.
        JPanel gridInterno = new JPanel(new MigLayout(
            "wrap, fillx, insets 0, gap 16",
            "[grow, fill, 260::]",  // mínimo 260px por tarjeta
            "[]"
        ));
        gridInterno.setOpaque(false);

        // El scroll horizontal nunca aparece — solo se envuelven las tarjetas hacia abajo
        JScrollPane gridScroll = new JScrollPane(gridInterno);
        gridScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        gridScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.getViewport().setOpaque(false);
        gridScroll.setOpaque(false);
        gridScroll.getVerticalScrollBar().putClientProperty(
            com.formdev.flatlaf.FlatClientProperties.STYLE,
            "trackArc:999; width:6; thumbInsets:2,2,2,2;");

        // Wrapper para pasar al método crearTarjeta igual que antes
        JPanel grid = new JPanel(new java.awt.GridLayout(0, 3, 16, 16)) {
            @Override public boolean isOpaque() { return false; }
        };
        grid.setOpaque(false);
        gridInterno.add(grid, "growx");

        // Selector de mes reutilizable
        String mesActual = new SimpleDateFormat("yyyy-MM").format(new Date());

        // ── Tarjeta 1: Ventas ──────────────────────────────────────────────────
        grid.add(crearTarjeta(
            "Ventas",
            "Historial de todas las facturas con montos, clientes, estados y métodos de pago.",
            "icons/sell.svg", "#3B82F6",
            new String[]{"Este mes", "Todos los tiempos"},
            new String[]{mesActual, null},
            (opcion) -> generarReporte("ventas", opcion)
        ));

        // ── Tarjeta 2: Inventario ─────────────────────────────────────────────
        grid.add(crearTarjeta(
            "Inventario",
            "Lista completa de productos con precios, stock disponible y valor total del inventario.",
            "icons/shopping-cart.svg", "#10B981",
            new String[]{"Generar reporte"},
            new String[]{null},
            (opcion) -> generarReporte("inventario", opcion)
        ));

        // ── Tarjeta 3: Gastos ─────────────────────────────────────────────────
        grid.add(crearTarjeta(
            "Gastos y Egresos",
            "Detalle de todos los gastos registrados con resumen por categoría al final.",
            "icons/dollar.svg", "#EF4444",
            new String[]{"Este mes", "Todos los tiempos"},
            new String[]{mesActual, null},
            (opcion) -> generarReporte("gastos", opcion)
        ));

        // ── Tarjeta 4: Balance ────────────────────────────────────────────────
        grid.add(crearTarjeta(
            "Balance General",
            "Comparativa de ingresos vs gastos mes a mes para los últimos 12 meses.",
            "icons/bank.svg", "#8B5CF6",
            new String[]{"Generar reporte"},
            new String[]{null},
            (opcion) -> generarReporte("balance", opcion)
        ));

        // ── Tarjeta 5: Productos vendidos ─────────────────────────────────────
        grid.add(crearTarjeta(
            "Productos Más Vendidos",
            "Ranking de productos por unidades vendidas e ingresos generados históricamente.",
            "icons/category.svg", "#F97316",
            new String[]{"Generar reporte"},
            new String[]{null},
            (opcion) -> generarReporte("productos_vendidos", opcion)
        ));

        // ── Tarjeta 6: Cuentas por cobrar ─────────────────────────────────────
        grid.add(crearTarjeta(
            "Cuentas por Cobrar",
            "Facturas pendientes de pago con fechas de vencimiento y montos adeudados.",
            "icons/cash.svg", "#EF4444",
            new String[]{"Generar reporte"},
            new String[]{null},
            (opcion) -> generarReporte("cuentas_cobrar", opcion)
        ));

        add(gridScroll, "grow, push");
    }

    // Genera el reporte en un hilo de fondo y lo abre al terminar
    private void generarReporte(String tipo, String filtro) {
        Notifications.getInstance().show(Notifications.Type.INFO,
            Notifications.Location.BOTTOM_CENTER, "Generando reporte...");

        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return switch (tipo) {
                    case "ventas"            -> ReporteService.reporteVentas(filtro);
                    case "inventario"        -> ReporteService.reporteInventario();
                    case "gastos"            -> ReporteService.reporteGastos(filtro);
                    case "balance"           -> ReporteService.reporteBalance();
                    case "productos_vendidos"-> ReporteService.reporteProductosVendidos();
                    case "cuentas_cobrar"    -> ReporteService.reporteCuentasCobrar();
                    default -> throw new Exception("Tipo de reporte desconocido: " + tipo);
                };
            }

            @Override
            protected void done() {
                try {
                    File archivo = get();
                    Notifications.getInstance().show(Notifications.Type.SUCCESS,
                        Notifications.Location.BOTTOM_CENTER,
                        "✅ Reporte generado: " + archivo.getName());

                    // Preguntar si quiere abrirlo ahora
                    MessageAlerts.getInstance().showMessage(
                        "Reporte listo",
                        "¿Abrir el archivo Excel ahora?\n" + archivo.getName(),
                        MessageAlerts.MessageType.SUCCESS,
                        MessageAlerts.OK_CANCEL_OPTION,
                        (pc, i) -> {
                            if (i == MessageAlerts.OK_OPTION) abrirArchivo(archivo);
                        });
                } catch (Exception ex) {
                    MessageAlerts.getInstance().showMessage("Error al generar reporte",
                        ex.getMessage() != null ? ex.getMessage() : "Error desconocido.");
                }
            }
        }.execute();
    }

    // Abre el archivo Excel con el programa predeterminado del sistema
    private void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            } else {
                MessageAlerts.getInstance().showMessage("No se puede abrir",
                    "Tu sistema no soporta abrir archivos automáticamente.\nEncuentra el archivo en:\n" + archivo.getAbsolutePath());
            }
        } catch (Exception ex) {
            MessageAlerts.getInstance().showMessage("Error al abrir",
                "No se pudo abrir el archivo. Búscalo manualmente en:\n" + archivo.getParent());
        }
    }

    // Abre la carpeta donde se guardan los reportes en el explorador de archivos
    private void abrirCarpetaReportes() {
        try {
            File carpeta = new File(System.getProperty("user.home")
                + File.separator + "Ivencore" + File.separator + "Reportes");
            carpeta.mkdirs();
            Desktop.getDesktop().open(carpeta);
        } catch (Exception ex) {
            MessageAlerts.getInstance().showMessage("Error", "No se pudo abrir la carpeta.");
        }
    }

    // ── Construye una tarjeta de reporte ──────────────────────────────────────
    private JPanel crearTarjeta(String titulo, String descripcion,
                                 String iconPath, String colorHex,
                                 String[] botones, String[] filtros,
                                 java.util.function.Consumer<String> accion) {
        JPanel card = new JPanel(new MigLayout("wrap, fill, insets 20 22 20 22", "[fill]", "[][grow][][]"));
        card.putClientProperty(FlatClientProperties.STYLE,
            "arc:18; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");
        card.setBorder(new DropShadowBorder(new java.awt.Insets(2, 2, 5, 2), 8));

        // Franja de color arriba
        JPanel franja = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(colorHex));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.fillRect(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        franja.setPreferredSize(new Dimension(0, 4));
        franja.setOpaque(false);
        card.add(franja, "growx, h 4!, gapbottom 10");

        // Icono + título
        JPanel headerCard = new JPanel(new MigLayout("insets 0, fillx", "[][grow]"));
        headerCard.setOpaque(false);

        FlatSVGIcon icon = new FlatSVGIcon(iconPath, 0.55f);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.decode(colorHex)));
        JLabel lblIcon = new JLabel(icon);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");

        headerCard.add(lblIcon);
        headerCard.add(lblTitulo, "growx, gapleft 8");
        card.add(headerCard, "growx");

        // Descripción
        JLabel lblDesc = new JLabel("<html><p style='width:220px;'>" + descripcion + "</p></html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:+0;");
        card.add(lblDesc, "growx, gaptop 6, push");

        // Botones de acción
        JPanel pBotones = new JPanel(new MigLayout("insets 0, gap 8", "[grow, fill]"));
        pBotones.setOpaque(false);

        for (int b = 0; b < botones.length; b++) {
            final String filtro = filtros[b];
            JButton btn = new JButton(botones[b]);
            btn.setIcon(new FlatSVGIcon("icons/clipboard-data.svg", 0.27f));
            btn.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:" + colorHex + "; foreground:#ffffff; font:bold;" +
                "margin:8,14,8,14; borderWidth:0; focusWidth:0;");
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> accion.accept(filtro));
            pBotones.add(btn, "growx");
        }
        card.add(pBotones, "growx, gaptop 12");

        return card;
    }

    // Interfaz funcional para simplificar el callback de generación
    @FunctionalInterface
    interface AccionReporte { void ejecutar(String filtro); }
}
