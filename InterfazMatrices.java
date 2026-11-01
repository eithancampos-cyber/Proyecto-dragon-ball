import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * VISTA del Modo Clásico: dos matrices de 6x6 (una por jugador) sobre fondo4.jpg.
 * Solo dibuja y avisa de los clics; las reglas están en ModeloClasico y la
 * coordinación en ControladorClasico.
 * Es un JPanel para mostrarse en la ventana única que crea Main (su API hacia Main no cambia).
 */
public class InterfazMatrices extends JPanel {

    public static final int ANCHO = 960;
    public static final int ALTO = 540;

    private static final double ZONA_X_INICIO = 0.04;
    private static final double ZONA_X_FIN = 0.96;
    private static final double ZONA_Y_INICIO = 0.38;
    private static final double ZONA_Y_FIN = 0.96;
    private static final double BOTON_CENTRO_X = 0.485;
    private static final double BOTON_Y = 0.05;

    // Colores de las celdas según su estado
    private static final Color FONDO_OCULTA = new Color(25, 15, 45, 170);
    private static final Color FONDO_VISIBLE = new Color(70, 45, 125, 215);
    private static final Color FONDO_ENCONTRADA = new Color(20, 110, 50, 190);
    private static final Color TEXTO_OCULTA = new Color(150, 120, 200);
    private static final Color TEXTO_ENCONTRADA = new Color(255, 215, 0);
    private static final Color DORADO = new Color(255, 215, 0);

    /** Aviso de que el usuario tocó una celda (lo implementa el controlador). */
    public interface OyenteCelda {
        void alTocar(int jugador, int fila, int col);
    }

    private BufferedImage fondo;
    private Clip sonidoClic;

    private final Celda[][] matrizA = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final Celda[][] matrizB = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final TitledBorder[] bordes = new TitledBorder[2];
    private final JPanel[] paneles = new JPanel[2];

    private BotonDragonBall btnVolver;
    private Runnable alVolver;
    private OyenteCelda oyenteCelda;

