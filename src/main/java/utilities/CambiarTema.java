package utilities;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

// Toggle de tema claro/oscuro en el footer del menú lateral.
// El botón activo se resalta con el color teal del sistema (#009991).
// El inactivo queda transparente con texto apagado.
public class CambiarTema extends JPanel {

    private static final String ACCENT = "#009991";

    private JButton btnOscuro;
    private JButton btnClaro;

    public CambiarTema() {
        init();
    }

    // Construye el pill con los dos botones y marca el modo activo.
    private void init() {
        setOpaque(false);
        putClientProperty(FlatClientProperties.STYLE, "background:null");
        setLayout(new MigLayout("al center, insets 0", "[fill, 200]", "fill"));

        // Contenedor pill redondeado
        JPanel pill = new JPanel(new MigLayout("fill, insets 3, gap 0", "[fill][fill]", "fill"));
        pill.putClientProperty(FlatClientProperties.STYLE,
                "arc:999;" +
                "[light]background:darken(@background,8%);" +
                "[dark]background:darken(@background,10%);");

        btnOscuro = boton("");
        btnOscuro.setIcon(new FlatSVGIcon("icons/oscuro.svg", 0.25f));
        btnClaro  = boton("");
        btnClaro.setIcon(new FlatSVGIcon("icons/claro.svg", 0.25f));

        pill.add(btnOscuro, "growx");
        pill.add(btnClaro,  "growx");
        add(pill, "growx");

        actualizarEstado();

        btnOscuro.addActionListener(e -> cambiar(true));
        btnClaro.addActionListener(e  -> cambiar(false));
    }

    private JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Marca visualmente cuál modo está activo ahora mismo
    private void actualizarEstado() {
        boolean isDark = FlatLaf.isLafDark();

        String activo =
            "arc:999; borderWidth:0; focusWidth:0; innerFocusWidth:0; margin:7,8,7,8; font:bold -1;" +
            "background:" + ACCENT + "; foreground:#ffffff;";

        String inactivo =
            "arc:999; borderWidth:0; focusWidth:0; innerFocusWidth:0; margin:7,8,7,8; font:-1;" +
            "background:null; foreground:$Label.disabledForeground;";

        btnOscuro.putClientProperty(FlatClientProperties.STYLE, isDark  ? activo : inactivo);
        btnClaro.putClientProperty(FlatClientProperties.STYLE,  !isDark ? activo : inactivo);

        if (btnOscuro.isShowing()) { btnOscuro.repaint(); btnClaro.repaint(); }
    }

    // Cambia el tema con animación y actualiza los botones.
    private void cambiar(boolean dark) {
        if (dark == FlatLaf.isLafDark()) return;
        EventQueue.invokeLater(() -> {
            FlatAnimatedLafChange.showSnapshot();
            if (dark) FlatMacDarkLaf.setup(); else FlatMacLightLaf.setup();
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            actualizarEstado();
        });
    }
}
