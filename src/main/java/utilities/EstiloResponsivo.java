package utilities;

// Genera estilos de botones y componentes adaptados al tamaño de la pantalla.
// En laptops pequeños los márgenes se reducen para que todo quepa mejor.
public class EstiloResponsivo {

    // Devuelve el factor de escala según la pantalla (0.87=pequeño, 1.0=normal).
    public static float escala() {
        int fs = Integer.parseInt(System.getProperty("ivencore.fontScale", "15"));
        return fs / 15.0f;
    }


    // Devuelve el estilo completo para los botones de acción de los paneles.
    // Los márgenes se ajustan según el tamaño de pantalla.
    public static String botonPrimary() {
        return boton("#86c5de", "#2563eb");
    }

    public static String botonEdit() {
        return boton("#ffda49", "#f0c000");
    }

    public static String botonDelete() {
        return boton("#7e5aff", "#5a3fbf");
    }

    public static String botonDanger() {
        return boton("#ff0000", "#cc0000");
    }

    public static String botonGreen() {
        return boton("#10b981", "#0d9668");
    }

    public static String botonBlue() {
        return boton("#3b82f6", "#2563eb");
    }

    private static String boton(String bg, String hover) {
        String m = margen();
        return "arc:15; background:" + bg + "; foreground:#ffffff; font:bold;" +
               " margin:" + m + "; hoverBackground:" + hover +
               "; borderWidth:0; focusWidth:0; innerFocusWidth:0";
    }

    // Márgenes del botón según tamaño de pantalla.
    // Pantalla pequeña → márgenes más chicos para que los botones quepan.
    public static String margen() {
        float escala = escala();
        if (escala < 0.88f) return "5,10,5,10";   // laptop pequeño 13px
        if (escala < 0.94f) return "6,14,6,14";   // laptop mediano 14px
        return "10,20,10,20";                       // Full HD normal 15px+
    }

    // Insets para los paneles de contenido
    public static String insets() {
        float escala = escala();
        if (escala < 0.88f) return "12";
        if (escala < 0.94f) return "16";
        return "20";
    }
}
