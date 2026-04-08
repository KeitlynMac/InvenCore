package utilities;

import com.formdev.flatlaf.FlatClientProperties;
import gui.panels.*;
import net.miginfocom.swing.MigLayout;
import raven.drawer.Drawer;
import raven.drawer.component.SimpleDrawerBuilder;
import raven.drawer.component.footer.SimpleFooterData;
import raven.drawer.component.header.SimpleHeaderData;
import raven.drawer.component.menu.*;
import raven.drawer.component.menu.data.Item;
import raven.swing.AvatarIcon;
import raven.drawer.component.menu.data.MenuItem;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

// Construye el menú lateral (drawer) de la aplicación.
// Define el header con el logo, el menú de navegación y el footer con el toggle de tema y datos del usuario.
public class MyDrawer extends SimpleDrawerBuilder {

    private final CambiarTema themesChange = new CambiarTema();

    // TRUCO: Usamos una variable estática para asegurarnos de que el "escuchador"
    // se agregue solo una vez y no colapse la memoria al reconstruir el menú.
    private static boolean listenerInstalado = false;

    public MyDrawer() {
        if (!listenerInstalado) {
            javax.swing.UIManager.addPropertyChangeListener(evt -> {
                if ("lookAndFeel".equals(evt.getPropertyName())) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // FORZAMOS LA RECONSTRUCCIÓN:
                        // Le decimos a la librería que reemplace el menú actual por uno totalmente nuevo.
                        // Al hacer esto, los FlatSVGIcon leen el nuevo tema y se pintan del color correcto.
                        Drawer.getInstance().setDrawerBuilder(new MyDrawer());
                    });
                }
            });
            listenerInstalado = true;
        }
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        return new SimpleHeaderData()
                .setIcon(cargarAvatar(60))
                .setTitle(SesionUsuario.getInstance().getNombreDisplay())
                .setDescription("Administrador del negocio");
    }

// ... (DE AQUÍ EN ADELANTE, DEJA TU CÓDIGO EXACTAMENTE IGUAL COMO LO TIENES) ...
// ... getSimpleFooterData(), getHeader(), getSimpleMenuOption(), etc. ...

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData().setDescription("BYKEI©");
    }

    @Override
    // El encabezado del drawer: muestra el nombre del programa en teal.
    public Component getHeader() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 22 20 14 20", "[center]", "[center]"));
        panel.setOpaque(false);
        // Logo con color teal del sistema
        JLabel lblLogo = new JLabel("ivencore");
        lblLogo.putClientProperty(FlatClientProperties.STYLE, "font:bold +12;");
        panel.add(lblLogo);
        return panel;
    }

    @Override
    public SimpleMenuOption getSimpleMenuOption() {

        // ¡CAMBIO CLAVE! Usamos MenuItem[] para poder mezclar Etiquetas (Label) y Botones (Item)
        MenuItem[] items = new MenuItem[]{

                new Item.Label("PRINCIPAL"), // Esto es solo texto, no se puede clickear
                new Item("Dashboard",       "dashboard.svg"),
                new Item("Crear Venta",     "crearventa.svg"),

                new Item.Label("GESTIÓN"),
                new Item("Clientes",        "grupoclientes.svg"),
                new Item("Productos",       "box.svg"),
                new Item("Historial Ventas","historial.svg"),
                new Item("Gastos",          "gastos.svg"),

                new Item.Label("FINANZAS"),
                new Item("Reportes Excel",  "excel.svg"),
                new Item("Cuentas x Cobrar","cobrar.svg"),

                new Item.Label("SISTEMA"),
                new Item("Mi Perfil",       "perfil.svg"),
                new Item("Configuración",   "configuracion.svg"),
                new Item("Copia Seguridad", "copia.svg"),
                new Item("Cerrar Sesión",   "salir.svg")
        };

        return new SimpleMenuOption()
                .setMenus(items)
                .setBaseIconPath("icons")
                .setIconScale(0.40f)
                .addMenuEvent(new MenuEvent() {
                    @Override
                    public void selected(MenuAction action, int[] index) {
                        if (index.length != 1) return;

                        // ¡MAGIA! Los Item.Label son "invisibles" para el conteo.
                        // Tu numeración vuelve a la normalidad del 0 al 11.
                        switch (index[0]) {
                            case 0 -> WindowsTabbed.getInstance().addTab(new pDashboard());
                            case 1 -> WindowsTabbed.getInstance().addTab(new pVenta());
                            case 2 -> WindowsTabbed.getInstance().addTab(new pClientes());
                            case 3 -> WindowsTabbed.getInstance().addTab(new pProductos());
                            case 4 -> WindowsTabbed.getInstance().addTab(new pHistorialVentas());
                            case 5 -> WindowsTabbed.getInstance().addTab(new pGastos());
                            case 6 -> WindowsTabbed.getInstance().addTab(new pReportes());
                            case 7 -> WindowsTabbed.getInstance().addTab(new pCuentasCobrar());
                            case 8 -> WindowsTabbed.getInstance().addTab(new pPerfil());
                            case 9 -> WindowsTabbed.getInstance().addTab(new pConfiguracion());
                            case 10 -> WindowsTabbed.getInstance().addTab(new gui.panels.pBackup());
                            case 11 -> cerrarSesion();
                        }
                        Drawer.getInstance().closeDrawer();
                    }
                });
    }

    @Override
    // El footer del drawer: toggle de tema + datos del usuario logueado.
    public Component getFooter() {
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 15", "[fill]"));
        panel.add(themesChange, "gapbottom 10");
        panel.add(new JSeparator(), "gapbottom 10");

        JPanel userPanel = new JPanel(new MigLayout("insets 0, gapx 10", "[][grow]", "center"));

        // Avatar dinámico con foto del usuario
        userPanel.add(new JLabel(cargarAvatar(40)));

        JPanel textPanel = new JPanel(new MigLayout("insets 0, wrap, gapy 2", "[fill]"));
        JLabel lblNombre = new JLabel(SesionUsuario.getInstance().getNombreDisplay());
        lblNombre.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        JLabel lblRol = new JLabel("Administrad@r");
        lblRol.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        textPanel.add(lblNombre);
        textPanel.add(lblRol);
        userPanel.add(textPanel);

        panel.add(userPanel);
        return panel;
    }

    /** Carga el avatar del usuario actual. Si no tiene foto, usa el default. */
    private AvatarIcon cargarAvatar(int size) {
        String fotoPath = SesionUsuario.getInstance().getFotoPath();
        if (fotoPath != null && new File(fotoPath).exists()) {
            try {
                return new AvatarIcon(new File(fotoPath).toURI().toURL(), size, size, 999);
            } catch (Exception ignored) {}
        }
        URL defUrl = getClass().getResource("/icons/admin.png");
        return new AvatarIcon(defUrl, size, size, 999);
    }

    // Pide confirmación y cierra la sesión limpiando todo y abriendo el login.
    private void cerrarSesion() {
        raven.alerts.MessageAlerts.getInstance().showMessage("Cerrar Sesión",
            "¿Seguro que quieres cerrar sesión?",
            raven.alerts.MessageAlerts.MessageType.WARNING, raven.alerts.MessageAlerts.OK_CANCEL_OPTION, (pc, i) -> {
                if (i == raven.alerts.MessageAlerts.OK_OPTION) {
                    SesionUsuario.getInstance().cerrar();

                    for (Window w : Window.getWindows()) {
                        w.dispose();
                    }

                    SwingUtilities.invokeLater(() -> {
                        gui.main.frLogin login = new gui.main.frLogin();
                        login.setVisible(true);
                    });
                }
            });
    }
}
