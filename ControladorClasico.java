import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
 * Mejores Jugadores: al terminar una partida con ganador, se calcula su premio (APIbccr) y se
 * registra en ModeloMejoresJugadores. El Jugador 1 es el usuario que inició sesión; si gana el
 * Jugador 2 se le pide su nombre. Las victorias forzadas con los botones de prueba NO se registran.
 *
 * Botones de prueba: "Gana Jugador 1" y "Gana Jugador 2" fuerzan el final de la partida
 * para ver las animaciones de victoria y derrota sin tener que jugar.
 */
public class ControladorClasico {

    private static final int PERIODO_RELOJ_MS = 100; // cada cuánto se le avisa al modelo que pasó el tiempo

    // Piezas del patrón MVC: el modelo (reglas), la vista (InterfazMatrices) y el ranking de mejores
    // jugadores.
    private final ModeloClasico modelo;
    private final InterfazMatrices vista;
    private final ModeloMejoresJugadores ranking;
    private final Supplier<String> usuarioActual; // nombre del usuario con sesión iniciada (puede ser null)
    // Timer de Swing que tapa las dos cartas distintas después de MS_OCULTAR_FALLO (900 ms).
    private Timer temporizadorFallo;
    private final Runnable[] alTerminarAnimacion = new Runnable[2]; // qué hacer cuando termina la animación de cada jugador
    // Momento (en ms) del último latido del reloj; sirve para calcular cuánto tiempo real pasó.
    private long ultimoTick;
    private boolean introPendiente; // true = falta mostrar la intro de la partida actual
    private boolean finalizando;    // true = la partida ya terminó y se están mostrando las animaciones finales
    private boolean falloPausado;   // true = la pausa detuvo el temporizador que tapa las cartas falladas

    public ControladorClasico(ModeloClasico modelo, InterfazMatrices vista,
                              ModeloMejoresJugadores ranking, Supplier<String> usuarioActual) {
        // Guarda las referencias recibidas.
        this.modelo = modelo;
        this.vista = vista;
        this.ranking = ranking;
        this.usuarioActual = usuarioActual;

        // Conecta vista y controlador: cada clic en una celda llega a alTocarCelda (referencia a
        // método).
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

        // Marca de tiempo inicial del reloj.
        ultimoTick = System.currentTimeMillis();
        // Dos Timers que corren siempre: uno para el reloj de turnos (cada 100 ms) y otro para las
        // animaciones (cada 33 ms, unos 30 cuadros por segundo).
        new Timer(PERIODO_RELOJ_MS, e -> latidoReloj()).start();
        new Timer(ModeloClasico.PERIODO_ANIMACION_MS, e -> latidoAnimacion()).start();

        // Deja modelo y vista en el estado inicial.
        reiniciar();
    }

    /** Deja todo listo para una partida nueva (la intro se reproduce en arrancarPartida). */
    private void reiniciar() {
        // Si había un par fallado esperando a taparse, se cancela.
        if (temporizadorFallo != null) temporizadorFallo.stop();
        falloPausado = false; // una partida nueva no debe retomar el par fallado de la anterior
        alTerminarAnimacion[0] = null;
        alTerminarAnimacion[1] = null;
        vista.detenerAnimacion();
        modelo.reiniciar(); // también quita las animaciones del modelo
        modelo.consumirCambioTurno();  // el aviso de turno sale después de la intro
        modelo.setBloqueado(true);     // no se juega mientras no termine la intro
        // Falta la intro: se reproduce cuando la pantalla se muestre (arrancarPartida).
        introPendiente = true;
        finalizando = false;
        // Tapa todas las celdas y deja visible solo el tablero del jugador que empieza.
        vista.reiniciarTablero();
        vista.mostrarSoloMatriz(modelo.getTurno());
        actualizarMarcadores();
    }

    /** Reproduce la intro si la partida es nueva; si no, solo anuncia el turno. */
    private void arrancarPartida() {
        // el Jugador 1 es quien inició sesión (puede haber cambiado de usuario desde la última partida)
        String usuario = usuarioActual.get();
        modelo.setNombre(0, (usuario == null || usuario.trim().isEmpty()) ? "Jugador 1" : usuario);
        // Si la intro ya se reprodujo, solo se vuelve a anunciar el turno.
        if (!introPendiente) {
            anunciarTurnoActual();
            return;
        }
        introPendiente = false;

        // intro de los dos personajes a la vez; cuando terminan los dos empieza la partida
        // juntar(2, ...) devuelve un Runnable que ejecuta su código recién en la segunda llamada, o sea
        // cuando terminan las intros de los dos jugadores.
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
        // El modelo guarda el estado de la animación; aquí se guarda qué hacer al terminar y se envía el
        // primer cuadro a la vista.
        modelo.iniciarAnimacion(jugador, animacion);
        alTerminarAnimacion[jugador] = alTerminar;
        enviarAnimacionAVista(jugador);
    }

