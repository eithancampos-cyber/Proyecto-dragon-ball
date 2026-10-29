import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Interfaz gráfica que solo MUESTRA dos matrices (cada una de 6x6).
 * Dibuja la imagen fondo4.jpg como fondo y coloca las matrices en la zona
 * central libre, entre las dos caras, para que nunca las tapen.
 * Es un JPanel para mostrarse en la ventana única que crea Main.
 */
public class InterfazMatrices extends JPanel {

    // Tamaño del panel: igual al área útil de la ventana en Main (960 x 540).
    // La proporción de la imagen es 1348 x 766, casi igual a 960 x 540.
    public static final int ANCHO = 960;
    public static final int ALTO = 540;

    // Zona que ocupan las matrices, como fracción del tamaño del panel.
    // Los personajes están arriba al centro, así que las matrices empiezan debajo de ellos.
    // Si cambias la imagen de fondo, ajusta solo estos valores.
    private static final double ZONA_X_INICIO = 0.04;  // margen izquierdo
    private static final double ZONA_X_FIN = 0.96;     // margen derecho
    private static final double ZONA_Y_INICIO = 0.38;  // justo debajo de los personajes
    private static final double ZONA_Y_FIN = 0.96;     // casi el borde inferior
    private static final double BOTON_CENTRO_X = 0.485; // centrado sobre los personajes
    private static final double BOTON_Y = 0.05;         // arriba de los personajes

    private BufferedImage fondo;
    private Clip sonidoClic; // sonido corto que suena al tocar una celda

    // Una etiqueta por cada celda de cada matriz
    private JLabel[][] matrizA = new JLabel[6][6];
    private JLabel[][] matrizB = new JLabel[6][6];

    // Acción de navegación que Main le asigna (volver al menú)
    private Runnable alVolver;

    public InterfazMatrices() {
        Dimension tam = new Dimension(ANCHO, ALTO);
        setPreferredSize(tam);
        setMinimumSize(tam); // sin esto, el GridBagLayout de Main lo encoge a 0x0 y no se ve nada
        setMaximumSize(tam);
        setLayout(null); // posicionamiento manual para respetar la zona libre

        cargarFondo();
        cargarSonido();

        // Cálculo de posiciones: las dos matrices ocupan TODA la zona y quedan
        // centradas en ella (horizontal y verticalmente).
        int zonaX0 = (int) (ANCHO * ZONA_X_INICIO);
        int zonaX1 = (int) (ANCHO * ZONA_X_FIN);
        int zonaY0 = (int) (ALTO * ZONA_Y_INICIO);
        int zonaY1 = (int) (ALTO * ZONA_Y_FIN);
        int separacion = 24;
        int anchoMatriz = (zonaX1 - zonaX0 - separacion) / 2;
        int altoMatriz = zonaY1 - zonaY0; // las celdas se estiran para aprovechar todo el espacio
        int centroX = (zonaX0 + zonaX1) / 2;
        int bloque = 2 * anchoMatriz + separacion;
        int xInicio = centroX - bloque / 2;
        int y = zonaY0;

        JPanel panelA = crearPanelMatriz(matrizA, "Jugador 1");
        panelA.setBounds(xInicio, y, anchoMatriz, altoMatriz);
        add(panelA);

        JPanel panelB = crearPanelMatriz(matrizB, "Jugador 2");
        panelB.setBounds(xInicio + anchoMatriz + separacion, y, anchoMatriz, altoMatriz);
        add(panelB);

        // Botón de navegación (estilo Dragon Ball), arriba de los personajes
        BotonDragonBall btnVolver = new BotonDragonBall("Volver al menú");
        int anchoBoton = 140;
        int altoBoton = 32;
        btnVolver.setBounds((int) (ANCHO * BOTON_CENTRO_X) - anchoBoton / 2, (int) (ALTO * BOTON_Y), anchoBoton, altoBoton);
        btnVolver.addActionListener(e -> {
            if (alVolver != null) alVolver.run(); // Main muestra el menú principal
        });
        add(btnVolver);
    }

    /** Carga fondo4.jpg desde el classpath o, si no está, desde la carpeta del proyecto. */
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

    /** Carga clic.wav (classpath o carpeta del proyecto) y lo deja listo para reproducirse sin retraso. */
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

    /** Reproduce el sonido desde el inicio (si ya sonaba, lo reinicia para clics rápidos). */
    private void reproducirClic() {
        if (sonidoClic == null) return;
        sonidoClic.stop();
        sonidoClic.setFramePosition(0);
        sonidoClic.start();
    }

    /** Dibuja la imagen ocupando todo el panel (antes que los componentes hijos). */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
            g2.dispose();
        } else {
            g.setColor(new Color(30, 20, 50)); // color de respaldo si falta la imagen
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Crea un panel con una cuadrícula de n x n celdas. */
    private JPanel crearPanelMatriz(JLabel[][] celdas, String titulo) {
        int n = celdas.length;

        // GridLayout(filas, columnas, espacioH, espacioV)
        JPanel panel = new JPanel(new GridLayout(n, n, 2, 2));
        panel.setOpaque(false); // deja ver el fondo entre las celdas
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), titulo,
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), Color.WHITE);
        panel.setBorder(borde);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Celda con fondo semitransparente: se ve el fondo detrás, pero el texto se lee bien
                JLabel celda = new JLabel("", SwingConstants.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(new Color(25, 15, 45, 170));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                celda.setOpaque(false);
                celda.setFont(new Font("Arial", Font.BOLD, 20));
                celda.setForeground(Color.WHITE);
                celda.setBorder(BorderFactory.createLineBorder(new Color(150, 120, 200)));
                celda.setCursor(new Cursor(Cursor.HAND_CURSOR));
                celda.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        reproducirClic(); // suena al tocar la celda
                    }
                });
                celdas[i][j] = celda;
                panel.add(celda); // GridLayout las acomoda fila por fila
            }
        }
        return panel;
    }

    // Setter para que Main conecte la navegación
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}