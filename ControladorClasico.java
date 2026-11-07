import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Controlador del modo clásico.
 * Recibe los clics de la vista, se los pasa al modelo y actualiza la vista
 * con lo que responde el modelo. Las reglas están todas en ModeloClasico.
 *
 * Animaciones (el estado vive en el modelo y la vista las dibuja; este controlador solo
 * las hace avanzar con un Timer y avisa a la vista):
 *  - INTRO: al empezar cada partida, sobre las dos matrices a la vez (el juego y el reloj
 *    esperan a que terminen).
 *  - VICTORIA / DERROTA: al terminar la partida, el ganador celebra (con su nombre debajo de
 *    "VICTORIA") y el otro pierde (en empate los dos pierden).
 *
 * Pausa: la vista avisa con setAlPausar / setAlReanudar. Mientras está en pausa el reloj y las
 * animaciones no avanzan, y si había un par fallado esperando a taparse, ese temporizador se
 * detiene y se vuelve a arrancar al continuar.
 *
 * Botones de prueba: "Gana Jugador 1" y "Gana Jugador 2" fuerzan el final de la partida
 * para ver las animaciones de victoria y derrota sin tener que jugar.
 */
public class ControladorClasico {

    private static final int PERIODO_RELOJ_MS = 100; // cada cuánto se le avisa al modelo que pasó el tiempo

    private final ModeloClasico modelo;
    private final InterfazMatrices vista;
    private Timer temporizadorFallo;
    private final Runnable[] alTerminarAnimacion = new Runnable[2]; // qué hacer cuando termina la animación de cada jugador
    private long ultimoTick;
    private boolean introPendiente; // true = falta mostrar la intro de la partida actual
    private boolean finalizando;    // true = la partida ya terminó y se están mostrando las animaciones finales
    private boolean falloPausado;   // true = la pausa detuvo el temporizador que tapa las cartas falladas

    public ControladorClasico(ModeloClasico modelo, InterfazMatrices vista) {
        this.modelo = modelo;
        this.vista = vista;

        vista.setOyenteCelda(this::alTocarCelda);
        // pausa: el menú de la vista avisa cuando se pausa y cuando se continúa
        vista.setAlPausar(this::alPausar);
        vista.setAlReanudar(this::alReanudar);
        // al volver al menú se descarta la partida
        vista.getBtnVolver().addActionListener(e -> reiniciar());
        // botones de prueba: fuerzan que gane el jugador 1 o el jugador 2
        vista.getBtnGanaJ1().addActionListener(e -> forzarGanador(0));
        vista.getBtnGanaJ2().addActionListener(e -> forzarGanador(1));
        // cada vez que se abre esta pantalla: intro si la partida es nueva, o aviso de turno si no
        vista.setAlMostrarse(this::arrancarPartida);

        ultimoTick = System.currentTimeMillis();
        new Timer(PERIODO_RELOJ_MS, e -> latidoReloj()).start();
        new Timer(ModeloClasico.PERIODO_ANIMACION_MS, e -> latidoAnimacion()).start();

        reiniciar();
    }

    /** Deja todo listo para una partida nueva (la intro se reproduce en arrancarPartida). */
    private void reiniciar() {
        if (temporizadorFallo != null) temporizadorFallo.stop();
        falloPausado = false; // una partida nueva no debe retomar el par fallado de la anterior
        alTerminarAnimacion[0] = null;
        alTerminarAnimacion[1] = null;
        vista.detenerAnimacion();
        modelo.reiniciar(); // también quita las animaciones del modelo
        modelo.consumirCambioTurno();  // el aviso de turno sale después de la intro
        modelo.setBloqueado(true);     // no se juega mientras no termine la intro
        introPendiente = true;
        finalizando = false;
        vista.reiniciarTablero();
        actualizarMarcadores();
    }

    /** Reproduce la intro si la partida es nueva; si no, solo anuncia el turno. */
    private void arrancarPartida() {
        if (!introPendiente) {
            anunciarTurnoActual();
            return;
        }
        introPendiente = false;

        // intro de los dos personajes a la vez; cuando terminan los dos empieza la partida
        Runnable finIntro = juntar(2, () -> {
            modelo.setBloqueado(false);
            anunciarTurnoActual();
        });
        reproducirAnimacion(0, ModeloClasico.Animacion.INTRO, finIntro);
        reproducirAnimacion(1, ModeloClasico.Animacion.INTRO, finIntro);
    }

    // ---------- Animaciones ----------

