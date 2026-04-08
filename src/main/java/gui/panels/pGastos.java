package gui.panels;

import Model.Gasto;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import dao.GastosDAO;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


// Panel de gastos y egresos del negocio.
// Los gastos registrados aquí se usan para calcular el balance en el dashboard.

// Panel para registrar y gestionar los gastos del negocio.
// Muestra tarjetas resumen (gasto del día, mes y total) y la tabla de gastos.
public class pGastos extends TabbedForm {

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


    private final GastosDAO dao = new GastosDAO();
    private JTable tabla;
    private DefaultTableModel modelo;
    private JTextField txtBuscar;

    private static final String[] CATEGORIAS = {
        "Arriendo", "Servicios", "Sueldos", "Proveedores",
        "Marketing", "Transporte", "Mantenimiento", "Otros"
    };

    public pGastos() { init(); }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[fill, grow]", "[][grow]"));

        // ---- HEADER ----
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[fill, grow]", ""));

        JLabel lbTitulo = new JLabel("GASTOS Y EGRESOS");
        lbTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +6;");
        lbTitulo.setBorder(new EmptyBorder(0, 10, 4, 0));

        txtBuscar = new JTextField();
        txtBuscar.putClientProperty(FlatClientProperties.STYLE,
            "arc:15; borderWidth:0; focusWidth:0; innerFocusWidth:0; margin:10,20,10,20;");
        txtBuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por descripción o categoría");
        txtBuscar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
            new FlatSVGIcon("icons/search.svg", 0.30f));

        JButton btnNuevo  = crearBoton("",   "#3b64ff", "#ffffff", "icons/plus.svg");
        JButton btnEditar = crearBoton("",  STYLE_EDIT, "icons/pencil.svg");
        JButton btnBorrar = crearBoton("",STYLE_DELETE, "icons/square-rounded-x.svg");

        headerPanel.add(lbTitulo, "wrap, gapbottom 10");
        headerPanel.add(txtBuscar, "split 4, growx, pushx");
        headerPanel.add(btnNuevo,  "align right, gapleft 15, sizegroup btn, width 80:100:120");
        headerPanel.add(btnEditar, "sizegroup btn, width 80:100:120");
        headerPanel.add(btnBorrar, "wrap, sizegroup btn, width 80:100:120");

        // ---- TARJETAS RESUMEN ----
        String hoy = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String mes  = new SimpleDateFormat("yyyy-MM").format(new Date());
        double gastoHoy = dao.totalHoy(hoy);
        double gastoMes = dao.totalMes(mes);

        JPanel tarjetas = new JPanel(new MigLayout("insets 0, gap 15", "[grow, fill][grow, fill]", "[]"));
        tarjetas.putClientProperty(FlatClientProperties.STYLE, "background:null");
        tarjetas.add(crearTarjeta("Gasto Hoy",     String.format("$%.2f", gastoHoy), "#ef4444"));
        tarjetas.add(crearTarjeta("Gasto del Mes", String.format("$%.2f", gastoMes), "#f97316"));

        headerPanel.add(tarjetas, "wrap, growx, gaptop 10");

        // ---- TABLA ----
        JPanel panel = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10", "[fill]", "[push]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:25; background:$Table.background;");
        panel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        modelo.addColumn("ID");
        modelo.addColumn("Descripción");
        modelo.addColumn("Monto");
        modelo.addColumn("Categoría");
        modelo.addColumn("Fecha");
        modelo.addColumn("Notas");

        tabla = new JTable(modelo);
        tabla.setFocusable(false);

        // 1. Ocultar la columna ID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(0);

        // 2. ── Renderizador Genérico para Datos (Igual que en Productos) ──
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    // Alineamos Descripción(1) y Notas(5) a la izquierda, el resto al centro
                    if (col == 1 || col == 5) {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    // Margen de 15px para separar las columnas visualmente
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                }
                return c;
            }
        });

        // 3. ── Renderizador del Header (Igual que en Productos) ──
        tabla.getTableHeader().setDefaultRenderer((t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            lbl.setForeground(Color.decode("#9f9f9f"));
            lbl.setOpaque(true);
            lbl.setBackground(t.getTableHeader().getBackground());

            if (col == 1 || col == 5) {
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
            }
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
            return lbl;
        });

        // 4. ── Anchos de Columna Equitativos ──
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int anchoEquitativo = 150;
        for (int i = 1; i <= 5; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchoEquitativo);
        }

        // 5. ── Estilos FlatLaf ──
        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35; hoverBackground:null; pressedBackground:null; separatorColor:$TableHeader.background; font:bold +2; foreground:#9f9f9f");
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50; showHorizontalLines:true; intercellSpacing:0,1; cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font:+1;");

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");
        panel.add(scroll, "grow, push");


        add(headerPanel, "wrap, growx");
        add(panel, "grow, push");

        actualizarTabla();

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { actualizarTabla(); }
        });
        btnNuevo.addActionListener(e -> mostrarFormulario(null));
        btnEditar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección vacía", "Selecciona un gasto para editar."); return; }
            Gasto g = gastoDesdeTabla(fila);
            mostrarFormulario(g);
        });
        btnBorrar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) { MessageAlerts.getInstance().showMessage("Selección vacía", "Selecciona un gasto para eliminar."); return; }
            int id = Integer.parseInt(tabla.getValueAt(fila, 0).toString());
            MessageAlerts.getInstance().showMessage("Eliminar Gasto", "¿Eliminar este gasto permanentemente?",
                MessageAlerts.MessageType.ERROR, MessageAlerts.OK_OPTION, (pc, i) -> {
                    if (i == MessageAlerts.OK_OPTION) {
                        if (dao.eliminar(id)) {
                            Notifications.getInstance().show(Notifications.Type.SUCCESS, Notifications.Location.BOTTOM_CENTER, "Gasto eliminado");
                            actualizarTabla();
                        }
                    }
                });
        });
    }

    // Abre el popup de registro/edición. Si gasto es null, es un registro nuevo.
    private void mostrarFormulario(Gasto gasto) {
        boolean esEdicion = gasto != null;
        JPanel form = new JPanel(new MigLayout("wrap, fillx, insets 20", "[grow, fill]", "[]8[]"));

        JTextField txtDesc = crearCampo("Ej: Pago de arriendo");
        JTextField txtMonto = crearCampo("Ej: 250.00");
        JComboBox<String> cbCat = new JComboBox<>(CATEGORIAS);
        cbCat.putClientProperty(FlatClientProperties.STYLE, "arc:15;");
        JTextField txtFecha = crearCampo(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        JTextField txtNotas = crearCampo("Opcional");

        if (esEdicion) {
            txtDesc.setText(gasto.getDescripcion());
            txtMonto.setText(String.valueOf(gasto.getMonto()));
            for (int i = 0; i < CATEGORIAS.length; i++) {
                if (CATEGORIAS[i].equalsIgnoreCase(gasto.getCategoria())) { cbCat.setSelectedIndex(i); break; }
            }
            txtFecha.setText(gasto.getFecha());
            txtNotas.setText(gasto.getNotas() != null ? gasto.getNotas() : "");
        }

        form.add(crearLabel("Descripción: *")); form.add(txtDesc);
        form.add(crearLabel("Monto ($): *"));   form.add(txtMonto);
        form.add(crearLabel("Categoría:"));     form.add(cbCat);
        form.add(crearLabel("Fecha:"));         form.add(txtFecha);
        form.add(crearLabel("Notas:"));         form.add(txtNotas);

        DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
        String titulo = esEdicion ? "Editar Gasto" : "Registrar Gasto";
        GlassPanePopup.showPopup(new SimplePopupBorder(form, titulo, new String[]{"Cancelar", "Guardar"}, (pc, i) -> {
            if (i == 1) {
                String desc = txtDesc.getText().trim();
                String montoStr = txtMonto.getText().trim().replace(",", ".");
                if (desc.isEmpty() || montoStr.isEmpty()) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Campos obligatorios", "La descripción y el monto son obligatorios."); return;
                }
                try {
                    double monto = Double.parseDouble(montoStr);
                    if (monto <= 0) { raven.alerts.MessageAlerts.getInstance().showMessage("Monto inválido", "El monto debe ser mayor a 0."); return; }

                    Gasto g = esEdicion ? gasto : new Gasto();
                    g.setDescripcion(desc);
                    g.setMonto(monto);
                    g.setCategoria(cbCat.getSelectedItem().toString());
                    g.setFecha(txtFecha.getText().trim().isEmpty()
                        ? new SimpleDateFormat("yyyy-MM-dd").format(new Date()) : txtFecha.getText().trim());
                    g.setNotas(txtNotas.getText().trim());

                    boolean ok = esEdicion ? dao.editar(g) : dao.registrar(g);
                    if (ok) {
                        Notifications.getInstance().show(Notifications.Type.SUCCESS,
                            Notifications.Location.BOTTOM_CENTER,
                            esEdicion ? "Gasto actualizado" : "Gasto registrado");
                        pc.closePopup();
                        // invokeLater para que corra después de que el popup termine de cerrarse
                        javax.swing.SwingUtilities.invokeLater(this::actualizarTabla);
                    }
                } catch (NumberFormatException ex) {
                    raven.alerts.MessageAlerts.getInstance().showMessage("Formato incorrecto", "El monto debe ser un número válido. Ej: 150.00");
                }
            } else pc.closePopup();
        }), opt);
    }

    // Recarga los gastos desde la BD con el texto de búsqueda actual.
    private void actualizarTabla() {
        List<Gasto> lista = dao.buscar(txtBuscar.getText(), null);
        modelo.setRowCount(0);
        for (Gasto g : lista) {
            modelo.addRow(new Object[]{
                g.getIdGasto(), g.getDescripcion(),
                String.format("$%.2f", g.getMonto()),
                g.getCategoria(), g.getFecha(),
                g.getNotas() != null ? g.getNotas() : ""
            });
        }
    }

    private Gasto gastoDesdeTabla(int fila) {
        Gasto g = new Gasto();
        g.setIdGasto(Integer.parseInt(tabla.getValueAt(fila, 0).toString()));
        g.setDescripcion(tabla.getValueAt(fila, 1).toString());
        g.setMonto(Double.parseDouble(tabla.getValueAt(fila, 2).toString().replace("$", "")));
        g.setCategoria(tabla.getValueAt(fila, 3) != null ? tabla.getValueAt(fila, 3).toString() : "");
        g.setFecha(tabla.getValueAt(fila, 4).toString());
        g.setNotas(tabla.getValueAt(fila, 5).toString());
        return g;
    }

    // ---- helpers ----
    private JPanel crearTarjeta(String titulo, String valor, String color) {
        JPanel c = new JPanel(new MigLayout("fill, insets 15", "[grow][]", "[][]"));
        c.putClientProperty(FlatClientProperties.STYLE,
            "arc:20; [light]background:darken(@background,4%); [dark]background:lighten(@background,4%)");
        JLabel lbT = new JLabel(titulo);
        lbT.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:bold;");
        JLabel lbV = new JLabel(valor);
        lbV.putClientProperty(FlatClientProperties.STYLE, "font:bold +8; foreground:" + color);
        FlatSVGIcon icon = new FlatSVGIcon("icons/dollar.svg", 0.7f);
        icon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(cl -> Color.decode(color)));
        c.add(lbT, "span 2, wrap");
        c.add(lbV);
        c.add(new JLabel(icon), "align right");
        return c;
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
    private JTextField crearCampo(String placeholder) {
        JTextField tf = new JTextField();
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0; margin:8,10,8,10;");
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return tf;
    }
    private JLabel crearLabel(String texto) {
        JLabel lb = new JLabel(texto);
        lb.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");
        return lb;
    }
}
