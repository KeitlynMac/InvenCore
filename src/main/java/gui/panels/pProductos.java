package gui.panels;

import dao.ProductoDAO;
import Model.Producto;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import gui.popus.pEditarProducto;
import gui.popus.pRegistrarProducto;
import net.miginfocom.swing.MigLayout;
import raven.alerts.MessageAlerts;
import raven.modal.component.DropShadowBorder;
import raven.popup.DefaultOption;
import raven.popup.GlassPanePopup;
import raven.popup.component.SimplePopupBorder;
import raven.toast.Notifications;
import utilities.TableBadgeCellRenderer;
import utilities.TabbedForm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

// Panel de gestión del inventario de productos.
// Permite buscar, filtrar por stock, ver, agregar, editar y eliminar productos.
public class pProductos extends TabbedForm {

    // Estilos de botones con márgenes adaptativos según el tamaño de pantalla
    private final String STYLE_PRIMARY;
    private final String STYLE_EDIT;
    private final String STYLE_DELETE;
    private final String STYLE_DANGER;
    private final String STYLE_GREEN;
    private final String STYLE_BLUE;
    private final String STYLE_NEUTRAL;

    // Inicializar con el margen adecuado para esta pantalla
    {
        STYLE_PRIMARY = utilities.EstiloResponsivo.botonPrimary();
        STYLE_EDIT    = utilities.EstiloResponsivo.botonEdit();
        STYLE_DELETE  = utilities.EstiloResponsivo.botonDelete();
        STYLE_DANGER  = utilities.EstiloResponsivo.botonDanger();
        STYLE_GREEN   = utilities.EstiloResponsivo.botonGreen();
        STYLE_BLUE    = utilities.EstiloResponsivo.botonBlue();
        STYLE_NEUTRAL = utilities.EstiloResponsivo.botonPrimary();
    }


    private final ProductoDAO pdb = new ProductoDAO();
    private JTable tabla;
    private DefaultTableModel modelo;
    private JTextField txtBuscar;
    private JButton    btnFiltroStock;
    private String     filtroStockActual = "Todos";

    // Índices de columna (la col 0 = ID oculta)
    private static final int COL_ID     = 0;
    private static final int COL_IMG    = 1;
    private static final int COL_CODIGO = 2;
    private static final int COL_NOMBRE = 3;
    private static final int COL_PRECIO = 4;
    private static final int COL_STOCK  = 5;
    private static final int COL_CAT    = 6;
    private static final int COL_DESC   = 7;
    private static final int COL_VENC   = 8;

    public static class StockInfo implements TableBadgeCellRenderer.Info {
        private final int stock;
        public StockInfo(int s) { this.stock = s; }
        @Override public String getText() {
            if (stock > 10) return stock + " (Suficiente)";
            if (stock > 0)  return stock + " (Poco Stock)";
            return stock + " (Agotado)";
        }
        @Override public Color getColor() {
            if (stock > 10) return Color.decode("#10b981");
            if (stock > 0)  return Color.decode("#f59e0b");
            return Color.decode("#ef4444");
        }
        @Override public Icon getIcon() { return null; }
    }

    public pProductos() { init(); }

    private void instalarGlassPane() {
        Component root = SwingUtilities.getRoot(this);
        if (root instanceof JFrame frame) {
            GlassPanePopup.install(frame);
            Notifications.getInstance().setJFrame(frame);
        }
    }

