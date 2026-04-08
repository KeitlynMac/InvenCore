package gui.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.DropShadowBorder;
import raven.popup.DefaultOption;
import raven.popup.GlassPanePopup;
import raven.popup.component.SimplePopupBorder;
import utilities.LicenciaManager;
import utilities.SesionUsuario;
import Model.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.prefs.Preferences; // <-- Importante para guardar configuraciones

// Ventana de inicio de sesión de ivencore.
// Si el programa no está activado muestra el botón de activación.
// Después de autenticar abre la ventana principal.
public class frLogin extends JFrame {

    private JTextField      txtUsuario;
    private JPasswordField  txtClave;
    private JButton         btnLogin;
    private JButton         btnCrearUsuario;
    private JLabel          lbMensaje;
    private Image           imagenFondo;
    private JCheckBox       chkRecordar; // <-- Nuestro nuevo Checkbox

    public frLogin() { init(); }

    private void init() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("ivencore — Iniciar Sesión");
        setMinimumSize(new Dimension(800, 600));
        setSize(1000, 700);

        GlassPanePopup.install(this);
        raven.toast.Notifications.getInstance().setJFrame(this);

        URL urlFondo = getClass().getResource("/images/logo.png");
        if (urlFondo != null) imagenFondo = new ImageIcon(urlFondo).getImage();

