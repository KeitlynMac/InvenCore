package utilities;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;

// Permite poner botones de acción (Editar / Eliminar) dentro de celdas de JTable.
// Extiende AbstractCellEditor para que Swing lo trate como editor válido.
public class BotonEditor extends AbstractCellEditor implements TableCellEditor {

    private final JPanel panel;
    private final JTable tabla;
    private int fila;

    public BotonEditor(JTable tabla) {
        this.tabla = tabla;
        this.panel = new JPanel(new MigLayout("insets 0, fillx, center", "[]10[]", "[]"));
        this.panel.setOpaque(true);

        // Botón de eliminar
        JButton btnEliminar = new JButton();
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; background: null; hoverBackground: null;");
        btnEliminar.setIcon(new FlatSVGIcon("icons/square-plus.svg", 0.30f));
        btnEliminar.addActionListener(e -> {
            // First, stop the editing, then remove the row
            fireEditingStopped();
            ((DefaultTableModel) this.tabla.getModel()).removeRow(fila);
            raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.SUCCESS, raven.toast.Notifications.Location.BOTTOM_CENTER, "Fila eliminada.");
        });

        // Botón de editar
        JButton btnEditar = new JButton();
        btnEditar.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; background: null; hoverBackground: null;");
        btnEditar.setIcon(new FlatSVGIcon("icons/pencil.svg", 0.30f));
        btnEditar.addActionListener(e -> {
            // Stop editing
            fireEditingStopped();
            raven.toast.Notifications.getInstance().show(raven.toast.Notifications.Type.INFO, raven.toast.Notifications.Location.BOTTOM_CENTER, "Editando fila #" + fila);
        });

        this.panel.add(btnEditar);
        this.panel.add(btnEliminar);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.fila = row;
        return this.panel;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
}