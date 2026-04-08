package gui.popus;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import net.miginfocom.swing.MigLayout;


// Pequeño popup para ingresar el monto de un método de pago (efectivo o transferencia).

// Popup para ingresar el monto en efectivo o transferencia en el panel de venta.
// Calcula automáticamente el cambio.
public class metodoPago extends JPanel {

    JLabel titulo = new JLabel("Efectivo");
    JTextField pago = new JTextField();
    boolean efectivo = false;

    public metodoPago(){
        init();
        this.efectivo = true;
    };


    public void init(){
        this.setLayout(new MigLayout("wrap, fillx","[center] ", "[center]"));

        titulo.putClientProperty(FlatClientProperties.STYLE, "" +
            "font: bold +9"
        );

        pago.putClientProperty(FlatClientProperties.STYLE, "" +
            "font: bold +2;"+
            "margin: 7, 10, 7 ,10;"
        );

        pago.setHorizontalAlignment(JTextField.CENTER);
        pago.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/dollar.svg", 0.35f));
        add(titulo);
        add(pago, "gaptop 10, width 150:150");

    }

    public JTextField getPago(){
        return pago;
    }

    public JLabel getTitulo() {
        return titulo;
    }

    public void setTitulo(JLabel titulo) {
        this.titulo = titulo;
    }
}
