import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * Modo Patrones: muestra una matriz de 5x5 grande sobre fondo5.jpg.
 * Cada celda suena (clic.wav) al tocarla. Es un JPanel para la ventana única de Main.
 */
public class MatrizPatrones extends JPanel {

    private static final int ANCHO = 960, ALTO = 540; // mismo tamaño que el área útil de la ventana

    private final Image fondo = Login.cargarImagen("fondo5.jpg"); // reutiliza el cargador de Login
    private final JLabel[][] matriz5 = new JLabel[5][5];
    private Clip sonidoClic;
    private Runnable alVolver; // navegación que Main le asigna

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

        // Botón arriba al centro
        BotonDragonBall btnVolver = new BotonDragonBall("Volver al menú");
        btnVolver.setBounds(ANCHO / 2 - 70, (int) (ALTO * 0.02), 140, 32);
        btnVolver.addActionListener(e -> { if (alVolver != null) alVolver.run(); });
        add(btnVolver);
    }

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
    private void reproducirClic() {
        if (sonidoClic == null) return;
        sonidoClic.stop();
        sonidoClic.setFramePosition(0);
        sonidoClic.start();
    }

    /** Dibuja la imagen cubriendo todo el panel sin deformarla (escala y recorta lo que sobra). */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo == null) {
            g.setColor(new Color(10, 30, 15)); // respaldo si falta la imagen
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        double escala = Math.max((double) getWidth() / fondo.getWidth(this), (double) getHeight() / fondo.getHeight(this));
        int w = (int) Math.round(fondo.getWidth(this) * escala), h = (int) Math.round(fondo.getHeight(this) * escala);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(fondo, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, this);
        g2.dispose();
    }

    /** Crea el panel con la cuadrícula de celdas (n x n según el arreglo). */
    private JPanel crearPanelMatriz(String titulo) {
        int n = matriz5.length;
        JPanel panel = new JPanel(new GridLayout(n, n, 4, 4));
        panel.setOpaque(false); // deja ver el fondo entre las celdas
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), titulo,
                TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.WHITE));

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matriz5[i][j] = crearCelda();
                panel.add(matriz5[i][j]);
            }
        }
        return panel;
    }

    /** Celda semitransparente que suena al tocarla. */
    private JLabel crearCelda() {
        JLabel celda = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(8, 25, 12, 175)); // fondo oscuro translúcido
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        celda.setOpaque(false);
        celda.setFont(new Font("Arial", Font.BOLD, 26));
        celda.setForeground(Color.WHITE);
        celda.setBorder(BorderFactory.createLineBorder(new Color(150, 210, 120), 2));
        celda.setCursor(new Cursor(Cursor.HAND_CURSOR));
        celda.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { reproducirClic(); }
        });
        return celda;
    }

    // Setter para que Main conecte la navegación
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}