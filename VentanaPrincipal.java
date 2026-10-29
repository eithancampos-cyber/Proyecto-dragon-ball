import java.awt.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;

public class VentanaPrincipal extends JFrame {

    // Misma proporción 16:9 de la imagen
    private static final int ANCHO = 960;
    private static final int ALTO = 540;

    private final JButton btnAjustes;
    private final JButton btnMejoresJugadores;
    private final JButton btnModoClasico;
    private final JButton btnModoPatrones;

    // Estado visual de la música (solo visual por ahora)
    private boolean musicaActiva = true;

    public VentanaPrincipal() {
        setTitle("Dragon Ball Super - Memory Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        PanelFondo fondo = new PanelFondo(cargarImagen("fondo.jpg"));
        fondo.setPreferredSize(new Dimension(ANCHO, ALTO));
        fondo.setLayout(null);
        setContentPane(fondo);

        // Extremo superior izquierdo: Ajustes
        btnAjustes = new BotonDragonBall("Ajustes");
        btnAjustes.setBounds(15, 15, 130, 40);
        btnAjustes.addActionListener(e -> {
            VentanaAjustes ajustes = new VentanaAjustes(this, musicaActiva);
            ajustes.setVisible(true);
            musicaActiva = ajustes.isMusicaActiva();
        });
        fondo.add(btnAjustes);

        // Extremo superior derecho: Mejores jugadores
        btnMejoresJugadores = new BotonDragonBall("Mejores Jugadores");
        btnMejoresJugadores.setBounds(ANCHO - 195, 15, 180, 40);
        btnMejoresJugadores.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se mostraría el top 5 con sus premios");
        });
        fondo.add(btnMejoresJugadores);

        // Centro: botones de modo, lado a lado
        btnModoClasico = new BotonDragonBall("Modo Clásico");
        btnModoClasico.setFont(new Font("Arial", Font.BOLD, 20));
        btnModoClasico.setBounds(225, 400, 240, 60);
        btnModoClasico.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí iniciaría el modo clásico (2 jugadores)");
        });
        fondo.add(btnModoClasico);

        btnModoPatrones = new BotonDragonBall("Modo Patrones");
        btnModoPatrones.setFont(new Font("Arial", Font.BOLD, 20));
        btnModoPatrones.setBounds(495, 400, 240, 60);
        btnModoPatrones.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí iniciaría el modo patrones (1 jugador)");
        });
        fondo.add(btnModoPatrones);

        pack();
        setLocationRelativeTo(null);
    }

    // Getters para que el controlador asigne los listeners (MVC)
    public JButton getBtnAjustes() { return btnAjustes; }
    public JButton getBtnMejoresJugadores() { return btnMejoresJugadores; }
    public JButton getBtnModoClasico() { return btnModoClasico; }
    public JButton getBtnModoPatrones() { return btnModoPatrones; }

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

    // Panel que dibuja la imagen escalada al tamaño de la ventana
    private static class PanelFondo extends JPanel {
        private final Image imagen;

        PanelFondo(Image imagen) {
            this.imagen = imagen;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagen != null) {
                g.drawImage(imagen, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(new Color(26, 16, 40));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaPrincipal().setVisible(true);
        });
    }
}