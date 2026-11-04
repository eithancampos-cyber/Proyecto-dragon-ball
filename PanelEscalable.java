import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Envuelve una pantalla de tamaño fijo (por ejemplo 960x540) y la escala para que
 * llene la ventana, manteniendo la proporción (con barras negras si sobra espacio).
 * Traduce el mouse para que botones, campos de texto, etc. sigan funcionando.
 */
public class PanelEscalable extends JPanel {

    /**
     * false = más rápido (recomendado). true = bordes más suaves pero más lento.
     * Si el juego va fluido con false y quieres probar mejor calidad, cámbialo a true.
     */
    private static final boolean CALIDAD_ALTA = false;

    private final JComponent contenido;
    private final int anchoBase;
    private final int altoBase;
    private final CapaMouse capa = new CapaMouse();

    public PanelEscalable(JComponent contenido, int anchoBase, int altoBase) {
        super(null);
        this.contenido = contenido;
        this.anchoBase = anchoBase;
        this.altoBase = altoBase;
        setOpaque(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(anchoBase, altoBase));
        add(capa);        // primero: queda por encima y recibe todo el mouse
        add(contenido);
    }

    // ----------------------- Escala y posición -----------------------

    private double escala() {
        return Math.min(getWidth() / (double) anchoBase, getHeight() / (double) altoBase);
    }

    private double desplazX() {
        return (getWidth() - anchoBase * escala()) / 2.0;
    }

    private double desplazY() {
        return (getHeight() - altoBase * escala()) / 2.0;
    }

    @Override
    public void doLayout() {
        capa.setBounds(0, 0, getWidth(), getHeight());
        contenido.setBounds(0, 0, anchoBase, altoBase);
    }

    // ----------------------- Dibujo -----------------------

    /** Un repintado de un hijo debe empezar desde este panel (para aplicar la escala). */
    @Override
    protected boolean isPaintingOrigin() {
        return true;
    }

