import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Modelo del modo clásico (2 jugadores, cada uno con su tablero de 6x6).
 * Aquí van todas las reglas del juego; no usa nada de Swing.
 *
 * - Cada turno empieza con 10 segundos.
 * - Si acierta una pareja, gana 7 segundos más y sigue jugando.
 * - Si falla o se le acaba el tiempo, le toca al otro jugador.
 * - Gana quien termine su tablero con menos intentos.
 *
 * Animaciones: el modelo guarda el ESTADO de la animación de cada jugador (cuál es, en qué
 * cuadro va, partículas en coordenadas de 0 a 1). No dibuja nada: la vista lo dibuja y el
 * controlador es quien lo hace avanzar con un Timer.
 */
public class ModeloClasico {

    public static final int N = 6;
    public static final int PARES = N * N / 2; // 18 pares

    // Tiempos en milisegundos
    public static final long MS_TURNO = 10_000;
    public static final long MS_BONO_ACIERTO = 7_000;
    public static final long MS_OCULTAR_FALLO = 900;  // cuánto se ven las 2 cartas distintas
    public static final long MS_BANNER_TURNO = 1_500; // lo que dura el aviso de turno

    public enum Resultado { IGNORADO, PRIMERA, PAREJA, NO_PAREJA }

    // ---------- Animaciones ----------

    /** Animación que tiene un jugador sobre su matriz. */
    public enum Animacion { NINGUNA, INTRO, VICTORIA, DERROTA }

    public static final int PERIODO_ANIMACION_MS = 33; // cada cuánto avanza un cuadro (~30 por segundo)
    public static final int CUADROS_INTRO = 45;        // ~1,5 s
    public static final int CUADROS_FINAL = 75;        // ~2,5 s (después se queda en el último cuadro)
    private static final int MAX_PARTICULAS = 120;

    /** Partícula de una animación. x e y van de 0 a 1 (la vista las escala a su matriz). */
    public static final class Particula {
        private float x, y;
        private final float vx, vy;
        private int vida;
        private final int vidaInicial;

        Particula(float x, float y, float vx, float vy, int vida) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.vida = vida; this.vidaInicial = vida;
        }

        /** Mueve la partícula un cuadro. Devuelve false cuando ya se acabó su vida. */
        boolean mover() {
            x += vx;
            y += vy;
            return --vida > 0;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        /** 1 = recién creada, 0 = a punto de desaparecer. */
        public float getAlfa() { return Math.max(0f, Math.min(1f, vida / (float) vidaInicial)); }
    }

    private final String[][][] cartas = new String[2][N][N];
    private final boolean[][][] encontrada = new boolean[2][N][N];
    private final int[] pares = new int[2];
    private final int[] intentos = new int[2];

    private int turno;       // 0 = jugador 1, 1 = jugador 2
    private boolean esperando; // true mientras se ven 2 cartas distintas
    private int fila1 = -1, col1 = -1; // primera carta que se volteó en el intento

    private long tiempoRestanteMs;
    private long graciaMs;      // el reloj espera mientras sale el aviso de turno
    private boolean turnoCambio; // para que la vista muestre el aviso
    private boolean bloqueado;   // true mientras suena una animación (intro): no se juega ni corre el reloj

    private final String[] nombres = {"Jugador 1", "Jugador 2"};
    private final Animacion[] animacion = {Animacion.NINGUNA, Animacion.NINGUNA};
    private final int[] cuadroAnimacion = new int[2];
    private final List<List<Particula>> particulas = new ArrayList<>();
    private final Random azar = new Random();

    {
        particulas.add(new ArrayList<>());
        particulas.add(new ArrayList<>());
    }

    public ModeloClasico() {
        reiniciar();
    }

    /** Baraja los dos tableros y empieza de cero. */
    public void reiniciar() {
        for (int j = 0; j < 2; j++) {
            List<String> baraja = new ArrayList<>();
            for (int k = 0; k < PARES; k++) {
                String simbolo = String.valueOf((char) ('A' + k)); // letras A a R
                baraja.add(simbolo);
                baraja.add(simbolo);
            }
            Collections.shuffle(baraja);

            int idx = 0;
            for (int f = 0; f < N; f++) {
                for (int c = 0; c < N; c++) {
                    cartas[j][f][c] = baraja.get(idx++);
                    encontrada[j][f][c] = false;
                }
            }
            pares[j] = 0;
            intentos[j] = 0;
        }
        turno = 0;
        esperando = false;
        limpiarSeleccion();
        tiempoRestanteMs = MS_TURNO;
        graciaMs = MS_BANNER_TURNO;
        turnoCambio = true;
        bloqueado = false;
        detenerAnimaciones();
    }

