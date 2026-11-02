import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * CONTROLADOR del Modo Clásico.
 * Recibe los clics de la vista, se los pasa al modelo y actualiza la vista
 * según el resultado. Ni el modelo ni la vista se conocen entre sí.
 */
public class ControladorClasico {

    private static final int RETARDO_MS = 900; // tiempo que se ven dos cartas distintas antes de ocultarse

    private final ModeloClasico modelo;
    private final InterfazMatrices vista;
    private Timer temporizador;

    public ControladorClasico(ModeloClasico modelo, InterfazMatrices vista) {
        this.modelo = modelo;
        this.vista = vista;

        vista.setOyenteCelda(this::alTocarCelda);
        // Al salir al menú se descarta la partida, así cada entrada al modo empieza de cero
        vista.getBtnVolver().addActionListener(e -> reiniciar());

        reiniciar();
    }

    /** Nueva partida: baraja, oculta todo y refresca los marcadores. */
    private void reiniciar() {
        if (temporizador != null) temporizador.stop();
        modelo.reiniciar();
        vista.reiniciarTablero();
        actualizarMarcadores();
    }

    private void alTocarCelda(int jugador, int fila, int col) {
        // La primera carta (si existe) se lee ANTES de voltear, porque el modelo la limpia al acertar
        final int fila1 = modelo.getFilaSeleccionada();
        final int col1 = modelo.getColSeleccionada();

        ModeloClasico.Resultado resultado = modelo.voltear(jugador, fila, col);
        if (resultado == ModeloClasico.Resultado.IGNORADO) return;

        vista.mostrarCarta(jugador, fila, col, modelo.getCarta(jugador, fila, col));

        if (resultado == ModeloClasico.Resultado.PAREJA) {
            vista.marcarEncontrada(jugador, fila1, col1);
            vista.marcarEncontrada(jugador, fila, col);
        } else if (resultado == ModeloClasico.Resultado.NO_PAREJA) {
            // Se dejan ver un momento y luego se ocultan; mientras tanto el modelo bloquea más clics
            temporizador = new Timer(RETARDO_MS, e -> {
                vista.ocultarCarta(jugador, fila1, col1);
                vista.ocultarCarta(jugador, fila, col);
                modelo.resolverFallo();
                actualizarMarcadores();
            });
            temporizador.setRepeats(false);
            temporizador.start();
        }

        actualizarMarcadores();
        if (modelo.isTerminado()) {
            SwingUtilities.invokeLater(this::terminarPartida); // deja que se pinte la última pareja primero
        }
    }

    private void actualizarMarcadores() {
        boolean fin = modelo.isTerminado();
        for (int j = 0; j < 2; j++) {
            vista.actualizarMarcador(j, modelo.getPares(j), modelo.getIntentos(j), !fin && modelo.getTurno() == j);
        }
    }

    private void terminarPartida() {
        int ganador = modelo.getGanador();
        String detalle = "Jugador 1: " + modelo.getIntentos(0) + " intentos\n"
                       + "Jugador 2: " + modelo.getIntentos(1) + " intentos";
        String mensaje = (ganador == -1)
                ? "¡Empate!\n" + detalle
                : "¡Gana el Jugador " + (ganador + 1) + "!\n" + detalle;

        if (vista.preguntarOtraPartida(mensaje)) {
            reiniciar();
        } else {
            reiniciar();
            vista.volverAlMenu();
        }
    }
}