import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * VISTA del Modo Clásico: dos matrices de 6x6 (una por jugador) sobre fondoInterfazMatrices.gif (animado).
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

    /** Avisa cuando se toca una celda (lo usa el controlador). */
    public interface OyenteCelda {
        void alTocar(int jugador, int fila, int col);
    }

    private final Image fondo = Login.cargarImagen("fondoInterfazMatrices.gif");
    private final Image fondoFijo = Login.cargarImagen("fondoEstaticoClasico.jpg"); 
    private Clip sonidoClic;

    private final Celda[][] matrizA = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final Celda[][] matrizB = new Celda[ModeloClasico.N][ModeloClasico.N];
    private final TitledBorder[] bordes = new TitledBorder[2];
    private final JPanel[] paneles = new JPanel[2];

    private BotonDragonBall btnPausa;     // reemplaza al antiguo botón "Volver al menú" de la pantalla
    private BotonDragonBall btnContinuar; // dentro del menú de pausa
    private BotonDragonBall btnFondo;     // dentro del menú de pausa: fondo animado / estático
    private BotonDragonBall btnVolver;    // dentro del menú de pausa
    private PanelPausa panelPausa;
    private boolean pausado = false;
    private boolean fondoEstatico = false;
    private Image fondoEstaticoImg;       // primer cuadro del GIF, se crea la primera vez que se necesita
    private Runnable alPausar;            // el controlador lo usa para frenar el reloj/turnos
    private Runnable alReanudar;          // el controlador lo usa para continuar
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

        int anchoBoton = 140;
        int altoBoton = 32;

        // Botón de pausa (ocupa el lugar del antiguo "Volver al menú")
        btnPausa = new BotonDragonBall("Pausa");
        btnPausa.setBounds((int) (ANCHO * BOTON_CENTRO_X) - anchoBoton / 2, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        btnPausa.addActionListener(e -> pausar());
        add(btnPausa);

        // Botones del menú de pausa
        btnContinuar = new BotonDragonBall("Continuar");
        btnContinuar.addActionListener(e -> reanudar());

        btnFondo = new BotonDragonBall("");
        btnFondo.addActionListener(e -> alternarFondo());
        actualizarTextoFondo();

        btnVolver = new BotonDragonBall("Volver al menú");
        btnVolver.addActionListener(e -> volverAlMenu());

        // BOTONES DE PRUEBA: fuerzan que gane un jugador para ver sus animaciones de victoria y derrota.
        // Jugador 1 arriba a la izquierda y jugador 2 arriba a la derecha. Se pueden borrar al terminar de probar.
        int margenBoton = 15;
        btnGanaJ1 = new BotonDragonBall("Gana Jugador 1");
        btnGanaJ1.setBounds(margenBoton, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        add(btnGanaJ1);

        btnGanaJ2 = new BotonDragonBall("Gana Jugador 2");
        btnGanaJ2.setBounds(ANCHO - margenBoton - anchoBoton, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        add(btnGanaJ2);

        // Menú de pausa: se agrega al final con índice 0 para que quede por encima de todo
        panelPausa = new PanelPausa(btnContinuar, btnFondo, btnVolver);
        add(panelPausa, 0);

        // Esc abre/cierra la pausa
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "pausa");
        getActionMap().put("pausa", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) alternarPausa();
            }
        });

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

    /**
     * MEJORA: el fondo se carga y se dibuja con el código compartido de Login (antes había una copia
     * propia con ImageIO.read, que con un GIF solo mostraría el primer cuadro). Se quitó la
     * interpolación bilineal (~7 ms por cuadro, y el GIF repinta varias veces por segundo).
     *
     * NOTA: fondoInterfazMatrices.gif mide 500x281 y aquí se estira a 960x540 (casi el doble), por
     * eso se verá borroso o con pixeles. Con un GIF de 960x540 se vería nítido.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Login.pintarFondo(g, fondoEstatico ? obtenerFondoEstatico() : fondo, this, new Color(30, 20, 50));
    }

    /** Copia el cuadro actual del GIF (normalmente el primero) en una imagen fija. */
    private Image obtenerFondoEstatico() {
         if (fondoFijo != null) return fondoFijo;
        if (fondoEstaticoImg == null && fondo != null) {
            Image cargada = new ImageIcon(fondo).getImage(); // ImageIcon espera a que la imagen cargue
            int w = cargada.getWidth(null);
            int h = cargada.getHeight(null);
            if (w > 0 && h > 0) {
                BufferedImage copia = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = copia.createGraphics();
                g2.drawImage(cargada, 0, 0, null);
                g2.dispose();
                fondoEstaticoImg = copia;
            }
        }
        return fondoEstaticoImg != null ? fondoEstaticoImg : fondo;
    }

    private void alternarFondo() {
        fondoEstatico = !fondoEstatico;
        actualizarTextoFondo();
        repaint();
    }

    private void actualizarTextoFondo() {
        btnFondo.setText("Fondo estático: " + (fondoEstatico ? "SÍ" : "NO"));
        btnFondo.setApagado(!fondoEstatico); // gris cuando está desactivado
    }

    // ------------------------------------------------------------------
    // Pausa
    // ------------------------------------------------------------------

    public void pausar() {
        if (pausado) return;
        pausado = true;
        panelPausa.setVisible(true);
        repaint();
        if (alPausar != null) alPausar.run();
    }

    public void reanudar() {
        if (!pausado) return;
        pausado = false;
        panelPausa.setVisible(false);
        repaint();
        if (alReanudar != null) alReanudar.run();
    }

    public void alternarPausa() {
        if (pausado) reanudar(); else pausar();
    }

    public boolean isPausado() { return pausado; }

    /** Menú de pausa con el estilo Dragon Ball: tapa la pantalla (y bloquea los clics) y muestra los botones. */
    private class PanelPausa extends JPanel {
        private final int cajaAncho = 320;
        private final int cajaAlto = 250;
        private final int cajaX = (ANCHO - cajaAncho) / 2;
        private final int cajaY = (ALTO - cajaAlto) / 2;

        PanelPausa(BotonDragonBall continuar, BotonDragonBall fondoBtn, BotonDragonBall volver) {
            setOpaque(false);
            setLayout(null);
            setBounds(0, 0, ANCHO, ALTO);
            setVisible(false);
            addMouseListener(new MouseAdapter() { }); // se traga los clics para que no lleguen a las celdas

            int bx = cajaX + 40;
            int bw = cajaAncho - 80;
            int bh = 40;
            continuar.setBounds(bx, cajaY + 75, bw, bh);
            fondoBtn.setBounds(bx, cajaY + 129, bw, bh);
            volver.setBounds(bx, cajaY + 183, bw, bh);
            add(continuar);
            add(fondoBtn);
            add(volver);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // capa oscura sobre todo el juego
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // caja del menú: café oscuro con borde negro y filete amarillo
            g2.setColor(new Color(60, 10, 0, 235));
            g2.fillRoundRect(cajaX, cajaY, cajaAncho, cajaAlto, 30, 30);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(5f));
            g2.drawRoundRect(cajaX, cajaY, cajaAncho, cajaAlto, 30, 30);
            g2.setColor(BotonDragonBall.AMARILLO);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cajaX + 5, cajaY + 5, cajaAncho - 10, cajaAlto - 10, 24, 24);

            textoCentrado(g2, "PAUSA", ANCHO / 2, cajaY + 52, 36, BotonDragonBall.AMARILLO, 255);
            g2.dispose();
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
        // MEJORA: el Timer se crea una sola vez y se reinicia; antes se creaba uno nuevo en cada aviso de turno
        if (timerAviso == null) {
            timerAviso = new Timer(30, e -> {
                if (System.currentTimeMillis() - inicioAviso >= duracionAviso) {
                    jugadorAviso = -1;
                    timerAviso.stop();
                }
                repaint();
            });
        }
        timerAviso.restart();
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (pausado) return; // el menú de pausa queda por encima de los avisos y animaciones
        if (jugadorAviso >= 0) pintarAviso((Graphics2D) g.create());

        // las animaciones (intro, victoria, derrota) van encima de todo, cada una sobre su matriz
        for (int j = 0; j < 2; j++) {
            if (animTipo[j] != ModeloClasico.Animacion.NINGUNA) pintarAnimacion((Graphics2D) g.create(), j);
        }
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
        reanudar(); // si estaba en pausa, cierra el menú y avisa al controlador
        if (alVolver != null) alVolver.run();
    }

    // ------------------------------------------------------------------
    // Animaciones (la vista solo dibuja lo que le manda el controlador)
    // ------------------------------------------------------------------

    private static final Color AURA_DERROTA = new Color(120, 120, 135);
    private static final Color TEXTO_DERROTA = new Color(175, 175, 190);

    private final ModeloClasico.Animacion[] animTipo =
            {ModeloClasico.Animacion.NINGUNA, ModeloClasico.Animacion.NINGUNA};
    private final double[] animProgreso = new double[2];
    private final int[] animCuadro = new int[2];
    private final String[] animNombre = {"", ""};
    @SuppressWarnings("unchecked")
    private final List<ModeloClasico.Particula>[] animParticulas =
            new List[]{Collections.emptyList(), Collections.emptyList()};

    /** Guarda cómo va la animación de un jugador y la redibuja. */
    public void mostrarAnimacion(int jugador, ModeloClasico.Animacion tipo, double progreso, int cuadro,
                                 String nombre, List<ModeloClasico.Particula> particulas) {
        animTipo[jugador] = tipo;
        animProgreso[jugador] = progreso;
        animCuadro[jugador] = cuadro;
        animNombre[jugador] = nombre;
        animParticulas[jugador] = particulas;
        repaint();
    }

    /** Quita todas las animaciones de la pantalla. */
    public void detenerAnimacion() {
        for (int j = 0; j < 2; j++) {
            animTipo[j] = ModeloClasico.Animacion.NINGUNA;
            animParticulas[j] = Collections.emptyList();
        }
        repaint();
    }

    private void pintarAnimacion(Graphics2D g, int jugador) {
        ModeloClasico.Animacion tipo = animTipo[jugador];
        Rectangle r = paneles[jugador].getBounds(); // la animación ocupa la zona de la matriz del jugador
        g.setClip(r);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color color = (jugador == 0) ? AZUL_TURNO : ROJO_TURNO;
        switch (tipo) {
            case INTRO:
                pintarIntro(g, r, animProgreso[jugador], color, animNombre[jugador]);
                break;
            case VICTORIA:
                pintarVictoria(g, r, animCuadro[jugador], animProgreso[jugador], color,
                        animNombre[jugador], animParticulas[jugador]);
                break;
            case DERROTA:
                pintarDerrota(g, r, animCuadro[jugador], animProgreso[jugador], animParticulas[jugador]);
                break;
            default:
                break;
        }
        g.dispose();
    }

    /** Intro: un anillo de energía del color del jugador que se expande y su nombre que aparece y se va. */
    private void pintarIntro(Graphics2D g, Rectangle r, double p, Color color, String nombre) {
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
        double envolvente = Math.sin(Math.PI * p); // 0 -> 1 -> 0

        g.setColor(conAlpha(Color.BLACK, (int) (120 * envolvente)));
        g.fillRect(r.x, r.y, r.width, r.height);

        int radio = (int) (p * r.width * 0.6);
        g.setStroke(new BasicStroke(6f));
        g.setColor(conAlpha(color, (int) (255 * (1 - p))));
        g.drawOval(cx - radio, cy - radio, radio * 2, radio * 2);

        textoCentrado(g, nombre, cx, cy + 14, 40, Color.WHITE, (int) (255 * envolvente));
    }

    /** Victoria: destello, aura que respira, ondas de choque, chispas doradas y "VICTORIA" con el nombre debajo. */
    private void pintarVictoria(Graphics2D g, Rectangle r, int cuadro, double p, Color color,
                                String nombre, List<ModeloClasico.Particula> particulas) {
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;

        g.setColor(conAlpha(new Color(10, 10, 25), (int) (170 * Math.min(1, p * 5))));
        g.fillRect(r.x, r.y, r.width, r.height);

        // aura
        float radio = (float) (r.width * 0.55 * Math.min(1, p * 3) + Math.sin(cuadro * 0.25) * 8);
        radio = Math.max(1f, radio);
        g.setPaint(new RadialGradientPaint(cx, cy, radio, new float[]{0f, 1f},
                new Color[]{conAlpha(color, 220), conAlpha(color, 0)}));
        g.fillOval((int) (cx - radio), (int) (cy - radio), (int) (radio * 2), (int) (radio * 2));

        // ondas de choque
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 3; i++) {
            int t = cuadro - i * 12;
            if (t > 0 && t < 50) {
                int rr = t * 6;
                g.setColor(conAlpha(Color.WHITE, (int) (255 * (1 - t / 50f))));
                g.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);
            }
        }

        // chispas doradas
        for (ModeloClasico.Particula pa : particulas) {
            g.setColor(conAlpha(DORADO, (int) (255 * pa.getAlfa())));
            g.fillOval(r.x + (int) (pa.getX() * r.width), r.y + (int) (pa.getY() * r.height), 6, 6);
        }

        // "VICTORIA" entra grande con un pequeño rebote; el nombre aparece debajo
        float t = Math.min(1f, cuadro / 12f);
        int tam = (int) (30 + 34 * easeOutBack(t));
        textoCentrado(g, "VICTORIA", cx, cy - 5, tam, Color.WHITE, 255);
        int alfaNombre = (int) (255 * Math.max(0, Math.min(1, (cuadro - 8) / 10.0)));
        textoCentrado(g, nombre, cx, cy + 42, 30, DORADO, alfaNombre);

        // destello inicial
        if (cuadro < 8) {
            g.setColor(conAlpha(Color.WHITE, (int) (255 * (1 - cuadro / 8f))));
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    /** Derrota: sacudida, aura gris que se apaga, cenizas que caen y "DERROTA" apagado. */
    private void pintarDerrota(Graphics2D g, Rectangle r, int cuadro, double p,
                               List<ModeloClasico.Particula> particulas) {
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;

        if (cuadro < 20) { // sacudida al inicio
            double fuerza = 1 - cuadro / 20.0;
            g.translate((int) (Math.sin(cuadro * 2.7) * 7 * fuerza), (int) (Math.cos(cuadro * 3.1) * 5 * fuerza));
        }

        g.setColor(conAlpha(new Color(5, 5, 12), (int) (190 * Math.min(1, p * 4))));
        g.fillRect(r.x - 12, r.y - 12, r.width + 24, r.height + 24);

        float radio = (float) Math.max(1, r.width * 0.5 * (1 - p * 0.85));
        g.setPaint(new RadialGradientPaint(cx, cy, radio, new float[]{0f, 1f},
                new Color[]{conAlpha(AURA_DERROTA, (int) (160 * (1 - p))), conAlpha(AURA_DERROTA, 0)}));
        g.fillOval((int) (cx - radio), (int) (cy - radio), (int) (radio * 2), (int) (radio * 2));

        for (ModeloClasico.Particula pa : particulas) {
            g.setColor(conAlpha(AURA_DERROTA, (int) (255 * pa.getAlfa())));
            g.fillOval(r.x + (int) (pa.getX() * r.width), r.y + (int) (pa.getY() * r.height), 6, 6);
        }

        textoCentrado(g, "DERROTA", cx, cy + 18, 56, TEXTO_DERROTA, (int) (255 * Math.min(1, cuadro / 15.0)));
    }

    /** Texto centrado en cx con la línea base en yBase, con contorno negro para que se lea sobre cualquier fondo. */
    private static void textoCentrado(Graphics2D g, String texto, int cx, int yBase, int tam, Color color, int alfa) {
        if (alfa <= 0 || texto == null || texto.isEmpty()) return;
        g.setFont(new Font("Arial", Font.BOLD, tam));
        int x = cx - g.getFontMetrics().stringWidth(texto) / 2;
        g.setColor(conAlpha(Color.BLACK, alfa));
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                if (dx != 0 || dy != 0) g.drawString(texto, x + dx, yBase + dy);
        g.setColor(conAlpha(color, alfa));
        g.drawString(texto, x, yBase);
    }

    /** Curva de entrada con rebote: va de 0 a 1 pasándose un poco. */
    private static float easeOutBack(float t) {
        float u = t - 1;
        return 1 + 2.70158f * u * u * u + 1.70158f * u * u;
    }

    // Setters y getters
    public void setOyenteCelda(OyenteCelda oyente) { this.oyenteCelda = oyente; }
    public void setAlMostrarse(Runnable accion) { this.alMostrarse = accion; }
    public JButton getBtnVolver() { return btnVolver; } // ahora está dentro del menú de pausa
    public JButton getBtnPausa() { return btnPausa; }
    public void setAlPausar(Runnable accion) { this.alPausar = accion; }
    public void setAlReanudar(Runnable accion) { this.alReanudar = accion; }
    public JButton getBtnGanaJ1() { return btnGanaJ1; }
    public JButton getBtnGanaJ2() { return btnGanaJ2; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}