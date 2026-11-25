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

    // Botones del menú. Se crean en el constructor y se exponen con getters para que los controladores
    // les asignen acciones.
    private final JButton btnAjustes;
    private final JButton btnMejoresJugadores;
    private final JButton btnModoClasico;
    private final JButton btnModoPatrones;

    // Imagen de fondo (fondo.jpg). Puede ser null si no se encontró; paintComponent lo contempla.
    private final Image fondo;

    // NUEVO: se eliminó "private boolean musicaActiva = true;" porque ahora el estado vive en GestorMusica

    // Acciones de navegación que Main le asigna
    // Runnable = bloque de código sin parámetros. Main lo guarda aquí y la vista lo ejecuta al presionar
    // el botón, así la vista no necesita conocer a Main.
    private Runnable alModoClasico;
    private Runnable alModoPatrones;
    private Runnable alCerrarSesion; // NUEVO: Acción para volver al Login

    public VentanaPrincipal() {
        // Carga el fondo y fija el tamaño preferido (960 x 540).
        fondo = cargarImagen("fondo.jpg");
        setPreferredSize(new Dimension(ANCHO, ALTO));
        // Layout nulo: cada botón se coloca con coordenadas fijas (setBounds).
        setLayout(null);

        // Extremo superior izquierdo: Ajustes
        btnAjustes = new BotonDragonBall("Ajustes");
        btnAjustes.setBounds(15, 15, 130, 40);
        btnAjustes.addActionListener(e -> {
            // Busca la ventana que contiene este panel para usarla como padre del diálogo de Ajustes. Si
            // no es un JFrame, el padre queda en null.
            Window ventana = SwingUtilities.getWindowAncestor(this);
            JFrame padre = (ventana instanceof JFrame) ? (JFrame) ventana : null;
            VentanaAjustes ajustes = new VentanaAjustes(padre, GestorMusica.isActiva()); // NUEVO: usa el estado real de la música
            // Ajustes es modal: esta línea no avanza hasta que el usuario cierre la ventana. Lo que
            // sigue se ejecuta después.
            ajustes.setVisible(true);

            // NUEVO: se eliminó "musicaActiva = ajustes.isMusicaActiva();" (el botón ya actualiza GestorMusica)

            // NUEVO: Verificamos si el usuario confirmó cerrar sesión en los ajustes
            if (ajustes.isCerrarSesionSolicitado()) {
                if (alCerrarSesion != null) {
                    alCerrarSesion.run();
                }
            }
        });
        add(btnAjustes);

        // Extremo superior derecho: Mejores jugadores
        btnMejoresJugadores = new BotonDragonBall("Mejores Jugadores");
        btnMejoresJugadores.setBounds(ANCHO - 195, 15, 180, 40);
        // la acción la asigna ControladorMejoresJugadores (muestra el Top 5 con sus premios)
        add(btnMejoresJugadores);

        // Centro: botones de modo, lado a lado
        // Botones de modo: solo avisan a Main mediante el Runnable. La comprobación != null evita
        // errores si Main aún no lo asignó.
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
    public void setAlCerrarSesion(Runnable accion) { this.alCerrarSesion = accion; } // NUEVO

    // Getters (MVC)
    public JButton getBtnAjustes() { return btnAjustes; }
    public JButton getBtnMejoresJugadores() { return btnMejoresJugadores; }
    public JButton getBtnModoClasico() { return btnModoClasico; }
    public JButton getBtnModoPatrones() { return btnModoPatrones; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Dibuja el fondo estirado al tamaño actual del panel; si no hay imagen, pinta un color sólido.
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(26, 16, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Busca la imagen como recurso y, si no, en rutas comunes del proyecto
    private Image cargarImagen(String nombre) {
        // 1) Intenta cargarla como recurso del classpath.
        URL url = getClass().getResource("/" + nombre);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        // 2) Si no, la busca como archivo en rutas comunes del proyecto (depende de desde dónde se
        // ejecute el programa).
        String[] rutas = {nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre};
        for (String ruta : rutas) {
            if (new File(ruta).exists()) {
                return new ImageIcon(ruta).getImage();
            }
        }
        // 3) Si nada funcionó, avisa por consola y devuelve null.
        System.err.println("No se encontró la imagen de fondo: " + nombre);
        return null;
    }
}