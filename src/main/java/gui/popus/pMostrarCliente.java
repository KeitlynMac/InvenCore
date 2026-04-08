package gui.popus;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Popup de solo lectura con los datos de un cliente.
// Se abre desde el panel de Clientes para ver el detalle.
public class pMostrarCliente extends TabbedForm {

    JLabel cedula_ruc = new JLabel();
    JLabel nombre = new JLabel();
    JLabel lbCorreo = new JLabel("Correo");
    JLabel correo = new JLabel();
    JLabel lbTelefono = new JLabel("Telefono");
    JLabel telefono = new JLabel();
    JLabel lbDireccion = new JLabel("Direccion");
    JLabel direccion = new JLabel();

    public pMostrarCliente(){
        init();
    }

    public void init(){
        this.setLayout(new MigLayout("wrap, fillx, insets 20", "[center]", "[center]"));


        cedula_ruc.putClientProperty(FlatClientProperties.STYLE, ""+
                "background: #c300d9;" +
                "foreground: #ffffff;" +
                "arc: 15;" +
                "font: bold +1;"

        );
        cedula_ruc.setBorder(new EmptyBorder(5,10,5,10));


        add(cedula_ruc);
        add(nombre);
        add(lbCorreo);
        add(correo);
        add(lbTelefono);
        add(telefono);
        add(lbDireccion);
        add(direccion);
    }

    public void setCliente(String cedula, String nombre, String correo, String telefono, String direccion){
        this.cedula_ruc.setText(cedula);
        this.nombre.setText(nombre);
        this.correo.setText(correo);
        this.telefono.setText(telefono);
        this.direccion.setText(direccion);
    }


    public JLabel getCedula_ruc() {
        return cedula_ruc;
    }
}
