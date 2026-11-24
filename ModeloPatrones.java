import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * MODELO del Modo Patrones (un jugador, matriz de 5x5).
 * No sabe nada de Swing: guarda el estado del juego y aplica las reglas.
 *
 * Reglas:
 *  - La partida empieza con un patrón aleatorio de 3 casillas.
 *  - Se muestra el patrón y el jugador debe tocar las mismas casillas en el mismo orden.
 *  - Si lo logra, el siguiente patrón es EL MISMO patrón anterior más 1 casilla nueva al final
 *    (3 casillas, luego esas mismas 3 + 1, luego esas 4 + 1, y así sucesivamente).
 *  - Cada partida nueva empieza con un patrón nuevo.
 *  - El jugador tiene 12 segundos EN TOTAL para repetir el patrón, y menos de 2 segundos
 *    entre una casilla y la siguiente (el límite de 2 s corre desde la primera casilla tocada).
 *  - El juego termina al tocar una casilla incorrecta o al agotarse alguno de los tiempos.
 *
 * El tablero es una matriz de estados (NORMAL, MOSTRADA, ACERTADA, FALLADA) que la vista dibuja.
 * El modelo no usa relojes propios: recibe la hora actual (ms) para poder probarse fácilmente.
 */
public class ModeloPatrones {

    // Constantes de configuración del modo (N define el tamaño N x N del tablero)
    public static final int N = 4;                        // matriz de N x N
    public static final int CASILLAS_INICIALES = 3;       // longitud del patrón en la ronda 1
    public static final long TIEMPO_TOTAL_MS = 12_000;    // límite global por ronda (12 segundos)
    public static final long TIEMPO_ENTRE_CASILLAS_MS = 2_000; // límite entre toques consecutivos (2 segundos)

    // Tipos enumerados que representan los distintos estados del juego y sus transiciones
    public enum Estado { NORMAL, MOSTRADA, ACERTADA, FALLADA }
    public enum Fase { INACTIVO, MOSTRANDO, JUGANDO, RONDA_SUPERADA, TERMINADO }
    public enum Resultado { IGNORADO, ACIERTO, RONDA_COMPLETA, FALLO, TIEMPO_AGOTADO }
    public enum MotivoFin { NINGUNO, CASILLA_INCORRECTA, TIEMPO_TOTAL, TIEMPO_ENTRE_CASILLAS }

    // Estructura del tablero y secuencia ordenada de coordenadas {fila, columna} a memorizar
    private final Estado[][] tablero = new Estado[N][N];
    private final List<int[]> patron = new ArrayList<>(); // cada casilla es {fila, columna}
    private final Random azar = new Random();

    // Variables de estado de la partida actual
    private Fase fase;
    private MotivoFin motivoFin;
    private int longitud;          // casillas del patrón de la ronda actual
    private int indice;            // cuántas casillas lleva acertadas en esta ronda
    private int rondasCompletadas;
    private long inicioTurno;      // hora en que empezó a repetir el patrón
    private long ultimaCasilla;    // hora de la última casilla tocada (-1 si aún no toca ninguna)

    /** Constructor: inicializa el modelo dejándolo en estado inactivo listo para jugar. */
    public ModeloPatrones() {
        reiniciar();
    }

    /** Deja todo en cero y en espera de que el jugador pulse "Iniciar". */
    public void reiniciar() {
        patron.clear();
        longitud = CASILLAS_INICIALES;
        indice = 0;
        rondasCompletadas = 0;
        motivoFin = MotivoFin.NINGUNO;
        ultimaCasilla = -1;
        fase = Fase.INACTIVO;
        limpiarTablero();
    }

    /** Empieza una partida nueva: primer patrón de 3 casillas. */
    public void iniciar() {
        reiniciar();
        generarPatron();
    }

    /**
     * Pasa a la siguiente ronda: el MISMO patrón de la ronda anterior más 1 casilla nueva al final.
     * Las casillas que ya estaban no cambian, solo se agrega una al final.
     */
    public void siguienteRonda() {
        longitud++;
        if (patron.isEmpty()) {
            generarPatron(); // por seguridad: si no había patrón, se crea uno de la longitud actual
            return;
        }
        // Agrega una casilla nueva evitando que sea igual a la última del patrón anterior
        patron.add(casillaAlAzar(patron.get(patron.size() - 1)));
        prepararMuestra();
    }

    /** Crea un patrón nuevo desde cero con las casillas de la longitud actual. */
    private void generarPatron() {
        patron.clear();
        int[] anterior = null;
        for (int k = 0; k < longitud; k++) {
            anterior = casillaAlAzar(anterior);
            patron.add(anterior);
        }
        prepararMuestra();
    }

    /** Elige una casilla al azar distinta de la anterior (así nunca se repite la misma dos veces seguidas). */
    private int[] casillaAlAzar(int[] anterior) {
        int[] casilla;
        do {
            casilla = new int[]{azar.nextInt(N), azar.nextInt(N)};
        } while (anterior != null && anterior[0] == casilla[0] && anterior[1] == casilla[1]);
        return casilla;
    }

