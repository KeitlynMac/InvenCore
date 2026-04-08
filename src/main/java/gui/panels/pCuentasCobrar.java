package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import dao.FacturaDAO;
import raven.popup.GlassPanePopup;
import net.miginfocom.swing.MigLayout;
import raven.alerts.MessageAlerts;
import raven.modal.component.DropShadowBorder;
import raven.toast.Notifications;
import utilities.HeaderTabla;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel de Cuentas por Cobrar.
 * Muestra todas las facturas con estado "Por Pagar" y permite
 * marcarlas como pagadas o enviar recordatorio por correo.
 */

// Lista las facturas que aún no han sido pagadas completamente.
// Las filas en rojo son facturas vencidas.
// Permite marcar como pagadas y enviar recordatorios por correo.

// Panel de cuentas por cobrar.
// Lista las facturas pendientes de pago y permite registrar abonos parciales o totales.
// Después de cada abono genera un recibo PDF y lo envía por correo al cliente.
public class pCuentasCobrar extends TabbedForm {

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


    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lbTotalDeuda;

    public pCuentasCobrar() { init(); }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[fill, grow]", "[][grow]"));

        // ---- HEADER ----
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", ""));

        JLabel lbTitulo = new JLabel("CUENTAS POR COBRAR");
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbTitulo.setBorder(new EmptyBorder(0, 10, 4, 0));

        lbTotalDeuda = new JLabel("Total pendiente: $0.00");
        lbTotalDeuda.putClientProperty(FlatClientProperties.STYLE, "font:bold +3; foreground:#ef4444;");

        JButton btnMarcarPagado = crearBoton("Pagado", "#10b981", "#ffffff", "icons/cash.svg");
        JButton btnEnviarRecordatorio = crearBoton("Recordatorio", "#3b82f6", "#ffffff", "icons/email.svg");

        headerPanel.add(lbTitulo, "wrap, gapbottom 5");
        headerPanel.add(lbTotalDeuda, "wrap, gapbottom 10");
        headerPanel.add(btnMarcarPagado, "split 2, align right, gapleft push");
        headerPanel.add(btnEnviarRecordatorio, "wrap");

        // ---- TABLA ----
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10", "[fill]", "[push]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:25; background:$Table.background;");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        modelo.addColumn("N° Factura");
        modelo.addColumn("Cliente");
        modelo.addColumn("Cédula/RUC");
        modelo.addColumn("Fecha Venta");
        modelo.addColumn("Total");
        modelo.addColumn("Pagado");
        modelo.addColumn("Deuda");
        modelo.addColumn("Vencimiento");

