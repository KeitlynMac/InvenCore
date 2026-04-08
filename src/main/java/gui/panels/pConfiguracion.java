package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.DropShadowBorder;
import raven.toast.Notifications;
import utilities.ConfigManager;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


// Configuración del sistema.
// Aquí se guardan los datos del negocio para el PDF y el correo de envío automático.

// Panel de configuración del negocio y del correo de envío automático.
// Los datos se guardan en la tabla Configuracion de la BD (incluida en los backups).
// La clave de correo se guarda hasheada — nunca en texto plano.
public class pConfiguracion extends TabbedForm {

    // --- Campos del Negocio ---
    private JTextField txtNombre;
    private JTextField txtRuc;
    private JTextField txtDireccion;
    private JTextField txtTelefono;
    private JTextField txtEmailNegocio;

    // --- Campos de Correo (para envío de facturas) ---
    private JTextField txtEmailRemitente;
    private JPasswordField txtEmailClave;

    public pConfiguracion() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 20", "[fill, grow]", "[][][][]"));

        JLabel lblTitulo = new JLabel("CONFIGURACIÓN");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lblTitulo.setBorder(new EmptyBorder(0, 10, 10, 0));
        add(lblTitulo);

        // ============================================
        // SECCIÓN 1: Datos del Negocio
        // ============================================
        JPanel panelNegocio = crearPanel("Datos del Negocio (aparecen en las facturas PDF)");

        txtNombre       = crearCampo("Ej: Mi Tienda");
        txtRuc          = crearCampo("Ej: 0910000000001");
        txtDireccion    = crearCampo("Ej: Av. Principal 123");
        txtTelefono     = crearCampo("Ej: 09xxxxxxxx");
        txtEmailNegocio = crearCampo("Ej: negocio@correo.com");

        agregarFila(panelNegocio, "Nombre del Negocio: *", txtNombre);
        agregarFila(panelNegocio, "RUC / CI:", txtRuc);
        agregarFila(panelNegocio, "Dirección:", txtDireccion);
        agregarFila(panelNegocio, "Teléfono:", txtTelefono);
        agregarFila(panelNegocio, "Email del Negocio:", txtEmailNegocio);
        add(panelNegocio, "gaptop 5");

        // ============================================
        // SECCIÓN 2: Correo para envío de facturas
        // ============================================
        JPanel panelEmail = crearPanel("Correo para Envío Automático de Facturas (Gmail)");

        JLabel lblAviso = new JLabel("<html><body style='width:500px; color:#f59e0b;'>" +
                "Para usar Gmail: activa la verificación en 2 pasos en tu cuenta Google y luego genera una " +
                "\"Contraseña de aplicación\" en myaccount.google.com -> Seguridad. Usa esa clave aquí, NO tu contraseña de Gmail." +
                "</body></html>");
        panelEmail.add(lblAviso, "span 2, growx, gapbottom 10");

        txtEmailRemitente = crearCampo("tucorreo@gmail.com");
        txtEmailClave     = new JPasswordField();
        txtEmailClave.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; borderWidth:0; margin:8,10,8,10; showRevealButton:true;");
        txtEmailClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña de aplicación de Gmail");

        agregarFila(panelEmail, "Correo Remitente:", txtEmailRemitente);
        agregarFila(panelEmail, "Clave de Aplicación:", txtEmailClave);

        add(panelEmail, "gaptop 5");

        // ============================================
        // SECCIÓN 3: Botones de acción
        // ============================================
        JPanel panelBotones = new JPanel(new MigLayout("insets 0", "push[][]", ""));
        panelBotones.putClientProperty(FlatClientProperties.STYLE, "background:null");

        JButton btnProbar = new JButton("Probar Correo");
        btnProbar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; background:#3b82f6; foreground:#ffffff; font:bold; margin:8,20,8,20;");

        JButton btnGuardar = new JButton("Guardar Configuración");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; background:#10b981; foreground:#ffffff; font:bold; margin:8,20,8,20;");

        panelBotones.add(btnProbar);
        panelBotones.add(btnGuardar);
        add(panelBotones, "gaptop 10, growx");

        // --- Cargar datos actuales ---
        cargarDatos();

