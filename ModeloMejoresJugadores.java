import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * MODELO de los Mejores Jugadores (Top 5 del Modo Clásico).
 * No usa Swing ni conoce al BCCR: recibe el nombre, los intentos y el premio ya calculado,
 * decide si la marca entra al Top 5 y guarda el ranking en mejores_jugadores.txt
 * (una línea por jugador:  nombre;intentos;premio;fecha).
 *
 * Reglas:
 *  - Gana puesto quien termina su tablero con MENOS intentos (a más intentos, menos premio).
 *  - Si dos marcas empatan en intentos, queda arriba la más antigua (el que llegó primero).
 *  - Solo se guardan los MAX mejores.
 *  - Con UNA_ENTRADA_POR_JUGADOR un mismo jugador aparece una sola vez, con su mejor marca;
 *    así una persona no puede llenar ella sola los 5 puestos.
 */
public class ModeloMejoresJugadores {

    public static final int MAX = 5;
    public static final int NOMBRE_MAX = ModeloUsuarios.USUARIO_MAX;
    /** true = cada jugador aparece una sola vez (su mejor marca). false = el Top 5 son las 5 mejores partidas. */
    private static final boolean UNA_ENTRADA_POR_JUGADOR = true;

    /** Una marca del ranking. Es inmutable. */
    public static final class Puntaje {
        private final String nombre;
        private final int intentos;
        private final double premio;   // colones; -1 si no se pudo calcular (BCCR sin conexión)
        private final LocalDate fecha;

        private Puntaje(String nombre, int intentos, double premio, LocalDate fecha) {
            this.nombre = nombre;
            this.intentos = intentos;
            this.premio = premio;
            this.fecha = fecha;
        }

        public String getNombre() { return nombre; }
        public int getIntentos() { return intentos; }
        public double getPremio() { return premio; }
        public LocalDate getFecha() { return fecha; }
    }

    // menos intentos primero; si empatan, la fecha más antigua primero (List.sort es estable)
    private static final Comparator<Puntaje> ORDEN =
            Comparator.comparingInt(Puntaje::getIntentos).thenComparing(Puntaje::getFecha);

    private final Path archivo;
    private final List<Puntaje> top = new ArrayList<>();

    public ModeloMejoresJugadores() {
        this(Paths.get("mejores_jugadores.txt"));
    }

    public ModeloMejoresJugadores(Path archivo) {
        this.archivo = archivo;
        cargar();
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Copia del ranking ordenado (el primero es el mejor). La vista no puede modificar el modelo. */
    public synchronized List<Puntaje> getTop() {
        return Collections.unmodifiableList(new ArrayList<>(top));
    }

    // ------------------------------------------------------------------
    // Registro de una victoria
    // ------------------------------------------------------------------

    /**
     * Intenta meter una marca en el ranking.
     *
     * @return el puesto logrado (1 a MAX), o -1 si no entra (no supera a las que ya están
     *         o el jugador ya tiene una marca mejor o igual)
     * @throws IOException si no se pudo escribir el archivo; en ese caso el ranking en memoria no cambia
     */
    public synchronized int registrar(String nombre, int intentos, double premio) throws IOException {
        if (intentos <= 0) return -1;

        Puntaje nuevo = new Puntaje(limpiarNombre(nombre), intentos, premio, LocalDate.now());
        List<Puntaje> copia = new ArrayList<>(top);

        if (UNA_ENTRADA_POR_JUGADOR) {
            for (Puntaje p : copia) {
                if (p.nombre.equalsIgnoreCase(nuevo.nombre)) {
                    if (p.intentos <= intentos) return -1; // ya tiene una marca igual o mejor
                    copia.remove(p);                        // mejoró: se reemplaza la anterior
                    break;
                }
            }
        }

        copia.add(nuevo);
        copia.sort(ORDEN);
        if (copia.size() > MAX) copia.subList(MAX, copia.size()).clear();

        int indice = copia.indexOf(nuevo);
        if (indice < 0) return -1; // quedó fuera del Top 5

        escribir(copia); // si falla lanza IOException y 'top' no se toca
        top.clear();
        top.addAll(copia);
        return indice + 1;
    }

    /** Quita ';' y saltos de línea (romperían el archivo), recorta y limita el largo. */
    private static String limpiarNombre(String nombre) {
        String n = (nombre == null) ? "" : nombre.replaceAll("[;\\r\\n]", " ").trim();
        if (n.isEmpty()) n = "Jugador";
        return n.length() > NOMBRE_MAX ? n.substring(0, NOMBRE_MAX).trim() : n;
    }

    // ------------------------------------------------------------------
    // Archivo mejores_jugadores.txt
    // ------------------------------------------------------------------

    private void cargar() {
        if (!Files.exists(archivo)) return;
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] p = linea.split(";", -1);
                if (p.length < 4) continue;
                try {
                    int intentos = Integer.parseInt(p[1].trim());
                    if (intentos <= 0) continue;
                    top.add(new Puntaje(p[0].trim(), intentos,
                            Double.parseDouble(p[2].trim()), LocalDate.parse(p[3].trim())));
                } catch (NumberFormatException | DateTimeParseException e) {
                    // línea dañada: se ignora
                }
            }
            top.sort(ORDEN);
            if (top.size() > MAX) top.subList(MAX, top.size()).clear();
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }

    private void escribir(List<Puntaje> lista) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Puntaje p : lista) {
            lineas.add(String.format(Locale.ROOT, "%s;%d;%.2f;%s", p.nombre, p.intentos, p.premio, p.fecha));
        }
        // se escribe a un archivo temporal y luego se reemplaza, para no dejar el archivo a medias
        Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.write(tmp, lineas, StandardCharsets.UTF_8);
        Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
    }
}
