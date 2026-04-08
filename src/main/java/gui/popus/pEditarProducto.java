package gui.popus;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.*;


// Formulario popup para editar un producto existente.
// Carga los datos actuales y permite modificarlos. El ID del producto viene oculto.

// Popup para editar los datos de un producto existente.
// Recibe el producto actual y pre-llena los campos para editar.
public class pEditarProducto extends JPanel {

    private static final String[] CATEGORIAS = {
        "General","Alimentos","Bebidas","Limpieza","Tecnología",
        "Ropa","Hogar","Salud","Papelería","Otros"
    };

    private JTextField    txtId, txtCodigo, txtNombre, txtPrecio, txtStock, txtFechaVenc;
    private JTextArea     txtDescripcion;
    private JComboBox<String> cbCategoria;
    private JLabel        lblImagenPreview;
    private String        rutaImagenFinal;

    public pEditarProducto() { init(); }

    private void init() {
        setLayout(new MigLayout("fill, wrap 2, insets 20", "[160!][grow, fill]", "[]8[]"));

        JPanel imgPanel = new JPanel(new MigLayout("wrap, insets 0", "[center]"));
        imgPanel.putClientProperty(FlatClientProperties.STYLE, "background:null");

        lblImagenPreview = new JLabel();
        lblImagenPreview.setPreferredSize(new Dimension(110, 110));
        lblImagenPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagenPreview.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, true));
        lblImagenPreview.setText("<html><center><small>Sin<br>imagen</small></center></html>");

        JButton btnSelImg = new JButton("Cambiar Imagen");
        btnSelImg.putClientProperty(FlatClientProperties.STYLE,
            "arc:10; background:#3b82f6; foreground:#ffffff; font:bold; margin:5,8,5,8;");
        btnSelImg.addActionListener(e -> seleccionarImagen());

        imgPanel.add(lblImagenPreview, "width 110!, height 110!");
        imgPanel.add(btnSelImg, "growx, gaptop 5");
        add(imgPanel, "span 2, align center, gapbottom 10");

        txtId = new JTextField(); txtId.setFocusable(false);
        txtId.putClientProperty(FlatClientProperties.STYLE,
            "arc:15; borderWidth:0; margin:8,10,8,10; [light]background:darken(@background,3%); [dark]background:lighten(@background,6%);");

        add(lbl("#"));               add(txtId, "growx");
        add(lbl("Código *"));        add(campo(txtCodigo  = new JTextField(), ""));
        add(lbl("Nombre *"));        add(campo(txtNombre  = new JTextField(), ""));
        add(lbl("Precio ($) *"));    add(campo(txtPrecio  = new JTextField(), ""));
        add(lbl("Stock *"));         add(campo(txtStock   = new JTextField(), ""));
        add(lbl("Categoría"));
        cbCategoria = new JComboBox<>(CATEGORIAS);
        cbCategoria.putClientProperty(FlatClientProperties.STYLE, "arc:15;");
        add(cbCategoria, "growx");
        add(lbl("Fecha Vencimiento")); add(campo(txtFechaVenc = new JTextField(), "YYYY-MM-DD (opcional)"));

        add(lbl("Descripción"), "aligny top");
        txtDescripcion = new JTextArea(3, 20);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        //txtDescripcion.putClientProperty(FlatClientProperties.STYLE,
        //   "arc:15; borderWidth:0; margin:8,10,8,10;");
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        scrollDesc.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,20)));
        add(scrollDesc, "growx, height 70!");
    }

    public void cargarDatos(String id, String codigo, String nombre, String precio,
                             String stock, String categoria, String descripcion,
                             String imagenPath, String fechaVenc) {
        txtId.setText(id);
        txtCodigo.setText(codigo);
        txtNombre.setText(nombre);
        txtPrecio.setText(precio);
        txtStock.setText(stock);
        if (categoria != null) {
            for (int i = 0; i < CATEGORIAS.length; i++)
                if (CATEGORIAS[i].equalsIgnoreCase(categoria)) { cbCategoria.setSelectedIndex(i); break; }
        }
        txtDescripcion.setText(descripcion != null ? descripcion : "");
        txtFechaVenc.setText(fechaVenc != null ? fechaVenc : "");
        rutaImagenFinal = imagenPath;
        if (imagenPath != null && new File(imagenPath).exists()) {
            ImageIcon ico = new ImageIcon(
                new ImageIcon(imagenPath).getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH));
            lblImagenPreview.setIcon(ico);
            lblImagenPreview.setText("");
        }
    }

    /** Backward-compatible overload */
    public void cargarDatos(String id, String codigo, String nombre, String precio, String stock, String categoria) {
        cargarDatos(id, codigo, nombre, precio, stock, categoria, null, null, null);
    }
    public void cargarDatos(String id, String codigo, String nombre, String precio, String stock) {
        cargarDatos(id, codigo, nombre, precio, stock, null, null, null, null);
    }

    private void seleccionarImagen() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png","jpg","jpeg"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                String dir = System.getProperty("user.dir") + File.separator + "img_productos";
                new File(dir).mkdirs();
                String dest = dir + File.separator + f.getName();
                Files.copy(f.toPath(), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
                rutaImagenFinal = dest;
                lblImagenPreview.setIcon(new ImageIcon(
                    new ImageIcon(dest).getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH)));
                lblImagenPreview.setText("");
            } catch (Exception ex) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Error de imagen", "No se pudo cargar la imagen seleccionada.");
            }
        }
    }

    // Valida y guarda los cambios del producto en la BD.
    public boolean guardarDatos() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Campos obligatorios", "El código y el nombre son obligatorios."); return false;
        }
        try {
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",","."));
            int    stock  = Integer.parseInt(txtStock.getText().trim());
            if (precio < 0 || stock < 0) {
                raven.alerts.MessageAlerts.getInstance().showMessage("Valor inválido", "Precio y stock no pueden ser negativos."); return false;
            }
            Model.Producto p = new Model.Producto();
            p.setIdProducto(Integer.parseInt(txtId.getText().trim()));
            p.setCodigo(codigo);
            p.setNombre(nombre);
            p.setPrecio(precio);
            p.setStock(stock);
            p.setCategoria(cbCategoria.getSelectedItem().toString());
            p.setDescripcion(txtDescripcion.getText().trim());
            p.setImagenPath(rutaImagenFinal);
            p.setFechaVencimiento(txtFechaVenc.getText().trim().isEmpty() ? null : txtFechaVenc.getText().trim());

            if (new dao.ProductoDAO().editar(p)) {
                raven.toast.Notifications.getInstance().show(
                    raven.toast.Notifications.Type.SUCCESS,
                    raven.toast.Notifications.Location.BOTTOM_CENTER, "Producto actualizado");
                return true;
            }
        } catch (NumberFormatException e) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Formato incorrecto", "Precio y stock deben ser números válidos (ej: 9.99 y 100).");
        }
        return false;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;");
        return l;
    }
    private JTextField campo(JTextField tf, String ph) {
        tf.putClientProperty(FlatClientProperties.STYLE, "arc:15; borderWidth:0; margin:8,10,8,10;");
        if (!ph.isEmpty()) tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, ph);
        return tf;
    }
}
