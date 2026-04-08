package utilities;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

// Controla qué panel se muestra en el área central de la ventana.
// También maneja el historial de navegación para los botones Atrás/Adelante.
// Solo hay una instancia (singleton) porque solo hay un área de contenido.
public class WindowsTabbed {

    private static WindowsTabbed instance;
    private JPanel body;
    private TabbedForm temp;

    // --- NUEVAS VARIABLES: MEMORIA DEL HISTORIAL ---
    private List<TabbedForm> historial = new ArrayList<>();
    private int indiceActual = -1;
    private boolean estaNavegando = false; // Evita duplicar pantallas al retroceder

    public static WindowsTabbed getInstance() {
        if (instance == null) {
            instance = new WindowsTabbed();
        }
        return instance;
    }

    // Conecta el contenedor donde se van a mostrar los paneles.
    public void install(JFrame frame, JPanel body) {
        this.body = body;
        body.setLayout(new BorderLayout());
    }

    // Muestra un panel nuevo en el área central.
    public boolean addTab(TabbedForm component) {
        showForm(component);
        return true;
    }

    // Reemplaza el contenido actual y guarda el panel en el historial.
    public void showForm(TabbedForm component) {
        body.removeAll();

        // Envolver el panel en un JScrollPane para que funcione en pantallas pequeñas.
        // El scroll solo aparece si el contenido no cabe — si cabe, es invisible.
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(component);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().putClientProperty(
            com.formdev.flatlaf.FlatClientProperties.STYLE,
            "trackArc:999; width:6; thumbInsets:2,2,2,2;");
        scroll.getHorizontalScrollBar().putClientProperty(
            com.formdev.flatlaf.FlatClientProperties.STYLE,
            "trackArc:999; thumbInsets:2,2,2,2;");
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        body.add(scroll, java.awt.BorderLayout.CENTER);
        body.repaint();
        body.revalidate();
        component.formOpen();
        temp = component;

        // --- LÓGICA DE GUARDADO EN MEMORIA ---
        if (!estaNavegando) {
            // Si el usuario abrió una ventana nueva, borramos su historial "futuro"
            if (indiceActual < historial.size() - 1) {
                historial.subList(indiceActual + 1, historial.size()).clear();
            }
            historial.add(component);
            indiceActual++;
        }
    }

    // MÉTODOS PARA LOS BOTONES
    public void undo() {
        if (indiceActual > 0) {
            indiceActual--;
            estaNavegando = true; // Pausar guardado

            try {
                // Instanciar el panel de nuevo para que cargue los datos frescos
                TabbedForm panelViejo = historial.get(indiceActual);
                TabbedForm panelFresco = panelViejo.getClass().getDeclaredConstructor().newInstance();
                historial.set(indiceActual, panelFresco); // Actualizar historial con la versión fresca
                showForm(panelFresco);
            } catch (Exception e) {
                // Fallback de seguridad por si falla la reflexión
                showForm(historial.get(indiceActual));
            }

            estaNavegando = false; // Reanudar guardado
        }
    }

    // Va al panel siguiente (Adelante) y actualiza los datos
    public void redo() {
        if (indiceActual < historial.size() - 1) {
            indiceActual++;
            estaNavegando = true;

            try {
                // Instanciar el panel de nuevo para que cargue los datos frescos
                TabbedForm panelViejo = historial.get(indiceActual);
                TabbedForm panelFresco = panelViejo.getClass().getDeclaredConstructor().newInstance();
                historial.set(indiceActual, panelFresco); // Actualizar historial
                showForm(panelFresco);
            } catch (Exception e) {
                // Fallback de seguridad
                showForm(historial.get(indiceActual));
            }

            estaNavegando = false;
        }
    }

    // Recarga el panel actual desde cero — re-instancia para traer datos frescos de la BD.
    public void refresh() {
        if (temp == null) return;
        try {
            // Re-instanciar el panel actual para recargar datos frescos desde la BD
            TabbedForm nuevo = temp.getClass().getDeclaredConstructor().newInstance();
            estaNavegando = true;
            showForm(nuevo);
            // Reemplazar en historial para que undo/redo apunten al panel fresco
            historial.set(indiceActual, nuevo);
            estaNavegando = false;
        } catch (Exception e) {
            // Fallback: llamar formOpen() si no se puede re-instanciar
            temp.formOpen();
        }
    }
}