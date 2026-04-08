package gui.popus;

import Model.Cliente;
import dao.ClientesDAO;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

// Popup de búsqueda para asignar un cliente a una venta.
// Igual que pSeleccionarProducto pero para la tabla de clientes.
public class pSeleccionarCliente extends JPanel {
    private JTextField txtBuscar;
    private JTable tabla;
    private DefaultTableModel modelo;
    private List<Cliente> listaActual;
    private JButton btnNuevo;

    public pSeleccionarCliente() {
        setLayout(new MigLayout("wrap, fill, insets 15", "[fill, grow][]", "[][grow, fill]"));

        // 1. Panel más ancho, altura controlada
        setPreferredSize(new Dimension(850, 450));
        setMinimumSize(new Dimension(700, 400));

        // --- PANEL SUPERIOR ---
        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Escribe la cédula o nombre...");
        txtBuscar.putClientProperty("FlatLaf.style", "arc: 15; margin: 8,15,8,15; font: +1;");

        btnNuevo = new JButton("Nuevo Cliente");
        btnNuevo.putClientProperty("FlatLaf.style", "arc: 15; background: #10b981; foreground: #ffffff; font: bold; margin: 8,15,8,15; borderWidth:0; focusWidth:0;");

        add(txtBuscar, "growx, pushx, gapbottom 15");
        add(btnNuevo, "gapbottom 15");

        // --- TABLA ---
        modelo = new DefaultTableModel(new String[]{"Cédula/RUC", "Nombre", "Correo"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tabla = new JTable(modelo);
        tabla.setFocusable(false);

        // Renderizador Genérico (Centrado y Padding de 15px)
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    if (col == 1) { // Nombre a la izquierda por ser texto largo
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else { // El resto centrado
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                }
                return c;
            }
        });

        // Renderizador de Encabezados (Alineado con los datos)
        tabla.getTableHeader().setDefaultRenderer((t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            lbl.setForeground(Color.decode("#9f9f9f"));
            lbl.setOpaque(true);
            lbl.setBackground(t.getTableHeader().getBackground());

            if (col == 1) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            return lbl;
        });

        // Anchos de Columna Equitativos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(180); // Cédula
        tabla.getColumnModel().getColumn(0).setMaxWidth(200);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(350); // Nombre (Más ancho)
        tabla.getColumnModel().getColumn(2).setPreferredWidth(250); // Correo

        // Estilos de la tabla
        tabla.getTableHeader().putClientProperty("FlatLaf.style",
                "height:35; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background; font:bold +2;");
        tabla.putClientProperty("FlatLaf.style",
                "rowHeight: 50; showHorizontalLines: true; font: +1; selectionBackground:#009991; selectionForeground:#ffffff; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground;");

        // 2. SCROLL RESTRINGIDO: "h 0:300:350" limita la altura de la tabla para activar la barra lateral
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new EmptyBorder(0,0,0,0));
        scroll.getVerticalScrollBar().putClientProperty("FlatLaf.style",
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");

        add(scroll, "span 2, grow, push, h 0:300:350");

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                cargarDatos(txtBuscar.getText().trim());
            }
        });

        cargarDatos("");
    }

    private void cargarDatos(String busqueda) {
        modelo.setRowCount(0);
        listaActual = new ClientesDAO().buscar(busqueda);
        for (Cliente c : listaActual) {
            modelo.addRow(new Object[]{c.getCedula(), c.getNombre(), c.getCorreo()});
        }
    }

    // Devuelve el cliente seleccionado, o null si el usuario cerró sin elegir.
    public Cliente getClienteSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila >= 0 && listaActual != null) {
            return listaActual.get(fila);
        }
        return null;
    }

    public JButton getBtnNuevo() { return btnNuevo; }
}