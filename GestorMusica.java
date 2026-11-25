import java.io.File;
import java.net.URL;
import javax.sound.sampled.*;

/** Maneja la música de fondo (musica.wav) para todo el programa. */
public class GestorMusica {

    // Único Clip de audio compartido por todo el programa. Es static porque solo existe una música de
    // fondo.
    private static Clip clip;
    // Indica si la música debe sonar o no. Lo cambia el botón de Ajustes.
    private static boolean activa = true; // estado del botón de Ajustes

    // Constructor privado: es una clase de utilidad (solo métodos static) y no se instancia con new.
    private GestorMusica() { }

    /** Carga el audio y lo pone a sonar si la música está activada. Llamar una vez al inicio. */
    public static void iniciar() {
        try {
            // 1) Primero intenta cargar musica.wav como recurso del classpath (sirve cuando el proyecto
            // va empaquetado en un .jar).
            URL url = GestorMusica.class.getResource("/musica.wav");
            // Flujo de audio. Empieza en null para saber después si se logró abrir algún archivo.
            AudioInputStream audio = null;
            if (url != null) {
                audio = AudioSystem.getAudioInputStream(url);
            } else {
                // 2) Si no está en el classpath, lo busca como archivo en las rutas más comunes del
                // proyecto.
                for (String ruta : new String[]{"musica.wav", "Proyecto-dragon-ball/musica.wav", "src/musica.wav"}) {
                    File f = new File(ruta);
                    if (f.exists()) { audio = AudioSystem.getAudioInputStream(f); break; }
                }
            }
            // Ninguna ruta funcionó: se avisa por consola y el programa sigue funcionando sin música.
            if (audio == null) {
                System.err.println("No se encontró musica.wav");
                return;
            }
            // Un Clip carga el audio completo en memoria y permite repetirlo (loop) y detenerlo.
            clip = AudioSystem.getClip();
            clip.open(audio);
            audio.close();
            // Según el valor de 'activa', empieza a sonar o se deja detenida.
            aplicar();
        // Cualquier error de audio (formato no soportado, equipo sin salida de sonido) no debe cerrar el
        // juego.
        } catch (Exception e) {
            System.err.println("No se pudo cargar musica.wav: " + e.getMessage());
        }
    }

    /**
     * Dice si la música está activada. VentanaPrincipal lo usa para abrir Ajustes con el estado actual.
     */
    public static boolean isActiva() { return activa; }

    /** Activa o apaga la música (lo llama el botón de Ajustes). */
    public static void setActiva(boolean valor) {
        activa = valor;
        aplicar();
    }

    /** Hace que el Clip refleje el estado de 'activa': lo pone a sonar en bucle o lo detiene. */
    private static void aplicar() {
        if (clip == null) return;
        if (activa) {
            // isRunning() evita reiniciar el bucle si la música ya está sonando.
            if (!clip.isRunning()) clip.loop(Clip.LOOP_CONTINUOUSLY); // se repite sin parar
        } else {
            // stop() detiene el audio en el punto actual; al activarla otra vez continúa desde ahí.
            clip.stop();
        }
    }
}