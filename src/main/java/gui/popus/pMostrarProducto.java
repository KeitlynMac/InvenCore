package gui.popus;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Popup de solo lectura que muestra todos los detalles de un producto.
// Se abre al hacer clic en la fila de la tabla de productos.
public class pMostrarProducto extends JPanel {

    JLabel codigo = new JLabel();
    JLabel nombre = new JLabel();
    JLabel lbprecio = new JLabel("Precio");
    JLabel precio = new JLabel();
    JLabel lbstock = new JLabel("Stock");
    JLabel stock = new JLabel();
    JLabel lbCantidad = new JLabel("Cantidad");
    JTextField cantidad = new JTextField();
    JLabel lbDescuento = new JLabel("Descuento");
    JTextField descuento = new JTextField();

    public pMostrarProducto(){
        init();
    }

    void init(){
        this.setLayout(new MigLayout("wrap, fillx, insets 20", "[center]", "[center]"));

        // ELIMINADO: VentaDAO nv = new VentaDAO(); <-- Esto causaba el error

        codigo.putClientProperty(FlatClientProperties.STYLE, ""+
                "background: #00a4e2;" +
                "foreground: #ffffff;" +
                "arc: 15;" +
                "font: bold +1;"

        );
        codigo.setBorder(new EmptyBorder(5,10,5,10));

        nombre.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +4;"

        );

        lbprecio.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +3;" +
                "foreground: #009991"
        );

        precio.putClientProperty(FlatClientProperties.STYLE, ""+
                "arc: 15;" +
                "font: bold +2;"

        );
        precio.setBorder(new EmptyBorder(5,10,5,10));

        lbstock.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +3;" +
                "foreground: #0075cf"
        );

        lbCantidad.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +3;"
        );

        cantidad.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +1;"
        );

        lbDescuento.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +3;"
        );

        descuento.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +1;"
        );

        cantidad.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,"Cantidad");
        descuento.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,"Descuento");

        add(codigo);
        add(nombre, "gaptop 8");
        add(lbprecio, "split 4, gaptop 10");
        add(precio, "align right, gaptop 10, gapright 19 ");
        add(lbstock, "align right, gaptop 10");
        add(stock, "align right, gaptop 10");
        add(lbCantidad, "split 2, gaptop 20, gapright 26");
        add(cantidad, "align right, gaptop 20, width 90:90");
        add(lbDescuento, "split 2, gapright 15");
        add(descuento, "align right, width 90:90");
    }

    public void setDatosProducto(String codigo, String nombre, String precio, String stock) {
        this.codigo.setText(codigo);
        this.nombre.setText(nombre);
        this.precio.setText("$" + precio);
        this.stock.setText(stock);
        coloresStock();
    }

    public void coloresStock(){
        int stockColor = Integer.parseInt(stock.getText());

        if(stockColor > 10){
            stock.putClientProperty(FlatClientProperties.STYLE, ""+
                    "background: #009991;" +
                    "foreground: #ffffff;" +
                    "arc: 15;" +
                    "font: bold +2;"

            );
            stock.setBorder(new EmptyBorder(5,17,5,17));

        }else if (stockColor <= 10 && stockColor >= 3){ // Corregida ligeramente la lógica
            stock.putClientProperty(FlatClientProperties.STYLE, ""+
                    "background: #ff9a08;" +
                    "foreground: #ffffff;" +
                    "arc: 15;" +
                    "font: bold +2;"

            );
            stock.setBorder(new EmptyBorder(5,17,5,17));
        }else if (stockColor < 3){
            stock.putClientProperty(FlatClientProperties.STYLE, ""+
                    "background: #ff0808;" +
                    "foreground: #ffffff;" +
                    "arc: 15;" +
                    "font: bold +2;"
            );
            stock.setBorder(new EmptyBorder(5,17,5,17));
        }
    }

    public JLabel getCodigo() {
        return codigo;
    }

    public JLabel getNombre() {
        return nombre;
    }

    public JLabel getPrecio() {
        return precio;
    }

    public JLabel getStock() {
        return stock;
    }

    public JTextField getCantidad() {
        return cantidad;
    }

    public JTextField getDescuento() {
        return descuento;
    }
}