package gui.panels;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import Model.Cliente;
import Model.DetalleFactura;
import Model.Factura;
import Model.Notificacion;
import Model.Producto;
import dao.ClientesDAO;
import dao.FacturaDAO;
import dao.ProductoDAO;
import services.GeneradorPDF;

import net.miginfocom.swing.MigLayout;
import raven.alerts.MessageAlerts;
import raven.modal.component.DropShadowBorder;
import raven.popup.DefaultOption;
import raven.popup.GlassPanePopup;
import raven.popup.component.SimplePopupBorder;
import utilities.TabbedForm;

// Panel principal para crear ventas/facturas.
// El usuario agrega productos, asigna un cliente, ingresa el pago y genera la venta.
// Al guardar crea el PDF y envía el correo automáticamente si hay email configurado.
public class pVenta extends TabbedForm {

    private JLabel lbCedulaCliente = null;
    private JLabel cedulaClienteValor = null;
    private JPanel cifrasPanel;
    private DefaultTableModel modelo;
    private JTable tabla;

    private JLabel serie, subtotal, descuento, total, pagado, cambio, deuda, efectivo, transferencia;
    private Cliente clienteActual = null;

    public pVenta() {
        init();
    }


    private FlatSVGIcon crearIcono(String path, float scale, String hexColor) {
        FlatSVGIcon icon = new FlatSVGIcon(path, scale);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.decode(hexColor)));
        return icon;
    }

    void init() {
        JPanel contenedor = new JPanel(new MigLayout("wrap 2, fill, insets 10", "[grow, fill]10[grow, fill]", "[grow, fill]"));
        JPanel contenedorHeaderTabla = new JPanel(new MigLayout("wrap, fill, insets 0", "[grow, fill]", "[][grow, fill, 150::]"));
        JPanel header = new JPanel(new MigLayout("insets 10, fillx", "[grow, fill]10[grow, fill]10[][][]", "[]"));
        JPanel ptabla = new JPanel(new MigLayout("wrap, fillx, insets 20 10 10 10", "[fill]", "[push]"));
        JPanel facturaPanel = new JPanel(new MigLayout("wrap, fillx, insets 20", "[fill]", "[push]"));

        header.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; [light]background:darken(@background,6%); [dark]background:lighten(@background,3%)");


        // --- BOTONES SUPERIORES ---
        JButton btnCatProductos = new JButton("Productos");
        btnCatProductos.setIcon(crearIcono("icons/box.svg", 0.30f, "#ffffff"));
        btnCatProductos.putClientProperty(FlatClientProperties.STYLE,
                "arc: 15; background: #86c5de; foreground: #ffffff; font: bold; margin: 10,20,10,20; hoverBackground: #2563eb; borderWidth:0; focusWidth:0; innerFocusWidth:0");

        JButton btnCatClientes = new JButton("Clientes");
        btnCatClientes.setIcon(crearIcono("icons/clientes.svg", 0.30f, "#ffffff"));
        btnCatClientes.putClientProperty(FlatClientProperties.STYLE,
                "arc: 15; background: #86c5de; foreground: #ffffff; font: bold; margin: 10,20,10,20; hoverBackground: #2563eb; borderWidth:0; focusWidth:0; innerFocusWidth:0");

        JButton btnEliminar = new JButton("Eliminar fila");
        btnEliminar.setIcon(crearIcono("icons/filaremove.svg", 0.30f, "#ffffff"));
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground: #ffffff; background:#7e5aff;");

        JButton btnEditar = new JButton("Editar fila");
        btnEditar.setIcon(crearIcono("icons/pencil.svg", 0.30f, "#ffffff"));
        btnEditar.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground: #ffffff; background: #ffda49;");

        JButton btnCancelarVenta = new JButton("Cancelar Venta");
        btnCancelarVenta.setIcon(crearIcono("icons/cancelarventa.svg", 0.30f, "#ffffff"));
        btnCancelarVenta.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground: #ffffff; background: #ff0000;");

        // --- TABLA ---
        ptabla.putClientProperty(FlatClientProperties.STYLE, "arc: 25; background:$Table.background;");

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4; // Solo Cantidad y Descuento %
            }
        };
        modelo.addColumn("Código");
        modelo.addColumn("Nombre");
        modelo.addColumn("Precio");
        modelo.addColumn("Cantidad");
        modelo.addColumn("Desc. %");
        modelo.addColumn("Subtotal");
        modelo.addColumn("Total");

        tabla = new JTable(modelo);
        tabla.setFocusable(false);
        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        // --- RENDERIZADOR DE CELDAS (Centrado y Espaciado) ---
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (c instanceof JLabel lbl) {
                    if (col == 1) { // El Nombre alineado a la izquierda por ser texto largo
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    } else { // El resto centrado
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
                }
                return c;
            }
        });

        // --- RENDERIZADOR DE ENCABEZADOS (Centrado y Espaciado) ---
        tabla.getTableHeader().setDefaultRenderer((t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val != null ? val.toString() : "");
            lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD));
            lbl.setForeground(java.awt.Color.decode("#9f9f9f"));
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

        // --- ANCHOS DE COLUMNA EQUITATIVOS ---
        int anchoEquitativo = 150;

        tabla.getColumnModel().getColumn(0).setPreferredWidth(anchoEquitativo);   // Código
        tabla.getColumnModel().getColumn(1).setPreferredWidth(300);               // Nombre (Doble espacio)
        tabla.getColumnModel().getColumn(2).setPreferredWidth(anchoEquitativo);   // Precio
        tabla.getColumnModel().getColumn(3).setPreferredWidth(anchoEquitativo);   // Cantidad
        tabla.getColumnModel().getColumn(4).setPreferredWidth(anchoEquitativo);   // Desc. %
        tabla.getColumnModel().getColumn(5).setPreferredWidth(anchoEquitativo);   // Subtotal
        tabla.getColumnModel().getColumn(6).setPreferredWidth(anchoEquitativo);   // Total

        tabla.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:35; hoverBackground:null; pressedBackground:null;" +
                        "separatorColor:$TableHeader.background; font:bold +2; foreground: #9f9f9f");
        tabla.putClientProperty(FlatClientProperties.STYLE,
                "rowHeight:50; selectionBackground:#009991; selectionForeground:#ffffff; showHorizontalLines:true; intercellSpacing:0,1;" +
                        "cellFocusColor:$TableHeader.hoverBackground; selectionBackground:$TableHeader.hoverBackground; font: +1;");
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
                "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; background:$Table.background;");

        // Listener de stock en vivo
        tabla.addPropertyChangeListener(evt -> {
            if ("tableCellEditor".equals(evt.getPropertyName()) && !tabla.isEditing()) {
                int row = tabla.getSelectedRow();
                int col = tabla.getSelectedColumn();

                if (row >= 0 && col == 3) {
                    try {
                        String cod = modelo.getValueAt(row, 0).toString();
                        List<Producto> resultados = new ProductoDAO().buscar(cod);
                        if (resultados.isEmpty()) { modelo.setValueAt(1, row, 3); return; }
                        int stockReal = resultados.get(0).getStock();
                        int qty = Integer.parseInt(modelo.getValueAt(row, 3).toString());

                        if (qty > stockReal) {
                            MessageAlerts.getInstance().showMessage("Stock insuficiente", "Solo tienes " + stockReal + " unidades disponibles de ese producto.");
                            modelo.setValueAt(stockReal, row, 3);
                        } else if (qty <= 0) {
                            modelo.setValueAt(1, row, 3);
                        }
                    } catch (Exception ex) {
                        modelo.setValueAt(1, row, 3);
                    }
                }
                recalcularCifras();
            }
        });

        // --- PANEL DE FACTURA ---
        facturaPanel.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; [light]background:darken(@background,6%); [dark]background:lighten(@background,3%)");
        facturaPanel.setBorder(new DropShadowBorder(new Insets(5, 5, 10, 5), 15));

        cifrasPanel = new JPanel(new MigLayout("insets 0", "[grow, fill]", "[]"));
        cifrasPanel.putClientProperty(FlatClientProperties.STYLE, "background:null");

        serie = new JLabel(new FacturaDAO().obtenerNuevaSerie());
        serie.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");

        JLabel lbFactura = new JLabel("Nº Factura");
        lbFactura.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #ff3723;");
        cifrasPanel.add(lbFactura, "split 2, gapright 150, gapbottom 10");
        cifrasPanel.add(serie, "align right, wrap");

        JLabel lbsubtotal = new JLabel("Subtotal");
        subtotal = new JLabel("$0.00");
        subtotal.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");
        cifrasPanel.add(lbsubtotal, "split 2, gapright 173, gapbottom 5, gaptop 12");
        cifrasPanel.add(subtotal, "align right, wrap");

        cifrasPanel.add(new JSeparator(), "span, growx, wrap");

        JLabel lbDescuento = new JLabel("Descuento");
        descuento = new JLabel("$0.00");
        descuento.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
        cifrasPanel.add(lbDescuento, "split 2, gapright 154, gapbottom 5, gaptop 5");
        cifrasPanel.add(descuento, "align right, gapleft 0, wrap");

        cifrasPanel.add(new JSeparator(), "span, growx, wrap");

        JLabel lbtotal = new JLabel("TOTAL");
        lbtotal.putClientProperty(FlatClientProperties.STYLE, "font: bold +3; ");
        total = new JLabel("$0.00");
        total.putClientProperty(FlatClientProperties.STYLE, "font: bold +3;");
        cifrasPanel.add(lbtotal, "split 2, gapright 180, gaptop 5");
        cifrasPanel.add(total, "align right, wrap");

        facturaPanel.add(cifrasPanel, "growx, wrap");

        // --- MÉTODOS DE PAGO ---
        JPanel metodoPago = new JPanel(new MigLayout("insets 0", "[grow, fill]", "[]"));
        metodoPago.putClientProperty(FlatClientProperties.STYLE, "background:null");

        JLabel lbpagado = new JLabel("PAGADO");
        pagado = new JLabel("$0.00");
        pagado.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
        metodoPago.add(lbpagado, "split 2, gaptop 15, gapright 170");
        metodoPago.add(pagado, "align right, wrap");

        JLabel lbCambio = new JLabel("CAMBIO");
        cambio = new JLabel("$0.00");
        cambio.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
        metodoPago.add(lbCambio, "split 2, gaptop 15, gapright 170");
        metodoPago.add(cambio, "align right, wrap");

        JLabel lbDeuda = new JLabel("DEUDA");
        deuda = new JLabel("$0.00");
        deuda.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #ff3723;");
        metodoPago.add(lbDeuda, "split 2, gaptop 15, gapright 180");
        metodoPago.add(deuda, "align right, wrap");

        metodoPago.add(new JSeparator(), "span, growx, wrap");
        metodoPago.add(new JLabel("AÑADIR PAGO"), "wrap, gaptop 15");

        JButton btnEfectivo = new JButton("EFECTIVO");
        btnEfectivo.setIcon(crearIcono("icons/cash.svg", 0.50f, "#ffffff"));
        btnEfectivo.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; background:#00e39b; foreground: #ffffff; font: bold;" +
                        "margin:8,30,8,30; hoverBackground: #007d77; borderWidth:0; focusWidth:0; innerFocusWidth:0");
        efectivo = new JLabel("$0.00");
        efectivo.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
        metodoPago.add(btnEfectivo, "split 2, growx, pushx, gaptop 8");
        metodoPago.add(efectivo, "wrap");

        JButton btnTransferencia = new JButton("TRANSFERENCIA");
        btnTransferencia.setIcon(crearIcono("icons/celular.svg", 0.40f, "#ffffff"));
        btnTransferencia.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; background: #00e39b; foreground: #ffffff; font: bold;" +
                        "margin:8,30,8,30; hoverBackground: #007d77; borderWidth:0; focusWidth:0; innerFocusWidth:0");
        transferencia = new JLabel("$0.00");
        transferencia.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
        metodoPago.add(btnTransferencia, "split 2, growx, pushx, gaptop 14");
        metodoPago.add(transferencia, "wrap");

        facturaPanel.add(metodoPago, "growx, wrap");

        JButton btnGenerarVenta = new JButton("Generar Venta");
        btnGenerarVenta.setIcon(crearIcono("icons/caja.svg", 0.40f, "#ffffff"));
        btnGenerarVenta.putClientProperty(FlatClientProperties.STYLE,
                "arc: 20; background: #009991; foreground: #ffffff; font: bold;" +
                        "margin:12,30,12,30; hoverBackground: #007d77; borderWidth:0; focusWidth:0; innerFocusWidth:0");
        metodoPago.add(btnGenerarVenta, "growx, wrap, gaptop 20");

        // --- ACCIONES DE BOTONES ---
        btnCatProductos.addActionListener(e -> {
            gui.popus.pSeleccionarProducto pSel = new gui.popus.pSeleccionarProducto();
            DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };

            GlassPanePopup.showPopup(new SimplePopupBorder(pSel, "Catálogo de Productos", new String[]{"Cancelar", "Agregar Marcados"}, (pc, i) -> {
                if (i == 1) {
                    boolean alertaStock = false;
                    for (Producto pr : pSel.getProductosSeleccionados()) {
                        int stockReal = pr.getStock();
                        if (stockReal > 0) {
                            int filaExistente = -1;
                            for (int f = 0; f < modelo.getRowCount(); f++) {
                                if (modelo.getValueAt(f, 0).toString().equals(pr.getCodigo())) {
                                    filaExistente = f; break;
                                }
                            }
                            if (filaExistente != -1) {
                                int cantActual = Integer.parseInt(modelo.getValueAt(filaExistente, 3).toString());
                                if (cantActual + 1 <= stockReal) {
                                    modelo.setValueAt(cantActual + 1, filaExistente, 3);
                                } else {
                                    alertaStock = true;
                                }
                            } else {
                                modelo.addRow(new Object[]{pr.getCodigo(), pr.getNombre(), formatearDinero(pr.getPrecio()), 1, 0, "$0.00", "$0.00"});
                            }
                        }
                    }
                    recalcularCifras();
                    pc.closePopup();
                    if (alertaStock) MessageAlerts.getInstance().showMessage("Límite de stock", "Algunos productos no se pudieron agregar porque alcanzaron su límite de stock.");
                } else { pc.closePopup(); }
            }), opt);
        });

        btnCatClientes.addActionListener(e -> {
            gui.popus.pSeleccionarCliente pSelCli = new gui.popus.pSeleccionarCliente();
            DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };

            pSelCli.getBtnNuevo().addActionListener(ev -> {
                GlassPanePopup.closePopupLast();
                SwingUtilities.invokeLater(() -> abrirPopupRegistroRapidoCliente());
            });

            GlassPanePopup.showPopup(new SimplePopupBorder(pSelCli, "Seleccionar Cliente", new String[]{"Cancelar", "Seleccionar"}, (pc, i) -> {
                if (i == 1) {
                    Cliente c = pSelCli.getClienteSeleccionado();
                    if (c != null) {
                        clienteActual = c;
                        if (lbCedulaCliente == null) {
                            lbCedulaCliente = new JLabel("Cliente:");
                            lbCedulaCliente.putClientProperty(FlatClientProperties.STYLE, "font: +2;");
                            cedulaClienteValor = new JLabel("");
                            cedulaClienteValor.putClientProperty(FlatClientProperties.STYLE, "font: bold +1;");
                            cifrasPanel.add(lbCedulaCliente, "split 2, gapright 0, gaptop 10");
                            cifrasPanel.add(cedulaClienteValor, "align right, gapleft 110, wrap");
                        }
                        cedulaClienteValor.setText(clienteActual.getNombre().toUpperCase());
                        cifrasPanel.revalidate();
                        pc.closePopup();
                    } else { MessageAlerts.getInstance().showMessage("Sin cliente", "Debes seleccionar un cliente de la lista primero."); }
                } else { pc.closePopup(); }
            }), opt);
        });

        btnEliminar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                modelo.removeRow(fila);
                recalcularCifras();
            } else { MessageAlerts.getInstance().showMessage("Selección", "Selecciona una fila para eliminar."); }
        });

        btnEditar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0) return;
            JPanel pCantidad = new JPanel(new net.miginfocom.swing.MigLayout("wrap, fillx, insets 15", "[grow, fill]"));
            JTextField txtCant = new JTextField();
            txtCant.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:10; margin:8,10,8,10;");
            txtCant.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 3");
            pCantidad.add(new JLabel("Nueva cantidad:"));
            pCantidad.add(txtCant, "growx");
            DefaultOption optCant = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
            GlassPanePopup.showPopup(new raven.popup.component.SimplePopupBorder(pCantidad, "Editar Cantidad", new String[]{"Cancelar", "Aplicar"}, (pc2, i2) -> {
                if (i2 == 1) {
                    try {
                        int cant = Integer.parseInt(txtCant.getText().trim());
                        String cod = modelo.getValueAt(fila, 0).toString();
                        List<Producto> resultados = new ProductoDAO().buscar(cod);
                        if (!resultados.isEmpty()) {
                            int stockReal = resultados.get(0).getStock();
                            if (cant > stockReal) {
                                MessageAlerts.getInstance().showMessage("Stock insuficiente", "La cantidad máxima disponible es " + stockReal + " unidades.");
                                cant = stockReal;
                            }
                        }
                        if (cant <= 0) cant = 1;
                        modelo.setValueAt(cant, fila, 3);
                        recalcularCifras();
                    } catch (Exception ex) {
                        MessageAlerts.getInstance().showMessage("Valor inválido", "Ingresa un número entero válido.");
                    }
                }
                pc2.closePopup();
            }), optCant);
        });

        btnCancelarVenta.addActionListener(e -> {
            // Solo pedir confirmación si hay productos en la tabla
            if (modelo.getRowCount() == 0) {
                limpiarVenta();
                return;
            }
            raven.alerts.MessageAlerts.getInstance().showMessage(
                "¿Cancelar esta venta?",
                "Se perderán todos los productos y el cliente seleccionado. ¿Estás seguro?",
                raven.alerts.MessageAlerts.MessageType.WARNING,
                raven.alerts.MessageAlerts.OK_CANCEL_OPTION,
                (pc, i) -> {
                    if (i == raven.alerts.MessageAlerts.OK_OPTION) limpiarVenta();
                }
            );
        });

        btnEfectivo.addActionListener(e -> {
            gui.popus.metodoPago mp = new gui.popus.metodoPago();
            DefaultOption option = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
            GlassPanePopup.showPopup(new SimplePopupBorder(mp, "Pago en Efectivo", new String[]{"Cancelar", "Agregar"}, (pc, i) -> {
                if (i == 1) {
                    try {
                        double m = Double.parseDouble(mp.getPago().getText().replace(",", "."));
                        efectivo.setText(formatearDinero(m));
                        recalcularCifras();
                    } catch (Exception ex) {}
                    pc.closePopup();
                } else { pc.closePopup(); }
            }), option);
        });

        btnTransferencia.addActionListener(e -> {
            gui.popus.metodoPago mp = new gui.popus.metodoPago();
            mp.getTitulo().setText("Transferencia");
            DefaultOption option = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return true; } };
            GlassPanePopup.showPopup(new SimplePopupBorder(mp, "Pago por Transferencia", new String[]{"Cancelar", "Agregar"}, (pc, i) -> {
                if (i == 1) {
                    try {
                        double m = Double.parseDouble(mp.getPago().getText().replace(",", "."));
                        transferencia.setText(formatearDinero(m));
                        recalcularCifras();
                    } catch (Exception ex) {}
                    pc.closePopup();
                } else { pc.closePopup(); }
            }), option);
        });

        btnGenerarVenta.addActionListener(e -> procesarVenta());

        // --- ENSAMBLADO ---
        header.add(btnCatProductos, "growx");
        header.add(btnCatClientes, "growx");
        header.add(btnEliminar, "sizegroup btn");
        header.add(btnEditar, "sizegroup btn");
        header.add(btnCancelarVenta, "sizegroup btn");

        ptabla.add(scrollPane, "grow, push");

        contenedorHeaderTabla.setLayout(new MigLayout("wrap, fill, insets 0", "[grow, fill]", "[][grow, fill, 150::]"));
        contenedorHeaderTabla.add(header, "growx");
        contenedorHeaderTabla.add(ptabla, "grow, pushy, gaptop 10");

        contenedor.setLayout(new MigLayout("wrap 2, fill, insets 0", "[grow 70, fill, 500::]20[grow 30, fill, 320::400]", "[grow, fill, 450::]"));
        contenedor.add(contenedorHeaderTabla, "grow, push");
        contenedor.add(facturaPanel, "growy");

        setLayout(new MigLayout("fill, insets 20"));
        add(contenedor, "grow, push");
    }

    private void recalcularCifras() {
        double dSubtotal = 0, dTotal = 0, dDescTotal = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            try {
                double precioUnit = parseDinero(modelo.getValueAt(i, 2).toString());
                int cantidad = Integer.parseInt(modelo.getValueAt(i, 3).toString());
                double porcDesc = Double.parseDouble(modelo.getValueAt(i, 4).toString().replace("%", ""));

                if (porcDesc > 100) porcDesc = 100;

                double subFila = precioUnit * cantidad;
                double descFila = subFila * (porcDesc / 100);
                double totFila = subFila - descFila;

                modelo.setValueAt(formatearDinero(subFila), i, 5);
                modelo.setValueAt(formatearDinero(totFila), i, 6);

                dSubtotal += subFila;
                dDescTotal += descFila;
                dTotal += totFila;
            } catch (Exception e) {}
        }
        subtotal.setText(formatearDinero(dSubtotal));
        descuento.setText(formatearDinero(dDescTotal));
        total.setText(formatearDinero(dTotal));

        double dEfectivo = parseDinero(efectivo.getText());
        double dTransf = parseDinero(transferencia.getText());
        double dPagado = dEfectivo + dTransf;
        pagado.setText(formatearDinero(dPagado));

        if (dPagado >= dTotal) {
            cambio.setText(formatearDinero(dPagado - dTotal));
            deuda.setText("$0.00");
        } else {
            cambio.setText("$0.00");
            deuda.setText(formatearDinero(dTotal - dPagado));
        }
    }

    // Valida la venta antes de guardar:
    // - Que haya productos en la tabla.
    // - Que Consumidor Final pague completo (no puede quedar con deuda).
    // - Confirma si el pago es parcial y hay un cliente asignado.
    private void procesarVenta() {
        if (modelo.getRowCount() == 0) {
            MessageAlerts.getInstance().showMessage("Sin productos", "Agrega al menos un producto antes de generar la venta.");
            return;
        }
        double dTotal  = parseDinero(total.getText());
        double dPagado = parseDinero(pagado.getText());

        if (clienteActual == null && dPagado < dTotal) {
            MessageAlerts.getInstance().showMessage("Pago incompleto",
                    "Las ventas a Consumidor Final deben pagarse en su totalidad.\n" +
                            "Asigna un cliente con nombre para registrar deudas, o completa el pago.");
            return;
        }

        if (dTotal > 0 && dPagado < dTotal) {
            MessageAlerts.getInstance().showMessage("¿Registrar como deuda?",
                    "El pago es menor al total. ¿Quieres registrar la diferencia como DEUDA del cliente?",
                    MessageAlerts.MessageType.WARNING, MessageAlerts.OK_CANCEL_OPTION, (pc2, i2) -> {
                        if (i2 == MessageAlerts.OK_OPTION) ejecutarGuardadoVenta();
                    });
        } else {
            ejecutarGuardadoVenta();
        }
    }

    // Construye la factura, la guarda en la BD, genera el PDF y envía el correo.
    // Todo lo pesado corre en un SwingWorker para no congelar la UI.
    private void ejecutarGuardadoVenta() {
        double dTotal  = parseDinero(total.getText());
        double dPagado = parseDinero(pagado.getText());
        Factura f = new Factura();
        f.setNoSerie(serie.getText());
        f.setIdCliente(clienteActual != null ? clienteActual.getIdCliente() : 0);
        f.setFechaVenta(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        f.setMontoTotal(dTotal);
        f.setPago(dPagado);
        f.setDeuda(parseDinero(deuda.getText()));
        f.setEstado(f.getDeuda() > 0 ? "Por Pagar" : "Pagado");

        // Fecha de vencimiento SOLO si hay deuda — una venta pagada completa no vence
        String fechaVenc = null;
        if (f.getDeuda() > 0) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, 10);
            fechaVenc = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        }
        f.setFechaVencimiento(fechaVenc);

        String metodoStr = (parseDinero(efectivo.getText()) > 0 && parseDinero(transferencia.getText()) > 0) ? "Mixto" :
                (parseDinero(transferencia.getText()) > 0 ? "Transferencia" : "Efectivo");
        f.setMetodoPago(metodoStr);

        List<DetalleFactura> detalles = new ArrayList<>();
        ArrayList<String[]> listaPdf = new ArrayList<>();
        ProductoDAO pDao = new ProductoDAO();

        for (int i = 0; i < modelo.getRowCount(); i++) {
            String cod = modelo.getValueAt(i, 0).toString();
            int cant = Integer.parseInt(modelo.getValueAt(i, 3).toString());
            double precio = parseDinero(modelo.getValueAt(i, 2).toString());
            double porc = Double.parseDouble(modelo.getValueAt(i, 4).toString().replace("%", ""));
            double descDol = (precio * cant) * (porc / 100);

            List<Producto> resultadosBusqueda = pDao.buscar(cod);
            if (resultadosBusqueda.isEmpty()) {
                MessageAlerts.getInstance().showMessage("Producto no encontrado", "No se encontró el código '" + cod + "' en la base de datos.");
                return;
            }
            Producto prodDB = resultadosBusqueda.get(0);
            detalles.add(new DetalleFactura(prodDB.getIdProducto(), cant, precio));

            listaPdf.add(new String[]{String.valueOf(cant), cod, modelo.getValueAt(i, 1).toString(),
                    modelo.getValueAt(i, 2).toString().replace("$",""), String.format("%.2f", descDol),
                    modelo.getValueAt(i, 5).toString().replace("$",""), modelo.getValueAt(i, 6).toString().replace("$","")});
        }
        f.setDetalles(detalles);

        final Cliente cliPdf = clienteActual;
        final String subPdf = subtotal.getText(), descPdf = descuento.getText();
        final String pagPdf = pagado.getText(), camPdf = cambio.getText(), deuPdf = deuda.getText();
        final String efPdf = efectivo.getText(), trPdf = transferencia.getText();
        final String numFac = f.getNoSerie();

        raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.INFO, raven.toast.Notifications.Location.BOTTOM_CENTER, "Procesando venta...");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                if (!new FacturaDAO().guardarFacturaCompleta(f)) return false;

                java.io.File pdfFile = new GeneradorPDF().generarFacturaPdf(numFac,
                        cliPdf != null ? cliPdf.getCedula() : "9999999999", cliPdf != null ? cliPdf.getNombre() : "Consumidor Final",
                        cliPdf != null ? cliPdf.getCorreo() : "", cliPdf != null ? cliPdf.getTelefono() : "", cliPdf != null ? cliPdf.getDireccion() : "",
                        listaPdf, parseDinero(subPdf), parseDinero(descPdf), f.getMontoTotal(), parseDinero(pagPdf), parseDinero(camPdf),
                        f.getDeuda(), f.getMetodoPago(), f.getEstado(), parseDinero(efPdf), parseDinero(trPdf), f.getFechaVencimiento());

                if (cliPdf != null && cliPdf.getCorreo() != null && !cliPdf.getCorreo().isEmpty() && pdfFile != null) {
                    String emailRemitente = utilities.ConfigManager.get("email.remitente", "");
                    String emailClave    = utilities.ConfigManager.getEmailClaveSmtp();
                    if (!emailRemitente.isEmpty() && !emailClave.isEmpty()) {
                        String negNombre = utilities.ConfigManager.get("negocio.nombre", "Tu Negocio");
                        services.Email email = new services.Email(emailRemitente, emailClave);
                        email.enviarEmailConAdjunto(
                                cliPdf.getCorreo(),
                                "Factura #" + numFac + " de " + negNombre,
                                "Estimado(a) " + cliPdf.getNombre() + ",\n\nAdjunto encontrarás tu factura #" + numFac + ".\n\nGracias por su compra.\n\n" + negNombre,
                                pdfFile);
                    }
                }
                return true;
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        utilities.NotificacionManager.getInstance().agregar(new Model.Notificacion(
                                "✅ Venta Completada",
                                "Factura #" + numFac + " registrada por " +
                                        (cliPdf != null ? cliPdf.getNombre() : "Consumidor Final"),
                                Model.Notificacion.Tipo.VENTA
                        ));
                        MessageAlerts.getInstance().showMessage("Éxito", "Venta registrada con éxito.");
                        limpiarVenta();
                    } else {
                        MessageAlerts.getInstance().showMessage("Error al guardar", "No se pudo guardar la venta. Intenta nuevamente.");
                    }
                } catch (java.util.concurrent.ExecutionException ex) {
                    MessageAlerts.getInstance().showMessage("Error inesperado", "Ocurrió un error al procesar la venta: " + ex.getCause().getMessage());
                } catch (Exception ex) {
                    MessageAlerts.getInstance().showMessage("Error inesperado", ex.getMessage());
                }
            }
        }.execute();
    }

    private void abrirPopupRegistroRapidoCliente() {
        JPanel pNuevo = new JPanel(new MigLayout("wrap 2, fillx, insets 15", "[][grow, fill]", "[]10[]"));
        JTextField txtCedula = new JTextField(); txtCedula.putClientProperty("FlatLaf.style", "arc:10; margin:5,10,5,10");
        JTextField txtNombre = new JTextField(); txtNombre.putClientProperty("FlatLaf.style", "arc:10; margin:5,10,5,10");
        JTextField txtCorreo = new JTextField(); txtCorreo.putClientProperty("FlatLaf.style", "arc:10; margin:5,10,5,10");
        JTextField txtTel = new JTextField(); txtTel.putClientProperty("FlatLaf.style", "arc:10; margin:5,10,5,10");
        JTextField txtDir = new JTextField(); txtDir.putClientProperty("FlatLaf.style", "arc:10; margin:5,10,5,10");

        pNuevo.add(new JLabel("Cédula/RUC: *")); pNuevo.add(txtCedula);
        pNuevo.add(new JLabel("Nombre Completo: *")); pNuevo.add(txtNombre);
        pNuevo.add(new JLabel("Teléfono:")); pNuevo.add(txtTel);
        pNuevo.add(new JLabel("Correo Electrónico:")); pNuevo.add(txtCorreo);
        pNuevo.add(new JLabel("Dirección:")); pNuevo.add(txtDir);

        DefaultOption opt = new DefaultOption() { @Override public boolean closeWhenClickOutside() { return false; } };
        GlassPanePopup.showPopup(new SimplePopupBorder(pNuevo, "Registro Rápido de Cliente", new String[]{"Cancelar", "Guardar y Seleccionar"}, (pc, i) -> {
            if (i == 1) {
                if (txtCedula.getText().trim().isEmpty() || txtNombre.getText().trim().isEmpty()) {
                    MessageAlerts.getInstance().showMessage("Campos requeridos", "La cédula y el nombre son obligatorios.");
                    return;
                }
                Cliente nuevoCli = new Cliente(0, txtCedula.getText().trim(), txtNombre.getText().trim(), txtTel.getText().trim(), txtCorreo.getText().trim(), txtDir.getText().trim());
                if (new ClientesDAO().registrar(nuevoCli)) {
                    List<Cliente> clis = new ClientesDAO().buscar(nuevoCli.getCedula());
                    if(!clis.isEmpty()) {
                        clienteActual = clis.get(0);
                        if (lbCedulaCliente == null) {
                            lbCedulaCliente = new JLabel("Cliente:");
                            cedulaClienteValor = new JLabel("");
                            cedulaClienteValor.putClientProperty(FlatClientProperties.STYLE, "font: bold +1; foreground: #009991;");
                            cifrasPanel.add(lbCedulaCliente, "split 2, gapright 10, gaptop 10");
                            cifrasPanel.add(cedulaClienteValor, "align right, wrap");
                        }
                        cedulaClienteValor.setText(clienteActual.getNombre());
                        cifrasPanel.revalidate();
                    }
                    pc.closePopup();
                    raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS, raven.toast.Notifications.Location.BOTTOM_CENTER, "Cliente guardado exitosamente.");
                } else {
                    MessageAlerts.getInstance().showMessage("Error al guardar", "No se pudo guardar el cliente. Intenta nuevamente.");
                }
            } else { pc.closePopup(); }
        }), opt);
    }

    // Resetea la pantalla de venta a su estado inicial.
    private void limpiarVenta() {
        modelo.setRowCount(0);
        clienteActual = null;
        if (lbCedulaCliente != null) {
            cifrasPanel.remove(lbCedulaCliente);
            cifrasPanel.remove(cedulaClienteValor);
            lbCedulaCliente = null;
        }
        efectivo.setText("$0.00");
        transferencia.setText("$0.00");
        serie.setText(new FacturaDAO().obtenerNuevaSerie());
        recalcularCifras();
        cifrasPanel.revalidate();
        cifrasPanel.repaint();
    }

    // Convierte un texto con formato $XX.XX a double.
    private double parseDinero(String texto) {
        try { return Double.parseDouble(texto.replace("$", "").replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    private String formatearDinero(double valor) {
        return "$" + String.format("%.2f", valor).replace(",", ".");
    }
}