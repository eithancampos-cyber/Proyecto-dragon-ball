import java.awt.*;
import java.awt.event.HierarchyEvent;
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

    // Personajes de las animaciones: uno parado encima de cada matriz
    private static final double PERSONAJE_ALTO = 0;     // alto de los personajes (fracción del alto de la ventana; 0 = tamaño original del GIF)
    private static final int PERSONAJE_DESPLAZ_X = 0;      // jugador 1: píxeles desde el centro de su matriz (positivo = derecha)
    private static final int PERSONAJE2_DESPLAZ_X = 0;     // jugador 2: igual, desde el centro de su matriz
    private static final int PERSONAJE_AJUSTE_PIES_Y = 30; // píxeles extra: positivo baja los pies, negativo los sube

    // Colores de las celdas según su estado
    private static final Color FONDO_OCULTA = new Color(25, 15, 45, 170);
    private static final Color FONDO_VISIBLE = new Color(70, 45, 125, 215);
    private static final Color FONDO_ENCONTRADA = new Color(20, 110, 50, 190);
    private static final Color TEXTO_OCULTA = new Color(150, 120, 200);
    private static final Color TEXTO_ENCONTRADA = new Color(255, 215, 0);
    private static final Color DORADO = new Color(255, 215, 0);

    /** Avisa cuando se toca una celda (lo usa el controlador). */
    public interface OyenteCelda {
        void alTocar(int jugador, int fila, int col);
    }

    private BufferedImage fondo;
    private Clip sonidoClic;
    private Animador animador;        // personaje del jugador 1
    private Animador animadorJ2;      // personaje del jugador 2

    private final Celda[][] matrizA = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final Celda[][] matrizB = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final TitledBorder[] bordes = new TitledBorder[2];
    private final JPanel[] paneles = new JPanel[2];

    private BotonDragonBall btnVolver;
    private BotonDragonBall btnGanaJ1; // botón de prueba: hace ganar al jugador 1
    private BotonDragonBall btnGanaJ2; // botón de prueba: hace ganar al jugador 2
    private Runnable alVolver;
    private OyenteCelda oyenteCelda;
    private Runnable alMostrarse; // se ejecuta cuando esta pantalla se muestra

    // Aviso de turno: azul para el jugador 1 y rojo para el 2
    private static final Color AZUL_TURNO = new Color(30, 110, 255);
    private static final Color ROJO_TURNO = new Color(235, 45, 45);
    private Timer timerAviso;
    private long inicioAviso;
    private int duracionAviso;
    private int jugadorAviso = -1; // -1 = no hay aviso

    public InterfazMatrices() {
        Dimension tam = new Dimension(ANCHO, ALTO);
        setPreferredSize(tam);
        setMinimumSize(tam);
        setMaximumSize(tam);
        setLayout(null);

        cargarFondo();
        cargarSonido();
        // Las animaciones son GIF, uno por animación, en la carpeta "animaciones":
        // intro.gif, victoria.gif y derrota.gif
        animador = new Animador(this);
        // jugador 2: usa los GIF de la carpeta "animaciones2"; si falta alguno, usa el de "animaciones" volteado en espejo
        animadorJ2 = new Animador(this, "animaciones2", "animaciones",
                java.util.EnumSet.of(Animador.Animacion.INTRO,
                        Animador.Animacion.VICTORIA, Animador.Animacion.DERROTA));

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

        // cada personaje queda centrado sobre su matriz, con los pies en el borde de arriba de su panel
        // (así el título con el marcador sigue visible debajo de él)
        int piesY = y + PERSONAJE_AJUSTE_PIES_Y;
        int altoPersonaje = (int) (ALTO * PERSONAJE_ALTO);
        animador.setAnclaje(xInicio + anchoMatriz / 2 + PERSONAJE_DESPLAZ_X, piesY, altoPersonaje);
        animadorJ2.setAnclaje(xInicio + anchoMatriz + separacion + anchoMatriz / 2 + PERSONAJE2_DESPLAZ_X,
                piesY, altoPersonaje);

        btnVolver = new BotonDragonBall("Volver al menú");
        int anchoBoton = 140;
        int altoBoton = 32;
        btnVolver.setBounds((int) (ANCHO * BOTON_CENTRO_X) - anchoBoton / 2, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        btnVolver.addActionListener(e -> volverAlMenu());
        add(btnVolver);

        // BOTONES DE PRUEBA: fuerzan que gane un jugador para ver sus animaciones de victoria y derrota.
        // Jugador 1 arriba a la izquierda y jugador 2 arriba a la derecha. Se pueden borrar al terminar de probar.
        int margenBoton = 15;
        btnGanaJ1 = new BotonDragonBall("Gana Jugador 1");
        btnGanaJ1.setBounds(margenBoton, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        add(btnGanaJ1);

        btnGanaJ2 = new BotonDragonBall("Gana Jugador 2");
        btnGanaJ2.setBounds(ANCHO - margenBoton - anchoBoton, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        add(btnGanaJ2);

        // detecta cuando la pantalla se muestra
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && alMostrarse != null) {
                alMostrarse.run();
            }
        });

        // crea el modelo y el controlador de este modo
        new ControladorClasico(new ModeloClasico(), this);
    }

    // ------------------------------------------------------------------
    // Carga de recursos
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
            URL recurso = getClass().getResource("/carta_volteo.wav");
            AudioInputStream audio = (recurso != null)
                    ? AudioSystem.getAudioInputStream(recurso)
                    : AudioSystem.getAudioInputStream(new File("carta_volteo.wav"));
            sonidoClic = AudioSystem.getClip();
            sonidoClic.open(audio);
            audio.close();
        } catch (Exception e) {
            System.err.println("No se pudo cargar carta_volteo.wav: " + e.getMessage());
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
    // Aviso de turno (se dibuja encima, sin quitar nada)
    // ------------------------------------------------------------------

    /** Muestra "Turno de jugador X" durante 'ms' milisegundos. */
    public void mostrarTurno(int jugador, int ms) {
        jugadorAviso = jugador;
        duracionAviso = ms;
        inicioAviso = System.currentTimeMillis();
        if (timerAviso != null) timerAviso.stop();
        timerAviso = new Timer(30, e -> {
            if (System.currentTimeMillis() - inicioAviso >= duracionAviso) {
                jugadorAviso = -1;
                timerAviso.stop();
            }
            repaint();
        });
        timerAviso.start();
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (jugadorAviso >= 0) pintarAviso((Graphics2D) g.create());

        // las animaciones (intro, victoria, derrota) van encima de todo
        Graphics2D g2 = (Graphics2D) g.create();
        animador.pintar(g2);
        animadorJ2.pintar(g2);
        g2.dispose();
    }

    private void pintarAviso(Graphics2D g2) {
        double f = (System.currentTimeMillis() - inicioAviso) / (double) duracionAviso;
        // aparece rápido y se va desvaneciendo al final
        float a = (float) (f < 0.15 ? f / 0.15 : f > 0.6 ? (1 - f) / 0.4 : 1);
        a = Math.max(0f, Math.min(1f, a));

        Color base = (jugadorAviso == 0) ? AZUL_TURNO : ROJO_TURNO;
        int w = getWidth(), h = getHeight();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // color de fondo, más fuerte arriba y se pierde hacia abajo
        g2.setPaint(new GradientPaint(0, 0, conAlpha(base, (int) (60 * a)), 0, h * 0.6f, conAlpha(base, 0)));
        g2.fillRect(0, 0, w, h);

        // franja del mensaje
        // se dibuja fila por fila para que los bordes queden suaves
        int yBanda = 66, altoBanda = 48;
        for (int i = 0; i < altoBanda; i++) {
            double vertical = Math.sin(Math.PI * (i + 0.5) / altoBanda); // 0 en los bordes, 1 al centro
            int alfa = (int) (105 * a * vertical);
            g2.setPaint(new LinearGradientPaint(0, 0, w, 0, new float[]{0f, 0.2f, 0.8f, 1f},
                    new Color[]{conAlpha(base, 0), conAlpha(base, alfa), conAlpha(base, alfa), conAlpha(base, 0)}));
            g2.fillRect(0, yBanda + i, w, 1);
        }

        String texto = "Turno de jugador " + (jugadorAviso + 1);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(texto)) / 2;
        int y = yBanda + (altoBanda - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(conAlpha(Color.BLACK, (int) (255 * a)));
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                if (dx != 0 || dy != 0) g2.drawString(texto, x + dx, y + dy);
        g2.setColor(conAlpha(Color.WHITE, (int) (255 * a)));
        g2.drawString(texto, x, y);
        g2.dispose();
    }

    private static Color conAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    // ------------------------------------------------------------------
    // Construcción de las matrices
    // ------------------------------------------------------------------

    /** Celda que cambia de color según su estado. */
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

    /** Crea el panel de un jugador con sus celdas. */
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
    // Métodos que usa el controlador
    // ------------------------------------------------------------------

    public void mostrarCarta(int jugador, int fila, int col, String texto) { celda(jugador, fila, col).mostrar(texto); }
    public void ocultarCarta(int jugador, int fila, int col) { celda(jugador, fila, col).ocultar(); }
    public void marcarEncontrada(int jugador, int fila, int col) { celda(jugador, fila, col).encontrada(); }

    /** Tapa todas las celdas. */
    public void reiniciarTablero() {
        for (int i = 0; i < ModeloClasico.N; i++) {
            for (int j = 0; j < ModeloClasico.N; j++) {
                matrizA[i][j].ocultar();
                matrizB[i][j].ocultar();
            }
        }
    }

    /** Pone pares, intentos y tiempo en el título de cada matriz (segundos < 0 = no muestra tiempo). */
    public void actualizarMarcador(int jugador, int pares, int intentos, boolean esSuTurno, int segundos) {
        TitledBorder borde = bordes[jugador];
        borde.setTitle((esSuTurno ? "► " : "") + "Jugador " + (jugador + 1)
                + "  |  Pares: " + pares + "/" + ModeloClasico.PARES
                + "  |  Intentos: " + intentos
                + (segundos >= 0 ? "  |  Tiempo: " + segundos + "s" : ""));
        borde.setTitleColor(esSuTurno ? DORADO : Color.WHITE);
        paneles[jugador].repaint();
    }

    /** Muestra el resultado. Devuelve true si quieren jugar otra vez. */
    public boolean preguntarOtraPartida(String mensaje) {
        Object[] opciones = {"Jugar de nuevo", "Volver al menú"};
        int eleccion = JOptionPane.showOptionDialog(this, mensaje, "Fin de la partida",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);
        return eleccion == 0;
    }

    public void volverAlMenu() {
        if (alVolver != null) alVolver.run();
    }

    // ------------------------------------------------------------------
    // Animaciones
    // ------------------------------------------------------------------

    /** Reproduce una animación del personaje de un jugador (0 o 1); 'alTerminar' puede ser null. */
    public void reproducirAnimacion(int jugador, Animador.Animacion animacion, Runnable alTerminar) {
        (jugador == 0 ? animador : animadorJ2).reproducir(animacion, alTerminar);
    }

    /** Corta todas las animaciones y las quita de la pantalla. */
    public void detenerAnimacion() {
        animador.detener();
        animadorJ2.detener();
    }

    // Setters y getters
    public void setOyenteCelda(OyenteCelda oyente) { this.oyenteCelda = oyente; }
    public void setAlMostrarse(Runnable accion) { this.alMostrarse = accion; }
    public JButton getBtnVolver() { return btnVolver; }
    public JButton getBtnGanaJ1() { return btnGanaJ1; }
    public JButton getBtnGanaJ2() { return btnGanaJ2; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}