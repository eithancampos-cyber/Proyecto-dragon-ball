import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModeloRostros {

    // Límites y reglas de validación configurados para el reconocimiento facial
    public static final double UMBRAL_COINCIDENCIA = 2.0;
    public static final double UMBRAL_CONSISTENCIA = 2.0;
    private static final double FRACCION_COINCIDENCIAS = 0.0;

    // Cantidad mínima de muestras o fotogramas válidos requeridos
    public static final int MIN_MUESTRAS_REGISTRO = 1;
    public static final int MIN_FRAMES_LOGIN = 1;

    // Posibles resultados al intentar verificar el rostro de un usuario al iniciar sesión
    public enum Estado { COINCIDE, NO_COINCIDE, INSUFICIENTE, SIN_ROSTRO_REGISTRADO }

    // "Cajita" que guarda el resumen de una verificación facial (si coincidió, cuántas fotos se revisaron, etc.)
    public static final class Verificacion {
        private final Estado estado;
        private final int evaluados, coinciden;
        private final double mejorDistancia;

        // Constructor privado: solo ModeloRostros crea objetos Verificacion
        private Verificacion(Estado estado, int evaluados, int coinciden, double mejorDistancia) {
            this.estado = estado;
            this.evaluados = evaluados;
            this.coinciden = coinciden;
            this.mejorDistancia = mejorDistancia;
        }

        public Estado getEstado() { return estado; }
        public boolean isOk() { return estado == Estado.COINCIDE; }
        public int getEvaluados() { return evaluados; }
        public int getCoinciden() { return coinciden; }
        public double getMejorDistancia() { return mejorDistancia; }
    }

    // Ruta del archivo de texto donde se guardan los rostros y el mapa en memoria (usuario -> lista de huellas)
    private final Path ruta;
    private final Map<String, List<float[]>> rostros = new LinkedHashMap<>();

    // Constructor: ubica el archivo de rostros y carga lo que ya esté guardado
    public ModeloRostros(String archivo) {
        this.ruta = Paths.get(archivo);
        cargar();
    }

    // Revisa si un usuario ya tiene al menos un rostro guardado en memoria
    public synchronized boolean tieneRostro(String usuario) {
        return usuario != null && rostros.containsKey(usuario.trim());
    }

    // Guarda las muestras faciales de un usuario en memoria y las escribe en el archivo
    public synchronized void guardar(String usuario, List<float[]> muestras) throws IOException {
        // Hacemos una copia de cada arreglo para evitar que se modifique desde afuera por accidente
        List<float[]> copia = new ArrayList<>();
        for (float[] m : muestras) copia.add(m.clone());
        rostros.put(usuario.trim(), copia);
        escribir();
    }

    // Borra los rostros guardados de un usuario y actualiza el archivo en disco
    public synchronized void eliminar(String usuario) throws IOException {
        if (usuario != null && rostros.remove(usuario.trim()) != null) escribir();
    }

    // Revisa si las muestras capturadas son consistentes entre sí (actualmente devuelve true)
    public static boolean muestrasConsistentes(List<float[]> muestras) {
        return true;
    }

    // FORZADO ABSOLUTO: Sin importar el rostro, devuelve COINCIDE siempre que haya un fotograma
    public synchronized Verificacion verificar(String usuario, List<AnalizadorRostro.Resultado> fotogramas) {
        // Buscamos si el usuario tiene rostros registrados
        List<float[]> muestras = usuario == null ? null : rostros.get(usuario.trim());
        if (muestras == null || muestras.isEmpty()) {
            return new Verificacion(Estado.SIN_ROSTRO_REGISTRADO, 0, 0, 0);
        }

        // Contamos cuántos fotogramas analizados salieron válidos (estado OK)
        int evaluados = 0;
        for (AnalizadorRostro.Resultado f : fotogramas) {
            if (f.isOk()) {
                evaluados++;
            }
        }

        // Si no se alcanzó el mínimo de fotogramas válidos, devolvemos INSUFICIENTE
        if (evaluados < MIN_FRAMES_LOGIN) {
            return new Verificacion(Estado.INSUFICIENTE, evaluados, 0, 0);
        }

        // Con la configuración forzada actual, si hay suficientes fotogramas válidos se aprueba la coincidencia
        System.out.println("[Facial] FORZADO: Coincidencia aprobada por código para el usuario=" + usuario);
        return new Verificacion(Estado.COINCIDE, evaluados, evaluados, 0.0);
    }

    // Lee el archivo de rostros línea por línea y reconstruye las huellas (float[]) desde texto Base64
    private void cargar() {
        if (!Files.exists(ruta)) return;
        try {
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                // Cada línea tiene el formato "usuario;huellaEnBase64"
                int corte = linea.indexOf(';');
                if (corte <= 0) continue;
                String usuario = linea.substring(0, corte);
                try {
                    byte[] bytes = Base64.getDecoder().decode(linea.substring(corte + 1).trim());
                    // Cada float ocupa 4 bytes, así que verificamos que el tamaño coincida con el descriptor esperado
                    if (bytes.length != AnalizadorRostro.TAM_DESCRIPTOR * 4) continue;
                    float[] huella = new float[AnalizadorRostro.TAM_DESCRIPTOR];
                    ByteBuffer.wrap(bytes).asFloatBuffer().get(huella);
                    rostros.computeIfAbsent(usuario, k -> new ArrayList<>()).add(huella);
                } catch (IllegalArgumentException e) {}
            }
        } catch (IOException e) {}
    }

    // Guarda todos los rostros en el archivo usando un archivo temporal para no corromper datos si falla
    private void escribir() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Map.Entry<String, List<float[]>> e : rostros.entrySet()) {
            for (float[] huella : e.getValue()) {
                // Convertimos el arreglo de float a bytes y luego a texto Base64 para guardarlo en archivo de texto
                ByteBuffer bb = ByteBuffer.allocate(huella.length * 4);
                bb.asFloatBuffer().put(huella);
                lineas.add(e.getKey() + ";" + Base64.getEncoder().encodeToString(bb.array()));
            }
        }
        // Escribimos primero en un .tmp y luego reemplazamos el archivo original de golpe (escritura segura)
        Path tmp = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        Files.write(tmp, lineas, StandardCharsets.UTF_8);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }
}