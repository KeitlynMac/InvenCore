package gui.popus;

import Model.Notificacion;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import utilities.NotificacionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel de notificaciones completamente responsivo.
 * Se adapta al tamaño del contenedor padre.
 */

// Panel del popup de notificaciones.
// Se actualiza en tiempo real cuando llegan notificaciones nuevas.
// Al hacer clic en una notificación la marca como leída.

// Panel flotante de notificaciones que se abre desde la campana.
// Muestra todas las notificaciones en orden cronológico inverso.
// Tiene botón para marcar todas como leídas.
public class pNotificaciones extends JPanel {

    private final JPanel listaPanel;

    public pNotificaciones() {
        setLayout(new MigLayout("wrap, fill, insets 0", "[fill, grow]", "[][1!][grow, fill]"));
        setOpaque(false);

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new MigLayout("insets 14 16 12 16, fillx", "[grow][]"));
        header.setOpaque(false);

        JLabel lblTitulo = new JLabel("Notificaciones");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font:bold +5;");

        JButton btnLimpiar = new JButton("Marcar leídas");
        btnLimpiar.setOpaque(false);
        btnLimpiar.setContentAreaFilled(false);
        btnLimpiar.setBorderPainted(false);
        btnLimpiar.putClientProperty(FlatClientProperties.STYLE,
            "foreground:#3b82f6; font:bold; focusWidth:0;");
        btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiar.addActionListener(e -> {
            NotificacionManager.getInstance().marcarTodasLeidas();
            refrescar();
        });

        header.add(lblTitulo, "growx");
        header.add(btnLimpiar, "align right");

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(128, 128, 128, 40));

        // ── Lista ────────────────────────────────────────────────────────────
        listaPanel = new JPanel();
        listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
        listaPanel.setOpaque(false);
        listaPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(listaPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE,
            "trackArc:999; trackInsets:3,3,3,3; thumbInsets:3,3,3,3; width:6;");
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(header,  "growx");
        add(sep,     "growx, h 1!");
        add(scroll,  "grow, push");

        refrescar();

        // Escuchar cambios
        NotificacionManager.getInstance().addListener(
            nots -> SwingUtilities.invokeLater(this::refrescar));
    }

    public void refrescar() {
        listaPanel.removeAll();
        List<Notificacion> lista = NotificacionManager.getInstance().getLista();

        if (lista.isEmpty()) {
            JPanel vacio = new JPanel(new MigLayout("fill", "[center]", "[center]"));
            vacio.setOpaque(false);
            vacio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            JLabel lbv = new JLabel("<html><center>🔔<br><br>Sin notificaciones</center></html>",
                SwingConstants.CENTER);
            lbv.putClientProperty(FlatClientProperties.STYLE,
                "foreground:$Label.disabledForeground; font:+2;");
            vacio.add(lbv);
            listaPanel.add(vacio);
        } else {
            for (Notificacion n : lista) {
                JPanel card = crearTarjeta(n);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                listaPanel.add(card);
                listaPanel.add(Box.createVerticalStrut(5));
            }
        }

        listaPanel.revalidate();
        listaPanel.repaint();
    }

    private JPanel crearTarjeta(Notificacion n) {
        String colorAccent = switch (n.getTipo()) {
            case VENTA -> "#10b981";
            case STOCK -> "#f97316";
            case COBRO -> "#ef4444";
            default    -> "#3b82f6";
        };

        JPanel card = new JPanel(new MigLayout("insets 10 12 10 12, fillx", "[8!][grow][]", "[]3[]"));
        card.putClientProperty(FlatClientProperties.STYLE,
            "arc:10; [light]background:" + (n.isLeida() ? "darken(@background,2%)" : "darken(@background,5%)") +
            "; [dark]background:" + (n.isLeida() ? "lighten(@background,3%)" : "lighten(@background,7%)") + ";");

        // Barra de color izquierda
        JPanel barra = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode(colorAccent));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 4, 4));
            }
        };
        barra.setOpaque(false);
        barra.setPreferredSize(new Dimension(4, 30));

        JLabel lblTitulo = new JLabel(n.getTitulo());
        lblTitulo.putClientProperty(FlatClientProperties.STYLE,
            "font:bold +1;" + (n.isLeida() ? "foreground:$Label.disabledForeground;" : ""));

        JLabel lblFecha = new JLabel(n.getFechaFormato());
        lblFecha.putClientProperty(FlatClientProperties.STYLE,
            "foreground:$Label.disabledForeground; font:-1;");

        JLabel lblMsg = new JLabel(
            "<html><p style='width:220px;'>" + n.getMensaje() + "</p></html>");
        lblMsg.putClientProperty(FlatClientProperties.STYLE,
            "foreground:$Label.disabledForeground; font:+0;");

        card.add(barra,     "spany 2, growy");
        card.add(lblTitulo, "growx");
        card.add(lblFecha,  "align right, wrap");
        card.add(lblMsg,    "span 2, growx");

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                n.marcarLeida();
                refrescar();
            }
            @Override public void mouseEntered(MouseEvent e) { card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.repaint(); }
        });

        return card;
    }
}
