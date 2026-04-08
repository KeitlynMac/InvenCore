package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import dao.UsuarioDAO;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.DropShadowBorder;
import raven.toast.Notifications;
import utilities.SesionUsuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.*;


// Panel de perfil del usuario.
// Permite cambiar nombre, foto y contraseña del usuario logueado.

// Panel del perfil del usuario logueado.
// Permite cambiar el nombre, la foto y la contraseña.
public class pPerfil extends utilities.TabbedForm {

    private JLabel         lblFotoPreview;
    private JTextField     txtNombreCompleto;
    private JTextField     txtUsuario;
    private JPasswordField txtClaveActual;
    private JPasswordField txtNuevaClave;
    private JPasswordField txtConfirmarClave;
    private String         nuevaRutaFoto;

    public pPerfil() { init(); }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 20", "[fill, grow]", "[][grow]"));

        JLabel lbTitulo = new JLabel("MI PERFIL");
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbTitulo.setBorder(new EmptyBorder(0, 10, 10, 0));
        add(lbTitulo);

        JPanel contenido = new JPanel(new MigLayout("fill, insets 0, gap 20", "[250!][grow, fill]", "[grow, fill]"));
        contenido.putClientProperty(FlatClientProperties.STYLE, "background:null");

        // ==============================
        // COLUMNA IZQUIERDA: Avatar
        // ==============================
        JPanel panelFoto = new JPanel(new MigLayout("wrap, fillx, insets 30", "[center]", "[]15[]15[]"));
        panelFoto.putClientProperty(FlatClientProperties.STYLE,
            "arc:20; [light]background:darken(@background,4%); [dark]background:lighten(@background,4%)");
        panelFoto.setBorder(new DropShadowBorder(new Insets(2, 2, 5, 2), 10));