    /**
     * Swing nos pide repintar una zona en coordenadas de la pantalla base (960x540).
     * La convertimos a coordenadas reales y repintamos SOLO esa zona.
     */
    @Override
    public void paintImmediately(int x, int y, int w, int h) {
        double s = escala();
        boolean completo = (x <= 0 && y <= 0 && w >= getWidth() && h >= getHeight());
        if (completo || s <= 0) {
            super.paintImmediately(0, 0, getWidth(), getHeight());
            return;
        }
        int izq = Math.max(0, (int) Math.floor(desplazX() + x * s) - 2);
        int arr = Math.max(0, (int) Math.floor(desplazY() + y * s) - 2);
        int der = Math.min(getWidth(), (int) Math.ceil(desplazX() + (x + w) * s) + 2);
        int aba = Math.min(getHeight(), (int) Math.ceil(desplazY() + (y + h) * s) + 2);
        if (der > izq && aba > arr) {
            super.paintImmediately(izq, arr, der - izq, aba - arr);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        double s = escala();
        if (s <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (CALIDAD_ALTA) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }
        g2.translate(desplazX(), desplazY());
        g2.scale(s, s);
        g2.clipRect(0, 0, anchoBase, altoBase);
        super.paintChildren(g2);
        g2.dispose();
    }

    // ----------------------- Mouse -----------------------

    private class CapaMouse extends JComponent
            implements MouseListener, MouseMotionListener, MouseWheelListener {

        private Component presionado;   // componente donde se presionó el botón
        private Component bajoMouse;    // componente sobre el que está el mouse

        CapaMouse() {
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }

        private Point aBase(Point p) {
            double s = escala();
            if (s <= 0) return new Point(-1, -1);
            return new Point((int) Math.floor((p.x - desplazX()) / s),
                             (int) Math.floor((p.y - desplazY()) / s));
        }

        private Component masProfundo(Point pb) {
            if (pb.x < 0 || pb.y < 0 || pb.x >= anchoBase || pb.y >= altoBase) return null;
            return SwingUtilities.getDeepestComponentAt(contenido, pb.x, pb.y);
        }

        /** Sube por la jerarquía hasta encontrar un componente que escuche el mouse. */
        private Component objetivo(Component c) {
            while (c != null && c != PanelEscalable.this) {
                if (c.getMouseListeners().length > 0 || c.getMouseMotionListeners().length > 0) return c;
                c = c.getParent();
            }
            return null;
        }

        private Component objetivoRueda(Component c) {
            while (c != null && c != PanelEscalable.this) {
                if (c.getMouseWheelListeners().length > 0) return c;
                c = c.getParent();
            }
            return null;
        }

        private void enviar(MouseEvent o, int id, Component destino, Point pb) {
            if (destino == null) return;
            Point p = SwingUtilities.convertPoint(contenido, pb, destino);
            destino.dispatchEvent(new MouseEvent(destino, id, o.getWhen(), o.getModifiersEx(),
                    p.x, p.y, o.getXOnScreen(), o.getYOnScreen(),
                    o.getClickCount(), o.isPopupTrigger(), o.getButton()));
        }

        /** Envía MOUSE_EXITED / MOUSE_ENTERED cuando el mouse cambia de componente. */
        private void cambiarBajoMouse(MouseEvent o, Component nuevo, Point pb) {
            if (nuevo == bajoMouse) return;
            if (bajoMouse != null) enviar(o, MouseEvent.MOUSE_EXITED, bajoMouse, pb);
            bajoMouse = nuevo;
            if (nuevo != null) enviar(o, MouseEvent.MOUSE_ENTERED, nuevo, pb);
        }

        @Override public void mousePressed(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            Component t = objetivo(masProfundo(pb));
            cambiarBajoMouse(e, t, pb);
            presionado = t;
            enviar(e, MouseEvent.MOUSE_PRESSED, t, pb);
        }

        @Override public void mouseReleased(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            enviar(e, MouseEvent.MOUSE_RELEASED, presionado, pb);
            presionado = null;
            cambiarBajoMouse(e, objetivo(masProfundo(pb)), pb);
        }

        @Override public void mouseClicked(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            enviar(e, MouseEvent.MOUSE_CLICKED, objetivo(masProfundo(pb)), pb);
        }

        @Override public void mouseMoved(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            Component profundo = masProfundo(pb);
            Component t = objetivo(profundo);
            cambiarBajoMouse(e, t, pb);
            enviar(e, MouseEvent.MOUSE_MOVED, t, pb);
            Cursor cursor = (profundo != null) ? profundo.getCursor() : Cursor.getDefaultCursor();
            if (getCursor() != cursor) setCursor(cursor);   // solo si cambió
        }

        @Override public void mouseDragged(MouseEvent e) {
            if (presionado == null) return;
            Point pb = aBase(e.getPoint());
            Component sobre = objetivo(masProfundo(pb));
            cambiarBajoMouse(e, sobre == presionado ? presionado : null, pb);
            enviar(e, MouseEvent.MOUSE_DRAGGED, presionado, pb);
        }

        @Override public void mouseEntered(MouseEvent e) { }

        @Override public void mouseExited(MouseEvent e) {
            if (presionado == null) cambiarBajoMouse(e, null, aBase(e.getPoint()));
        }

        @Override public void mouseWheelMoved(MouseWheelEvent e) {
            Point pb = aBase(e.getPoint());
            Component t = objetivoRueda(masProfundo(pb));
            if (t == null) return;
            Point p = SwingUtilities.convertPoint(contenido, pb, t);
            t.dispatchEvent(new MouseWheelEvent(t, e.getID(), e.getWhen(), e.getModifiersEx(),
                    p.x, p.y, e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(),
                    e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(),
                    e.getWheelRotation(), e.getPreciseWheelRotation()));
        }
    }
}