import java.awt.Window;
import javax.swing.SwingUtilities;

/**
 * CONTROLADOR de los Mejores Jugadores.
 * Cuando se pulsa "Mejores Jugadores" en el menú principal, le pide el Top 5 al modelo
 * (ModeloMejoresJugadores) y se lo entrega a la vista (VentanaMejoresJugadores).
 * Se crea un diálogo nuevo en cada clic, así siempre muestra el ranking actualizado.
 *
 * El registro de las victorias NO se hace aquí sino en ControladorClasico, que es quien
 * sabe cuándo termina una partida.
 */
public class ControladorMejoresJugadores {

    // Referencias al modelo que gestiona los puntajes y a la vista del menú principal
    private final ModeloMejoresJugadores modelo;
    private final VentanaPrincipal menu;

    /**
     * Constructor del controlador: guarda las referencias a modelo y vista
     * y registra el listener del botón "Mejores Jugadores" en el menú.
     */
    public ControladorMejoresJugadores(ModeloMejoresJugadores modelo, VentanaPrincipal menu) {
        this.modelo = modelo;
        this.menu = menu;
        // Cada vez que se presiona el botón en el menú, se dispara mostrar()
        menu.getBtnMejoresJugadores().addActionListener(e -> mostrar());
    }

    /**
     * Obtiene la ventana contenedora (JFrame/Window) del menú, consulta el Top actualizado
     * en el modelo y abre una nueva instancia modal de VentanaMejoresJugadores.
     */
    private void mostrar() {
        Window padre = SwingUtilities.getWindowAncestor(menu);
        new VentanaMejoresJugadores(padre, modelo.getTop()).setVisible(true);
    }
}