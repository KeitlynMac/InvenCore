package utilities;

import javax.swing.JPanel;

// Panel base que extienden todos los paneles del sistema.
// formOpen() se llama automáticamente cada vez que el panel se muestra en pantalla.
// Lo sobreescribo en los paneles que necesitan animar o recargar datos al abrirse.
public class TabbedForm extends JPanel {

    // Se ejecuta cuando el panel se hace visible. Por defecto no hace nada.
    // Los paneles que necesiten recargar datos o animar al abrirse lo sobreescriben.
    public void formOpen(){
    }
}