    // ---------- Jugadas ----------

    /** Voltea una carta del tablero del jugador. Devuelve qué pasó. */
    public Resultado voltear(int jugador, int fila, int col) {
        if (isTerminado() || esperando || bloqueado || jugador != turno) return Resultado.IGNORADO;
        if (encontrada[jugador][fila][col]) return Resultado.IGNORADO;

        // primera carta
        if (fila1 == -1) {
            fila1 = fila;
            col1 = col;
            return Resultado.PRIMERA;
        }

        // la misma carta otra vez no cuenta
        if (fila == fila1 && col == col1) return Resultado.IGNORADO;

        // segunda carta: es un intento
        intentos[jugador]++;
        if (cartas[jugador][fila1][col1].equals(cartas[jugador][fila][col])) {
            encontrada[jugador][fila1][col1] = true;
            encontrada[jugador][fila][col] = true;
            pares[jugador]++;
            limpiarSeleccion();
            tiempoRestanteMs += MS_BONO_ACIERTO;
            // si ya completó su tablero, el turno pasa al otro (si todavía le falta)
            if (pares[jugador] == PARES) pasarTurno();
            return Resultado.PAREJA;
        }

        esperando = true; // se espera a que el controlador llame a resolverFallo()
        return Resultado.NO_PAREJA;
    }

    /** Tapa el par fallado y pasa el turno. */
    public void resolverFallo() {
        limpiarSeleccion();
        esperando = false;
        pasarTurno();
    }

    /**
     * Le resta tiempo al reloj del turno.
     * Devuelve true si se acabó el tiempo y se perdió el turno.
     */
    public boolean descontar(long ms) {
        if (isTerminado() || esperando || bloqueado) return false;

        // mientras sale el aviso de turno no se gasta tiempo
        if (graciaMs > 0) {
            if (ms <= graciaMs) {
                graciaMs -= ms;
                return false;
            }
            ms -= graciaMs;
            graciaMs = 0;
        }

        tiempoRestanteMs -= ms;
        if (tiempoRestanteMs <= 0) {
            limpiarSeleccion();
            pasarTurno();
            return true;
        }
        return false;
    }

    /** Pasa el turno al otro jugador (si todavía no termina) y reinicia el reloj. */
    private void pasarTurno() {
        int otro = 1 - turno;
        if (pares[otro] < PARES) {
            turno = otro;
            turnoCambio = true;
            graciaMs = MS_BANNER_TURNO;
        }
        tiempoRestanteMs = MS_TURNO;
    }

    private void limpiarSeleccion() {
        fila1 = -1;
        col1 = -1;
    }

    // ---------- Final de la partida ----------

    public boolean isTerminado() {
        return pares[0] == PARES && pares[1] == PARES;
    }

    /** 0 = gana jugador 1, 1 = gana jugador 2, -1 = empate. */
    public int getGanador() {
        if (intentos[0] < intentos[1]) return 0;
        if (intentos[1] < intentos[0]) return 1;
        return -1;
    }

    /** Intentos del ganador (para el premio). -1 si hubo empate. */
    public int getIntentosGanador() {
        int g = getGanador();
        return g == -1 ? -1 : intentos[g];
    }

    /** Mensaje de gane provisional, después se puede cambiar por la animación. */
    public String getMensajeFinal() {
        int ganador = getGanador();
        String detalle = nombres[0] + ": " + intentos[0] + " intentos\n"
                       + nombres[1] + ": " + intentos[1] + " intentos";
        return (ganador == -1)
                ? "¡Empate!\n" + detalle
                : "¡Gana " + nombres[ganador] + "!\n" + detalle;
    }

    // ---------- Animaciones ----------

    private static int duracion(Animacion tipo) {
        switch (tipo) {
            case INTRO: return CUADROS_INTRO;
            case VICTORIA:
            case DERROTA: return CUADROS_FINAL;
            default: return 0;
        }
    }