        lblFotoPreview = new JLabel();
        lblFotoPreview.setPreferredSize(new Dimension(130, 130));
        lblFotoPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblFotoPreview.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100, 60), 2));
        cargarFotoActual();

        JButton btnCambiarFoto = new JButton("Cambiar Foto");
        btnCambiarFoto.putClientProperty(FlatClientProperties.STYLE,
            "arc:15; background:#3b82f6; foreground:#ffffff; font:bold; margin:8,20,8,20;");

        JLabel lblNombreDisplay = new JLabel(SesionUsuario.getInstance().getNombreDisplay(), SwingConstants.CENTER);
        lblNombreDisplay.putClientProperty(FlatClientProperties.STYLE, "font:bold +4;");

        JLabel lblUserTag = new JLabel("@" + (SesionUsuario.getInstance().getUsuario() != null
            ? SesionUsuario.getInstance().getUsuario().getUser() : ""), SwingConstants.CENTER);
        lblUserTag.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font: +1;");

        panelFoto.add(lblFotoPreview, "width 130!, height 130!");
        panelFoto.add(btnCambiarFoto, "growx");
        panelFoto.add(lblNombreDisplay, "growx");
        panelFoto.add(lblUserTag, "growx");

        // ==============================
        // COLUMNA DERECHA: Datos
        // ==============================
        JPanel panelDatos = new JPanel(new MigLayout("wrap, fillx, insets 25", "[grow, fill]", "[]8[]15[]8[]15[]8[]8[]8[]"));
        panelDatos.putClientProperty(FlatClientProperties.STYLE,
            "arc:20; [light]background:darken(@background,4%); [dark]background:lighten(@background,4%)");
        panelDatos.setBorder(new DropShadowBorder(new Insets(2, 2, 5, 2), 10));

        JLabel secInfo = new JLabel("Información Personal");
        secInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        panelDatos.add(secInfo);

        txtNombreCompleto = crearCampo("Tu nombre completo");
        txtUsuario = crearCampo("");
        txtUsuario.setEditable(false);
        txtUsuario.putClientProperty(FlatClientProperties.STYLE,
            "arc:15; borderWidth:0; margin:8,10,8,10; [light]background:darken(@background,3%); [dark]background:lighten(@background,6%);");

        panelDatos.add(crearLabel("Nombre Completo:"));
        panelDatos.add(txtNombreCompleto, "growx");
        panelDatos.add(crearLabel("Usuario (no editable):"));
        panelDatos.add(txtUsuario, "growx");

        JLabel secClave = new JLabel("Cambiar Contraseña");
        secClave.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        panelDatos.add(secClave, "gaptop 10");

        txtClaveActual    = crearPassword("Contraseña actual");
        txtNuevaClave     = crearPassword("Nueva contraseña (mín. 6 caracteres)");
        txtConfirmarClave = crearPassword("Confirmar nueva contraseña");

        panelDatos.add(crearLabel("Clave Actual:"));
        panelDatos.add(txtClaveActual, "growx");
        panelDatos.add(crearLabel("Nueva Clave:"));
        panelDatos.add(txtNuevaClave, "growx");
        panelDatos.add(crearLabel("Confirmar Clave:"));
        panelDatos.add(txtConfirmarClave, "growx");

        JButton btnGuardar = new JButton("Guardar Cambios");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
            "arc:15; background:#10b981; foreground:#ffffff; font:bold; margin:10,25,10,25;");
        panelDatos.add(btnGuardar, "gaptop 15, align right");

        contenido.add(panelFoto,  "growy");
        contenido.add(panelDatos, "grow, push");
        add(contenido, "grow, push");

        cargarDatosUsuario();

        // ---- Eventos ----
        btnCambiarFoto.addActionListener(e -> seleccionarFoto());
        btnGuardar.addActionListener(e -> guardarCambios(lblNombreDisplay, lblUserTag));
    }

    private void cargarDatosUsuario() {
        Model.Usuario u = SesionUsuario.getInstance().getUsuario();
        if (u == null) return;
        txtNombreCompleto.setText(u.getNombreCompleto() != null ? u.getNombreCompleto() : "");
        txtUsuario.setText(u.getUser());
    }

    private void cargarFotoActual() {
        String ruta = SesionUsuario.getInstance().getFotoPath();
        if (ruta != null && new File(ruta).exists()) {
            ImageIcon ico = new ImageIcon(new ImageIcon(ruta).getImage()
                .getScaledInstance(130, 130, Image.SCALE_SMOOTH));
            lblFotoPreview.setIcon(ico);
            lblFotoPreview.setText("");
        } else {
            lblFotoPreview.setIcon(null);
            lblFotoPreview.setText("<html><center>Sin<br>foto</center></html>");
        }
    }

    private void seleccionarFoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File fSel = fc.getSelectedFile();
            try {
                String destDir = System.getProperty("user.dir") + java.io.File.separator + "perfiles";
                new java.io.File(destDir).mkdirs();
                String dest = destDir + java.io.File.separator + fSel.getName();
                Files.copy(fSel.toPath(), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
                nuevaRutaFoto = dest;
                ImageIcon ico = new ImageIcon(new ImageIcon(dest).getImage()
                    .getScaledInstance(130, 130, Image.SCALE_SMOOTH));
                lblFotoPreview.setIcon(ico);
                lblFotoPreview.setText("");
            } catch (Exception ex) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Error de imagen", "No se pudo cargar la imagen: " + ex.getMessage());
            }
        }
    }

    private void guardarCambios(JLabel lblNombreDisplay, JLabel lblUserTag) {
        Model.Usuario u = SesionUsuario.getInstance().getUsuario();
        if (u == null) return;

        String nombreNuevo = txtNombreCompleto.getText().trim();
        String claveActual = new String(txtClaveActual.getPassword());
        String claveNueva  = new String(txtNuevaClave.getPassword());
        String claveConf   = new String(txtConfirmarClave.getPassword());

        // Si quiere cambiar contraseña, validar
        String claveParaGuardar = null;
        if (!claveNueva.isEmpty() || !claveActual.isEmpty()) {
            if (claveActual.isEmpty()) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Falta tu clave actual", "Ingresa tu contraseña actual para poder cambiarla."); return;
            }
            // Verificar clave actual
            Model.Usuario verificado = new dao.UsuarioDAO().autenticar(u.getUser(), claveActual);
            if (verificado == null) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Clave incorrecta", "La contraseña actual que ingresaste no es correcta."); return;
            }
            if (claveNueva.length() < 6) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Clave muy corta", "La nueva contraseña debe tener al menos 6 caracteres."); return;
            }
            if (!claveNueva.equals(claveConf)) {
                raven.alerts.MessageAlerts.getInstance().showMessage("No coinciden", "La nueva contraseña y la confirmación son diferentes."); return;
            }
            claveParaGuardar = claveNueva;
        }

        String fotoAGuardar = nuevaRutaFoto != null ? nuevaRutaFoto : u.getFotoPath();

        if (new dao.UsuarioDAO().actualizarPerfil(u.getIdVendedor(), nombreNuevo, fotoAGuardar, claveParaGuardar)) {
            // Actualizar sesión
            u.setNombreCompleto(nombreNuevo);
            u.setFotoPath(fotoAGuardar);
            lblNombreDisplay.setText(u.getDisplayName());
            lblUserTag.setText("@" + u.getUser());

            Notifications.getInstance().show(Notifications.Type.SUCCESS,
                Notifications.Location.BOTTOM_CENTER, "Perfil actualizado correctamente");

            // Limpiar campos de clave
            txtClaveActual.setText("");
            txtNuevaClave.setText("");
            txtConfirmarClave.setText("");
        } else {
            raven.alerts.MessageAlerts.getInstance().showMessage("Error al guardar", "No se pudieron guardar los cambios. Intenta nuevamente.");
        }
    }

    private JTextField crearCampo(String placeholder) {
        JTextField tf = new JTextField();
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0; margin:8,10,8,10;");
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return tf;
    }

    private JPasswordField crearPassword(String placeholder) {
        JPasswordField pf = new JPasswordField();
        pf.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0; margin:8,10,8,10; showRevealButton:true;");
        pf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return pf;
    }

    private JLabel crearLabel(String texto) {
        JLabel lb = new JLabel(texto);
        lb.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");
        return lb;
    }
}
