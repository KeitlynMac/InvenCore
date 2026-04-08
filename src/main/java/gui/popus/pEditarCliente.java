package gui.popus;

import dao.ClientesDAO;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;


// Formulario para editar un cliente existente. Llama a cargarDatos() antes de mostrarlo.

// Popup para editar los datos de un cliente existente.
public class pEditarCliente extends JPanel {

    ClientesDAO cdb = new ClientesDAO();
    private JTextField txtId;
    private JTextField txtCedulaRuc;
    private JTextField txtNombre;
    private JTextField txtCorreo;
    private JTextField txtTelefono;
    private JTextField txtDireccion;

    public pEditarCliente() {
        init();
    }

    public void init() {
        this.setLayout(new MigLayout("wrap, fillx, insets 20", "[fill, grow]", "[center]"));


        JLabel lbId = new JLabel("#", JLabel.LEFT);
        txtId = new JTextField();
        txtId.setFocusable(false);
        lbId.putClientProperty(FlatClientProperties.STYLE, "" +
                "font: +1;"
        );

        txtId.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );


        JLabel lbCedulaRuc = new JLabel("CI o RUC", JLabel.LEFT);
        txtCedulaRuc = new JTextField();
        lbCedulaRuc.putClientProperty(FlatClientProperties.STYLE, "" +
                "font: +1;"
        );

        txtCedulaRuc.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );

        JLabel lbNombre = new JLabel("Nombres y Apellidos", JLabel.LEFT);
        txtNombre = new JTextField();
        lbNombre.putClientProperty(FlatClientProperties.STYLE, "" +
                "font:+1;"
        );

        txtNombre.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );

        JLabel lbCorreo = new JLabel("Correo", JLabel.LEFT);
        txtCorreo = new JTextField();
        lbCorreo.putClientProperty(FlatClientProperties.STYLE, "" +
                "font: +1;"
        );

        txtCorreo.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );

        JLabel lbTelefono = new JLabel("Telefono", JLabel.LEFT);
        txtTelefono = new JTextField();
        lbTelefono.putClientProperty(FlatClientProperties.STYLE, "" +
                "font: +1;"
        );

        txtTelefono.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );

        JLabel lbDireccion = new JLabel("Direccion", JLabel.LEFT);
        txtDireccion = new JTextField();
        lbDireccion.putClientProperty(FlatClientProperties.STYLE, "" +
                "font: +1;"
        );

        txtDireccion.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc: 15;" +
                "borderWidth: 0;" +
                "margin:10,10,10,10;"
        );

        add(lbId);
        add(txtId, "growx");
        add(lbCedulaRuc);
        add(txtCedulaRuc, "growx");
        add(lbNombre);
        add(txtNombre, "growx");
        add(lbCorreo);
        add(txtCorreo, "growx");
        add(lbTelefono);
        add(txtTelefono, "growx");
        add(lbDireccion);
        add(txtDireccion, "growx");


    }

    public void cargarDatos(String id, String cedula, String nombre, String correo, String telefono, String direccion) {
        txtId.setText(id);
        txtCedulaRuc.setText(cedula);
        txtNombre.setText(nombre);
        txtCorreo.setText(correo);
        txtTelefono.setText(telefono);
        txtDireccion.setText(direccion);
    }


    // Guarda los cambios del cliente.
    public boolean guardarDatos() {
        // Validación básica
        if (txtCedulaRuc.getText().trim().isEmpty() || txtNombre.getText().trim().isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Campos obligatorios", "La Cédula y Nombre no pueden estar vacíos.");
            return false;
        }

        Model.Cliente c = new Model.Cliente();
        // BUG FIX: Se debe establecer el ID para que el UPDATE funcione correctamente
        c.setIdCliente(Integer.parseInt(txtId.getText().trim()));
        c.setCedula(txtCedulaRuc.getText().trim());
        c.setNombre(txtNombre.getText().trim());
        c.setCorreo(txtCorreo.getText().trim());
        c.setTelefono(txtTelefono.getText().trim());
        c.setDireccion(txtDireccion.getText().trim());

        dao.ClientesDAO dao = new dao.ClientesDAO();
        if (dao.editar(c)) {
            raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS, raven.toast.Notifications.Location.BOTTOM_CENTER, "Cliente actualizado exitosamente");
            return true;
        }
        return false;
    }
}
