import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * VISTA del Modo Patrones: matriz de 5x5 sobre fondoPatrones.gif (animado) y un panel con la información del juego.
 * Solo dibuja y avisa de los clics; las reglas están en ModeloPatrones y el flujo del juego
 * en ControladorPatrones.
 * Es un JPanel para la ventana única de Main (su API hacia Main no cambia).
 */
public class MatrizPatrones extends JPanel {

    private static final int ANCHO = 960, ALTO = 540; // mismo tamaño que el área útil de la ventana
    private static final Color DORADO = new Color(255, 215, 0);

    // Colores de las celdas según su estado
    private static final Color FONDO_NORMAL = new Color(8, 25, 12, 175);
    private static final Color FONDO_MOSTRADA = new Color(255, 228, 50, 245);
    private static final Color FONDO_ACERTADA = new Color(40, 170, 70, 225);
    private static final Color FONDO_FALLADA = new Color(205, 30, 30, 235);
    private static final Color BORDE_NORMAL = new Color(150, 210, 120);

    /** Aviso de que el usuario tocó una celda (lo implementa el controlador). */
    public interface OyenteCelda {
        void alTocar(int fila, int col);
    }

    private final Image fondo = Login.cargarImagen("fondoPatrones.gif"); // reutiliza el cargador de Login
    private final Celda[][] matriz = new Celda[ModeloPatrones.N][ModeloPatrones.N];
    private Clip sonidoClic;
    private Runnable alVolver;       // navegación que Main le asigna
    private OyenteCelda oyenteCelda;

    // Panel de información (izquierda)
    private final JLabel lblPatron = etiqueta(15, DORADO);
    private final JLabel lblRondas = etiqueta(13, Color.WHITE);
    private final JLabel lblProgreso = etiqueta(13, Color.WHITE);
    private final JLabel lblMensaje = etiqueta(14, Color.WHITE);
    private final JLabel lblTiempoTotal = etiqueta(12, Color.WHITE);
    private final JLabel lblTiempoEntre = etiqueta(12, Color.WHITE);
    private final Barra barraTotal = new Barra();
    private final Barra barraEntre = new Barra();

    private final BotonDragonBall btnVolver;
    private final BotonDragonBall btnIniciar;

    public MatrizPatrones() {
        Dimension tam = new Dimension(ANCHO, ALTO);
        setPreferredSize(tam);
        setMinimumSize(tam); // evita que el GridBagLayout de Main lo encoja
        setMaximumSize(tam);
        setLayout(null);

        cargarSonido();

        // Matriz grande: ocupa toda la parte central (x 25%-75%, y 11%-97%)
        JPanel panelMatriz = crearPanelMatriz("Matriz 5x5 - Modo Patrones");
        panelMatriz.setBounds((int) (ANCHO * 0.25), (int) (ALTO * 0.11), (int) (ANCHO * 0.5), (int) (ALTO * 0.86));
        add(panelMatriz);

        // Panel de información a la izquierda
        JPanel info = crearPanelInfo();
        info.setBounds(15, 62, 215, 270);
        add(info);

        // Botón Iniciar debajo del panel de información
        btnIniciar = new BotonDragonBall("¡Iniciar!");
        btnIniciar.setFont(new Font("Arial", Font.BOLD, 18));
        btnIniciar.setBounds(35, 348, 175, 44);
        add(btnIniciar);

        // Botón arriba al centro
        btnVolver = new BotonDragonBall("Volver al menú");
        btnVolver.setBounds(ANCHO / 2 - 70, (int) (ALTO * 0.02), 140, 32);
        btnVolver.addActionListener(e -> volverAlMenu());
        add(btnVolver);

        // Conecta modelo y controlador a esta vista (Main sigue haciendo solo "new MatrizPatrones()")
        new ControladorPatrones(new ModeloPatrones(), this);
    }

    // ------------------------------------------------------------------
    // Sonido y fondo
    // ------------------------------------------------------------------