    /** Deja el modelo listo para mostrarle el patrón al jugador. */
    private void prepararMuestra() {
        indice = 0;
        ultimaCasilla = -1;
        fase = Fase.MOSTRANDO;
        limpiarTablero();
    }

    /** Enciende la casilla número i del patrón (y apaga las demás). Es para mostrarlo al jugador. */
    public void mostrar(int i) {
        limpiarTablero();
        int[] casilla = patron.get(i);
        tablero[casilla[0]][casilla[1]] = Estado.MOSTRADA;
    }

    /** Restablece todas las celdas del tablero al estado NORMAL. */
    public void limpiarTablero() {
        for (int f = 0; f < N; f++) {
            for (int c = 0; c < N; c++) {
                tablero[f][c] = Estado.NORMAL;
            }
        }
    }

    /** Ya se mostró el patrón: empieza a correr el tiempo del jugador. */
    public void comenzarTurno(long ahora) {
        limpiarTablero();
        indice = 0;
        inicioTurno = ahora;
        ultimaCasilla = -1;
        fase = Fase.JUGANDO;
    }

    /** El jugador toca la casilla (fila, col). Evalúa validez, tiempo y coincidencia con el patrón. */
    public Resultado elegir(int fila, int col, long ahora) {
        // Ignora toques fuera del turno activo o fuera de los límites del tablero
        if (fase != Fase.JUGANDO) return Resultado.IGNORADO;
        if (fila < 0 || fila >= N || col < 0 || col >= N) return Resultado.IGNORADO;

        // Si ya se pasó de tiempo, esta casilla llegó tarde
        if (comprobarTiempo(ahora)) return Resultado.TIEMPO_AGOTADO;

        // Verifica si la coordenada coincide con la siguiente casilla esperada en la secuencia
        int[] esperada = patron.get(indice);
        if (esperada[0] == fila && esperada[1] == col) {
            tablero[fila][col] = Estado.ACERTADA;
            indice++;
            ultimaCasilla = ahora;
            // Si completó todas las casillas del patrón, cierra la ronda como superada
            if (indice == patron.size()) {
                rondasCompletadas++;
                fase = Fase.RONDA_SUPERADA;
                return Resultado.RONDA_COMPLETA;
            }
            return Resultado.ACIERTO;
        }

        // Si se equivocó de casilla, marca el error y finaliza la partida
        tablero[fila][col] = Estado.FALLADA;
        terminar(MotivoFin.CASILLA_INCORRECTA);
        return Resultado.FALLO;
    }

    /** Revisa los dos límites de tiempo. Si se agotó alguno, termina la partida y devuelve true. */
    public boolean comprobarTiempo(long ahora) {
        if (fase != Fase.JUGANDO) return false;
        // 1. Límite de 12 segundos desde que inició el turno del jugador
        if (ahora - inicioTurno > TIEMPO_TOTAL_MS) {
            terminar(MotivoFin.TIEMPO_TOTAL);
            return true;
        }
        // 2. Límite de 2 segundos entre una casilla tocada y la siguiente (solo tras tocar al menos una)
        if (ultimaCasilla >= 0 && ahora - ultimaCasilla > TIEMPO_ENTRE_CASILLAS_MS) {
            terminar(MotivoFin.TIEMPO_ENTRE_CASILLAS);
            return true;
        }
        return false;
    }

    /** Registra la causa del fin de partida y cambia la fase a TERMINADO. */
    private void terminar(MotivoFin motivo) {
        motivoFin = motivo;
        fase = Fase.TERMINADO;
    }

    // ------------------------------------------------------------------
    // Consultas para el controlador y la vista
    // ------------------------------------------------------------------

    /** Milisegundos que le quedan para terminar el patrón (12 s en total). */
    public long getMsRestantesTotal(long ahora) {
        return Math.max(0, TIEMPO_TOTAL_MS - (ahora - inicioTurno));
    }

    /** Milisegundos que le quedan para tocar la siguiente casilla; -1 si aún no ha tocado la primera. */
    public long getMsRestantesEntreCasillas(long ahora) {
        if (ultimaCasilla < 0) return -1;
        return Math.max(0, TIEMPO_ENTRE_CASILLAS_MS - (ahora - ultimaCasilla));
    }

    /** Copia de la matriz de estados (la vista la dibuja sin poder modificar el modelo). */
    public Estado[][] getTablero() {
        Estado[][] copia = new Estado[N][N];
        for (int f = 0; f < N; f++) {
            for (int c = 0; c < N; c++) {
                copia[f][c] = tablero[f][c];
            }
        }
        return copia;
    }

    // Getters de solo lectura para consultar el estado actual del juego
    public List<int[]> getPatron() { return Collections.unmodifiableList(patron); }
    public Fase getFase() { return fase; }
    public MotivoFin getMotivoFin() { return motivoFin; }
    public int getLongitud() { return longitud; }
    public int getIndice() { return indice; }
    public int getRondasCompletadas() { return rondasCompletadas; }

    /** Casillas del patrón más largo que repitió completo (0 si no logró ninguno). */
    public int getMayorPatronLogrado() {
        return rondasCompletadas == 0 ? 0 : CASILLAS_INICIALES + rondasCompletadas - 1;
    }
}