    /** Pausa: detiene el temporizador de las cartas falladas si estaba corriendo. */
    private void alPausar() {
        if (temporizadorFallo != null && temporizadorFallo.isRunning()) {
            temporizadorFallo.stop();
            // Se recuerda que fue la pausa quien lo detuvo, para reanudarlo después.
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
        // Recorre a los dos jugadores y avanza un cuadro la animación de quien la tenga activa.
        for (int j = 0; j < 2; j++) {
            if (!modelo.isAnimando(j)) continue;
            // 'termino' es true solo en el último cuadro.
            boolean termino = modelo.avanzarAnimacion(j);
            enviarAnimacionAVista(j);
            if (termino) {
                // Se vacía antes de ejecutarlo para que corra una sola vez (y pueda registrar otra
                // animación).
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
        // Arreglo de un elemento: truco para modificar el contador dentro de la lambda (las variables
        // locales capturadas deben ser final).
        int[] faltan = {veces};
        return () -> {
            if (--faltan[0] == 0) accion.run();
        };
    }

    /** Le dice al modelo cuánto tiempo pasó (solo si el modo clásico se está viendo). */
    private void latidoReloj() {
        // dt = tiempo real desde el último latido. Se limita a 250 ms para que, si la ventana se congela
        // un rato, no se descuente de golpe.
        long ahora = System.currentTimeMillis();
        long dt = Math.min(ahora - ultimoTick, 250);
        ultimoTick = ahora;
        if (!vista.isShowing() || modelo.isTerminado() || vista.isPausado()) return; // en pausa el reloj no corre

        // se guarda la carta volteada antes, por si se acaba el tiempo y hay que taparla
        int fila = modelo.getFilaSeleccionada();
        int col = modelo.getColSeleccionada();
        int jugador = modelo.getTurno();

        // descontar devuelve true si se acabó el tiempo; entonces, si había una carta volteada, se
        // vuelve a tapar.
        if (modelo.descontar(dt) && fila != -1) {
            vista.ocultarCarta(jugador, fila, col);
        }
        // Actualiza los marcadores y, si cambió el turno, muestra el aviso.
        actualizarMarcadores();
        refrescarTurno();
    }

    private void alTocarCelda(int jugador, int fila, int col) {
        if (vista.isPausado()) return; // el menú de pausa ya tapa las celdas; esto es solo por seguridad
        // se guarda la primera carta antes de voltear porque el modelo la borra si es pareja
        final int fila1 = modelo.getFilaSeleccionada();
        final int col1 = modelo.getColSeleccionada();

        // El modelo aplica las reglas y responde qué pasó.
        ModeloClasico.Resultado resultado = modelo.voltear(jugador, fila, col);
        // Jugada no válida: no se cambia nada en pantalla.
        if (resultado == ModeloClasico.Resultado.IGNORADO) return;

        // Se muestra la carta volteada (imagen del personaje).
        vista.mostrarCartaImagen(jugador, fila, col, modelo.getCarta(jugador, fila, col));

        // Acierto: las dos cartas quedan marcadas como encontradas (verde). Si fue la primera carta del
        // intento (PRIMERA) no hay nada más que hacer.
        if (resultado == ModeloClasico.Resultado.PAREJA) {
            vista.marcarEncontrada(jugador, fila1, col1);
            vista.marcarEncontrada(jugador, fila, col);
        } else if (resultado == ModeloClasico.Resultado.NO_PAREJA) {
            // las cartas se ven un momento y luego se tapan
            temporizadorFallo = new Timer((int) ModeloClasico.MS_OCULTAR_FALLO, e -> {
                vista.ocultarCarta(jugador, fila1, col1);
                vista.ocultarCarta(jugador, fila, col);
                // El modelo olvida el par fallado y pasa el turno.
                modelo.resolverFallo();
                actualizarMarcadores();
                refrescarTurno();
            });
            // Que se ejecute una sola vez (no que se repita cada 900 ms).
            temporizadorFallo.setRepeats(false);
            temporizadorFallo.start();
        }

        actualizarMarcadores();
        refrescarTurno();
        // Si con esta jugada alguien completó su tablero, se termina la partida. invokeLater lo deja
        // para cuando se termine de procesar este clic.
        if (modelo.isTerminado()) {
            SwingUtilities.invokeLater(() -> terminarPartida(modelo.getGanador(), modelo.getMensajeFinal(), true));
        }
    }

    private void actualizarMarcadores() {
        boolean fin = modelo.isTerminado();
        for (int j = 0; j < 2; j++) {
            boolean suTurno = !fin && modelo.getTurno() == j;
            // Solo el jugador en turno muestra el tiempo (para el otro se manda -1 = sin tiempo).
            vista.actualizarMarcador(j, modelo.getPares(j), modelo.getIntentos(j), suTurno,
                    suTurno ? modelo.getSegundosRestantes() : -1);
        }
    }

    /** Si cambió el turno, se muestra el aviso. */
    private void refrescarTurno() {
        // consumirCambioTurno devuelve true solo una vez por cada cambio de turno.
        if (modelo.consumirCambioTurno() && !modelo.isTerminado()) anunciarTurnoActual();
    }

    /** Pide a la vista mostrar el aviso 'Turno de jugador X' durante 1,5 s, si la partida sigue. */
    private void anunciarTurnoActual() {
        if (!modelo.isTerminado()) vista.mostrarTurno(modelo.getTurno(), (int) ModeloClasico.MS_BANNER_TURNO);
    }

    /** Botón de prueba: termina la partida dándole la victoria al jugador indicado (0 o 1). */
    private void forzarGanador(int ganador) {
        if (vista.isPausado() || finalizando || modelo.isBloqueado()) return; // no vale durante la intro ni si ya se está terminando
        if (temporizadorFallo != null) temporizadorFallo.stop(); // si había un par fallado esperando, se descarta
        modelo.setBloqueado(true); // congela el juego y el reloj mientras suenan las animaciones
        // El último parámetro es false: esta victoria de prueba no se registra en el ranking.
        terminarPartida(ganador, "¡Gana " + modelo.getNombre(ganador) + "!\n(victoria forzada con el botón de prueba)", false);
    }

    /**
     * Termina la partida: ganador = 0, 1 o -1 (empate); 'mensaje' es lo que se muestra al final.
     * 'cuentaParaRanking' es false en las victorias forzadas con los botones de prueba.
     */
    private void terminarPartida(int ganador, String mensaje, boolean cuentaParaRanking) {
        // Evita terminar dos veces la misma partida.
        if (finalizando) return;
        finalizando = true;

        // los intentos se leen YA, antes de que reiniciar() borre el modelo
        final boolean hayGanador = ganador == 0 || ganador == 1;
        final int intentos = hayGanador ? modelo.getIntentos(ganador) : -1;

        // el premio consulta el BCCR: se pide en otro hilo mientras suena la animación para no congelar la ventana
        final CompletableFuture<Double> premio = (hayGanador && cuentaParaRanking)
                ? CompletableFuture.supplyAsync(() -> APIbccr.calcularPremio(intentos))
                : null;

        // 'fin' es lo que se hace cuando termina la animación de victoria: registrar en el ranking,
        // preguntar si quieren otra partida y reiniciar o volver al menú.
        Runnable fin = () -> {
            // Solo las partidas reales (no las de prueba) llegan al ranking.
            String extra = (premio != null) ? registrarEnRanking(ganador, intentos, premio) : "";
            // Diálogo modal: el programa espera la respuesta del usuario.
            boolean otraPartida = vista.preguntarOtraPartida(mensaje + extra);
            reiniciar();
            // Si quieren otra partida se vuelve a empezar con la intro; si no, se regresa al menú
            // principal.
            if (otraPartida) arrancarPartida();
            else vista.volverAlMenu();
        };

        // Con ganador se reproduce primero su animación y 'fin' se ejecuta al acabar. En empate no hay
        // animación y 'fin' se llama de una vez.
        if (hayGanador) {
            reproducirAnimacion(ganador, ModeloClasico.Animacion.VICTORIA, fin); // solo celebra el ganador
        } else {
            fin.run(); // empate: no hay victoria que mostrar ni ranking
        }
    }

    /** Registra la victoria en el Top 5 y devuelve el texto que se agrega al mensaje final. */
    private String registrarEnRanking(int ganador, int intentos, CompletableFuture<Double> premioFuturo) {
        // El jugador 1 es el usuario con sesión y ya tiene nombre; si ganó el jugador 2 se le pide uno
        // para el ranking.
        String nombre = (ganador == 0)
                ? modelo.getNombre(0)
                : vista.pedirNombre("¡Ganó " + modelo.getNombre(1) + "!\nEscribe tu nombre para el ranking de Mejores Jugadores:",
                                    modelo.getNombre(1));

        // El premio lo calcula APIbccr en otro hilo; aquí se espera como máximo 3 s por el resultado.
        double premio;
        try {
            premio = premioFuturo.get(3, TimeUnit.SECONDS); // normalmente ya está listo (APIbccr guarda el tipo de cambio)
        } catch (Exception e) {
            premio = -1; // sin conexión con el BCCR: se guarda la marca sin premio
        }

        // Se arma el texto que se agrega al mensaje final.
        StringBuilder texto = new StringBuilder("\n\nPremio: " + VentanaMejoresJugadores.formatearPremio(premio));
        try {
            // registrar devuelve el puesto en el Top 5 (mayor que 0 si entró).
            int puesto = ranking.registrar(nombre, intentos, premio);
            texto.append(puesto > 0
                    ? "\n¡" + nombre + " entró al Top 5 de Mejores Jugadores en el puesto " + puesto + "!"
                    : "\n" + nombre + " no entró al Top 5 esta vez.");
        // Si falla al escribir el archivo del ranking, se avisa en el mensaje en vez de cerrar el
        // programa.
        } catch (IOException ex) {
            texto.append("\nNo se pudo guardar el ranking: ").append(ex.getMessage());
        }
        return texto.toString();
    }
}