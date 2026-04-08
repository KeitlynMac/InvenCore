import java.awt.Font;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import dao.InicializadorDB;
import gui.main.frLogin;

// Punto de entrada de ivencore.
// Instala la fuente Roboto, registra los temas FlatLaf y abre el login.
public class Ivencore {
    // Variables estáticas para mantener el bloqueo activo mientras el programa funcione
    private static FileChannel canalBloqueo;
    private static FileLock candado;

    public static void main(String[] args) {

        //Configuración de Tema
        try {
            FlatRobotoFont.install();
            FlatLaf.registerCustomDefaultsSource("flatlaf");
            FlatLaf.registerCustomDefaultsSource("flatlaf.themes");
            // Calcular el tamaño de fuente según el tamaño de la pantalla.
            // En pantallas pequeñas (laptops) todo se hace más compacto para que quepa.
            int fontSize = calcularFuente();
            UIManager.put("defaultFont", new Font("Roboto", Font.PLAIN, fontSize));
            // Guardar el factor de escala para que los paneles puedan usarlo
            System.setProperty("ivencore.fontScale", String.valueOf(fontSize));
            FlatMacLightLaf.setup();

            com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter.getInstance().setMapper(color -> {
                if (color.getRGB() == java.awt.Color.BLACK.getRGB()) {
                    java.awt.Color fg = javax.swing.UIManager.getColor("Label.foreground");
                    return fg != null ? fg : color;
                }
                return color;
            });
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Ivencore.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        // BLOQUEO DE INSTANCIAS MÚLTIPLES (Escudo Protector)
        try {
            String rutaCarpeta = System.getProperty("user.home") + File.separator + "Ivencore";
            new File(rutaCarpeta).mkdirs();

            File archivoLock = new File(rutaCarpeta + File.separator + "ivencore.lock");
            canalBloqueo = new RandomAccessFile(archivoLock, "rw").getChannel();
            candado = canalBloqueo.tryLock();

            if (candado == null) {
                // Como FlatLaf ya cargó, este JOptionPane se verá hermoso y moderno
                JOptionPane.showMessageDialog(null,
                        "El sistema Ivencore ya está abierto.\nRevisa tu barra de tareas.",
                        "Atención",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }

            archivoLock.deleteOnExit();

        } catch (Exception e) {
            System.err.println("Error al verificar instancias: " + e.getMessage());
        }
        // --- FIN DEL BLOQUEO ---

        // --- 3. INICIALIZAR LA BASE DE DATOS ---
        InicializadorDB dbInit = new InicializadorDB();
        dbInit.inicializarBaseDeDatos();

        // --- 4. Login ---
        java.awt.EventQueue.invokeLater(() -> {
            frLogin login = new frLogin();
            login.setVisible(true);
        });
    }

    // Calcula el tamaño de fuente adecuado según la resolución de la pantalla.
    // En pantallas 1366x768 (laptops pequeños) usamos 13px.
    // En Full HD (1920x1080) usamos 15px.
    // En pantallas grandes o 4K escalamos hasta 18px.
    private static int calcularFuente() {
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int ancho = screen.width;
        int alto  = screen.height;
        int pixeles = ancho * alto;

        if (pixeles <= 1366 * 768)   return 13;  // Laptop pequeño (HD)
        if (pixeles <= 1600 * 900)   return 14;  // Laptop mediano (HD+)
        if (pixeles <= 1920 * 1080)  return 15;  // Full HD — estándar
        if (pixeles <= 2560 * 1440)  return 16;  // 2K
        return 18;                               // 4K o más grande
    }



}