    /** Carga clic.wav (classpath o carpeta del proyecto); si falta, el juego sigue sin sonido. */
    private void cargarSonido() {
        try {
            URL url = getClass().getResource("/clic.wav");
            try (AudioInputStream audio = url != null
                    ? AudioSystem.getAudioInputStream(url)
                    : AudioSystem.getAudioInputStream(new File("clic.wav"))) {
                sonidoClic = AudioSystem.getClip();
                sonidoClic.open(audio);
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar clic.wav: " + e.getMessage());
        }
    }

    /** Reproduce el clic desde el inicio (reinicia si aún sonaba). */
    public void reproducirClic() {
        if (sonidoClic == null) return;
        sonidoClic.stop();
        sonidoClic.setFramePosition(0);
        sonidoClic.start();
    }

    /**
     * MEJORA: antes se calculaba "cubrir y recortar" y se activaba la interpolación bilineal en
     * cada repintado. Con un GIF el fondo se repinta varias veces por segundo, y esas dos cosas
     * eran trabajo repetido (el GIF ya es 16:9 como la ventana). Ahora se usa el método compartido
     * de Login, que estira la imagen sin esos cálculos.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Login.pintarFondo(g, fondo, this, new Color(10, 30, 15)); // color de respaldo si falta la imagen
    }

    // ------------------------------------------------------------------
    // Construcción de la matriz y del panel de información
    // ------------------------------------------------------------------

    /** Crea el panel con la cuadrícula de celdas (n x n según el arreglo). */
    private JPanel crearPanelMatriz(String titulo) {
        int n = matriz.length;
        JPanel panel = new JPanel(new GridLayout(n, n, 4, 4));
        panel.setOpaque(false); // deja ver el fondo entre las celdas
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), titulo,
                TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.WHITE));

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int fila = i, col = j;
                Celda celda = new Celda();
                celda.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        reproducirClic();
                        if (oyenteCelda != null) oyenteCelda.alTocar(fila, col);
                    }
                });
                matriz[i][j] = celda;
                panel.add(celda);
            }
        }
        return panel;
    }

    /** Panel semitransparente con ronda, mensaje y barras de tiempo. */
    private JPanel crearPanelInfo() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(8, 20, 12, 190));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        lblPatron.setBounds(14, 10, 190, 22);
        lblRondas.setBounds(14, 34, 190, 20);
        lblProgreso.setBounds(14, 54, 190, 20);
        lblMensaje.setBounds(14, 82, 190, 76);
        lblMensaje.setVerticalAlignment(SwingConstants.TOP);
        lblTiempoTotal.setBounds(14, 168, 190, 18);
        barraTotal.setBounds(14, 188, 187, 14);
        lblTiempoEntre.setBounds(14, 212, 190, 18);
        barraEntre.setBounds(14, 232, 187, 14);

        panel.add(lblPatron);
        panel.add(lblRondas);
        panel.add(lblProgreso);
        panel.add(lblMensaje);
        panel.add(lblTiempoTotal);
        panel.add(barraTotal);
        panel.add(lblTiempoEntre);
        panel.add(barraEntre);
        return panel;
    }

    private static JLabel etiqueta(int tamano, Color color) {
        JLabel l = new JLabel();
        l.setFont(new Font("Arial", Font.BOLD, tamano));
        l.setForeground(color);
        return l;
    }

    /** Celda semitransparente cuyo color cambia según su estado. */
    private static class Celda extends JLabel {
        // MEJORA: los 4 bordes se crean una sola vez. Antes cada llamada a setEstado creaba un
        // Color y un LineBorder nuevos por celda (25 celdas cada vez que se pintaba el tablero).
        private static final Border LINEA_NORMAL  = BorderFactory.createLineBorder(BORDE_NORMAL, 2);
        private static final Border LINEA_MOSTRADA = BorderFactory.createLineBorder(Color.WHITE, 2);
        private static final Border LINEA_ACERTADA = BorderFactory.createLineBorder(new Color(150, 255, 170), 2);
        private static final Border LINEA_FALLADA  = BorderFactory.createLineBorder(new Color(255, 150, 150), 2);

        private Color colorFondo = FONDO_NORMAL;
        private ModeloPatrones.Estado estado = ModeloPatrones.Estado.NORMAL;

        Celda() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setBorder(LINEA_NORMAL);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void setEstado(ModeloPatrones.Estado nuevo) {
            // MEJORA: si la celda ya está en ese estado no se cambia ni se repinta. pintarTablero
            // recorre las 25 celdas, y casi siempre solo cambia una; con el fondo animado además
            // ya se repinta todo por cada cuadro del GIF.
            if (nuevo == estado) return;
            estado = nuevo;
            switch (nuevo) {
                case MOSTRADA: colorFondo = FONDO_MOSTRADA; setBorder(LINEA_MOSTRADA); break;
                case ACERTADA: colorFondo = FONDO_ACERTADA; setBorder(LINEA_ACERTADA); break;
                case FALLADA:  colorFondo = FONDO_FALLADA;  setBorder(LINEA_FALLADA);  break;
                default:       colorFondo = FONDO_NORMAL;   setBorder(LINEA_NORMAL);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(colorFondo);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    /** Barra de tiempo: verde, amarilla o roja según lo que queda; gris si no aplica. */
    private static class Barra extends JComponent {
        private double fraccion = 0;
        private boolean activa = false;

        void set(double fraccion, boolean activa) {
            this.fraccion = Math.max(0, Math.min(1, fraccion));
            this.activa = activa;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            if (activa && fraccion > 0) {
                Color color = fraccion > 0.5 ? new Color(60, 200, 90) : fraccion > 0.25 ? DORADO : new Color(230, 50, 40);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, Math.max(h, (int) ((w - 1) * fraccion)), h - 1, h, h);
            }
            g2.setColor(new Color(255, 255, 255, 170));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Métodos que usa el controlador para actualizar la pantalla
    // ------------------------------------------------------------------

    /** Pinta cada celda según la matriz de estados del modelo. */
    public void pintarTablero(ModeloPatrones.Estado[][] estados) {
        for (int i = 0; i < ModeloPatrones.N; i++) {
            for (int j = 0; j < ModeloPatrones.N; j++) {
                matriz[i][j].setEstado(estados[i][j]);
            }
        }
    }

    public void setMensaje(String texto) {
        lblMensaje.setText("<html><body style='width:170px'>" + texto.replace("\n", "<br>") + "</body></html>");
    }

    public void actualizarMarcador(int casillasPatron, int rondasSuperadas, int aciertosEnRonda) {
        lblPatron.setText("Patrón: " + casillasPatron + " casillas");
        lblRondas.setText("Rondas superadas: " + rondasSuperadas);
        lblProgreso.setText("Progreso: " + aciertosEnRonda + "/" + casillasPatron);
    }

    /** Muestra el tiempo que queda; msEntre es -1 mientras no se haya tocado la primera casilla. */
    public void actualizarTiempos(long msTotal, long msEntre) {
        lblTiempoTotal.setText(String.format("Tiempo total: %.1f s", msTotal / 1000.0));
        barraTotal.set((double) msTotal / ModeloPatrones.TIEMPO_TOTAL_MS, true);
        if (msEntre < 0) {
            lblTiempoEntre.setText("Entre casillas: —");
            barraEntre.set(0, false);
        } else {
            lblTiempoEntre.setText(String.format("Entre casillas: %.1f s", msEntre / 1000.0));
            barraEntre.set((double) msEntre / ModeloPatrones.TIEMPO_ENTRE_CASILLAS_MS, true);
        }
    }

    public void reiniciarTiempos() {
        lblTiempoTotal.setText("Tiempo total: —");
        lblTiempoEntre.setText("Entre casillas: —");
        barraTotal.set(0, false);
        barraEntre.set(0, false);
    }

    public void setBotonIniciarVisible(boolean visible) {
        btnIniciar.setVisible(visible);
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
    public JButton getBtnIniciar() { return btnIniciar; }
    public JButton getBtnVolver() { return btnVolver; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}