    private void init() {
        instalarGlassPane();
        setLayout(new MigLayout("fill, insets 20", "[fill, grow]", "[][grow]"));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", ""));

        JLabel lbProducto = new JLabel("PRODUCTOS", JLabel.LEFT);
        lbProducto.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbProducto.setBorder(new EmptyBorder(0, 10, 4, 0));

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.STYLE,
                "arc:15; borderWidth:0; focusWidth:0; innerFocusWidth:0; margin:10,20,10,20;");
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre, Código o Categoría");
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("icons/search.svg", 0.30f));

        btnFiltroStock = crearBoton("Stock: Todos", STYLE_NEUTRAL, "icons/filtro.svg");
        btnFiltroStock.putClientProperty(FlatClientProperties.STYLE, "arc:15; focusWidth:0; innerFocusWidth:0; background:#00e39b; font: bold; foreground: #ffffff");
        JPopupMenu popupFiltro = new JPopupMenu();
        for (String op : new String[]{"Todos","Suficiente","Poco Stock","Agotado"}) {
            JMenuItem item = new JMenuItem(op);
            item.addActionListener(e -> {
                filtroStockActual = op;
                btnFiltroStock.setText(op.equals("Todos") ? "Stock: Todos" : op);
                actualizarTabla();

            });
            popupFiltro.add(item);
        }
        btnFiltroStock.addActionListener(e -> popupFiltro.show(btnFiltroStock, 0, btnFiltroStock.getHeight()));

        JButton btnNew    = crearBoton("",   "#3b64ff","#ffffff","icons/plus.svg");
        JButton btnEdit   = crearBoton("",  STYLE_EDIT, "icons/pencil.svg");
        JButton btnDelete = crearBoton("",STYLE_DELETE, "icons/square-rounded-x.svg");

        headerPanel.add(lbProducto, "wrap, gapbottom 10");
        headerPanel.add(txtBuscar, "split 5, growx, pushx");
        headerPanel.add(btnFiltroStock, "width 150:150:150");
        headerPanel.add(btnNew,    "align right, gapleft 15, sizegroup btn, width 80:100:120");
        headerPanel.add(btnEdit,   "sizegroup btn, width 80:100:120");
        headerPanel.add(btnDelete, "wrap, sizegroup btn, width 80:100:120");

        // ── Tabla ──────────────────────────────────────────────────────────
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10","[fill]","[push]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:25; background:$Table.background;");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == COL_IMG)   return ImageIcon.class;
                if (col == COL_STOCK) return StockInfo.class;
                return super.getColumnClass(col);
            }
        };
        modelo.addColumn("ID");
        modelo.addColumn("Imagen");
        modelo.addColumn("Código");
        modelo.addColumn("Nombre");
        modelo.addColumn("Precio");
        modelo.addColumn("Stock");
        modelo.addColumn("Categoría");
        modelo.addColumn("Descripción");
        modelo.addColumn("Vence");

        tabla = new JTable(modelo);
        tabla.setRowHeight(60);
        tabla.setFocusable(false);

        // Ocultar columna ID
        ocultar(COL_ID);

        // Columna imagen
        tabla.getColumnModel().getColumn(COL_IMG).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Badge de stock
        TableBadgeCellRenderer.apply(tabla, StockInfo.class);

        // ── Renderizador Genérico ──
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    if (col == COL_DESC) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    // Aumentamos el margen a 15px en cada lado para separar las columnas
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                }
                return c;
            }
        });

        // ── Renderizador del Header ──
        tabla.getTableHeader().setDefaultRenderer((t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            lbl.setForeground(Color.decode("#9f9f9f"));
            lbl.setOpaque(true);
            lbl.setBackground(t.getTableHeader().getBackground());

            if (col == COL_DESC) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }
            // Mismo margen en los títulos para que estén alineados con los datos
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            return lbl;
        });

        // ── Anchos de Columna Equitativos ──

        // La imagen se queda fija porque es un cuadro pequeño de 50x50
        tabla.getColumnModel().getColumn(COL_IMG).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(COL_IMG).setMinWidth(70);
        tabla.getColumnModel().getColumn(COL_IMG).setMaxWidth(70);

        // Para el resto de columnas: Les damos a TODAS el mismo tamaño preferido
        // y eliminamos el límite máximo. Así Java distribuye el espacio vacío en partes iguales.
        int anchoEquitativo = 150;

        tabla.getColumnModel().getColumn(COL_CODIGO).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_NOMBRE).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_PRECIO).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_STOCK).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_CAT).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_DESC).setPreferredWidth(anchoEquitativo);
        tabla.getColumnModel().getColumn(COL_VENC).setPreferredWidth(anchoEquitativo);

        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35; hoverBackground:null; pressedBackground:null;" +
                        "separatorColor:$TableHeader.background; font:bold +2; foreground:#9f9f9f");
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:60; selectionBackground:#009991; selectionForeground:#ffffff; showHorizontalLines:true; intercellSpacing:0,1;" +
                        "cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font:+1;");

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(new EmptyBorder(0,0,0,0));
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");
        panel.add(scrollPane, "grow, push");

        add(headerPanel, "wrap, growx");
        add(panel, "grow, push");

        actualizarTabla();

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { actualizarTabla(); }
        });
        btnNew.addActionListener(e    -> mostrarRegistrar());
        btnEdit.addActionListener(e   -> mostrarEditar());
        btnDelete.addActionListener(e -> eliminarSeleccionado());
    }

    private void ocultar(int col) {
        TableColumn tc = tabla.getColumnModel().getColumn(col);
        tc.setMinWidth(0); tc.setMaxWidth(0); tc.setWidth(0);
    }

    // Recarga los productos desde la BD con el filtro y búsqueda actuales.
    private void actualizarTabla() {
        List<Producto> filtrados = pdb.buscar(txtBuscar.getText());
        modelo.setRowCount(0);
        for (Producto p : filtrados) {
            String estadoStock;
            if      (p.getStock() > 10) estadoStock = "Suficiente";
            else if (p.getStock() > 0)  estadoStock = "Poco Stock";
            else                         estadoStock = "Agotado";

            if (!filtroStockActual.equals("Todos") && !filtroStockActual.equals(estadoStock)) continue;

            ImageIcon thumb = null;
            if (p.getImagenPath() != null && new File(p.getImagenPath()).exists()) {
                thumb = new ImageIcon(
                        new ImageIcon(p.getImagenPath()).getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));
            }

            modelo.addRow(new Object[]{
                    p.getIdProducto(),
                    thumb,
                    p.getCodigo(),
                    p.getNombre(),
                    String.format("$%.2f", p.getPrecio()),
                    new StockInfo(p.getStock()),
                    p.getCategoria() != null ? p.getCategoria() : "",
                    p.getDescripcion() != null ? p.getDescripcion() : "",
                    p.getFechaVencimiento() != null ? p.getFechaVencimiento() : ""
            });
        }
    }

    // Abre el popup para registrar un producto nuevo.
    private void mostrarRegistrar() {
        pRegistrarProducto pr = new pRegistrarProducto();
        DefaultOption option = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
        GlassPanePopup.showPopup(new SimplePopupBorder(pr, "Nuevo Producto", new String[]{"Cancelar","Guardar"}, (pc, i) -> {
            if (i == 1) { if (pr.guardarDatos()) { pc.closePopup(); actualizarTabla(); } }
            else pc.closePopup();
        }), option);
    }

    // Abre el popup para editar el producto seleccionado.
    private void mostrarEditar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección vacía","Selecciona el producto a editar."); return; }

        String idStr    = tabla.getValueAt(fila, COL_ID).toString();
        String codigo   = tabla.getValueAt(fila, COL_CODIGO).toString();
        List<Producto>  res = pdb.buscar(codigo);
        if (res.isEmpty()) return;
        Producto prod = res.get(0);

        pEditarProducto pe = new pEditarProducto();
        pe.cargarDatos(idStr, codigo,
                tabla.getValueAt(fila, COL_NOMBRE).toString(),
                tabla.getValueAt(fila, COL_PRECIO).toString().replace("$",""),
                String.valueOf(prod.getStock()),
                prod.getCategoria(),
                prod.getDescripcion(),
                prod.getImagenPath(),
                prod.getFechaVencimiento());

        DefaultOption option = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
        GlassPanePopup.showPopup(new SimplePopupBorder(pe, "Editar Producto", new String[]{"Cancelar","Guardar"}, (pc, i) -> {
            if (i == 1) { if (pe.guardarDatos()) { pc.closePopup(); actualizarTabla(); } }
            else pc.closePopup();
        }), option);
    }

    // Pide confirmación y elimina el producto seleccionado.
    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección vacía","Selecciona el producto a eliminar."); return; }
        int id = Integer.parseInt(tabla.getValueAt(fila, COL_ID).toString());
        if (pdb.tieneVentas(id)) {
            MessageAlerts.getInstance().showMessage("No permitido",
                    "Este producto tiene ventas registradas.\nPon su stock en 0 para desactivarlo."); return;
        }
        MessageAlerts.getInstance().showMessage("Eliminar Producto","¿Eliminar este producto?",
                MessageAlerts.MessageType.ERROR, MessageAlerts.OK_OPTION, (pc, i) -> {
                    if (i == MessageAlerts.OK_OPTION) {
                        if (pdb.eliminar(id)) {
                            Notifications.getInstance().show(Notifications.Type.SUCCESS,
                                    Notifications.Location.BOTTOM_CENTER, "Eliminado correctamente");
                            actualizarTabla();
                        } else MessageAlerts.getInstance().showMessage("Error","No se pudo eliminar.");
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