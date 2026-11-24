import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * CONTROLADOR del Modo Patrones.
 * Maneja el flujo del juego: mostrar el patrón, medir el tiempo mientras el jugador lo repite,
 * pasar de ronda o terminar. Las reglas viven en ModeloPatrones y el dibujo en MatrizPatrones.
 */
public class ControladorPatrones {

    // Constantes de tiempo (en milisegundos) para controlar el ritmo de cada fase del juego
    private static final int RETARDO_INICIAL_MS = 800;   // pausa antes de mostrar el patrón
    private static final int PASO_MUESTRA_MS = 450;      // tiempo que una casilla está encendida / apagada
    private static final int PAUSA_ENTRE_RONDAS_MS = 1200; // espera breve tras completar una ronda antes de avanzar
    private static final int REFRESCO_RELOJ_MS = 50;     // frecuencia de actualización visual del cronómetro (20 veces/seg)

    // Referencias a los otros dos componentes del patrón MVC
    private final ModeloPatrones modelo;
    private final MatrizPatrones vista;

    // Timers de Swing (se ejecutan en el Event Dispatch Thread, seguro para actualizar la UI)
    private final Timer temporizadorMuestra;  // enciende y apaga las casillas del patrón
    private final Timer reloj;                // revisa los límites de tiempo mientras el jugador juega
    private final Timer pausaRonda;           // pequeña pausa entre una ronda y la siguiente
    private int paso;                         // paso actual de la muestra (par = encender, impar = apagar)

    /**
     * Constructor del controlador: inicializa los temporizadores, conecta los eventos
     * de la vista (clics en celdas y botones) y deja el juego en estado inicial.
     */
    public ControladorPatrones(ModeloPatrones modelo, MatrizPatrones vista) {
        this.modelo = modelo;
        this.vista = vista;

        // Configura cada Timer con su acción correspondiente mediante expresiones lambda
        temporizadorMuestra = new Timer(PASO_MUESTRA_MS, e -> avanzarMuestra());
        reloj = new Timer(REFRESCO_RELOJ_MS, e -> alTicReloj());
        pausaRonda = new Timer(PAUSA_ENTRE_RONDAS_MS, e -> {
            modelo.siguienteRonda();
            empezarMuestra();
        });
        // Evita que la pausa entre rondas se repita en bucle; solo se dispara una vez por ronda ganada
        pausaRonda.setRepeats(false);

        // Registra los callbacks/listeners de interacción en la vista
        vista.setOyenteCelda(this::alTocarCelda);
        vista.getBtnIniciar().addActionListener(e -> iniciarPartida());
        // Al salir al menú se descarta la partida, así cada entrada al modo empieza de cero
        vista.getBtnVolver().addActionListener(e -> reiniciar());

        // Deja la pantalla lista para empezar desde cero
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

    /** Arranca una nueva partida: limpia timers, resetea el modelo, oculta Iniciar y muestra el patrón. */
    private void iniciarPartida() {
        detenerTemporizadores();
        modelo.iniciar();
        vista.setBotonIniciarVisible(false);
        empezarMuestra();
    }

    /** Detiene todos los temporizadores activos para evitar disparos en momentos indebidos. */
    private void detenerTemporizadores() {
        temporizadorMuestra.stop();
        reloj.stop();
        pausaRonda.stop();
    }

    // ------------------------------------------------------------------
    // 1) Mostrar el patrón
    // ------------------------------------------------------------------

    /** Prepara y arranca la secuencia visual en la que se ilumina el patrón a memorizar. */
    private void empezarMuestra() {
        detenerTemporizadores();
        paso = 0; // Reinicia el contador de pasos (encender/apagar)
        vista.reiniciarTiempos();
        vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), 0);
        vista.setMensaje("Memoriza el patrón\n(" + modelo.getLongitud() + " casillas)");
        vista.pintarTablero(modelo.getTablero());

        // Aplica el retardo inicial solo antes del primer tic del temporizador
        temporizadorMuestra.setInitialDelay(RETARDO_INICIAL_MS);
        temporizadorMuestra.start();
    }

    /**
     * Ejecutado en cada tic de temporizadorMuestra.
     * Como cada casilla requiere encenderse y luego apagarse, se usan 2 pasos por casilla:
     * - paso par: enciende la casilla correspondiente (o pasa al turno del jugador si ya terminó).
     * - paso impar: apaga el tablero para separar visualmente una casilla de la siguiente.
     */
    private void avanzarMuestra() {
        int i = paso / 2; // Índice real dentro del patrón actual
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

    /** Inicia la fase de respuesta del jugador, marca la hora de inicio y activa el reloj. */
    private void comenzarTurnoJugador() {
        modelo.comenzarTurno(System.currentTimeMillis());
        vista.pintarTablero(modelo.getTablero());
        vista.setMensaje("¡Tu turno!\nRepite el patrón\nen el mismo orden");
        // Actualiza los tiempos de inmediato antes de esperar el primer tic de 50 ms
        alTicReloj();
        reloj.start();
    }

    /**
     * Revisa periódicamente si se agotó alguno de los límites de tiempo
     * y actualiza los contadores restantes en la vista.
     */
    private void alTicReloj() {
        long ahora = System.currentTimeMillis();
        if (modelo.comprobarTiempo(ahora)) {
            terminarPartida();
            return;
        }
        vista.actualizarTiempos(modelo.getMsRestantesTotal(ahora), modelo.getMsRestantesEntreCasillas(ahora));
    }

    /**
     * Responde cuando el jugador hace clic/toca una celda (fila, col).
     * Consulta al modelo qué ocurrió y actualiza la vista o el flujo según el resultado.
     */
    private void alTocarCelda(int fila, int col) {
        ModeloPatrones.Resultado resultado = modelo.elegir(fila, col, System.currentTimeMillis());

        switch (resultado) {
            case IGNORADO:
                // Por ejemplo, si se toca el tablero cuando no es turno del jugador
                return;
            case ACIERTO:
                // Casilla correcta, pero aún faltan casillas por repetir en esta ronda
                vista.pintarTablero(modelo.getTablero());
                vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), modelo.getIndice());
                break;
            case RONDA_COMPLETA:
                // Completó todo el patrón de la ronda: detiene el reloj y programa la siguiente ronda
                reloj.stop();
                vista.pintarTablero(modelo.getTablero());
                vista.actualizarMarcador(modelo.getLongitud(), modelo.getRondasCompletadas(), modelo.getLongitud());
                vista.setMensaje("¡Correcto!\nSiguiente: " + (modelo.getLongitud() + 1) + " casillas\n(el mismo patrón + 1)");
                pausaRonda.start();
                break;
            case FALLO:
            case TIEMPO_AGOTADO:
                // Error de casilla o se acabó el tiempo al intentar elegir
                terminarPartida();
                break;
        }
    }

    // ------------------------------------------------------------------
    // 3) Fin de la partida
    // ------------------------------------------------------------------

    /**
     * Cierra la partida actual, arma el resumen con el motivo de derrota y estadísticas,
     * y pregunta al usuario si desea jugar de nuevo o regresar al menú.
     */
    private void terminarPartida() {
        detenerTemporizadores();
        vista.pintarTablero(modelo.getTablero());
        vista.reiniciarTiempos();

        // Traduce el enum del motivo de fin a un mensaje legible para el jugador
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
        // (invokeLater encola el JOptionPane/diálogo después de que Swing procese el repintado)
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