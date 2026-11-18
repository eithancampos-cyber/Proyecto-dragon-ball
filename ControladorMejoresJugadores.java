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

    private final ModeloMejoresJugadores modelo;
    private final VentanaPrincipal menu;

    public ControladorMejoresJugadores(ModeloMejoresJugadores modelo, VentanaPrincipal menu) {
        this.modelo = modelo;
        this.menu = menu;
        menu.getBtnMejoresJugadores().addActionListener(e -> mostrar());
    }

    private void mostrar() {
        Window padre = SwingUtilities.getWindowAncestor(menu);
        new VentanaMejoresJugadores(padre, modelo.getTop()).setVisible(true);
    }
}
