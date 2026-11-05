import java.io.File;
import java.net.URL;
import javax.sound.sampled.*;

/** Maneja la música de fondo (musica.wav) para todo el programa. */
public class GestorMusica {

    private static Clip clip;
    private static boolean activa = true; // estado del botón de Ajustes

    private GestorMusica() { }

    /** Carga el audio y lo pone a sonar si la música está activada. Llamar una vez al inicio. */
    public static void iniciar() {
        try {
            URL url = GestorMusica.class.getResource("/musica.wav");
            AudioInputStream audio = null;
            if (url != null) {
                audio = AudioSystem.getAudioInputStream(url);
            } else {
                for (String ruta : new String[]{"musica.wav", "Proyecto-dragon-ball/musica.wav", "src/musica.wav"}) {
                    File f = new File(ruta);
                    if (f.exists()) { audio = AudioSystem.getAudioInputStream(f); break; }
                }
            }
            if (audio == null) {
                System.err.println("No se encontró musica.wav");
                return;
            }
            clip = AudioSystem.getClip();
            clip.open(audio);
            audio.close();
            aplicar();
        } catch (Exception e) {
            System.err.println("No se pudo cargar musica.wav: " + e.getMessage());
        }
    }

    public static boolean isActiva() { return activa; }

    /** Activa o apaga la música (lo llama el botón de Ajustes). */
    public static void setActiva(boolean valor) {
        activa = valor;
        aplicar();
    }

    private static void aplicar() {
        if (clip == null) return;
        if (activa) {
            if (!clip.isRunning()) clip.loop(Clip.LOOP_CONTINUOUSLY); // se repite sin parar
        } else {
            clip.stop();
        }
    }
}