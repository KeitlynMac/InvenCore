package utilities;

import com.formdev.flatlaf.FlatClientProperties;
import gui.popus.pNotificaciones;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

// Botón de campana de notificaciones dibujado completamente en código Java.
// Muestra un badge rojo con el número de notificaciones no leídas.
// Al hacer clic abre el panel de notificaciones debajo de la barra superior.
public class BellButton extends JButton {

    private long    noLeidas = 0;
    private JDialog popupDialog;

    public BellButton() {
        setPreferredSize(new Dimension(40, 34));
        setMinimumSize(new Dimension(40, 34));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Notificaciones");

        NotificacionManager.getInstance().addListener(lista -> {
            noLeidas = lista.stream().filter(n -> !n.isLeida()).count();
            SwingUtilities.invokeLater(this::repaint);
        });

        addActionListener(e -> togglePopup());
    }

    // ── Dibujo de la campana + badge ─────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth(), h = getHeight();
        boolean hover = getModel().isRollover();

        // Fondo hover redondeado
        if (hover || (popupDialog != null && popupDialog.isVisible())) {
            g2.setColor(UIManager.getColor("Button.toolbar.hoverBackground") != null
                ? UIManager.getColor("Button.toolbar.hoverBackground")
                : new Color(128, 128, 128, 40));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
        }

        // Campana — dibujada centrada
        Color bellColor = UIManager.getColor("Label.foreground") != null
            ? UIManager.getColor("Label.foreground") : Color.WHITE;
        if (hover) bellColor = bellColor.brighter();

        drawBell(g2, w, h, bellColor);

        // Badge de notificaciones no leídas
        if (noLeidas > 0) {
            String txt = noLeidas > 99 ? "99+" : String.valueOf(noLeidas);
            Font badgeFont = new Font("SansSerif", Font.BOLD, 9);
            g2.setFont(badgeFont);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(txt);
            int bw = Math.max(tw + 7, 15), bh = 15;
            int bx = w / 2 + 4, by = 2;

            // Sombra
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fill(new RoundRectangle2D.Float(bx + 1, by + 1, bw, bh, bh, bh));

            // Fondo rojo
            g2.setColor(Color.decode("#ef4444"));
            g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh));

            // Número blanco
            g2.setColor(Color.WHITE);
            g2.drawString(txt, bx + (bw - tw) / 2, by + bh - 3);
        }

        g2.dispose();
    }

    private void drawBell(Graphics2D g2, int w, int h, Color color) {
        // Coordenadas relativas al centro
        float cx = w / 2f;
        float cy = h / 2f - 1;
        float scale = Math.min(w, h) / 34f; // escala al tamaño del botón

        g2.setStroke(new BasicStroke(1.8f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);

        // Cuerpo de la campana (arco + trapecio)
        float bw = 12 * scale; // ancho base
        float bh = 9 * scale;  // alto cuerpo
        float top = cy - 5 * scale;

        Path2D.Float bell = new Path2D.Float();
        bell.moveTo(cx - bw / 2, cy + 3 * scale); // esquina izquierda base
        bell.lineTo(cx + bw / 2, cy + 3 * scale); // esquina derecha base
        bell.lineTo(cx + bw * 0.45f, top + bh * 0.6f);
        bell.curveTo(cx + bw * 0.5f, top, cx + bw * 0.1f, top - 2 * scale, cx, top - 3 * scale);
        bell.curveTo(cx - bw * 0.1f, top - 2 * scale, cx - bw * 0.5f, top, cx - bw * 0.45f, top + bh * 0.6f);
        bell.closePath();
        g2.draw(bell);

        // Tope (círculo)
        float cr = 2f * scale;
        g2.draw(new Ellipse2D.Float(cx - cr, top - 3 * scale - cr * 2, cr * 2, cr * 2));

        // Badajo (arco en la parte baja)
        float br = 2.5f * scale;
        g2.draw(new Arc2D.Float(cx - br, cy + 3 * scale, br * 2, br * 2, 180, 180, Arc2D.OPEN));
    }

    @Override
    protected void paintBorder(Graphics g) {} // sin borde

    // ── Popup responsivo ──────────────────────────────────────────────────────
        // Abre o cierra el panel de notificaciones. Si ya está abierto, lo cierra.
    private void togglePopup() {
        if (popupDialog != null && popupDialog.isVisible()) {
            popupDialog.dispose();
            popupDialog = null;
            repaint();
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;

        popupDialog = new JDialog(owner);
        popupDialog.setUndecorated(true);
        popupDialog.setBackground(new Color(0, 0, 0, 0));

        // Panel con fondo rounded + sombra simulada via borde
        JPanel contenido = new JPanel(new BorderLayout());
        contenido.putClientProperty(FlatClientProperties.STYLE,
            "arc:16; [light]background:@background; [dark]background:lighten(@background,8%);");
        contenido.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(120, 120, 120, 50), 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        pNotificaciones panelNot = new pNotificaciones();
        contenido.add(panelNot);
        popupDialog.add(contenido);

        // Tamaño responsivo: 30% del ancho del frame pero mín 380 máx 480
        int frameW = owner.getWidth();
        int popupW = Math.min(480, Math.max(380, (int)(frameW * 0.28)));
        int popupH = (int)(owner.getHeight() * 0.65);
        popupH = Math.min(popupH, 560);
        popupH = Math.max(popupH, 400);

        popupDialog.setSize(popupW, popupH);

        // Posicionar: alineado al borde derecho del botón, bajo la barra
        try {
            Point btnLoc = getLocationOnScreen();
            int px = btnLoc.x + getWidth() - popupW;
            int py = btnLoc.y + getHeight() + 6;

            Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
            if (px < screen.x) px = screen.x + 4;
            if (px + popupW > screen.x + screen.width)  px = screen.x + screen.width - popupW - 4;
            if (py + popupH > screen.y + screen.height) py = btnLoc.y - popupH - 4;

            popupDialog.setLocation(px, py);
        } catch (Exception ex) {
            popupDialog.setLocationRelativeTo(owner);
        }

        popupDialog.setVisible(true);
        repaint();

        // Cerrar al perder foco
        popupDialog.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) {
                if (popupDialog != null) { popupDialog.dispose(); popupDialog = null; repaint(); }
            }
        });
    }
}
