import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MODELO del Modo Clásico (memoria de pares, 1 contra 1).
 * No sabe nada de Swing: solo guarda el estado del juego y aplica las reglas.
 *
 * Reglas:
 *  - Cada jugador tiene su propio tablero de 6x6 (18 pares), barajado distinto.
 *  - En su turno, el jugador voltea 2 cartas de SU tablero.
 *  - Si son pareja, las conserva y vuelve a jugar.
 *  - Si no lo son, se ocultan de nuevo y el turno pasa al otro jugador.
 *  - Si un jugador completa su tablero, el otro sigue jugando solo hasta terminar.
 *  - Gana quien complete su tablero con menos intentos (empate si igualan).
 */
public class ModeloClasico {

    public static final int N = 6;                 // tablero de N x N
    public static final int PARES = N * N / 2;     // 18 pares por tablero

    /** Resultado de voltear una carta. */
    public enum Resultado { IGNORADO, PRIMERA, PAREJA, NO_PAREJA }

    private final String[][][] cartas = new String[2][N][N];
    private final boolean[][][] encontrada = new boolean[2][N][N];
    private final int[] pares = new int[2];
    private final int[] intentos = new int[2];

    private int turno;            // 0 = Jugador 1, 1 = Jugador 2
    private boolean esperando;    // hay 2 cartas distintas a la vista; se bloquea el juego hasta ocultarlas
    private int fila1 = -1, col1 = -1; // primera carta volteada del turno actual

    public ModeloClasico() {
        reiniciar();
    }

    /** Baraja los dos tableros y deja todo en cero. Empieza el Jugador 1. */
    public void reiniciar() {
        for (int j = 0; j < 2; j++) {
            List<String> baraja = new ArrayList<>();
            for (int k = 0; k < PARES; k++) {
                String simbolo = String.valueOf((char) ('A' + k)); // A..R (cambia aquí si quieres otros símbolos)
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
    }

    /** Intenta voltear la carta (fila, col) del tablero del jugador indicado. */
    public Resultado voltear(int jugador, int fila, int col) {
        if (isTerminado() || esperando || jugador != turno) return Resultado.IGNORADO;
        if (encontrada[jugador][fila][col]) return Resultado.IGNORADO;

        // Primera carta del intento
        if (fila1 == -1) {
            fila1 = fila;
            col1 = col;
            return Resultado.PRIMERA;
        }

        // Tocar de nuevo la misma carta no cuenta
        if (fila == fila1 && col == col1) return Resultado.IGNORADO;

        // Segunda carta: se completa un intento
        intentos[jugador]++;
        if (cartas[jugador][fila1][col1].equals(cartas[jugador][fila][col])) {
            encontrada[jugador][fila1][col1] = true;
            encontrada[jugador][fila][col] = true;
            pares[jugador]++;
            limpiarSeleccion();
            // Acierto: repite turno, salvo que ya terminó su tablero y al otro aún le falta
            int otro = 1 - jugador;
            if (pares[jugador] == PARES && pares[otro] < PARES) turno = otro;
            return Resultado.PAREJA;
        }

        esperando = true; // el controlador debe llamar a resolverFallo() tras mostrarlas un momento
        return Resultado.NO_PAREJA;
    }

    /** Oculta el par fallado y pasa el turno (si el otro jugador aún no terminó). */
    public void resolverFallo() {
        limpiarSeleccion();
        esperando = false;
        int otro = 1 - turno;
        if (pares[otro] < PARES) turno = otro;
    }

    private void limpiarSeleccion() {
        fila1 = -1;
        col1 = -1;
    }

    /** La partida termina cuando ambos tableros están completos. */
    public boolean isTerminado() {
        return pares[0] == PARES && pares[1] == PARES;
    }

    /** 0 = gana Jugador 1, 1 = gana Jugador 2, -1 = empate. Solo tiene sentido si la partida terminó. */
    public int getGanador() {
        if (intentos[0] < intentos[1]) return 0;
        if (intentos[1] < intentos[0]) return 1;
        return -1;
    }

    // Getters para que el controlador consulte el estado
    public String getCarta(int jugador, int fila, int col) { return cartas[jugador][fila][col]; }
    public boolean isEncontrada(int jugador, int fila, int col) { return encontrada[jugador][fila][col]; }
    public int getTurno() { return turno; }
    public int getPares(int jugador) { return pares[jugador]; }
    public int getIntentos(int jugador) { return intentos[jugador]; }
    public int getFilaSeleccionada() { return fila1; }
    public int getColSeleccionada() { return col1; }
}