    public InterfazMatrices() {
        Dimension tam = new Dimension(ANCHO, ALTO);
        setPreferredSize(tam);
        setMinimumSize(tam);
        setMaximumSize(tam);
        setLayout(null);

        cargarFondo();
        cargarSonido();

        int zonaX0 = (int) (ANCHO * ZONA_X_INICIO);
        int zonaX1 = (int) (ANCHO * ZONA_X_FIN);
        int zonaY0 = (int) (ALTO * ZONA_Y_INICIO);
        int zonaY1 = (int) (ALTO * ZONA_Y_FIN);
        int separacion = 24;
        int anchoMatriz = (zonaX1 - zonaX0 - separacion) / 2;
        int altoMatriz = zonaY1 - zonaY0;
        int centroX = (zonaX0 + zonaX1) / 2;
        int bloque = 2 * anchoMatriz + separacion;
        int xInicio = centroX - bloque / 2;
        int y = zonaY0;

        JPanel panelA = crearPanelMatriz(matrizA, 0);
        panelA.setBounds(xInicio, y, anchoMatriz, altoMatriz);
        add(panelA);

        JPanel panelB = crearPanelMatriz(matrizB, 1);
        panelB.setBounds(xInicio + anchoMatriz + separacion, y, anchoMatriz, altoMatriz);
        add(panelB);

        btnVolver = new BotonDragonBall("Volver al menú");
        int anchoBoton = 140;
        int altoBoton = 32;
        btnVolver.setBounds((int) (ANCHO * BOTON_CENTRO_X) - anchoBoton / 2, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        btnVolver.addActionListener(e -> volverAlMenu());
        add(btnVolver);

        // Conecta modelo y controlador a esta vista (Main sigue haciendo solo "new InterfazMatrices()")
        new ControladorClasico(new ModeloClasico(), this);
    }

    // ------------------------------------------------------------------
    // Carga de recursos (sin cambios)
    // ------------------------------------------------------------------

    private void cargarFondo() {
        try {
            URL recurso = getClass().getResource("/fondo4.jpg");
            if (recurso != null) {
                fondo = ImageIO.read(recurso);
            } else {
                fondo = ImageIO.read(new File("fondo4.jpg"));
            }
        } catch (IOException e) {
            System.err.println("No se pudo cargar fondo4.jpg: " + e.getMessage());
        }
    }

    private void cargarSonido() {
        try {
            URL recurso = getClass().getResource("/clic.wav");
            AudioInputStream audio = (recurso != null)
                    ? AudioSystem.getAudioInputStream(recurso)
                    : AudioSystem.getAudioInputStream(new File("clic.wav"));
            sonidoClic = AudioSystem.getClip();
            sonidoClic.open(audio);
            audio.close();
        } catch (Exception e) {
            System.err.println("No se pudo cargar clic.wav: " + e.getMessage());
        }
    }

    private void reproducirClic() {
        if (sonidoClic == null) return;
        sonidoClic.stop();
        sonidoClic.setFramePosition(0);
        sonidoClic.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
            g2.dispose();
        } else {
            g.setColor(new Color(30, 20, 50));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ------------------------------------------------------------------
    // Construcción de las matrices
    // ------------------------------------------------------------------

    /** Celda con fondo semitransparente cuyo color cambia según su estado. */
    private static class Celda extends JLabel {
        private Color colorFondo = FONDO_OCULTA;

        Celda() {
            super("?", SwingConstants.CENTER);
            setOpaque(false);
            setFont(new Font("Arial", Font.BOLD, 20));
            setForeground(TEXTO_OCULTA);
            setBorder(BorderFactory.createLineBorder(new Color(150, 120, 200)));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void ocultar() { aplicar("?", TEXTO_OCULTA, FONDO_OCULTA); }
        void mostrar(String texto) { aplicar(texto, Color.WHITE, FONDO_VISIBLE); }
        void encontrada() { aplicar(getText(), TEXTO_ENCONTRADA, FONDO_ENCONTRADA); }

        private void aplicar(String texto, Color letra, Color fondo) {
            setText(texto);
            setForeground(letra);
            colorFondo = fondo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(colorFondo);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Crea el panel de un jugador con su cuadrícula de n x n celdas. */
    private JPanel crearPanelMatriz(Celda[][] celdas, int jugador) {
        int n = celdas.length;

        JPanel panel = new JPanel(new GridLayout(n, n, 2, 2));
        panel.setOpaque(false);
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), "Jugador " + (jugador + 1),
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), Color.WHITE);
        panel.setBorder(borde);
        bordes[jugador] = borde;
        paneles[jugador] = panel;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int fila = i, col = j;
                Celda celda = new Celda();
                celda.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        reproducirClic();
                        if (oyenteCelda != null) oyenteCelda.alTocar(jugador, fila, col);
                    }
                });
                celdas[i][j] = celda;
                panel.add(celda);
            }
        }
        return panel;
    }

    private Celda celda(int jugador, int fila, int col) {
        return (jugador == 0 ? matrizA : matrizB)[fila][col];
    }

    // ------------------------------------------------------------------
    // Métodos que usa el controlador para actualizar la pantalla
    // ------------------------------------------------------------------

    public void mostrarCarta(int jugador, int fila, int col, String texto) { celda(jugador, fila, col).mostrar(texto); }
    public void ocultarCarta(int jugador, int fila, int col) { celda(jugador, fila, col).ocultar(); }
    public void marcarEncontrada(int jugador, int fila, int col) { celda(jugador, fila, col).encontrada(); }

    /** Vuelve a dejar todas las celdas boca abajo. */
    public void reiniciarTablero() {
        for (int i = 0; i < ModeloClasico.N; i++) {
            for (int j = 0; j < ModeloClasico.N; j++) {
                matrizA[i][j].ocultar();
                matrizB[i][j].ocultar();
            }
        }
    }

    /** Muestra pares, intentos y de quién es el turno en el título de cada matriz. */
    public void actualizarMarcador(int jugador, int pares, int intentos, boolean esSuTurno) {
        TitledBorder borde = bordes[jugador];
        borde.setTitle((esSuTurno ? "► " : "") + "Jugador " + (jugador + 1)
                + "  |  Pares: " + pares + "/" + ModeloClasico.PARES
                + "  |  Intentos: " + intentos);
        borde.setTitleColor(esSuTurno ? DORADO : Color.WHITE);
        paneles[jugador].repaint();
    }

    /** Muestra el resultado; devuelve true si el usuario quiere jugar otra partida. */
    public boolean preguntarOtraPartida(String mensaje) {
        Object[] opciones = {"Jugar de nuevo", "Volver al menú"};
        int eleccion = JOptionPane.showOptionDialog(this, mensaje, "Fin de la partida",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        return eleccion == 0;
    }

    public void volverAlMenu() {
        if (alVolver != null) alVolver.run();
    }

    // Setters / getters para conectar controlador y Main
    public void setOyenteCelda(OyenteCelda oyente) { this.oyenteCelda = oyente; }
    public JButton getBtnVolver() { return btnVolver; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}