    /** Empieza una animación en la matriz de un jugador; 'alTerminar' (puede ser null) se ejecuta al acabar. */
    private void reproducirAnimacion(int jugador, ModeloClasico.Animacion animacion, Runnable alTerminar) {
        modelo.iniciarAnimacion(jugador, animacion);
        alTerminarAnimacion[jugador] = alTerminar;
        enviarAnimacionAVista(jugador);
    }

    /** Pausa: detiene el temporizador de las cartas falladas si estaba corriendo. */
    private void alPausar() {
        if (temporizadorFallo != null && temporizadorFallo.isRunning()) {
            temporizadorFallo.stop();
            falloPausado = true;
        }
    }

    /** Continuar: retoma el temporizador de las cartas falladas si la pausa lo había detenido. */
    private void alReanudar() {
        if (falloPausado && temporizadorFallo != null) {
            falloPausado = false;
            temporizadorFallo.start();
        }
    }

    /** Hace avanzar un cuadro las animaciones que estén corriendo. */
    private void latidoAnimacion() {
        if (vista.isPausado()) return; // en pausa las animaciones se congelan
        for (int j = 0; j < 2; j++) {
            if (!modelo.isAnimando(j)) continue;
            boolean termino = modelo.avanzarAnimacion(j);
            enviarAnimacionAVista(j);
            if (termino) {
                Runnable cb = alTerminarAnimacion[j];
                alTerminarAnimacion[j] = null;
                if (cb != null) cb.run();
            }
        }
    }

    /** Le pasa a la vista lo que tiene que dibujar para la animación del jugador. */
    private void enviarAnimacionAVista(int jugador) {
        vista.mostrarAnimacion(jugador, modelo.getAnimacion(jugador), modelo.getProgresoAnimacion(jugador),
                modelo.getCuadroAnimacion(jugador), modelo.getNombre(jugador), modelo.getParticulas(jugador));
    }

    /** Devuelve un Runnable que ejecuta 'accion' cuando se haya llamado 'veces' veces. */
    private static Runnable juntar(int veces, Runnable accion) {
        int[] faltan = {veces};
        return () -> {
            if (--faltan[0] == 0) accion.run();
        };
    }

    /** Le dice al modelo cuánto tiempo pasó (solo si el modo clásico se está viendo). */
    private void latidoReloj() {
        long ahora = System.currentTimeMillis();
        long dt = Math.min(ahora - ultimoTick, 250);
        ultimoTick = ahora;
        if (!vista.isShowing() || modelo.isTerminado() || vista.isPausado()) return; // en pausa el reloj no corre

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
        if (vista.isPausado()) return; // el menú de pausa ya tapa las celdas; esto es solo por seguridad
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
            SwingUtilities.invokeLater(() -> terminarPartida(modelo.getGanador(), modelo.getMensajeFinal()));
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

    /** Botón de prueba: termina la partida dándole la victoria al jugador indicado (0 o 1). */
    private void forzarGanador(int ganador) {
        if (vista.isPausado() || finalizando || modelo.isBloqueado()) return; // no vale durante la intro ni si ya se está terminando
        if (temporizadorFallo != null) temporizadorFallo.stop(); // si había un par fallado esperando, se descarta
        modelo.setBloqueado(true); // congela el juego y el reloj mientras suenan las animaciones
        terminarPartida(ganador, "¡Gana " + modelo.getNombre(ganador) + "!\n(victoria forzada con el botón de prueba)");
    }

    /** Termina la partida: ganador = 0, 1 o -1 (empate); 'mensaje' es lo que se muestra al final. */
    private void terminarPartida(int ganador, String mensaje) {
        if (finalizando) return; // por si se pide terminar dos veces
        finalizando = true;

        // el ganador celebra y el otro pierde; en empate pierden los dos
        ModeloClasico.Animacion animJ1 = (ganador == 0) ? ModeloClasico.Animacion.VICTORIA : ModeloClasico.Animacion.DERROTA;
        ModeloClasico.Animacion animJ2 = (ganador == 1) ? ModeloClasico.Animacion.VICTORIA : ModeloClasico.Animacion.DERROTA;

        // las animaciones se quedan en el último frame mientras se muestra el resultado
        Runnable fin = juntar(2, () -> {
            boolean otraPartida = vista.preguntarOtraPartida(mensaje);
            reiniciar();
            if (otraPartida) arrancarPartida();
            else vista.volverAlMenu();
        });
        reproducirAnimacion(0, animJ1, fin);
        reproducirAnimacion(1, animJ2, fin);
    }
}