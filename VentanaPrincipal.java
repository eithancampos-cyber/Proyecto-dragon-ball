import java.awt.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;

/**
 * Menú principal como pantalla (JPanel) para mostrarse dentro de la ventana
 * única que crea Main. La navegación a los modos la asigna Main.
 */
public class VentanaPrincipal extends JPanel {

    // Misma proporción 16:9 de la imagen
    private static final int ANCHO = 960;
    private static final int ALTO = 540;

    private final JButton btnAjustes;
    private final JButton btnMejoresJugadores;
    private final JButton btnModoClasico;
    private final JButton btnModoPatrones;

    private final Image fondo;

    // Estado visual de la música (solo visual por ahora)
    private boolean musicaActiva = true;

    // Acciones de navegación que Main le asigna
    private Runnable alModoClasico;
    private Runnable alModoPatrones;

    public VentanaPrincipal() {
        fondo = cargarImagen("fondo.jpg");
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        // Extremo superior izquierdo: Ajustes
        btnAjustes = new BotonDragonBall("Ajustes");
        btnAjustes.setBounds(15, 15, 130, 40);
        btnAjustes.addActionListener(e -> {
            Window ventana = SwingUtilities.getWindowAncestor(this);
            JFrame padre = (ventana instanceof JFrame) ? (JFrame) ventana : null;
            VentanaAjustes ajustes = new VentanaAjustes(padre, musicaActiva);
            ajustes.setVisible(true);
            musicaActiva = ajustes.isMusicaActiva();
        });
        add(btnAjustes);

        // Extremo superior derecho: Mejores jugadores
        btnMejoresJugadores = new BotonDragonBall("Mejores Jugadores");
        btnMejoresJugadores.setBounds(ANCHO - 195, 15, 180, 40);
        btnMejoresJugadores.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se mostraría el top 5 con sus premios");
        });
        add(btnMejoresJugadores);

        // Centro: botones de modo, lado a lado
        btnModoClasico = new BotonDragonBall("Modo Clásico");
        btnModoClasico.setFont(new Font("Arial", Font.BOLD, 20));
        btnModoClasico.setBounds(225, 400, 240, 60);
        btnModoClasico.addActionListener(e -> {
            if (alModoClasico != null) alModoClasico.run();
        });
        add(btnModoClasico);

        btnModoPatrones = new BotonDragonBall("Modo Patrones");
        btnModoPatrones.setFont(new Font("Arial", Font.BOLD, 20));
        btnModoPatrones.setBounds(495, 400, 240, 60);
        btnModoPatrones.addActionListener(e -> {
            if (alModoPatrones != null) alModoPatrones.run();
        });
        add(btnModoPatrones);
    }

    // Setters para que Main conecte la navegación
    public void setAlModoClasico(Runnable accion) { this.alModoClasico = accion; }
    public void setAlModoPatrones(Runnable accion) { this.alModoPatrones = accion; }

    // Getters (MVC)
    public JButton getBtnAjustes() { return btnAjustes; }
    public JButton getBtnMejoresJugadores() { return btnMejoresJugadores; }
    public JButton getBtnModoClasico() { return btnModoClasico; }
    public JButton getBtnModoPatrones() { return btnModoPatrones; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(26, 16, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Busca la imagen como recurso y, si no, en rutas comunes del proyecto
    private Image cargarImagen(String nombre) {
        URL url = getClass().getResource("/" + nombre);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        String[] rutas = {nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre};
        for (String ruta : rutas) {
            if (new File(ruta).exists()) {
                return new ImageIcon(ruta).getImage();
            }
        }
        System.err.println("No se encontró la imagen de fondo: " + nombre);
        return null;
    }
}