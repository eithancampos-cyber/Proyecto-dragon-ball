import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * CONTROLADOR del Modo Patrones.
 * Maneja el flujo del juego: mostrar el patrón, medir el tiempo mientras el jugador lo repite,
 * pasar de ronda o terminar. Las reglas viven en ModeloPatrones y el dibujo en MatrizPatrones.
 */
public class ControladorPatrones {

    private static final int RETARDO_INICIAL_MS = 800;   // pausa antes de mostrar el patrón
    private static final int PASO_MUESTRA_MS = 450;      // tiempo que una casilla está encendida / apagada
    private static final int PAUSA_ENTRE_RONDAS_MS = 1200;
    private static final int REFRESCO_RELOJ_MS = 50;

    private final ModeloPatrones modelo;
    private final MatrizPatrones vista;

    private final Timer temporizadorMuestra;  // enciende y apaga las casillas del patrón
    private final Timer reloj;                // revisa los límites de tiempo mientras el jugador juega
    private final Timer pausaRonda;           // pequeña pausa entre una ronda y la siguiente
    private int paso;                         // paso actual de la muestra (par = encender, impar = apagar)

    public ControladorPatrones(ModeloPatrones modelo, MatrizPatrones vista) {
        this.modelo = modelo;
        this.vista = vista;

        temporizadorMuestra = new Timer(PASO_MUESTRA_MS, e -> avanzarMuestra());
        reloj = new Timer(REFRESCO_RELOJ_MS, e -> alTicReloj());
        pausaRonda = new Timer(PAUSA_ENTRE_RONDAS_MS, e -> {
            modelo.siguienteRonda();
            empezarMuestra();
        });
        pausaRonda.setRepeats(false);

        vista.setOyenteCelda(this::alTocarCelda);
        vista.getBtnIniciar().addActionListener(e -> iniciarPartida());
        // Al salir al menú se descarta la partida, así cada entrada al modo empieza de cero
        vista.getBtnVolver().addActionListener(e -> reiniciar());

        reiniciar();
    }

    /** Vuelve al estado de espera: tablero limpio y botón "Iniciar" disponible. */
    private void reiniciar() {
        detenerTemporizadores();
        modelo.reiniciar();
        vista.pintarTablero(modelo.getTablero());
        vista.reiniciarTiempos();
        vista.actualizarMarcador(ModeloPatrones.CASILLAS_INICIALES, 0, 0);
        vista.setMensaje("Pulsa Iniciar.\nMemoriza el patrón\ny repítelo.");
        vista.setBotonIniciarVisible(true);
    }

    private void iniciarPartida() {
        detenerTemporizadores();
        modelo.iniciar();
        vista.setBotonIniciarVisible(false);
        empezarMuestra();
    }

    private void detenerTemporizadores() {
        temporizadorMuestra.stop();
        reloj.stop();
        pausaRonda.stop();
    }

    // ------------------------------------------------------------------
    // 1) Mostrar el patrón
    // ------------------------------------------------------------------

    private void empezarMuestra() {
        detenerTemporizadores();
        paso = 0;
        vista.reiniciarTiempos();
        vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), 0);
        vista.setMensaje("Memoriza el patrón\n(" + modelo.getLongitud() + " casillas)");
        vista.pintarTablero(modelo.getTablero());

        temporizadorMuestra.setInitialDelay(RETARDO_INICIAL_MS);
        temporizadorMuestra.start();
    }

    private void avanzarMuestra() {
        int i = paso / 2;
        if (paso % 2 == 0) {
            if (i >= modelo.getLongitud()) {      // ya se mostraron todas: le toca al jugador
                temporizadorMuestra.stop();
                comenzarTurnoJugador();
                return;
            }
            modelo.mostrar(i);                    // enciende la casilla i
            vista.reproducirClic();
        } else {
            modelo.limpiarTablero();              // apaga entre casilla y casilla
        }
        vista.pintarTablero(modelo.getTablero());
        paso++;
    }

    // ------------------------------------------------------------------
    // 2) El jugador repite el patrón
    // ------------------------------------------------------------------

    private void comenzarTurnoJugador() {
        modelo.comenzarTurno(System.currentTimeMillis());
        vista.pintarTablero(modelo.getTablero());
        vista.setMensaje("¡Tu turno!\nRepite el patrón\nen el mismo orden");
        alTicReloj();
        reloj.start();
    }

    private void alTicReloj() {
        long ahora = System.currentTimeMillis();
        if (modelo.comprobarTiempo(ahora)) {
            terminarPartida();
            return;
        }
        vista.actualizarTiempos(modelo.getMsRestantesTotal(ahora), modelo.getMsRestantesEntreCasillas(ahora));
    }

    private void alTocarCelda(int fila, int col) {
        ModeloPatrones.Resultado resultado = modelo.elegir(fila, col, System.currentTimeMillis());

        switch (resultado) {
            case IGNORADO:
                return;
            case ACIERTO:
                vista.pintarTablero(modelo.getTablero());
                vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), modelo.getIndice());
                break;
            case RONDA_COMPLETA:
                reloj.stop();
                vista.pintarTablero(modelo.getTablero());
                vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), modelo.getLongitud());
                vista.setMensaje("¡Correcto!\nSiguiente: " + (modelo.getLongitud() + 1) + " casillas");
                pausaRonda.start();
                break;
            case FALLO:
            case TIEMPO_AGOTADO:
                terminarPartida();
                break;
        }
    }

    // ------------------------------------------------------------------
    // 3) Fin de la partida
    // ------------------------------------------------------------------

    private void terminarPartida() {
        detenerTemporizadores();
        vista.pintarTablero(modelo.getTablero());
        vista.reiniciarTiempos();

        String motivo;
        switch (modelo.getMotivoFin()) {
            case TIEMPO_TOTAL:
                motivo = "Se acabaron los " + (ModeloPatrones.TIEMPO_TOTAL_MS / 1000) + " segundos para repetir el patrón.";
                break;
            case TIEMPO_ENTRE_CASILLAS:
                motivo = "Pasaron más de " + (ModeloPatrones.TIEMPO_ENTRE_CASILLAS_MS / 1000) + " segundos entre dos casillas.";
                break;
            default:
                motivo = "Tocaste una casilla que no era del patrón.";
        }

        int mayor = modelo.getMayorPatronLogrado();
        String mensaje = "¡Fin del juego!\n" + motivo
                + "\n\nRondas superadas: " + modelo.getRondasCompletadas()
                + "\nPatrón más largo repetido: " + (mayor == 0 ? "ninguno" : mayor + " casillas");
        vista.setMensaje("¡Fin del juego!\nRondas superadas: " + modelo.getRondasCompletadas());

        // Deja que se pinte la casilla fallada antes de mostrar el diálogo
        SwingUtilities.invokeLater(() -> {
            if (vista.preguntarOtraPartida(mensaje)) {
                iniciarPartida();
            } else {
                reiniciar();
                vista.volverAlMenu();
            }
        });
    }
}