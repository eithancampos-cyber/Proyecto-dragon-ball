import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

/**
 * VISTA de los Mejores Jugadores: diálogo con el Top 5 y su premio, con el estilo Dragon Ball.
 * Solo dibuja la lista que le entrega el controlador (ControladorMejoresJugadores);
 * no lee archivos ni calcula premios.
 */
public class VentanaMejoresJugadores extends JDialog {

    // Paleta de colores del diálogo y colores de medalla según la posición en el Top
    private static final Color FONDO = new Color(26, 16, 40);
    private static final Color DORADO = new Color(255, 215, 0);
    private static final Color FILA = new Color(60, 40, 90, 200);
    // color del número de puesto: oro, plata, bronce y el resto
    private static final Color[] COLOR_PUESTO = {
        DORADO, new Color(205, 210, 220), new Color(205, 127, 50), BotonDragonBall.NARANJA, BotonDragonBall.NARANJA
    };

    // Dimensiones base del diálogo y de cada fila del ranking
    private static final int ANCHO = 520;
    private static final int ALTO_FILA = 46;

    /**
     * Constructor del diálogo modal: arma el título, encabezados, filas del Top y botón de cerrar,
     * ajustando el alto del contenido dinámicamente y centrando respecto a la ventana padre.
     */
    public VentanaMejoresJugadores(Window padre, List<ModeloMejoresJugadores.Puntaje> top) {
        // APPLICATION_MODAL bloquea la interacción con el resto de la app mientras este diálogo esté abierto
        super(padre, "Mejores Jugadores", ModalityType.APPLICATION_MODAL);
        setResizable(false);

        // Configura el contenedor principal con posicionamiento absoluto (null) y borde dorado
        JComponent contenido = (JComponent) getContentPane();
        contenido.setLayout(null);
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createLineBorder(new Color(255, 200, 0), 3));

        // Título y subtítulo explicativo del criterio de ordenamiento
        int y = 20;
        contenido.add(texto("MEJORES JUGADORES", 0, y, ANCHO, 40, new Font("Arial", Font.BOLD, 26), DORADO, SwingConstants.CENTER));

        y += 42;
        contenido.add(texto("Modo Clásico: menos intentos = mayor premio",
                0, y, ANCHO, 20, new Font("Arial", Font.ITALIC, 13), new Color(220, 220, 220), SwingConstants.CENTER));

        // encabezados de columna
        y += 36;
        Font fuenteEncabezado = new Font("Arial", Font.BOLD, 13);
        contenido.add(texto("#", 30, y, 40, 20, fuenteEncabezado, BotonDragonBall.NARANJA, SwingConstants.CENTER));
        contenido.add(texto("Jugador", 85, y, 170, 20, fuenteEncabezado, BotonDragonBall.NARANJA, SwingConstants.LEFT));
        contenido.add(texto("Intentos", 255, y, 80, 20, fuenteEncabezado, BotonDragonBall.NARANJA, SwingConstants.CENTER));
        contenido.add(texto("Premio", 345, y, 145, 20, fuenteEncabezado, BotonDragonBall.NARANJA, SwingConstants.RIGHT));

        // siempre MAX filas; los puestos libres se muestran vacíos
        y += 26;
        for (int i = 0; i < ModeloMejoresJugadores.MAX; i++) {
            ModeloMejoresJugadores.Puntaje p = i < top.size() ? top.get(i) : null;
            JPanel fila = crearFila(i, p);
            fila.setBounds(20, y, ANCHO - 40, ALTO_FILA);
            contenido.add(fila);
            y += ALTO_FILA + 6; // avanza la coordenada vertical con separación entre filas
        }

        // Mensaje amigable cuando aún no existen puntajes guardados
        if (top.isEmpty()) {
            contenido.add(texto("Todavía no hay partidas registradas. ¡Sé el primero!",
                    0, y, ANCHO, 20, new Font("Arial", Font.PLAIN, 13), Color.WHITE, SwingConstants.CENTER));
            y += 26;
        }

        // Botón para cerrar el diálogo
        y += 10;
        BotonDragonBall btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds((ANCHO - 150) / 2, y, 150, 40);
        btnVolver.addActionListener(e -> dispose());
        contenido.add(btnVolver);

        // Calcula el tamaño final del panel, ajusta la ventana con pack() y la centra sobre el padre
        y += 40 + 25;
        contenido.setPreferredSize(new Dimension(ANCHO, y));

        pack();
        setLocationRelativeTo(padre);
    }

    /** Una fila del ranking (coordenadas relativas a la fila; x=0 coincide con x=20 del diálogo). */
    private JPanel crearFila(int indice, ModeloMejoresJugadores.Puntaje p) {
        // Panel redondeado semitransparente cuyo borde resalta según el puesto y si está ocupado
        JPanel fila = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FILA);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(p != null ? COLOR_PUESTO[indice] : new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g2.dispose();
            }
        };
        fila.setOpaque(false);

        Font grande = new Font("Arial", Font.BOLD, 18);
        Font normal = new Font("Arial", Font.BOLD, 15);
        // el símbolo ₡ no está en todas las fuentes físicas: "Dialog" cae a una que sí lo tiene
        Font fuentePremio = new Font(Font.DIALOG, Font.BOLD, 15);

        // Número de posición (1..MAX)
        fila.add(texto(String.valueOf(indice + 1), 10, 0, 40, ALTO_FILA, grande, COLOR_PUESTO[indice], SwingConstants.CENTER));
        // Si el puesto está vacío, muestra un guion y termina
        if (p == null) {
            fila.add(texto("—", 65, 0, 170, ALTO_FILA, normal, new Color(170, 170, 170), SwingConstants.LEFT));
            return fila;
        }
        // Datos del jugador en esa posición: nombre, intentos y premio formateado
        fila.add(texto(p.getNombre(), 65, 0, 170, ALTO_FILA, normal, BotonDragonBall.AMARILLO, SwingConstants.LEFT));
        fila.add(texto(String.valueOf(p.getIntentos()), 235, 0, 80, ALTO_FILA, normal, Color.WHITE, SwingConstants.CENTER));
        fila.add(texto(formatearPremio(p.getPremio()), 325, 0, 145, ALTO_FILA, fuentePremio, Color.WHITE, SwingConstants.RIGHT));
        return fila;
    }

    /** Crea y posiciona un JLabel con la fuente, color y alineación especificados. */
    private static JLabel texto(String s, int x, int y, int ancho, int alto, Font fuente, Color color, int alineacion) {
        JLabel l = new JLabel(s, alineacion);
        l.setFont(fuente);
        l.setForeground(color);
        l.setBounds(x, y, ancho, alto);
        return l;
    }

    /** "₡2 812,50" (formato de Costa Rica); si el premio no se pudo calcular, "No disponible". */
    public static String formatearPremio(double premio) {
        if (premio <= 0) return "No disponible";
        // Configura formato numérico de Costa Rica con exactamente 2 decimales
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "₡" + nf.format(premio);
    }
}