        // --- Eventos ---
        btnGuardar.addActionListener(e -> guardarDatos());
        btnProbar.addActionListener(e -> probarCorreo());
    }

    // Lee la configuración guardada en la BD y llena los campos del formulario.
    private void cargarDatos() {
        txtNombre.setText(ConfigManager.get("negocio.nombre", ""));
        txtRuc.setText(ConfigManager.get("negocio.ruc", ""));
        txtDireccion.setText(ConfigManager.get("negocio.direccion", ""));
        txtTelefono.setText(ConfigManager.get("negocio.telefono", ""));
        txtEmailNegocio.setText(ConfigManager.get("negocio.email", ""));
        txtEmailRemitente.setText(ConfigManager.get("email.remitente", ""));
        // No mostramos la clave almacenada (está hasheada/ofuscada)
        // El campo queda vacío — solo se actualiza si el usuario escribe una nueva clave
        txtEmailClave.setText("");
        if (!ConfigManager.get("email.clave.smtp", "").isEmpty()) {
            txtEmailClave.putClientProperty("JTextField.placeholderText", "(clave guardada — escribe para cambiarla)");
        }
    }

    // Valida y guarda todos los campos en la BD. La clave solo se actualiza si se escribió una nueva.
    private void guardarDatos() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Campo requerido", "El nombre del negocio no puede estar vacío.");
            return;
        }

        Map<String, String> valores = new HashMap<>();
        valores.put("negocio.nombre",    nombre);
        valores.put("negocio.ruc",       txtRuc.getText().trim());
        valores.put("negocio.direccion", txtDireccion.getText().trim());
        valores.put("negocio.telefono",  txtTelefono.getText().trim());
        valores.put("negocio.email",     txtEmailNegocio.getText().trim());
        valores.put("email.remitente",   txtEmailRemitente.getText().trim());
        ConfigManager.setMultiple(valores);

        // La clave de correo se guarda hasheada/ofuscada si se ingresó una nueva
        String nuevaClave = new String(txtEmailClave.getPassword()).trim();
        if (!nuevaClave.isEmpty()) {
            ConfigManager.setEmailClave(nuevaClave);
        }

        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                Notifications.Location.BOTTOM_CENTER, "Configuración guardada correctamente.");
    }

    // Envía un correo de prueba para verificar que la configuración SMTP es correcta.
    private void probarCorreo() {
        String remitente = txtEmailRemitente.getText().trim();
        String clave     = new String(txtEmailClave.getPassword());

        if (remitente.isEmpty() || clave.isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Faltan datos", "Completa el correo y la clave de aplicación antes de probar.");
            return;
        }

        Notifications.getInstance().show(Notifications.Type.INFO,
                Notifications.Location.BOTTOM_CENTER, "Enviando correo de prueba...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    services.Email email = new services.Email(remitente, clave);
                    return email.enviarEmailConAdjunto(
                            remitente,
                            "✅ Prueba de correo — ivencore",
                            "Si recibes este mensaje, la configuración de correo es correcta.",
                            null
                    );
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                Notifications.Location.BOTTOM_CENTER, "¡Correo de prueba enviado con éxito!");
                    } else {
raven.alerts.MessageAlerts.getInstance().showMessage("Error al enviar", "No se pudo enviar. Usa la Contraseña de Aplicación de Google, no tu clave normal.");
                    }
                } catch (Exception ex) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Error inesperado", ex.getMessage());
                }
            }
        }.execute();
    }

    // ---- Helpers visuales ----

    private JPanel crearPanel(String titulo) {
        JPanel panel = new JPanel(new MigLayout("wrap 2, fillx, insets 20", "[170][grow, fill]", "[]8[]"));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:20; [light]background:darken(@background,4%); [dark]background:lighten(@background,4%)");
        panel.setBorder(new DropShadowBorder(new java.awt.Insets(2, 2, 5, 2), 10));

        JLabel lbTitulo = new JLabel(titulo);
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +2;");
        lbTitulo.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lbTitulo, "span 2, growx, gapbottom 5");

        return panel;
    }

    private void agregarFila(JPanel panel, String labelText, JComponent campo) {
        JLabel lb = new JLabel(labelText);
        lb.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");
        panel.add(lb);
        panel.add(campo, "growx");
    }

    private JTextField crearCampo(String placeholder) {
        JTextField tf = new JTextField();
        tf.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; borderWidth:0; margin:8,10,8,10;");
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return tf;
    }
}
