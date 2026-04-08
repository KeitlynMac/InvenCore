package gui.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.DropShadowBorder;
import raven.alerts.MessageAlerts;
import raven.toast.Notifications;
import services.BackupService;
import services.BackupService.CloudFolder;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Panel de Backup y Restauración de la base de datos.
 * Permite hacer copias de seguridad locales o en carpetas de nube detectadas automáticamente.
 */
public class pBackup extends TabbedForm {

    private JLabel   lblEstadoDB;
    private JPanel   panelNube;
    private JPanel   panelBackups;
    private Path     carpetaSeleccionada;

    public pBackup() { init(); }

    private void init() {
        // 1. Cambiamos el layout principal a BorderLayout para que el ScrollPane ocupe todo
        setLayout(new BorderLayout());

        // 2. Creamos el contenedor principal que agrupará todo el contenido
        JPanel contenedorPrincipal = new JPanel(new MigLayout("wrap, fillx, insets 24", "[fill, grow]", "[][][][grow]"));

        // ── Título ────────────────────────────────────────────────────────
        JLabel lbTitulo = new JLabel("COPIA DE SEGURIDAD");
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbTitulo.setBorder(new EmptyBorder(0, 6, 6, 0));
        contenedorPrincipal.add(lbTitulo); // <-- Lo agregamos al contenedor principal

        // ── Estado de la BD ───────────────────────────────────────────────
        JPanel panelEstado = new JPanel(new MigLayout("insets 18 20 18 20, fillx", "[grow][]", "[]5[]"));
        panelEstado.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");

        JLabel lbEstadoTit = new JLabel("Estado de la base de datos");
        lbEstadoTit.putClientProperty(FlatClientProperties.STYLE, "font:bold +2;");
        lblEstadoDB = new JLabel("Calculando…");
        lblEstadoDB.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JButton btnRefreshEstado = new JButton("Actualizar");
        btnRefreshEstado.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; background:null; foreground:#3b82f6; font:bold; borderWidth:0;");
        btnRefreshEstado.addActionListener(e -> actualizarEstado());

        panelEstado.add(lbEstadoTit, "growx, wrap");
        panelEstado.add(lblEstadoDB, "growx");
        panelEstado.add(btnRefreshEstado, "align right");
        contenedorPrincipal.add(panelEstado); // <-- Al contenedor
        actualizarEstado();

        // ── Opciones de destino ───────────────────────────────────────────
        JPanel panelDestino = new JPanel(new MigLayout("wrap, insets 0", "[fill, grow]"));
        panelDestino.setOpaque(false);

        JLabel lbDestTit = new JLabel("¿Dónde guardar la copia?");
        lbDestTit.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;");
        lbDestTit.setBorder(new EmptyBorder(0, 4, 6, 0));
        panelDestino.add(lbDestTit);

        // Nube detectada automáticamente
        panelNube = new JPanel(new MigLayout("insets 0, gap 12",
                "[grow, fill][grow, fill][grow, fill]"));
        panelNube.setOpaque(false);
        cargarBotonesCarpetasNube();
        panelDestino.add(panelNube, "growx");

        // Carpeta local personalizada
        JPanel panelLocal = new JPanel(new MigLayout("insets 14 18 14 18, fillx", "[grow][][]"));
        panelLocal.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");

        JLabel lbLocal = new JLabel("Carpeta local personalizada");
        lbLocal.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");

        JLabel lbLocalDesc = new JLabel("Elige cualquier carpeta de tu PC");
        lbLocalDesc.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JButton btnElegirCarpeta = new JButton("Elegir carpeta");
        btnElegirCarpeta.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:#3b82f6; foreground:#ffffff; font:bold; margin:7,16,7,16;");

        JButton btnBackupLocal = new JButton("Hacer Backup Aquí");
        btnBackupLocal.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:#10b981; foreground:#ffffff; font:bold; margin:7,16,7,16;");
        btnBackupLocal.setEnabled(false);

        final Path[] carpetaLocal = {null};

        btnElegirCarpeta.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Seleccionar carpeta para el backup");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                carpetaLocal[0] = fc.getSelectedFile().toPath();
                lbLocalDesc.setText("✓ " + fc.getSelectedFile().getAbsolutePath());
                btnBackupLocal.setEnabled(true);
                carpetaSeleccionada = carpetaLocal[0];
            }
        });

        btnBackupLocal.addActionListener(e -> {
            if (carpetaLocal[0] != null) ejecutarBackup(carpetaLocal[0]);
        });

        panelLocal.add(lbLocal, "span 3, wrap");
        panelLocal.add(lbLocalDesc, "growx");
        panelLocal.add(btnElegirCarpeta);
        panelLocal.add(btnBackupLocal);
        panelDestino.add(panelLocal, "growx, gaptop 12");

        contenedorPrincipal.add(panelDestino); // <-- Al contenedor

        // ── Historial de backups ───────────────────────────────────────────
        JPanel panelHistorial = crearPanelHistorial();
        contenedorPrincipal.add(panelHistorial, "grow, push"); // <-- Al contenedor

        // 3. ── CREAMOS Y AGREGAMOS EL SCROLL PANE ──────────────────────────
        JScrollPane scrollPane = new JScrollPane(contenedorPrincipal);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Quitamos el borde por defecto para mantener el diseño limpio
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Acelera la velocidad de la rueda del ratón
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // Bloquea el scroll horizontal
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "background:null;"); // Fondo transparente

        // Finalmente, agregamos el scroll al panel principal
        add(scrollPane, BorderLayout.CENTER);
    }

    // ── Tarjetas de nube ──────────────────────────────────────────────────────
    private void cargarBotonesCarpetasNube() {
        panelNube.removeAll();
        List<CloudFolder> carpetas = BackupService.detectarCarpetasNube();

        if (carpetas.isEmpty()) {
            JLabel lbNoCarpetas = new JLabel(
                    "<html><center>No se detectó OneDrive,<br>Google Drive ni Dropbox<br>" +
                            "<small>Usa la opción de carpeta local</small></center></html>",
                    SwingConstants.CENTER);
            lbNoCarpetas.putClientProperty(FlatClientProperties.STYLE,
                    "foreground:$Label.disabledForeground; font:-1;");
            panelNube.add(lbNoCarpetas, "span 3, growx");
        } else {
            for (CloudFolder cf : carpetas) {
                panelNube.add(crearTarjetaNube(cf));
            }
        }

        panelNube.revalidate();
        panelNube.repaint();
    }

    private JPanel crearTarjetaNube(CloudFolder cf) {
        JPanel card = new JPanel(new MigLayout("wrap, insets 18 16 18 16, fillx", "[center]"));
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Asumiendo que ahora usas los íconos SVG como lo vimos anteriormente
        JLabel lblIcon = new JLabel(cf.icono(), SwingConstants.CENTER);
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "font:bold +18;");

        JLabel lblNombre = new JLabel(cf.nombre(), SwingConstants.CENTER);
        lblNombre.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");

        JLabel lblRuta = new JLabel(
                "<html><center><small>" + abreviarRuta(cf.ruta().toString(), 30) + "</small></center></html>",
                SwingConstants.CENTER);
        lblRuta.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JButton btnBackup = new JButton("Backup Aquí");
        btnBackup.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:#10b981; foreground:#ffffff; font:bold; margin:7,20,7,20;");
        btnBackup.addActionListener(e -> ejecutarBackup(cf.ruta()));

        card.add(lblIcon,   "growx");
        card.add(lblNombre, "growx");
        card.add(lblRuta,   "growx");
        card.add(btnBackup, "growx, gaptop 6");

        return card;
    }

    // ── Historial y restauración ──────────────────────────────────────────────
    private JPanel crearPanelHistorial() {
        JPanel panel = new JPanel(new MigLayout("wrap, fill, insets 18 20 18 20", "[fill]", "[][grow, fill]"));
        panel.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; [light]background:darken(@background,3%); [dark]background:lighten(@background,5%);");

        JPanel headerH = new JPanel(new MigLayout("insets 0, fillx", "[grow][][]"));
        headerH.setOpaque(false);

        JLabel lbHistTit = new JLabel("Historial de Backups");
        lbHistTit.putClientProperty(FlatClientProperties.STYLE, "font:bold +2;");

        JButton btnRestaurar = new JButton("Restaurar seleccionado");
        btnRestaurar.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:#f97316; foreground:#ffffff; font:bold; margin:6,14,6,14;");

        JButton btnBuscarCarpeta = new JButton("Buscar en carpeta…");
        btnBuscarCarpeta.putClientProperty(FlatClientProperties.STYLE,
                "arc:12; background:null; foreground:#3b82f6; font:bold; " +
                        "borderWidth:1; borderColor:$Component.borderColor; margin:6,14,6,14;");

        headerH.add(lbHistTit, "growx");
        headerH.add(btnBuscarCarpeta);
        headerH.add(btnRestaurar);
        panel.add(headerH, "growx");

        // Tabla de historial
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> lista = new JList<>(listModel);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.putClientProperty(FlatClientProperties.STYLE, "font:+1;");
        lista.setCellRenderer(crearCellRenderer());

        JScrollPane scrollH = new JScrollPane(lista);
        scrollH.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.add(scrollH, "grow, push");

        final List<Path>[] backupsRef = new List[]{List.of()};

        btnBuscarCarpeta.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                carpetaSeleccionada = fc.getSelectedFile().toPath();
                cargarHistorial(carpetaSeleccionada, listModel, backupsRef);
            }
        });

        btnRestaurar.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx < 0 || idx >= backupsRef[0].size()) {
                MessageAlerts.getInstance().showMessage("Selección vacía", "Selecciona un backup de la lista para restaurar.");
                return;
            }
            final int idxFinal = idx;
            MessageAlerts.getInstance().showMessage("Confirmar Restauración",
                    "Esto reemplazará la base de datos actual. El programa se cerrará para aplicar los cambios.",
                    MessageAlerts.MessageType.WARNING, MessageAlerts.OK_CANCEL_OPTION, (pc3, i3) -> {
                        if (i3 == MessageAlerts.OK_OPTION) {
                            try {
                                BackupService.restaurarBackup(backupsRef[0].get(idxFinal));
                                Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                        Notifications.Location.BOTTOM_CENTER, "Backup restaurado. Cerrando...");
                                new javax.swing.Timer(2000, ev -> {
                                    dao.Conexion.cerrarPool();
                                    System.exit(0);
                                }) {{ setRepeats(false); }}.start();
                            } catch (Exception ex) {
                                MessageAlerts.getInstance().showMessage("Error al restaurar", ex.getMessage());
                            }
                        }
                    });
        });

        // Cargar historial de la carpeta de trabajo por defecto
        String carpetaMaestra = System.getProperty("user.home") + File.separator + "Ivencore";
        cargarHistorial(Paths.get(carpetaMaestra), listModel, backupsRef);

        return panel;
    }

    private void cargarHistorial(Path carpeta, DefaultListModel<String> model, List<Path>[] ref) {
        model.clear();
        List<Path> backups = BackupService.listarBackups(carpeta);
        ref[0] = backups;
        if (backups.isEmpty()) {
            model.addElement("  Sin backups en esta carpeta");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            for (Path p : backups) {
                try {
                    long bytes = Files.size(p);
                    String size = bytes < 1024 * 1024
                            ? String.format("%.0f KB", bytes / 1024.0)
                            : String.format("%.1f MB", bytes / (1024.0 * 1024));
                    long millis = Files.getLastModifiedTime(p).toMillis();
                    model.addElement(" * " + p.getFileName() + "  —  " +
                            sdf.format(new Date(millis)) + "  (" + size + ")");
                } catch (Exception ex) {
                    model.addElement("* " + p.getFileName());
                }
            }
        }
    }

    // ── Ejecución de backup ───────────────────────────────────────────────────
    private void ejecutarBackup(Path carpeta) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Path, Void>() {
            @Override protected Path doInBackground() throws Exception {
                return BackupService.hacerBackup(carpeta);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Path resultado = get();
                    Notifications.getInstance().show(Notifications.Type.SUCCESS,
                            Notifications.Location.BOTTOM_CENTER,
                            "✅ Backup guardado: " + resultado.getFileName());
                    actualizarEstado();
                    // Registrar en NotificacionManager
                    utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                            "Backup completado",
                            "Copia guardada en: " + abreviarRuta(resultado.getParent().toString(), 40),
                            Model.Notificacion.Tipo.INFO));
                } catch (Exception ex) {
                    MessageAlerts.getInstance().showMessage("Error en backup", "No se pudo crear el backup: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void actualizarEstado() {
        String tamanio = BackupService.tamanioBaseDatos();
        String fecha   = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        lblEstadoDB.setText("Base de datos: " + tamanio + "  ·  Última verificación: " + fecha);
    }

    private String abreviarRuta(String ruta, int max) {
        if (ruta.length() <= max) return ruta;
        return "…" + ruta.substring(ruta.length() - max);
    }

    private ListCellRenderer<String> crearCellRenderer() {
        return new ListCellRenderer<String>() {
            private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

            @Override
            public Component getListCellRendererComponent(
                    JList<? extends String> list, String val, int idx, boolean sel, boolean foc) {

                Component c = defaultRenderer.getListCellRendererComponent(list, val, idx, sel, foc);
                if (c instanceof JLabel) {
                    JLabel lbl = (JLabel) c;
                    lbl.setBorder(new EmptyBorder(10, 8, 10, 8));
                    lbl.putClientProperty(FlatClientProperties.STYLE, "font:+1;");
                }
                return c;
            }
        };
    }
}