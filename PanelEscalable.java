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

    // Pantalla interna con tamaño fijo, sus dimensiones base y la capa transparente que intercepta el mouse
    private final JComponent contenido;
    private final int anchoBase;
    private final int altoBase;
    private final CapaMouse capa = new CapaMouse();

    // Constructor: guarda el panel a escalar, su tamaño original y prepara el contenedor negro
    public PanelEscalable(JComponent contenido, int anchoBase, int altoBase) {
        super(null); // usamos posiciones exactas manuales
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

    // Calcula cuánto agrandar o achicar sin deformar (usa el menor factor entre ancho y alto)
    private double escala() {
        return Math.min(getWidth() / (double) anchoBase, getHeight() / (double) altoBase);
    }

    // Calcula el margen horizontal para centrar la pantalla escalada (deja barras negras a los lados si sobra ancho)
    private double desplazX() {
        return (getWidth() - anchoBase * escala()) / 2.0;
    }

    // Calcula el margen vertical para centrar la pantalla escalada (deja barras negras arriba/abajo si sobra alto)
    private double desplazY() {
        return (getHeight() - altoBase * escala()) / 2.0;
    }

    // Coloca la capa del mouse ocupando toda la ventana y el contenido con su tamaño base original
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
        // Si se pide repintar todo o la escala no es válida, repintamos toda la ventana
        if (completo || s <= 0) {
            super.paintImmediately(0, 0, getWidth(), getHeight());
            return;
        }
        // Convertimos el área base a píxeles reales de la ventana (con un margen de 2 px para evitar bordes sucios)
        int izq = Math.max(0, (int) Math.floor(desplazX() + x * s) - 2);
        int arr = Math.max(0, (int) Math.floor(desplazY() + y * s) - 2);
        int der = Math.min(getWidth(), (int) Math.ceil(desplazX() + (x + w) * s) + 2);
        int aba = Math.min(getHeight(), (int) Math.ceil(desplazY() + (y + h) * s) + 2);
        if (der > izq && aba > arr) {
            super.paintImmediately(izq, arr, der - izq, aba - arr);
        }
    }

    // Dibuja los hijos aplicando el desplazamiento y la escala configurada
    @Override
    protected void paintChildren(Graphics g) {
        double s = escala();
        if (s <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        // Configuramos suavizado de imagen y texto para que se vea bien al escalar
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (CALIDAD_ALTA) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }
        // Movemos el origen al centro/esquina útil, escalamos y recortamos al área base
        g2.translate(desplazX(), desplazY());
        g2.scale(s, s);
        g2.clipRect(0, 0, anchoBase, altoBase);
        super.paintChildren(g2);
        g2.dispose();
    }

    // ----------------------- Mouse -----------------------

    // Capa invisible encima de todo que recibe los eventos del mouse y los traduce a coordenadas base
    private class CapaMouse extends JComponent
            implements MouseListener, MouseMotionListener, MouseWheelListener {

        private Component presionado;   // componente donde se presionó el botón
        private Component bajoMouse;    // componente sobre el que está el mouse

        // Constructor: hace la capa transparente y registra los escuchas de clic, movimiento y rueda
        CapaMouse() {
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }

        // Convierte un punto de la ventana real (p) a la coordenada equivalente dentro del 960x540 base
        private Point aBase(Point p) {
            double s = escala();
            if (s <= 0) return new Point(-1, -1);
            return new Point((int) Math.floor((p.x - desplazX()) / s),
                             (int) Math.floor((p.y - desplazY()) / s));
        }

        // Busca el componente más interno ("hijo más profundo") que está en ese punto base
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

        // Sube por la jerarquía hasta encontrar un componente que escuche la rueda del mouse
        private Component objetivoRueda(Component c) {
            while (c != null && c != PanelEscalable.this) {
                if (c.getMouseWheelListeners().length > 0) return c;
                c = c.getParent();
            }
            return null;
        }

        // Crea y despacha un evento de mouse traducido al componente destino correspondiente
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

        // Cuando se presiona un botón del mouse: calcula destino, actualiza hover y reenvía el clic abajo
        @Override public void mousePressed(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            Component t = objetivo(masProfundo(pb));
            cambiarBajoMouse(e, t, pb);
            presionado = t;
            enviar(e, MouseEvent.MOUSE_PRESSED, t, pb);
        }

        // Cuando se suelta el botón: avisa al componente que se presionó y limpia el estado de presionado
        @Override public void mouseReleased(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            enviar(e, MouseEvent.MOUSE_RELEASED, presionado, pb);
            presionado = null;
            cambiarBajoMouse(e, objetivo(masProfundo(pb)), pb);
        }

        // Cuando se hace clic completo en un punto: reenvía el evento al componente correspondiente
        @Override public void mouseClicked(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            enviar(e, MouseEvent.MOUSE_CLICKED, objetivo(masProfundo(pb)), pb);
        }

        // Cuando se mueve el mouse sin presionar: actualiza entrada/salida, envía movimiento y cambia el cursor
        @Override public void mouseMoved(MouseEvent e) {
            Point pb = aBase(e.getPoint());
            Component profundo = masProfundo(pb);
            Component t = objetivo(profundo);
            cambiarBajoMouse(e, t, pb);
            enviar(e, MouseEvent.MOUSE_MOVED, t, pb);
            Cursor cursor = (profundo != null) ? profundo.getCursor() : Cursor.getDefaultCursor();
            if (getCursor() != cursor) setCursor(cursor);   // solo si cambió
        }

        // Cuando se arrastra con un botón presionado: mantiene el evento en el componente presionado
        @Override public void mouseDragged(MouseEvent e) {
            if (presionado == null) return;
            Point pb = aBase(e.getPoint());
            Component sobre = objetivo(masProfundo(pb));
            cambiarBajoMouse(e, sobre == presionado ? presionado : null, pb);
            enviar(e, MouseEvent.MOUSE_DRAGGED, presionado, pb);
        }

        @Override public void mouseEntered(MouseEvent e) { }

        // Cuando el mouse sale de la capa: si no está arrastrando, limpia el componente bajo el cursor
        @Override public void mouseExited(MouseEvent e) {
            if (presionado == null) cambiarBajoMouse(e, null, aBase(e.getPoint()));
        }

        // Reenvía el giro de la rueda del mouse al componente que la escuche, ajustando coordenadas
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