package utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

// Renderizador personalizado para los encabezados de las tablas.
// Hace que el texto del header se alinee igual que los datos de cada columna.
public class HeaderTabla implements TableCellRenderer {

    private final TableCellRenderer headerRenderer;

    public HeaderTabla(JTable tabla) {
        this.headerRenderer = tabla.getTableHeader().getDefaultRenderer();

        // Aplicar alineación consistente a las celdas de datos también
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean selected, boolean focused, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, col);
                if (c instanceof JLabel lbl) {
                    lbl.setHorizontalAlignment(alineacion(col, t));
                }
                return c;
            }
        });
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        Component c = headerRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);
        if (c instanceof JLabel lbl) {
            lbl.setHorizontalAlignment(alineacion(col, table));
        }
        return c;
    }

    /**
     * Determina la alineación para una columna.
     * Por defecto: LEFT para texto, RIGHT para dinero (si el nombre del header contiene "$" o "Total"),
     * CENTER para columnas que parecen códigos/estado.
     * Se puede sobreescribir para comportamiento personalizado.
     */
    protected int alineacion(int col, JTable tabla) {
        String nombre = "";
        try { nombre = tabla.getColumnName(col).toUpperCase(); } catch (Exception ignored) {}

        if (nombre.contains("$") || nombre.contains("PRECIO") || nombre.contains("MONTO")
                || nombre.contains("TOTAL") || nombre.contains("DEUDA")
                || nombre.contains("PAGADO") || nombre.contains("CAMBIO")) {
            return SwingConstants.RIGHT;
        }
        if (nombre.contains("ESTADO") || nombre.contains("CÓDIGO")
                || nombre.contains("CODIGO") || nombre.contains("#")
                || nombre.contains("IMAGEN") || nombre.equals("ID")) {
            return SwingConstants.CENTER;
        }
        return SwingConstants.LEFT;
    }

    /** Overload de compatibilidad para código heredado */
    protected int alineacion(int col) {
        return SwingConstants.LEFT;
    }
}
