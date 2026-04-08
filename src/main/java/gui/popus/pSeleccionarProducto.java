package gui.popus;

import Model.Producto;
import dao.ProductoDAO;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Popup de búsqueda para seleccionar un producto al crear una venta.
// Muestra la tabla de productos con buscador y devuelve el seleccionado.
public class pSeleccionarProducto extends JPanel {
    private JTextField txtBuscar;
    private JLabel lblContador;
    private JTable tabla;
    private DefaultTableModel modelo;
    private List<Producto> listaActual;

    public pSeleccionarProducto() {
        setLayout(new MigLayout("wrap, fill, insets 15", "[fill, grow]", "[][grow, fill]"));

        // 1. Panel mucho más ancho, misma altura
        setPreferredSize(new Dimension(1150, 550));
        setMinimumSize(new Dimension(900, 500));

        // --- PANEL SUPERIOR (Buscador + Contador) ---
        JPanel pnlTop = new JPanel(new MigLayout("insets 0", "[grow, fill][]", "[center]"));

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Escribe el nombre o código del producto...");
        txtBuscar.putClientProperty("FlatLaf.style", "arc: 15; margin: 8,15,8,15; font: +1;");

        lblContador = new JLabel("Seleccionados: 0");
        lblContador.putClientProperty("FlatLaf.style", "font: bold +2; foreground: #009991;");

        pnlTop.add(txtBuscar, "growx, pushx");
        pnlTop.add(lblContador, "gapleft 20, gapright 10");

        add(pnlTop, "gapbottom 10");

        // --- TABLA ---
        modelo = new DefaultTableModel(new Object[]{"Sel.", "Imagen", "Código", "Nombre", "Precio", "Stock"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;   // Checkbox
                if (columnIndex == 1) return ImageIcon.class; // Imagen
                return super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) { return column == 0; }
        };

        tabla = new JTable(modelo);
        tabla.setFocusable(false);

        // Listener para actualizar el contador cuando se marca/desmarca un checkbox
        modelo.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() == 0) {
                    actualizarContador();
                }
            }
        });

        // Renderizador de Imagen (Centrado y con fondo)
        tabla.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = new JLabel();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBackground(sel ? t.getSelectionBackground() : t.getBackground());
                if (val instanceof ImageIcon) lbl.setIcon((ImageIcon) val);
                else {
                    lbl.setText("—");
                    lbl.setForeground(Color.GRAY);
                }
                return lbl;
            }
        });

        // Renderizador Genérico (Centrado y Padding)
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    if (col == 3) { // Nombre a la izquierda
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

            if (col == 3) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            return lbl;
        });

        // Anchos de Columna Equitativos
        tabla.getColumnModel().getColumn(0).setPreferredWidth(60); // Checkbox
        tabla.getColumnModel().getColumn(0).setMinWidth(60);
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);

        tabla.getColumnModel().getColumn(1).setPreferredWidth(70); // Imagen
        tabla.getColumnModel().getColumn(1).setMinWidth(70);
        tabla.getColumnModel().getColumn(1).setMaxWidth(70);

        int anchoEquitativo = 150;
        tabla.getColumnModel().getColumn(2).setPreferredWidth(anchoEquitativo); // Código
        tabla.getColumnModel().getColumn(3).setPreferredWidth(350);             // Nombre (Mucho espacio)
        tabla.getColumnModel().getColumn(4).setPreferredWidth(anchoEquitativo); // Precio
        tabla.getColumnModel().getColumn(5).setPreferredWidth(anchoEquitativo); // Stock

        // Estilos de la tabla
        tabla.getTableHeader().putClientProperty("FlatLaf.style",
                "height:35; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background; font:bold +2;");
        tabla.putClientProperty("FlatLaf.style",
                "rowHeight: 60; showHorizontalLines: true; font: +1; selectionBackground:#009991; selectionForeground:#ffffff; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground;");

        // 2. SCROLL RESTRINGIDO: "h 0:400:450" obliga a que la tabla no mida más de 450px de alto, activando el scroll
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new EmptyBorder(0,0,0,0));
        scroll.getVerticalScrollBar().putClientProperty("FlatLaf.style",
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");
        add(scroll, "grow, push, h 0:400:450");

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                cargarDatos(txtBuscar.getText().trim());
            }
        });

        cargarDatos(""); // Cargar todos al inicio
    }

    private void actualizarContador() {
        int count = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Boolean marcado = (Boolean) modelo.getValueAt(i, 0);
            if (marcado != null && marcado) {
                count++;
            }
        }
        lblContador.setText("Seleccionados: " + count);
    }

    private void cargarDatos(String busqueda) {
        modelo.setRowCount(0);
        listaActual = new ProductoDAO().buscar(busqueda);
        for (Producto p : listaActual) {

            ImageIcon thumb = null;
            if (p.getImagenPath() != null && new File(p.getImagenPath()).exists()) {
                thumb = new ImageIcon(
                        new ImageIcon(p.getImagenPath()).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));
            }

            modelo.addRow(new Object[]{
                    false,
                    thumb,
                    p.getCodigo(),
                    p.getNombre(),
                    "$" + String.format("%.2f", p.getPrecio()),
                    p.getStock()
            });
        }
        actualizarContador(); // Reinicia a 0 al recargar
    }

    public List<Producto> getProductosSeleccionados() {
        List<Producto> seleccionados = new ArrayList<>();
        for (int i = 0; i < tabla.getRowCount(); i++) {
            Boolean marcado = (Boolean) tabla.getValueAt(i, 0);
            if (marcado != null && marcado) {
                seleccionados.add(listaActual.get(i));
            }
        }
        return seleccionados;
    }
}