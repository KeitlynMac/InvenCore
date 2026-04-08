package gui.popus;


import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.datetime.DatePicker;
import raven.datetime.DateSelectionAble;


import javax.swing.*;
import java.time.LocalDate;



// Popup con un DatePicker para que el usuario elija la fecha de vencimiento de la deuda.
// Solo se usa cuando la venta queda con deuda pendiente.
public class fechaVencimiento extends JPanel {



    public fechaVencimiento(){
        init();
    }

    void init(){
        this.setLayout(new MigLayout("wrap, fillx, insets 20", "[center]", "[center]"));

        mensaje.putClientProperty(FlatClientProperties.STYLE, ""+
                "font: bold +4;"
        );

        datePicker.setEditor(editor);
        datePicker.setDateSelectionAble(new DateSelectionAble() {
            @Override
            public boolean isDateSelectedAble(LocalDate localDate) {
                return !localDate.isBefore(LocalDate.now());
            }
        });

        fechaV = datePicker.getDateFormat();


        add(mensaje);
        add(editor);
    }

    public String getFechaV() {
        return fechaV;
    }

    JLabel mensaje = new JLabel("Ponerle fecha limite para pagar la factura");
    JFormattedTextField editor = new JFormattedTextField();
    DatePicker datePicker = new DatePicker();
    String fechaV;


}