    /** Empieza una animación en la matriz del jugador (reemplaza la que tuviera). */
    public void iniciarAnimacion(int jugador, Animacion tipo) {
        animacion[jugador] = tipo;
        cuadroAnimacion[jugador] = 0;
        particulas.get(jugador).clear();
    }

    /** True mientras la animación del jugador todavía no llega a su último cuadro. */
    public boolean isAnimando(int jugador) {
        return animacion[jugador] != Animacion.NINGUNA
                && cuadroAnimacion[jugador] < duracion(animacion[jugador]);
    }

    /**
     * Avanza un cuadro la animación del jugador.
     * Devuelve true una sola vez: justo en el cuadro en que la animación termina.
     * La intro se quita sola al terminar; victoria y derrota se quedan en el último cuadro.
     */
    public boolean avanzarAnimacion(int jugador) {
        if (!isAnimando(jugador)) return false;
        Animacion tipo = animacion[jugador];
        cuadroAnimacion[jugador]++;
        moverParticulas(jugador, tipo);

        if (cuadroAnimacion[jugador] < duracion(tipo)) return false;
        if (tipo == Animacion.INTRO) {
            animacion[jugador] = Animacion.NINGUNA;
            cuadroAnimacion[jugador] = 0;
            particulas.get(jugador).clear();
        }
        return true;
    }

    private void moverParticulas(int jugador, Animacion tipo) {
        List<Particula> lista = particulas.get(jugador);
        lista.removeIf(p -> !p.mover());
        if (lista.size() >= MAX_PARTICULAS) return;

        // victoria: chispas que suben desde abajo; derrota: cenizas que caen desde arriba
        for (int i = 0; i < 2; i++) {
            float deriva = (azar.nextFloat() - 0.5f) * 0.004f;
            if (tipo == Animacion.VICTORIA) {
                lista.add(new Particula(azar.nextFloat(), 1f, deriva,
                        -(0.008f + azar.nextFloat() * 0.016f), 50 + azar.nextInt(30)));
            } else if (tipo == Animacion.DERROTA) {
                lista.add(new Particula(azar.nextFloat(), 0f, deriva,
                        0.005f + azar.nextFloat() * 0.010f, 60 + azar.nextInt(40)));
            }
        }
    }

    /** Quita las animaciones de los dos jugadores. */
    public void detenerAnimaciones() {
        for (int j = 0; j < 2; j++) {
            animacion[j] = Animacion.NINGUNA;
            cuadroAnimacion[j] = 0;
            particulas.get(j).clear();
        }
    }

    // ---------- Consultas ----------

    /** Devuelve true una sola vez cada vez que cambia el turno. */
    public boolean consumirCambioTurno() {
        boolean r = turnoCambio;
        turnoCambio = false;
        return r;
    }

    /** Mientras esté bloqueado no se pueden voltear cartas y el reloj no avanza. */
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public boolean isBloqueado() { return bloqueado; }

    public Animacion getAnimacion(int jugador) { return animacion[jugador]; }
    public int getCuadroAnimacion(int jugador) { return cuadroAnimacion[jugador]; }
    /** Avance de la animación de 0 a 1. */
    public double getProgresoAnimacion(int jugador) {
        int d = duracion(animacion[jugador]);
        return d == 0 ? 0 : cuadroAnimacion[jugador] / (double) d;
    }
    public List<Particula> getParticulas(int jugador) { return Collections.unmodifiableList(particulas.get(jugador)); }

    public String getNombre(int jugador) { return nombres[jugador]; }
    public void setNombre(int jugador, String nombre) {
        if (nombre != null && !nombre.trim().isEmpty()) nombres[jugador] = nombre.trim();
    }

    public int getSegundosRestantes() { return (int) Math.ceil(Math.max(0, tiempoRestanteMs) / 1000.0); }

    public String getCarta(int jugador, int fila, int col) { return cartas[jugador][fila][col]; }
    public boolean isEncontrada(int jugador, int fila, int col) { return encontrada[jugador][fila][col]; }
    public int getTurno() { return turno; }
    public int getPares(int jugador) { return pares[jugador]; }
    public int getIntentos(int jugador) { return intentos[jugador]; }
    public int getFilaSeleccionada() { return fila1; }
    public int getColSeleccionada() { return col1; }
}