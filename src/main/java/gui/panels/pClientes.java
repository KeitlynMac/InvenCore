package gui.panels;

import dao.ClientesDAO;
import Model.Cliente;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import gui.popus.pEditarCliente;
import gui.popus.pRegistrarCliente;
import net.miginfocom.swing.MigLayout;
import raven.alerts.MessageAlerts;
import raven.modal.component.DropShadowBorder;
import raven.popup.DefaultOption;
import raven.popup.GlassPanePopup;
import raven.popup.component.SimplePopupBorder;
import raven.toast.Notifications;
import utilities.HeaderTabla;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;


// Gestión de clientes. Lista, busca, registra y edita clientes.
// No deja borrar un cliente que ya tiene facturas a su nombre.

// Panel de gestión de clientes.
// Busca, muestra, agrega, edita y elimina clientes.
public class pClientes extends TabbedForm {

    private final String STYLE_PRIMARY;
    private final String STYLE_EDIT;
    private final String STYLE_DELETE;
    private final String STYLE_DANGER;
    private final String STYLE_GREEN;
    private final String STYLE_BLUE;
    private final String STYLE_NEUTRAL;
    {
        STYLE_PRIMARY = utilities.EstiloResponsivo.botonPrimary();
        STYLE_EDIT    = utilities.EstiloResponsivo.botonEdit();
        STYLE_DELETE  = utilities.EstiloResponsivo.botonDelete();
        STYLE_DANGER  = utilities.EstiloResponsivo.botonDanger();
        STYLE_GREEN   = utilities.EstiloResponsivo.botonGreen();
        STYLE_BLUE    = utilities.EstiloResponsivo.botonBlue();
        STYLE_NEUTRAL = utilities.EstiloResponsivo.botonPrimary();
    }


    private final ClientesDAO cdb = new ClientesDAO();
    private JTable tabla;
    private DefaultTableModel modelo;

    public pClientes() {
        init();
    }

    private void instalarGlassPane() {
        Component root = SwingUtilities.getRoot(this);
        if (root instanceof JFrame frame) {
            GlassPanePopup.install(frame);
            Notifications.getInstance().setJFrame(frame);
        }
    }