        JPanel backgroundPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (imagenFondo != null) g.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
            }
        };

        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 30 20 30", "fill, 240:300:380"));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:20; [light]background:darken(@background,6%); [dark]background:lighten(@background,3%)");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        JLabel lbtitulo = new JLabel("ivencore", JLabel.CENTER);
        lbtitulo.setBorder(new EmptyBorder(4, 6, 4, 6));
        lbtitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +20");

        JLabel descripcion = new JLabel("Ingresa tus credenciales para continuar", JLabel.CENTER);
        descripcion.setBorder(new EmptyBorder(4, 6, 4, 6));
        descripcion.putClientProperty(FlatClientProperties.STYLE,
                "[light]foreground:lighten(@foreground,50%); [dark]foreground:darken(@foreground,50%)");

        txtUsuario = new JTextField();
        txtClave   = new JPasswordField();

        JLabel lbUsuario = new JLabel("Usuario");
        JLabel lbClave   = new JLabel("Clave");
        Font f = lbUsuario.getFont();
        lbUsuario.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        lbClave.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        lbUsuario.setBorder(new EmptyBorder(4, 0, 4, 0));
        lbClave.setBorder(new EmptyBorder(4, 0, 4, 0));

        txtClave.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0; margin:8,10,8,10; showRevealButton:true;showRevealButton:true;");
        txtUsuario.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ingresa tu usuario");
        txtUsuario.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/user.svg", 0.30f));
        txtUsuario.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:8,8,8,8;");
        txtClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ingresa tu clave");
        txtClave.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/lock-password.svg", 0.30f));


        // --- Inicializamos el Checkbox ---
        chkRecordar = new JCheckBox("Recordar usuario y contraseña");
        chkRecordar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chkRecordar.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setBackground(Color.decode("#009991"));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.putClientProperty(FlatClientProperties.STYLE, "font:bold; arc:10; margin:8,8,8,8;");

        btnCrearUsuario = new JButton();
        btnCrearUsuario.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        lbMensaje = new JLabel("Usuario o Clave incorrecta");
        lbMensaje.setForeground(Color.decode("#cc2b00"));
        lbMensaje.setVisible(false);

        panel.add(lbtitulo);
        panel.add(descripcion);
        panel.add(lbUsuario, "gapy 8");
        panel.add(txtUsuario);
        panel.add(lbClave, "gapy 8");
        panel.add(txtClave);
        panel.add(chkRecordar, "gapy 4"); // <-- Añadido al panel
        panel.add(lbMensaje);
        panel.add(btnLogin, "gapy 10");

        if (!existeUsuarioRegistrado()) {
            if (!LicenciaManager.estaActivado()) {
                btnCrearUsuario.setText("Activar Producto");
                btnCrearUsuario.putClientProperty(FlatClientProperties.STYLE,
                        "font:bold; arc:10; margin:8,8,8,8; background:null; borderWidth:1; borderColor:#009991; foreground:#009991;");
            } else {
                btnCrearUsuario.setText("Crear Usuario");
                btnCrearUsuario.setBackground(Color.decode("#009991"));
                btnCrearUsuario.putClientProperty(FlatClientProperties.STYLE, "font:bold; arc:10; margin:8,8,8,8;");
            }
            btnCrearUsuario.setForeground(Color.decode("#000000"));
            panel.add(btnCrearUsuario, "gapy 5");
        }

        backgroundPanel.setLayout(new MigLayout("fill, insets 20", "[center]", "[center]"));
        backgroundPanel.add(panel);

        setContentPane(backgroundPanel);
        setLocationRelativeTo(null);

        cargarPreferencias(); // <-- Leer credenciales si existen
        eventos();
    }

    // ─── NUEVOS MÉTODOS DE PREFERENCIAS ────────────────────────────────────────

    private void cargarPreferencias() {
        Preferences prefs = Preferences.userNodeForPackage(frLogin.class);
        boolean recordar = prefs.getBoolean("recordar", false);

        if (recordar) {
            String usuarioGuardado = prefs.get("usuario", "");
            String claveGuardada = prefs.get("clave", "");

            txtUsuario.setText(usuarioGuardado);
            txtClave.setText(claveGuardada);
            chkRecordar.setSelected(true);
        }
    }

    private void guardarPreferencias(String usuario, String clave) {
        Preferences prefs = Preferences.userNodeForPackage(frLogin.class);

        if (chkRecordar.isSelected()) {
            // Guardar en el sistema
            prefs.putBoolean("recordar", true);
            prefs.put("usuario", usuario);
            prefs.put("clave", clave);
        } else {
            // Borrar si el usuario desmarcó la casilla
            prefs.putBoolean("recordar", false);
            prefs.remove("usuario");
            prefs.remove("clave");
        }
    }

    // ───────────────────────────────────────────────────────────────────────────

    private void eventos() {
        btnLogin.addActionListener(e -> autenticar());
        txtClave.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) autenticar();
            }
        });
        btnCrearUsuario.addActionListener(e -> {
            if (LicenciaManager.estaActivado()) mostrarRegistroUsuario();
            else mostrarActivacion();
        });
    }

    // Comprueba si ya hay un usuario en la BD para decidir si mostrar el botón Crear Usuario.
    private boolean existeUsuarioRegistrado() {
        try (Connection conn = dao.Conexion.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Vendedor")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { return false; }
    }

    private void mostrarActivacion() {
        JPanel p = new JPanel(new MigLayout("wrap, fillx, insets 15", "[grow, fill]"));
        p.add(new JLabel("Este producto requiere una licencia de activación."));
        p.add(new JLabel("Ingresa tu clave del producto:"), "gaptop 10");
        JPasswordField txtClaveProd = new JPasswordField();
        txtClaveProd.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:5,10,5,10; showRevealButton:true;");
        p.add(txtClaveProd);

        DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
        GlassPanePopup.showPopup(new SimplePopupBorder(p, "Activación de Software", new String[]{"Cancelar", "Activar"}, (pc, i) -> {
            if (i == 1) {
                if (LicenciaManager.activar(new String(txtClaveProd.getPassword()))) {
                    raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS,
                            raven.toast.Notifications.Location.BOTTOM_CENTER, "¡Software Activado Correctamente!");
                    pc.closePopup();
                    new javax.swing.Timer(200, ev -> {
                        ((javax.swing.Timer) ev.getSource()).stop();
                        mostrarOpcionesPostActivacion();
                    }).start();
                } else {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Clave incorrecta", "La clave que ingresaste no es correcta. Contacta al proveedor del software.");
                }
            } else pc.closePopup();
        }), opt);
    }

    private void mostrarOpcionesPostActivacion() {
        JPanel pOpciones = new JPanel(new MigLayout("wrap, fillx, insets 20 15 20 15", "[grow, fill]", "[]18[]18[]"));
        pOpciones.setPreferredSize(new Dimension(440, 320));

        JLabel lblDesc = new JLabel(
                "<html><center>¿Cómo deseas continuar?</center></html>", SwingConstants.CENTER);
        lblDesc.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground; font:+1;");
        pOpciones.add(lblDesc, "growx");

        JPanel cardNuevo = crearTarjetaOpcion(
                "🆕  Crear Usuario Nuevo",
                "Primera instalación. Configura tu nombre, usuario y contraseña.",
                "#3b82f6"
        );
        cardNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pOpciones.add(cardNuevo, "growx");

        JPanel cardRestore = crearTarjetaOpcion(
                "♻️  Restaurar Base de Datos",
                "Ya tenías el programa antes. Selecciona tu archivo de respaldo (.db) para recuperar tus datos.",
                "#10b981"
        );
        cardRestore.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pOpciones.add(cardRestore, "growx");

        DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return false; } };
        final raven.popup.component.SimplePopupBorder[] popupRef = {null};

        popupRef[0] = new SimplePopupBorder(pOpciones, "Bienvenido a ivencore", new String[]{"Cancelar"}, (pc, i) -> {
            pc.closePopup();
        });

        cardNuevo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                GlassPanePopup.closePopupLast();
                SwingUtilities.invokeLater(() -> {
                    btnCrearUsuario.setText("Crear Usuario");
                    mostrarRegistroUsuario();
                });
            }
        });

        cardRestore.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                GlassPanePopup.closePopupLast();
                SwingUtilities.invokeLater(() -> mostrarRestauracionBackup());
            }
        });

        GlassPanePopup.showPopup(popupRef[0], opt);
    }

    private void mostrarRestauracionBackup() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecciona tu archivo de base de datos (.db)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Base de datos SQLite (*.db)", "db"));
        File docs = new File(System.getProperty("user.home") + File.separator + "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setCurrentDirectory(docs);

        int resultado = fc.showOpenDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) return;

        File archivoSeleccionado = fc.getSelectedFile();

        raven.alerts.MessageAlerts.getInstance().showMessage(
                "Confirmar Restauración",
                "¿Restaurar el archivo '" + archivoSeleccionado.getName() + "'?\nEl programa se cerrará para aplicar los cambios.",
                raven.alerts.MessageAlerts.MessageType.WARNING,
                raven.alerts.MessageAlerts.OK_CANCEL_OPTION,
                (pc3, i3) -> {
                    if (i3 != raven.alerts.MessageAlerts.OK_OPTION) return;

                    JDialog progreso = new JDialog(frLogin.this, "Restaurando...", false);
                    JLabel lblProg = new JLabel("   Restaurando base de datos, por favor espera...   ");
                    lblProg.setBorder(new EmptyBorder(20, 20, 20, 20));
                    progreso.add(lblProg);
                    progreso.pack();
                    progreso.setLocationRelativeTo(frLogin.this);
                    progreso.setVisible(true);

                    new SwingWorker<Boolean, Void>() {
                        String errorMsg = null;

                        @Override
                        protected Boolean doInBackground() {
                            try {
                                services.BackupService.restaurarBackup(archivoSeleccionado.toPath());
                                return true;
                            } catch (Exception ex) {
                                errorMsg = ex.getMessage();
                                return false;
                            }
                        }

                        @Override
                        protected void done() {
                            progreso.dispose();
                            try {
                                if (get()) {
                                    raven.alerts.MessageAlerts.getInstance().showMessage(
                                            "¡Restaurado con éxito!",
                                            "La base de datos fue restaurada. El programa se cerrará ahora.");
                                    new javax.swing.Timer(1500, ev -> System.exit(0)) {{
                                        setRepeats(false);
                                    }}.start();
                                } else {
                                    raven.alerts.MessageAlerts.getInstance().showMessage(
                                            "Error al restaurar",
                                            errorMsg != null ? errorMsg : "Error desconocido. Asegúrate de que el archivo sea un backup válido.");
                                }
                            } catch (Exception ex) {
                                raven.alerts.MessageAlerts.getInstance().showMessage(
                                        "Error inesperado", ex.getMessage());
                            }
                        }
                    }.execute();
                }
        );
    }

    private JPanel crearTarjetaOpcion(String titulo, String descripcion, String colorHex) {
        JPanel card = new JPanel(new MigLayout("fillx, insets 16 18 16 18", "[grow]")) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(colorHex));
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
            @Override public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(
                                Integer.parseInt(colorHex.substring(1, 3), 16),
                                Integer.parseInt(colorHex.substring(3, 5), 16),
                                Integer.parseInt(colorHex.substring(5, 7), 16), 60), 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
        };
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");

        JLabel lblTit = new JLabel(titulo);
        lblTit.putClientProperty(FlatClientProperties.STYLE, "font:bold +2;");

        JLabel lblDesc = new JLabel("<html><p style='width:340px;'>" + descripcion + "</p></html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground; font:+0;");

        card.add(lblTit, "growx, wrap");
        card.add(lblDesc, "growx, gaptop 4");

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.repaint(); }
        });

        return card;
    }

    private void mostrarRegistroUsuario() {
        JPanel pRegistro = new JPanel(new MigLayout("wrap, fillx, insets 20", "[grow, fill]", "[]8[]"));

        JLabel lblFotoPreview = new JLabel();
        lblFotoPreview.setPreferredSize(new Dimension(90, 90));
        lblFotoPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblFotoPreview.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 3, 5, 5, true));
        lblFotoPreview.setText("<html><center>Sin<br>foto</center></html>");
        lblFotoPreview.setFont(lblFotoPreview.getFont().deriveFont(11f));

        JButton btnSelFoto = new JButton("Seleccionar Foto");
        btnSelFoto.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:#3b82f6; foreground:#ffffff; font:bold;");

        final String[] rutaFotoFinal = {null};

        btnSelFoto.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File fSel = fc.getSelectedFile();
                try {
                    String destDir = System.getProperty("user.dir") + File.separator + "perfiles";
                    new File(destDir).mkdirs();
                    String dest = destDir + File.separator + fSel.getName();
                    Files.copy(fSel.toPath(), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
                    rutaFotoFinal[0] = dest;

                    ImageIcon ico = new ImageIcon(new ImageIcon(dest)
                            .getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH));
                    lblFotoPreview.setIcon(ico);
                    lblFotoPreview.setText("");
                } catch (Exception ex) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Error de imagen", "No se pudo cargar la imagen seleccionada.");
                }
            }
        });

        JPanel fotoPanel = new JPanel(new MigLayout("wrap, insets 0", "[center]"));
        fotoPanel.putClientProperty(FlatClientProperties.STYLE, "background:null");
        fotoPanel.add(lblFotoPreview, "width 90!, height 90!");
        fotoPanel.add(btnSelFoto, "growx, gaptop 5");

        JTextField txtNombreCompleto = new JTextField();
        txtNombreCompleto.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:8,10,8,10;");
        txtNombreCompleto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: Juan Pérez");

        JTextField txtNuevoUser = new JTextField();
        txtNuevoUser.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:8,10,8,10;");
        txtNuevoUser.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre de usuario");

        JPasswordField txtNuevaClave = new JPasswordField();
        txtNuevaClave.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:8,10,8,10; showRevealButton:true;");
        txtNuevaClave.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mínimo 6 caracteres");

        pRegistro.add(fotoPanel, "align center, gapbottom 5");
        pRegistro.add(crearLabel("Nombre Completo:"));
        pRegistro.add(txtNombreCompleto, "growx");
        pRegistro.add(crearLabel("Usuario:"));
        pRegistro.add(txtNuevoUser, "growx");
        pRegistro.add(crearLabel("Contraseña:"));
        pRegistro.add(txtNuevaClave, "growx");

        DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return false; } };
        GlassPanePopup.showPopup(new SimplePopupBorder(pRegistro, "Crear Usuario", new String[]{"Cancelar", "Guardar"}, (pc, i) -> {
            if (i == 1) {
                String nombre = txtNombreCompleto.getText().trim();
                String user   = txtNuevoUser.getText().trim();
                String clave  = new String(txtNuevaClave.getPassword());

                if (user.isEmpty() || clave.isEmpty()) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Campos requeridos", "El usuario y la contraseña son obligatorios."); return;
                }
                if (clave.length() < 6) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Clave muy corta", "La contraseña debe tener al menos 6 caracteres."); return;
                }

                dao.UsuarioDAO uDao = new dao.UsuarioDAO();
                if (uDao.registrar(user, clave, nombre, rutaFotoFinal[0])) {
                    raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS,
                            raven.toast.Notifications.Location.BOTTOM_CENTER, "Usuario creado con éxito.");
                    pc.closePopup();
                    btnCrearUsuario.setVisible(false);
                    txtUsuario.setText(user);
                } else {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Error al crear usuario", "El usuario ya existe o hubo un problema. Intenta con otro nombre.");
                }
            } else pc.closePopup();
        }), opt);
    }

    private JLabel crearLabel(String texto) {
        JLabel lb = new JLabel(texto);
        lb.putClientProperty(FlatClientProperties.STYLE, "font:bold;");
        return lb;
    }

    // Verifica las credenciales contra la BD y abre frPrincipal si son correctas.
    private void autenticar() {
        String user     = txtUsuario.getText().trim();
        String password = new String(txtClave.getPassword());

        if (user.isEmpty() || password.isEmpty()) {
            lbMensaje.setText("Por favor, llena todos los campos");
            lbMensaje.setVisible(true);
            return;
        }

        dao.UsuarioDAO uDao = new dao.UsuarioDAO();
        Usuario u = uDao.autenticar(user, password);

        if (u != null) {
            lbMensaje.setVisible(false);

            // --- ¡AQUÍ GUARDAMOS LAS PREFERENCIAS! ---
            guardarPreferencias(user, password);

            SesionUsuario.getInstance().iniciar(u);
            frPrincipal frp = new frPrincipal();
            frp.setVisible(true);
            this.dispose();
        } else {
            lbMensaje.setText("Usuario o Clave incorrecta");
            lbMensaje.setVisible(true);
        }
    }
}