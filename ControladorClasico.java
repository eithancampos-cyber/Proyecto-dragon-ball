import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Controlador del modo clásico.
 * Recibe los clics de la vista, se los pasa al modelo y actualiza la vista
 * con lo que responde el modelo. Las reglas están todas en ModeloClasico.
 */
public class ControladorClasico {

    private static final int PERIODO_RELOJ_MS = 100; // cada cuánto se le avisa al modelo que pasó el tiempo

    private final ModeloClasico modelo;
    private final InterfazMatrices vista;
    private Timer temporizadorFallo;
    private long ultimoTick;

    public ControladorClasico(ModeloClasico modelo, InterfazMatrices vista) {
        this.modelo = modelo;
        this.vista = vista;

        vista.setOyenteCelda(this::alTocarCelda);
        // al volver al menú se descarta la partida
        vista.getBtnVolver().addActionListener(e -> reiniciar());
        // cada vez que se abre esta pantalla se muestra de quién es el turno
        vista.setAlMostrarse(this::anunciarTurnoActual);

        ultimoTick = System.currentTimeMillis();
        new Timer(PERIODO_RELOJ_MS, e -> latidoReloj()).start();

        reiniciar();
    }

    /** Empieza una partida nueva. */
    private void reiniciar() {
        if (temporizadorFallo != null) temporizadorFallo.stop();
        modelo.reiniciar();
        vista.reiniciarTablero();
        actualizarMarcadores();
        refrescarTurno();
    }

    /** Le dice al modelo cuánto tiempo pasó (solo si el modo clásico se está viendo). */
    private void latidoReloj() {
        long ahora = System.currentTimeMillis();
        long dt = Math.min(ahora - ultimoTick, 250);
        ultimoTick = ahora;
        if (!vista.isShowing() || modelo.isTerminado()) return;

        // se guarda la carta volteada antes, por si se acaba el tiempo y hay que taparla
        int fila = modelo.getFilaSeleccionada();
        int col = modelo.getColSeleccionada();
        int jugador = modelo.getTurno();

        if (modelo.descontar(dt) && fila != -1) {
            vista.ocultarCarta(jugador, fila, col);
        }
        actualizarMarcadores();
        refrescarTurno();
    }

    private void alTocarCelda(int jugador, int fila, int col) {
        // se guarda la primera carta antes de voltear porque el modelo la borra si es pareja
        final int fila1 = modelo.getFilaSeleccionada();
        final int col1 = modelo.getColSeleccionada();

        ModeloClasico.Resultado resultado = modelo.voltear(jugador, fila, col);
        if (resultado == ModeloClasico.Resultado.IGNORADO) return;

        vista.mostrarCarta(jugador, fila, col, modelo.getCarta(jugador, fila, col));

        if (resultado == ModeloClasico.Resultado.PAREJA) {
            vista.marcarEncontrada(jugador, fila1, col1);
            vista.marcarEncontrada(jugador, fila, col);
        } else if (resultado == ModeloClasico.Resultado.NO_PAREJA) {
            // las cartas se ven un momento y luego se tapan
            temporizadorFallo = new Timer((int) ModeloClasico.MS_OCULTAR_FALLO, e -> {
                vista.ocultarCarta(jugador, fila1, col1);
                vista.ocultarCarta(jugador, fila, col);
                modelo.resolverFallo();
                actualizarMarcadores();
                refrescarTurno();
            });
            temporizadorFallo.setRepeats(false);
            temporizadorFallo.start();
        }

        actualizarMarcadores();
        refrescarTurno();
        if (modelo.isTerminado()) {
            SwingUtilities.invokeLater(this::terminarPartida);
        }
    }

    private void actualizarMarcadores() {
        boolean fin = modelo.isTerminado();
        for (int j = 0; j < 2; j++) {
            boolean suTurno = !fin && modelo.getTurno() == j;
            vista.actualizarMarcador(j, modelo.getPares(j), modelo.getIntentos(j), suTurno,
                    suTurno ? modelo.getSegundosRestantes() : -1);
        }
    }

    /** Si cambió el turno, se muestra el aviso. */
    private void refrescarTurno() {
        if (modelo.consumirCambioTurno() && !modelo.isTerminado()) anunciarTurnoActual();
    }

    private void anunciarTurnoActual() {
        if (!modelo.isTerminado()) vista.mostrarTurno(modelo.getTurno(), (int) ModeloClasico.MS_BANNER_TURNO);
    }

    private void terminarPartida() {
        // aquí se puede poner la animación de gane (modelo.getGanador() dice quién ganó)
        boolean otraPartida = vista.preguntarOtraPartida(modelo.getMensajeFinal());
        reiniciar();
        if (!otraPartida) vista.volverAlMenu();
    }
}