        tabla = new JTable(modelo);
        tabla.setFocusable(false);
        tabla.getTableHeader().setDefaultRenderer(new HeaderTabla(tabla));
        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
            "height:30; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background; font:bold +2; foreground:#9f9f9f");
        tabla.putClientProperty(FlatClientProperties.STYLE,
            "rowHeight:50; selectionBackground:#009991; selectionForeground:#ffffff; showHorizontalLines:true; intercellSpacing:0,1;" +
            "cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font:+1;");

        // Colorear filas vencidas en rojo
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    String venc = modelo.getValueAt(row, 7) != null ? modelo.getValueAt(row, 7).toString() : "";
                    try {
                        java.time.LocalDate fechaVenc = java.time.LocalDate.parse(venc.substring(0, 10));
                        if (fechaVenc.isBefore(java.time.LocalDate.now())) {
                            c.setBackground(new Color(255, 230, 230)); // Rojo claro = vencida
                        } else {
                            c.setBackground(t.getBackground());
                        }
                    } catch (Exception ignored) { c.setBackground(t.getBackground()); }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
            "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");
        panel.add(scroll, "grow, push");

        add(headerPanel, "wrap, growx");
        add(panel, "grow, push");

        cargarDatos();

        btnMarcarPagado.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección", "Selecciona una factura primero."); return; }

            String noSerie = tabla.getValueAt(fila, 0).toString();
            String cliente = tabla.getValueAt(fila, 1).toString();

            // Obtener deuda actual de la BD para mostrar en el popup
            double deudaActual = new FacturaDAO().obtenerDeuda(noSerie);
            if (deudaActual <= 0) {
                MessageAlerts.getInstance().showMessage("Sin deuda", "Esta factura ya está pagada en su totalidad.");
                return;
            }

            // Popup para ingresar el monto del pago (puede ser parcial)
            JPanel pPago = new JPanel(new net.miginfocom.swing.MigLayout("wrap, fillx, insets 15", "[grow, fill]"));
            JLabel lblInfo = new JLabel("Factura #" + noSerie + " — Cliente: " + cliente);
            lblInfo.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "font:bold;");
            JLabel lblDeuda = new JLabel("Deuda pendiente: $" + String.format("%.2f", deudaActual));
            lblDeuda.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "foreground:#ef4444; font:bold +2;");
            JLabel lblCampo = new JLabel("¿Cuánto abona ahora?");
            JTextField txtMonto = new JTextField(String.format("%.2f", deudaActual));
            txtMonto.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:10; margin:8,10,8,10; font:bold +3;");
            txtMonto.selectAll();

            pPago.add(lblInfo);
            pPago.add(lblDeuda, "gaptop 4");
            pPago.add(lblCampo, "gaptop 12");
            pPago.add(txtMonto, "growx");

            raven.popup.DefaultOption opt = new raven.popup.DefaultOption() {
                @Override public boolean closeWhenClickOutside() { return true; }
            };
            raven.popup.GlassPanePopup.showPopup(new raven.popup.component.SimplePopupBorder(
                pPago, "Registrar Pago",
                new String[]{"Cancelar", "Registrar Pago"},
                (pc2, i2) -> {
                    if (i2 == 1) {
                        try {
                            double montoPagado = Double.parseDouble(txtMonto.getText().replace(",", ".").trim());
                            if (montoPagado <= 0) {
                                MessageAlerts.getInstance().showMessage("Monto inválido", "El monto debe ser mayor a 0.");
                                return;
                            }
                            boolean ok = new FacturaDAO().registrarPagoParcial(noSerie, montoPagado);
                            if (ok) {
                                double deudaNueva = new FacturaDAO().obtenerDeuda(noSerie);
                                String msg = deudaNueva <= 0.001
                                    ? "Factura #" + noSerie + " pagada completamente ✅"
                                    : "Abono registrado. Deuda restante: $" + String.format("%.2f", deudaNueva);
                                Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                    Notifications.Location.BOTTOM_CENTER, msg);
                                utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                                    "💰 Pago Registrado",
                                    "Factura #" + noSerie + " — abono de $" + String.format("%.2f", montoPagado),
                                    Model.Notificacion.Tipo.COBRO));
                                pc2.closePopup();
                                cargarDatos();

                                // Generar recibo PDF del abono en hilo separado
                                final double deudaAntFinal = deudaActual;
                                final double deudaNuevaFinal = deudaNueva;
                                final double montoFinal = montoPagado;
                                new javax.swing.SwingWorker<java.io.File, Void>() {
                                    String correoCliente = "";
                                    boolean correoEnviado = false;

                                    @Override protected java.io.File doInBackground() {
                                        // Obtener datos del cliente desde la BD
                                        java.util.Map<String, String> datos = new FacturaDAO().obtenerDatosFactura(noSerie);
                                        correoCliente = datos.getOrDefault("correo", "");

                                        java.io.File recibo = services.GeneradorReciboDeuda.generarRecibo(
                                            noSerie,
                                            datos.getOrDefault("cliente", cliente),
                                            datos.getOrDefault("cedula",  ""),
                                            montoFinal, deudaAntFinal, deudaNuevaFinal);

                                        // Guardar la ruta del recibo en la BD
                                        if (recibo != null) {
                                            new FacturaDAO().actualizarRutaRecibo(noSerie, montoFinal, recibo.getAbsolutePath());

                                            // Enviar el recibo por correo si el cliente tiene email y hay config de correo
                                            String emailRemitente = utilities.ConfigManager.get("email.remitente", "");
                                            String emailClave     = utilities.ConfigManager.getEmailClaveSmtp();
                                            if (!correoCliente.isEmpty() && !emailRemitente.isEmpty() && !emailClave.isEmpty()) {
                                                try {
                                                    String negNombre = utilities.ConfigManager.get("negocio.nombre", "Tu Negocio");
                                                    String cuerpo = "Estimado(a) " + datos.getOrDefault("cliente", cliente) + ",\n\n" +
                                                        "Le confirmamos que hemos recibido su abono de $" +
                                                        String.format("%.2f", montoFinal) +
                                                        " correspondiente a la factura #" + noSerie + ".\n\n" +
                                                        "Deuda restante: $" + String.format("%.2f", deudaNuevaFinal) +
                                                        (deudaNuevaFinal <= 0.001 ? "\nSu cuenta ha quedado SALDADA. ¡Gracias por su pago!" :
                                                         "\nQueda pendiente el saldo indicado.") +
                                                        "\n\nAdjunto encontrará su recibo de pago.\n\nAtentamente,\n" + negNombre;

                                                    new services.Email(emailRemitente, emailClave)
                                                        .enviarEmailConAdjunto(
                                                            correoCliente,
                                                            "Recibo de Abono — Factura #" + noSerie + " | " + negNombre,
                                                            cuerpo,
                                                            recibo);
                                                    correoEnviado = true;
                                                } catch (Exception ex) {
                                                    System.err.println("Error enviando recibo por correo: " + ex.getMessage());
                                                }
                                            }
                                        }
                                        return recibo;
                                    }

                                    @Override protected void done() {
                                        try {
                                            java.io.File recibo = get();
                                            if (recibo != null) {
                                                // Si se envió el correo, avisarlo en la notificación
                                                if (correoEnviado) {
                                                    Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                                        Notifications.Location.BOTTOM_CENTER,
                                                        "📧 Recibo enviado a " + correoCliente);
                                                }
                                                // Preguntar si abrir el recibo localmente
                                                MessageAlerts.getInstance().showMessage(
                                                    correoEnviado ? "Recibo enviado y generado" : "Recibo generado",
                                                    (correoEnviado ? "El recibo fue enviado a " + correoCliente + ".\n" : "") +
                                                    "¿Quieres abrirlo también aquí?",
                                                    MessageAlerts.MessageType.SUCCESS,
                                                    MessageAlerts.OK_CANCEL_OPTION,
                                                    (pc3, i3) -> {
                                                        if (i3 == MessageAlerts.OK_OPTION) {
                                                            try { java.awt.Desktop.getDesktop().open(recibo); }
                                                            catch (Exception ex) {
                                                                MessageAlerts.getInstance().showMessage(
                                                                    "Error", "No se pudo abrir el recibo: " + ex.getMessage());
                                                            }
                                                        }
                                                    });
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }.execute();
                            } else {
                                MessageAlerts.getInstance().showMessage("Error", "No se pudo registrar el pago.");
                            }
                        } catch (NumberFormatException ex) {
                            MessageAlerts.getInstance().showMessage("Formato incorrecto", "Ingresa un número válido. Ej: 25.50");
                        }
                    } else pc2.closePopup();
                }
            ), opt);
        });

        btnEnviarRecordatorio.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección", "Selecciona una factura."); return; }
            enviarRecordatorio(fila);
        });
    }

    // Recarga las facturas pendientes y actualiza el total de deuda.
    private void cargarDatos() {
        List<Object[]> pendientes = new FacturaDAO().listarCuentasPorCobrar();
        modelo.setRowCount(0);
        double totalDeuda = 0;
        for (Object[] row : pendientes) {
            modelo.addRow(row);
            try { totalDeuda += Double.parseDouble(row[6].toString().replace("$", "")); } catch (Exception ignored) {}
        }
        lbTotalDeuda.setText(String.format("Total pendiente: $%.2f", totalDeuda));
    }

    // Envía un correo de recordatorio de pago al cliente de la fila seleccionada.
    private void enviarRecordatorio(int fila) {
        String emailRemitente = utilities.ConfigManager.get("email.remitente", "");
        String emailClave     = utilities.ConfigManager.getEmailClaveSmtp();
        if (emailRemitente.isEmpty()) {
            MessageAlerts.getInstance().showMessage("Sin correo configurado", "Ve a Configuración y agrega tu correo de envío primero.");
            return;
        }
        String noSerie  = tabla.getValueAt(fila, 0).toString();
        String cliente  = tabla.getValueAt(fila, 1).toString();
        String cedula   = tabla.getValueAt(fila, 2).toString();
        String deuda    = tabla.getValueAt(fila, 6).toString();
        String vencimiento = tabla.getValueAt(fila, 7) != null ? tabla.getValueAt(fila, 7).toString() : "N/A";

        if (cedula.equals("9999999999")) {
            MessageAlerts.getInstance().showMessage("Sin destinatario", "No se puede enviar correo a un Consumidor Final porque no tiene datos registrados."); return;
        }
        dao.ClientesDAO cDao = new dao.ClientesDAO();
        java.util.List<Model.Cliente> clientes = cDao.buscar(cedula);
        if (clientes.isEmpty() || clientes.get(0).getCorreo() == null || clientes.get(0).getCorreo().isEmpty()) {
            MessageAlerts.getInstance().showMessage("Sin correo", "Este cliente no tiene correo electrónico registrado."); return;
        }
        String correo = clientes.get(0).getCorreo();
        String negNombre = utilities.ConfigManager.get("negocio.nombre", "Tu Negocio");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    String cuerpo = "Estimado(a) " + cliente + ",\n\n" +
                        "Le recordamos que tiene una deuda pendiente de " + deuda +
                        " correspondiente a la factura #" + noSerie + ".\n" +
                        "Fecha de vencimiento: " + vencimiento + ".\n\n" +
                        "Por favor, realice su pago a la brevedad posible.\n\n" +
                        "Atentamente,\n" + negNombre;
                    return new services.Email(emailRemitente, emailClave)
                        .enviarEmailConAdjunto(correo, "Recordatorio de Pago — " + negNombre, cuerpo, null);
                } catch (Exception e) { return false; }
            }
            @Override protected void done() {
                try {
                    if (get()) Notifications.getInstance().show(Notifications.Type.SUCCESS,
                        Notifications.Location.BOTTOM_CENTER, "Recordatorio enviado a " + correo);
                    else MessageAlerts.getInstance().showMessage("Error al enviar", "No se pudo enviar el correo. Revisa la configuración.");
                } catch (Exception ex) { MessageAlerts.getInstance().showMessage("Error inesperado", ex.getMessage()); }
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