    private void init() {
        instalarGlassPane();
        setLayout(new MigLayout("fill, insets 20", "[center]", "[center]"));

        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10", "[fill]", "[push]"));
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", ""));

        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 25; background:$Table.background;");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        JLabel lbTitulo = new JLabel("CLIENTES", JLabel.LEFT);
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbTitulo.setBorder(new EmptyBorder(0, 10, 4, 0));

        JTextField txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.STYLE,
                "arc: 15; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin:10,20,10,20;");
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por Cédula o Nombre");
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new FlatSVGIcon("icons/search.svg", 0.30f));

        JButton btnNew = crearBoton("", "#3b64ff", "#ffffff", "icons/users-plus.svg");
        JButton btnEdit = crearBoton("", STYLE_EDIT, "icons/pencil.svg");
        JButton btnDelete = crearBoton("", null, null, "icons/square-rounded-x.svg");

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelo.addColumn("ID");
        modelo.addColumn("Cédula/RUC");
        modelo.addColumn("Nombres");
        modelo.addColumn("Correo");
        modelo.addColumn("Teléfono");
        modelo.addColumn("Dirección");

        tabla = new JTable(modelo);
        tabla.setFocusable(false);
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:30; hoverBackground:null; pressedBackground:null;" +
                        "separatorColor:$TableHeader.background; font:bold +2; foreground: #9f9f9f");

        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50; selectionBackground:#009991; selectionForeground:#ffffff; showHorizontalLines:true; intercellSpacing:0,1;" +
                        "cellFocusColor:$TableHeader.hoverBackground;" +
                        "selectionBackground:$TableHeader.hoverBackground; font: +1;");

        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");

        tabla.getTableHeader().setDefaultRenderer(new HeaderTabla(tabla));

        headerPanel.add(lbTitulo, "wrap, gapbottom 10");
        headerPanel.add(txtBuscar, "split 4, growx, pushx");
        headerPanel.add(btnNew, "align right, gapleft 15, sizegroup btn, width 80:100:120");
        headerPanel.add(btnEdit, "sizegroup btn, width 80:100:120");
        headerPanel.add(btnDelete, "wrap, sizegroup btn, width 80:100:120");

        panel.add(scrollPane, "grow, push");
        add(headerPanel, "wrap, growx");
        add(panel, "grow, push");

        // Cargar datos inicialmente
        actualizarTabla();

        // Búsqueda en tiempo real
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                List<Cliente> filtrados = cdb.buscar(txtBuscar.getText());
                llenarTabla(filtrados);
            }
        });

        // Eventos
        btnNew.addActionListener(e -> mostrarRegistrar());
        btnEdit.addActionListener(e -> mostrarEditar());
        btnDelete.addActionListener(e -> eliminarSeleccionado());
    }

    // Recarga la lista de clientes desde la BD.
    private void actualizarTabla() {
        llenarTabla(cdb.obtenerTodos());
    }

    private void llenarTabla(List<Cliente> lista) {
        modelo.setRowCount(0);
        for (Cliente c : lista) {
            modelo.addRow(new Object[]{
                    c.getIdCliente(), c.getCedula(), c.getNombre(), c.getCorreo(), c.getTelefono(), c.getDireccion()
            });
        }
    }

    // Abre el popup para agregar un cliente nuevo.
    private void mostrarRegistrar() {
        pRegistrarCliente pr = new pRegistrarCliente();
        DefaultOption option = new DefaultOption() {
            @Override
            public boolean closeWhenClickOutside() {
                return true;
            }
        };
        GlassPanePopup.showPopup(new SimplePopupBorder(pr, "Nuevo Cliente",
                new String[]{"Cancelar", "Guardar"}, (pc, i) -> {
            if (i == 1) {
                if (pr.guardarDatos()) {
                    pc.closePopup();
                    actualizarTabla();
                }
            } else {
                pc.closePopup();
            }
        }), option);
    }

    // Abre el popup para editar el cliente seleccionado.
    private void mostrarEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            MessageAlerts.getInstance().showMessage("Selección vacía", "Selecciona el cliente que deseas editar.");
            return;
        }

        pEditarCliente pe = new pEditarCliente();
        pe.cargarDatos(
                tabla.getValueAt(fila, 0).toString(),
                tabla.getValueAt(fila, 1).toString(),
                tabla.getValueAt(fila, 2).toString(),
                tabla.getValueAt(fila, 3).toString(),
                tabla.getValueAt(fila, 4).toString(),
                tabla.getValueAt(fila, 5).toString()
        );

        DefaultOption option = new DefaultOption() {
            @Override
            public boolean closeWhenClickOutside() {
                return true;
            }
        };
        GlassPanePopup.showPopup(new SimplePopupBorder(pe, "Editar Cliente",
                new String[]{"Cancelar", "Guardar Cambios"}, (pc, i) -> {
            if (i == 1) {
                if (pe.guardarDatos()) {
                    pc.closePopup();
                    actualizarTabla();
                }
            } else {
                pc.closePopup();
            }
        }), option);
    }

    // Pide confirmación y elimina el cliente seleccionado.
    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            MessageAlerts.getInstance().showMessage("Selección vacía", "Selecciona el cliente que deseas eliminar.");
            return;
        }

        int idCliente = Integer.parseInt(tabla.getValueAt(fila, 0).toString());

        // Verificar integridad referencial antes de eliminar
        if (cdb.tieneFacturas(idCliente)) {
            MessageAlerts.getInstance().showMessage("No permitido",
                    "Este cliente tiene facturas registradas y no puede ser eliminado.\nSi ya no es activo, puedes dejar el registro como está.");
            return;
        }

        MessageAlerts.getInstance().showMessage("Eliminar Cliente",
                "¿Estás seguro que quieres eliminar este cliente?",
                MessageAlerts.MessageType.ERROR, MessageAlerts.OK_OPTION, (popupController, i) -> {
                    if (i == MessageAlerts.OK_OPTION) {
                        if (cdb.eliminar(idCliente)) {
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.BOTTOM_CENTER, "Cliente eliminado");
                            actualizarTabla();
                        } else {
                            MessageAlerts.getInstance().showMessage("Error", "No se pudo eliminar.");
                        }
                    }
                });
    }

    private FlatSVGIcon crearIcono(String path, float scale, String hexColor) {
        FlatSVGIcon icon = new FlatSVGIcon(path, scale);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.decode(hexColor)));
        return icon;
    }

    private JButton crearBoton(String texto, String estilo, String iconPath) {
        JButton btn = new JButton(texto);
        btn.setIcon(crearIcono(iconPath, 0.30f, "#ffffff"));
        btn.putClientProperty(FlatClientProperties.STYLE, estilo);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton crearBoton(String texto, String bg, String fg, String iconPath) {
        String estilo;
        if ("#3b64ff".equals(bg) || "#3b82f6".equals(bg) || "#86c5de".equals(bg)) estilo = STYLE_PRIMARY;
        else if ("#10b981".equals(bg)) estilo = STYLE_GREEN;
        else if ("#ff0000".equals(bg) || "#ef4444".equals(bg)) estilo = STYLE_DANGER;
        else if ("#ffda49".equals(bg)) estilo = STYLE_EDIT;
        else if (bg == null) estilo = STYLE_DELETE;
        else estilo = STYLE_NEUTRAL;
        return crearBoton(texto, estilo, iconPath);
    }
}