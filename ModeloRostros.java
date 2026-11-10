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
import java.util.Locale;
import java.util.Map;

/**
 * MODELO de los rostros registrados. Es la pieza que ASOCIA cada rostro con su usuario.
 *
 * Guarda en rostros.txt (aparte de usuarios.txt, que no se toca) una línea por muestra:
 *     usuario;huella_en_base64
 * Cada usuario tiene varias muestras (se toman varias capturas al registrarlo).
 * No usa Swing: solo guarda, compara y decide si un rostro coincide con el usuario.
 */
public class ModeloRostros {

    /**
     * Distancia máxima para dar un fotograma por "la misma persona" (0 = idéntico, 2 = totalmente distinto).
     * MÁS BAJO = MÁS ESTRICTO. Si tu propio rostro te rechaza, súbelo de a 0.02; si deja entrar a
     * otra persona, bájalo. En la consola se imprime la distancia de cada intento para que la ajustes.
     */
    public static final double UMBRAL_COINCIDENCIA = 0.06;

    /**
     * Al registrar, las capturas deben parecerse entre sí (si no, la persona se movió mucho, cambió la luz
     * o se mezclaron personas). Es más holgado que el umbral de coincidencia porque aquí las huellas se
     * comparan sin el ajuste de desplazamiento que sí se aplica al hacer login.
     */
    public static final double UMBRAL_CONSISTENCIA = 0.10;

    public static final int MIN_MUESTRAS_REGISTRO = 3; // fotogramas buenos mínimos al registrar
    public static final int MIN_FRAMES_LOGIN = 2;      // fotogramas buenos mínimos al hacer login

    public enum Estado { COINCIDE, NO_COINCIDE, INSUFICIENTE, SIN_ROSTRO_REGISTRADO }

    /** Resultado de comparar lo que ve la cámara contra el rostro registrado de un usuario. */
    public static final class Verificacion {
        private final Estado estado;
        private final int evaluados, coinciden;
        private final double mejorDistancia;

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

    private final Path ruta;
    private final Map<String, List<float[]>> rostros = new LinkedHashMap<>(); // clave: usuario tal cual se registró

    public ModeloRostros(String archivo) {
        this.ruta = Paths.get(archivo);
        cargar();
    }

    // ------------------------------------------------------------------
    // Consultas y registro
    // ------------------------------------------------------------------

    public synchronized boolean tieneRostro(String usuario) {
        return usuario != null && rostros.containsKey(usuario.trim());
    }

    /** Guarda (o reemplaza) las muestras del usuario. */
    public synchronized void guardar(String usuario, List<float[]> muestras) throws IOException {
        List<float[]> copia = new ArrayList<>();
        for (float[] m : muestras) copia.add(m.clone());
        rostros.put(usuario.trim(), copia);
        escribir();
    }

    /** Borra el rostro del usuario (si tenía). */
    public synchronized void eliminar(String usuario) throws IOException {
        if (usuario != null && rostros.remove(usuario.trim()) != null) escribir();
    }

    /** ¿Las muestras tomadas al registrar se parecen entre sí? (misma persona, sin moverse). */
    public static boolean muestrasConsistentes(List<float[]> muestras) {
        if (muestras.size() < MIN_MUESTRAS_REGISTRO) return false;
        double suma = 0;
        int pares = 0;
        for (int i = 0; i < muestras.size(); i++) {
            for (int j = i + 1; j < muestras.size(); j++) {
                suma += AnalizadorRostro.distancia(muestras.get(i), muestras.get(j));
                pares++;
            }
        }
        double promedio = suma / pares;
        System.out.printf(Locale.ROOT, "[Facial] registro: distancia promedio entre capturas=%.3f (maximo %.2f)%n",
                promedio, UMBRAL_CONSISTENCIA);
        return promedio <= UMBRAL_CONSISTENCIA;
    }

    // ------------------------------------------------------------------
    // Verificación
    // ------------------------------------------------------------------

    /**
     * Compara los fotogramas de la cámara con el rostro registrado de 'usuario'.
     * Solo cuenta los fotogramas donde se vio un rostro completo. Un fotograma "coincide" si su
     * huella queda a menos de UMBRAL_COINCIDENCIA de alguna muestra del usuario; hace falta que
     * coincida la mayoría (y al menos 2), así un solo fotograma afortunado no basta.
     */
    public synchronized Verificacion verificar(String usuario, List<AnalizadorRostro.Resultado> fotogramas) {
        List<float[]> muestras = usuario == null ? null : rostros.get(usuario.trim());
        if (muestras == null || muestras.isEmpty()) {
            return new Verificacion(Estado.SIN_ROSTRO_REGISTRADO, 0, 0, 2);
        }

        int evaluados = 0, coinciden = 0;
        double mejor = 2;
        for (AnalizadorRostro.Resultado f : fotogramas) {
            if (!f.isOk()) continue;
            evaluados++;
            double d = 2;
            for (float[] variante : f.getDescriptores()) {
                for (float[] muestra : muestras) d = Math.min(d, AnalizadorRostro.distancia(variante, muestra));
            }
            mejor = Math.min(mejor, d);
            if (d <= UMBRAL_COINCIDENCIA) coinciden++;
        }

        int necesarias = Math.max(2, (int) Math.ceil(evaluados * 0.6));
        Estado estado = evaluados < MIN_FRAMES_LOGIN ? Estado.INSUFICIENTE
                : coinciden >= necesarias ? Estado.COINCIDE : Estado.NO_COINCIDE;
        System.out.printf(Locale.ROOT, "[Facial] login usuario=%s fotogramas=%d coinciden=%d mejorDistancia=%.3f umbral=%.2f -> %s%n",
                usuario, evaluados, coinciden, mejor, UMBRAL_COINCIDENCIA, estado);
        return new Verificacion(estado, evaluados, coinciden, mejor);
    }

    // ------------------------------------------------------------------
    // Archivo rostros.txt
    // ------------------------------------------------------------------

    private void cargar() {
        if (!Files.exists(ruta)) return;
        try {
            for (String linea : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                int corte = linea.indexOf(';');
                if (corte <= 0) continue;
                String usuario = linea.substring(0, corte);
                try {
                    byte[] bytes = Base64.getDecoder().decode(linea.substring(corte + 1).trim());
                    if (bytes.length != AnalizadorRostro.TAM_DESCRIPTOR * 4) continue; // línea dañada o de otra versión
                    float[] huella = new float[AnalizadorRostro.TAM_DESCRIPTOR];
                    ByteBuffer.wrap(bytes).asFloatBuffer().get(huella);
                    rostros.computeIfAbsent(usuario, k -> new ArrayList<>()).add(huella);
                } catch (IllegalArgumentException e) {
                    // línea con base64 inválido: se ignora
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ruta + ": " + e.getMessage());
        }
    }

    private void escribir() throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Map.Entry<String, List<float[]>> e : rostros.entrySet()) {
            for (float[] huella : e.getValue()) {
                ByteBuffer bb = ByteBuffer.allocate(huella.length * 4);
                bb.asFloatBuffer().put(huella);
                lineas.add(e.getKey() + ";" + Base64.getEncoder().encodeToString(bb.array()));
            }
        }
        // se escribe a un archivo temporal y luego se reemplaza, para no dejar rostros.txt a medias
        Path tmp = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        Files.write(tmp, lineas, StandardCharsets.UTF_8);
        Files.move(tmp, ruta, StandardCopyOption.REPLACE_EXISTING);
    }
}