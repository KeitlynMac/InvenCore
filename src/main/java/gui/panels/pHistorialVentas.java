package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import dao.ClientesDAO;
import dao.FacturaDAO;
import Model.Cliente;
import net.miginfocom.swing.MigLayout;
import raven.datetime.DatePicker;
import raven.datetime.TimePicker;
import raven.modal.component.DropShadowBorder;
import utilities.TableBadgeCellRenderer;
import utilities.HeaderTabla;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class pHistorialVentas extends TabbedForm {

    // Estilos de botones con márgenes adaptativos según el tamaño de pantalla
    private final String STYLE_PRIMARY;
    private final String STYLE_EDIT;
    private final String STYLE_DELETE;
    private final String STYLE_DANGER;
    private final String STYLE_GREEN;
    private final String STYLE_BLUE;
    private final String STYLE_NEUTRAL;

    // Inicializar con el margen adecuado para esta pantalla
    {
        STYLE_PRIMARY = utilities.EstiloResponsivo.botonPrimary();
        STYLE_EDIT    = utilities.EstiloResponsivo.botonEdit();
        STYLE_DELETE  = utilities.EstiloResponsivo.botonDelete();
        STYLE_DANGER  = utilities.EstiloResponsivo.botonDanger();
        STYLE_GREEN   = utilities.EstiloResponsivo.botonGreen();
        STYLE_BLUE    = utilities.EstiloResponsivo.botonBlue();
        STYLE_NEUTRAL = utilities.EstiloResponsivo.botonPrimary();
    }

    private JTextField txtBuscar;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private JFormattedTextField txtFecha;
    private JFormattedTextField txtHora;

    // --- VARIABLES DE LOS BOTONES DE FILTRO ---
    private JButton btnFiltroEstado;
    private JButton btnFiltroMetodo;
    private String estadoActual = "Estado (Todos)";
    private String metodoActual = "Método (Todos)";

    private JButton btnBuscar;
    private JTable tabla;
    private DefaultTableModel modelo;

    public enum EstadoFactura implements TableBadgeCellRenderer.Info {
        PAGADO("Pagado",     Color.decode("#10b981")),
        POR_PAGAR("Por Pagar", Color.decode("#ef4444")),
        ANULADA("Anulada",   Color.decode("#9f9f9f")); // gris — la venta no cuenta

        private final String text;
        private final Color color;
        EstadoFactura(String text, Color color) { this.text = text; this.color = color; }
        @Override public String getText() { return text; }
        @Override public Color getColor() { return color; }
        @Override public Icon getIcon() { return null; }
    }

    public pHistorialVentas() {
        init();
        cargarDatos("", "");
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[center]", "[center]"));

        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", ""));

        JLabel lblTitulo = new JLabel("HISTORIAL DE VENTAS", JLabel.LEFT);
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lblTitulo.setBorder(new EmptyBorder(0, 10, 4, 0));

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Cliente, Cédula o Factura");
        txtBuscar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin:10,20,10,20;");
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/search.svg", 0.30f));

        // --- BOTÓN FILTRO ESTADO ---
        btnFiltroEstado = crearBoton("Estado", STYLE_NEUTRAL, "icons/filtro.svg");
        btnFiltroEstado.putClientProperty(FlatClientProperties.STYLE, "arc: 15; focusWidth: 0; innerFocusWidth: 0; background:#00e39b; font: bold; foreground: #ffffff;");
        JPopupMenu popupEstado = new JPopupMenu();
        for (String op : new String[]{"Estado (Todos)", "Pagado", "Por Pagar", "Anulada"}) {
            JMenuItem item = new JMenuItem(op);
            item.addActionListener(e -> {
                estadoActual = op;
                btnFiltroEstado.setText(op.contains("Todos") ? "Estado: Todos" : op);
                btnBuscar.doClick();
            });
            popupEstado.add(item);
        }
        btnFiltroEstado.addActionListener(e -> popupEstado.show(btnFiltroEstado, 0, btnFiltroEstado.getHeight()));

        // --- BOTÓN FILTRO MÉTODO PAGO ---
        btnFiltroMetodo = crearBoton("Método", STYLE_NEUTRAL, "icons/filtro.svg");
        btnFiltroMetodo.putClientProperty(FlatClientProperties.STYLE, "arc: 15; focusWidth: 0; innerFocusWidth: 0; background:#00e39b; font: bold; foreground: #ffffff;");
        JPopupMenu popupMetodo = new JPopupMenu();
        for (String op : new String[]{"Método (Todos)", "Efectivo", "Transferencia", "Mixto"}) {
            JMenuItem item = new JMenuItem(op);
            item.addActionListener(e -> {
                metodoActual = op;
                btnFiltroMetodo.setText(op.contains("Todos") ? "Método: Todos" : op);
                btnBuscar.doClick();
            });
            popupMetodo.add(item);
        }
        btnFiltroMetodo.addActionListener(e -> popupMetodo.show(btnFiltroMetodo, 0, btnFiltroMetodo.getHeight()));

        datePicker = new DatePicker();
        datePicker.setDateFormat("yyyy-MM-dd");
        datePicker.setCloseAfterSelected(true);
        txtFecha = new JFormattedTextField();
        txtFecha.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Fecha");
        txtFecha.putClientProperty(FlatClientProperties.STYLE, "arc: 15; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin:10,20,10,20;");
        txtFecha.setEditable(false);
        datePicker.setEditor(txtFecha);

        timePicker = new TimePicker();
        timePicker.set24HourView(true);
        txtHora = new JFormattedTextField();
        txtHora.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Hora");
        txtHora.putClientProperty(FlatClientProperties.STYLE, "arc: 15; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin:10,20,10,20;");
        txtHora.setEditable(false);
        timePicker.setEditor(txtHora);

        btnBuscar = crearBoton("", "#3b64ff", "#ffffff", "icons/search.svg");
        JButton btnAnular = crearBoton("Anular", STYLE_DANGER, "icons/cancelarventa.svg");
        btnAnular.setToolTipText("Anula la venta seleccionada y devuelve el stock");

        headerPanel.add(lblTitulo, "wrap, gapbottom 10");
        headerPanel.add(txtBuscar, "split 7, growx, pushx");
        headerPanel.add(btnFiltroEstado, "width 140:140:160");
        headerPanel.add(btnFiltroMetodo, "width 140:140:160");
        headerPanel.add(txtFecha, "width 110:110:130");
        headerPanel.add(txtHora, "width 90:90:110");
        headerPanel.add(btnBuscar, "gapleft 10, sizegroup btn, width 80:100:120");
        headerPanel.add(btnAnular, "sizegroup btn, width 120:140:160");

        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10", "[fill]", "[push]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 25; background:$Table.background;");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 5) return EstadoFactura.class;
                return super.getColumnClass(columnIndex);
            }
        };

        modelo.addColumn("N° Factura");  // 0
        modelo.addColumn("Cédula/RUC");  // 1
        modelo.addColumn("Cliente");     // 2
        modelo.addColumn("Fecha");       // 3
        modelo.addColumn("Total");       // 4
        modelo.addColumn("Estado");      // 5
        modelo.addColumn("Método Pago"); // 6
        modelo.addColumn("Vencimiento"); // 7
        modelo.addColumn("Pagado");      // 8
        modelo.addColumn("Archivo");     // 9
        modelo.addColumn("Correo");      // 10

        tabla = new JTable(modelo);
        tabla.setFocusable(false);

        // 1. ── Renderizador Genérico para Datos ──
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    if (col == 2) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }

                    if (col == 9 || col == 10) {
                        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    } else {
                        lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    }
                }
                return c;
            }
        });

        // 2. ── Renderizador del Header ──
        tabla.getTableHeader().setDefaultRenderer((t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            lbl.setForeground(Color.decode("#9f9f9f"));
            lbl.setOpaque(true);
            lbl.setBackground(t.getTableHeader().getBackground());

            if (col == 2) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }

            if (col == 9 || col == 10) {
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            } else {
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            }
            return lbl;
        });

        // 3. ── Anchos de Columna Milimétricos ──
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);  // N Factura
        tabla.getColumnModel().getColumn(1).setPreferredWidth(100); // Cedula
        tabla.getColumnModel().getColumn(2).setPreferredWidth(180); // Cliente
        tabla.getColumnModel().getColumn(3).setPreferredWidth(140); // Fecha
        tabla.getColumnModel().getColumn(4).setPreferredWidth(80);  // Total
        tabla.getColumnModel().getColumn(5).setPreferredWidth(90);  // Estado
        tabla.getColumnModel().getColumn(6).setPreferredWidth(100); // Metodo
        tabla.getColumnModel().getColumn(7).setPreferredWidth(90);  // Vencimiento
        tabla.getColumnModel().getColumn(8).setPreferredWidth(80);  // Pagado

        for (int i = 9; i <= 10; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(75);
            tabla.getColumnModel().getColumn(i).setMinWidth(75);
            tabla.getColumnModel().getColumn(i).setMaxWidth(85);
        }

        // 4. ── Estilos FlatLaf ──
        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background; font:bold +2; foreground: #9f9f9f");
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font: +1;");

        // 5. ── Insignia de Estado (Badge) ──
        TableBadgeCellRenderer.apply(tabla, EstadoFactura.class);

        // 6. ── Renderizadores de Iconos (Sin texto) ──
        FlatSVGIcon iconPdf = new FlatSVGIcon("icons/pdf.svg", 0.35f);
        iconPdf.setColorFilter(new FlatSVGIcon.ColorFilter(color -> java.awt.Color.decode("#e90005")));

        tabla.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText("");
                label.setIcon(iconPdf);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        FlatSVGIcon iconEnviar = new FlatSVGIcon("icons/email.svg", 0.35f);
        iconEnviar.setColorFilter(new FlatSVGIcon.ColorFilter(color -> java.awt.Color.decode("#5387ff")));

        tabla.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setText("");
                label.setIcon(iconEnviar);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");

        panel.add(scrollPane, "grow, push");

        // --- ACCIONES ---
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                int columna = tabla.columnAtPoint(e.getPoint());
                if (fila >= 0) {
                    String numeroFactura = tabla.getValueAt(fila, 0).toString();
                    if (columna == 9) { abrirFacturaPDF(numeroFactura); }
                    else if (columna == 10) { enviarFacturaPorCorreo(numeroFactura, tabla.getValueAt(fila, 1).toString(), tabla.getValueAt(fila, 2).toString()); }
                }
            }
        });

        tabla.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int columna = tabla.columnAtPoint(e.getPoint());
                tabla.setCursor(new Cursor((columna == 9 || columna == 10) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        // Listener del botón Anular Venta
        btnAnular.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) {
                raven.alerts.MessageAlerts.getInstance().showMessage(
                    "Selección requerida", "Selecciona una factura de la tabla para anularla.");
                return;
            }
            String noSerie = tabla.getValueAt(fila, 0).toString();
            String estadoActualFila = tabla.getValueAt(fila, 5) instanceof EstadoFactura
                ? ((EstadoFactura) tabla.getValueAt(fila, 5)).getText()
                : tabla.getValueAt(fila, 5).toString();

            if ("Anulada".equalsIgnoreCase(estadoActualFila)) {
                raven.alerts.MessageAlerts.getInstance().showMessage(
                    "Ya anulada", "La factura #" + noSerie + " ya está anulada.");
                return;
            }

            String cliente = tabla.getValueAt(fila, 2).toString();
            String total   = tabla.getValueAt(fila, 4).toString();

            raven.alerts.MessageAlerts.getInstance().showMessage(
                "¿Anular venta?",
                "Factura #" + noSerie + "\nCliente: " + cliente + "\nTotal: " + total +
                "\n\n⚠ Esto devolverá el stock y la venta no se contará en los reportes.\n¿Estás seguro?",
                raven.alerts.MessageAlerts.MessageType.WARNING,
                raven.alerts.MessageAlerts.OK_CANCEL_OPTION,
                (pc, i) -> {
                    if (i == raven.alerts.MessageAlerts.OK_OPTION) {
                        boolean ok = new dao.FacturaDAO().anularVenta(noSerie);
                        if (ok) {
                            raven.toast.Notifications.getInstance().show(
                                raven.toast.Notifications.Type.SUCCESS,
                                raven.toast.Notifications.Location.BOTTOM_CENTER,
                                "Venta #" + noSerie + " anulada — stock restaurado");

                            utilities.NotificacionManager.getInstance().agregar(
                                new Model.Notificacion(
                                    "🚫 Venta Anulada",
                                    "Factura #" + noSerie + " de " + cliente + " — " + total,
                                    Model.Notificacion.Tipo.INFO));

                            // Recargar la tabla para reflejar el nuevo estado
                            cargarDatos(txtBuscar.getText().trim(), "");
                        } else {
                            raven.alerts.MessageAlerts.getInstance().showMessage(
                                "Error", "No se pudo anular la venta. Intenta nuevamente.");
                        }
                    }
                });
        });

        btnBuscar.addActionListener(e -> {
            String fechaBuscada = "";
            if (datePicker.isDateSelected()) {
                fechaBuscada = datePicker.getSelectedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (timePicker.isTimeSelected()) {
                    fechaBuscada += " " + timePicker.getSelectedTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                }
            }
            cargarDatos(txtBuscar.getText().trim(), fechaBuscada);
        });

        add(headerPanel, "wrap, growx");
        add(panel, "grow, push");
    }

    private void cargarDatos(String busqueda, String fecha) {
        modelo.setRowCount(0);
        FacturaDAO dao = new FacturaDAO();
        List<Object[]> facturas = dao.listarFacturas(busqueda, fecha);

        for (Object[] fac : facturas) {
            String estadoBD = fac[5].toString();
            String metodoBD = fac[6] != null ? fac[6].toString() : "N/A";

            if (!estadoActual.contains("Todos") && !estadoActual.equalsIgnoreCase(estadoBD)) continue;
            if (!metodoActual.contains("Todos") && !metodoActual.equalsIgnoreCase(metodoBD)) continue;

            Object[] nuevaFila = new Object[11];
            for (int ci = 0; ci < Math.min(fac.length, 8); ci++) nuevaFila[ci] = fac[ci];

            if (estadoBD.equalsIgnoreCase("Pagado")) {
                nuevaFila[5] = EstadoFactura.PAGADO;
            } else if (estadoBD.equalsIgnoreCase("Anulada")) {
                nuevaFila[5] = EstadoFactura.ANULADA;
            } else {
                nuevaFila[5] = EstadoFactura.POR_PAGAR;
            }

            nuevaFila[8] = fac.length > 8 ? fac[8] : "$0.00";
            nuevaFila[9]  = "Abrir";
            nuevaFila[10] = "Enviar";
            modelo.addRow(nuevaFila);
        }
    }

    private void abrirFacturaPDF(String numFac) {
        try {
            String rutaCarpeta = System.getProperty("user.home") + File.separator + "Ivencore" + File.separator + "Facturas";
            File archivoPDF = new File(rutaCarpeta + File.separator + "Factura_" + numFac + ".pdf");
            if (archivoPDF.exists()) { java.awt.Desktop.getDesktop().open(archivoPDF); }
            else { raven.alerts.MessageAlerts.getInstance().showMessage("PDF no encontrado", "No se encontró el archivo PDF de esta factura. Quizás nunca se generó."); }
        } catch (Exception ex) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Error al abrir PDF", "No se pudo abrir el archivo: " + ex.getMessage());
        }
    }

    private void enviarFacturaPorCorreo(String numFac, String cedula, String nombreCliente) {
        String rutaCarpeta = System.getProperty("user.home") + File.separator + "Ivencore" + File.separator + "Facturas";
        File archivoPDF = new File(rutaCarpeta + File.separator + "Factura_" + numFac + ".pdf");
        if (!archivoPDF.exists()) { raven.alerts.MessageAlerts.getInstance().showMessage("PDF no encontrado", "No se encontró el PDF de esta factura en el sistema."); return; }
        if (cedula.equals("9999999999")) { raven.alerts.MessageAlerts.getInstance().showMessage("Sin correo", "No se puede enviar correo a Consumidor Final porque no tiene datos registrados."); return; }

        ClientesDAO cDao = new ClientesDAO();
        List<Cliente> clientes = cDao.buscar(cedula);
        if (clientes.isEmpty() || clientes.get(0).getCorreo() == null || clientes.get(0).getCorreo().trim().isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Sin correo", "Este cliente no tiene un correo electrónico registrado."); return;
        }

        String correoDestino = clientes.get(0).getCorreo();
        raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.INFO, raven.toast.Notifications.Location.BOTTOM_CENTER, "Conectando al servidor para enviar correo...");

        String emailRemitente = utilities.ConfigManager.get("email.remitente", "");
        String emailClave     = utilities.ConfigManager.getEmailClaveSmtp();
        if (emailRemitente.isEmpty() || emailClave.isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Sin correo configurado",
                    "No tienes correo configurado. Ve a Configuración y agrega tu correo de envío.");
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                String negNombre = utilities.ConfigManager.get("negocio.nombre", "Tu Negocio");
                services.Email email = new services.Email(emailRemitente, emailClave);
                return email.enviarEmailConAdjunto(
                        correoDestino,
                        "Factura #" + numFac + " de " + negNombre,
                        "Estimado(a) " + nombreCliente + ",\n\nAdjunto encontrarás tu factura #" + numFac + ".\n\nGracias por su compra.\n\n" + negNombre,
                        archivoPDF);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS, raven.toast.Notifications.Location.BOTTOM_CENTER, "¡Factura enviada!");
                    } else {
                        raven.alerts.MessageAlerts.getInstance().showMessage("Error al enviar",
                                "No se pudo enviar el correo. Revisa la configuración de correo.");
                    }
                } catch (java.util.concurrent.ExecutionException ex) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Error", ex.getCause().getMessage());
                } catch (Exception ex) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Error inesperado", ex.getMessage());
                }
            }
        }.execute();
    }

    private FlatSVGIcon crearIcono(String path, float scale, String hexColor) {
        FlatSVGIcon icon = new FlatSVGIcon(path, scale);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.decode(hexColor)));
        return icon;
    }

    private JButton crearBoton(String texto, String estilo, String iconPath) {
        JButton btn = new JButton(texto);
        btn.setIcon(crearIcono(iconPath, 0.30f, "#ffffff"));
        btn.putClientProperty(FlatClientProperties.STYLE, estilo);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBoton(String texto, String bg, String fg, String iconPath) {
        String estilo;
        if ("#3b64ff".equals(bg) || "#3b82f6".equals(bg) || "#86c5de".equals(bg)) estilo = STYLE_PRIMARY;
        else if ("#10b981".equals(bg)) estilo = STYLE_GREEN;
        else if ("#ff0000".equals(bg) || "#ef4444".equals(bg)) estilo = STYLE_DANGER;
        else if ("#ffda49".equals(bg)) estilo = STYLE_EDIT;
        else if (bg == null) estilo = STYLE_DELETE;
        else estilo = STYLE_NEUTRAL;
        return crearBoton(texto, estilo, iconPath);
    }
}