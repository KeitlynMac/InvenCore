package gui.main;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.drawer.Drawer;
import raven.popup.GlassPanePopup;
import utilities.*;

import javax.swing.*;
import java.awt.*;


// Ventana principal del programa.
// Contiene la barra de navegación con la campana de notificaciones y el menú lateral.

// Ventana principal de ivencore.
// Contiene la barra de herramientas con los botones de navegación y la campana.
// El área central la controla WindowsTabbed.
public class frPrincipal extends JFrame {

    private JPanel  panel;
    private JButton cmdMenu, cmdUndo, cmdRedo, cmdRefresh;
    private BellButton bellButton;

    public frPrincipal() {
        GlassPanePopup.install(this);
        MyDrawer drawer = new MyDrawer();
        Drawer.getInstance().setDrawerBuilder(drawer);
        init();
        // Iniciar monitor de notificaciones (daemon thread)
        NotificacionManager.getInstance().iniciarMonitor();
    }

    private void init() {
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Ivencore — " + SesionUsuario.getInstance().getNombreDisplay());
        // Tamaño mínimo razonable — en pantallas pequeñas el usuario puede
        // reducir la ventana sin que se rompa el layout.
        setMinimumSize(new Dimension(900, 600));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(true);


        panel = new JPanel(new BorderLayout());
        panel.putClientProperty(FlatClientProperties.STYLE,
            "[light]background:darken(@background,6%); [dark]background:lighten(@background,3%)");

        panel.add(crearBarraMenu(), BorderLayout.NORTH);
        add(panel);

        JPanel contentPanel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        WindowsTabbed.getInstance().install(this, contentPanel);
        WindowsTabbed.getInstance().addTab(new gui.panels.pDashboard());
    }

    // Construye la barra superior con el menú, los botones de navegación y la campana.
    private JPanel crearBarraMenu() {
        // "insets 3" mantiene la barra delgada, "push []" manda la campana al extremo derecho
        JPanel barra = new JPanel(new MigLayout("insets 3", "[][][][] push []"));

        // Creamos los botones con sus escalas originales
        cmdMenu    = crearBoton(new FlatSVGIcon("icons/category.svg",    0.6f));
        cmdUndo    = crearBoton(new FlatSVGIcon("icons/back.svg",        0.4f));
        cmdRedo    = crearBoton(new FlatSVGIcon("icons/chevron-right.svg", 0.4f));
        cmdRefresh = crearBoton(new FlatSVGIcon("icons/refresh.svg",     0.4f));

        // --- AJUSTE PARA LA CAMPANA ---
        bellButton = new BellButton();

        // Forzamos que el icono de la campana sea más grande (0.6f para igualar al menú)
        if (bellButton.getIcon() instanceof FlatSVGIcon svg) {
            bellButton.setIcon(svg.derive(0.6f));
        }
        // Quitamos el borde o fondo si la librería le pone uno por defecto para que sea minimalista
        bellButton.putClientProperty(FlatClientProperties.STYLE, "borderWidth:0; background:null;");

        // Tooltips
        cmdMenu.setToolTipText("Menú");
        cmdUndo.setToolTipText("Atrás");
        cmdRedo.setToolTipText("Adelante");
        cmdRefresh.setToolTipText("Recargar");
        bellButton.setToolTipText("Notificaciones");

        // Listeners
        cmdMenu.addActionListener(e    -> raven.drawer.Drawer.getInstance().showDrawer());
        cmdUndo.addActionListener(e    -> WindowsTabbed.getInstance().undo());
        cmdRedo.addActionListener(e    -> WindowsTabbed.getInstance().redo());
        cmdRefresh.addActionListener(e -> WindowsTabbed.getInstance().refresh());

        // Agregamos a la barra
        barra.add(cmdMenu);
        barra.add(cmdUndo);
        barra.add(cmdRedo);
        barra.add(cmdRefresh);

        // "width 45!, height 45!" asegura que el botón tenga espacio para el icono grande
        barra.add(bellButton, "width 45!, height 45!");

        return barra;
    }

    // Helper para crear los botones de la barra con estilo uniforme.
    private JButton crearBoton(Icon icon) {
        // Aplicar filtro dinámico: el ícono toma el color de texto del botón
        // En modo claro = oscuro, en modo oscuro = claro. Se actualiza al cambiar tema.
        if (icon instanceof FlatSVGIcon svg) {
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> {
                java.awt.Color fg = javax.swing.UIManager.getColor("Button.foreground");
                return fg != null ? fg : java.awt.Color.GRAY;
            }));
        }
        JButton b = new JButton(icon);
        b.putClientProperty(FlatClientProperties.STYLE,
            "background:$Button.toolbar.background; arc:10; margin:3,3,3,3;" +
            "borderWidth:0; focusWidth:0; innerFocusWidth:0;");
        return b;
    }

    @Override
    public void dispose() {
        NotificacionManager.getInstance().detenerMonitor();
        super.dispose();
    }
}
