import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Reproduce las animaciones del juego (parte de la VISTA).
 * Cada animación es una carpeta con frames numerados desde 0:
 *
 *   animaciones/intro/0.png, 1.png, 2.png ...
 *   animaciones/poder/0.png ...
 *   animaciones/victoria/0.png ...
 *   animaciones/derrota/0.png ...
 *
 * La carpeta "animaciones" va junto a fondo4.jpg (en el classpath o en la carpeta del proyecto).
 *
 * Hay un Animador por personaje:
 *  - Jugador 1: carpeta "animaciones".
 *  - Jugador 2: carpeta "animaciones2" con las mismas subcarpetas. Si no existe, usa las imágenes
 *    de "animaciones" volteadas en espejo, para que mire hacia el otro lado.
 *  - Choque de poderes: animaciones/choque/ (se dibuja entre los dos personajes).
 * Si una animación no tiene frames, se salta y se ejecuta de inmediato lo que venía después,
 * así el juego nunca se queda trabado.
 */
public class Animador {

    /** Las animaciones: carpeta, fotogramas por segundo y si se queda en el último frame. */
    public enum Animacion {
        INTRO("intro", 3, false),
        PODER("poder", 2, true),
        VICTORIA("victoria", 2, true),
        DERROTA("derrota", 2, true),
        CHOQUE("choque", 24, true);

        final String carpeta;
        final int fps;
        final boolean mantenerUltimo;

        Animacion(String carpeta, int fps, boolean mantenerUltimo) {
            this.carpeta = carpeta;
            this.fps = fps;
            this.mantenerUltimo = mantenerUltimo;
        }
    }

    private final String raiz;          // carpeta principal de las animaciones
    private final String raizRespaldo;  // si falta algo en 'raiz', se usa esta carpeta volteada en espejo (puede ser null)

    private final JComponent destino; // el componente donde se dibuja (para llamar a repaint)
    private final Map<Animacion, List<BufferedImage>> frames = new EnumMap<>(Animacion.class);

    private List<BufferedImage> actual; // null = no hay nada que dibujar
    private int indice;
    private boolean mantener;
    private Timer timer;
    private Runnable alTerminar;

    // Posición del personaje: se ancla por lo que realmente se ve (píxeles no transparentes)
    private Rectangle caja;          // zona visible que abarca el personaje en todos los frames
    private double escala = 1;
    private int centroX, piesY, tamano;
    private boolean porAncho;        // true = 'tamano' es el ancho en pantalla; false = el alto

    /** Animador del jugador 1 (todas las animaciones de personaje, carpeta "animaciones"). */
    public Animador(JComponent destino) {
        this(destino, "animaciones", null,
                EnumSet.of(Animacion.INTRO, Animacion.PODER, Animacion.VICTORIA, Animacion.DERROTA));
    }

    /**
     * @param raiz         carpeta donde están las subcarpetas de las animaciones
     * @param raizRespaldo carpeta que se usa, volteada en espejo, si en 'raiz' no hay frames (null = no hay respaldo)
     * @param cuales       qué animaciones carga este animador
     */
    public Animador(JComponent destino, String raiz, String raizRespaldo, Set<Animacion> cuales) {
        this.destino = destino;
        this.raiz = raiz;
        this.raizRespaldo = raizRespaldo;
        for (Animacion a : cuales) {
            frames.put(a, cargar(a));
        }
        calcularCaja();
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    private List<BufferedImage> cargar(Animacion anim) {
        List<BufferedImage> lista = cargarDe(raiz, anim);

        // sin frames propios: usa los de la carpeta de respaldo, volteados para que miren al otro lado
        if (lista.isEmpty() && raizRespaldo != null) {
            for (BufferedImage img : cargarDe(raizRespaldo, anim)) lista.add(espejar(img));
            if (!lista.isEmpty()) {
                System.out.println("[Animador] " + raiz + "/" + anim.carpeta + " no existe: se usa "
                        + raizRespaldo + "/" + anim.carpeta + " en espejo");
            }
        }

        if (lista.isEmpty()) {
            System.err.println("[Animador] NO se encontraron frames de '" + raiz + "/" + anim.carpeta
                    + "'. Carpeta de trabajo: " + new File(".").getAbsolutePath());
        }
        return lista;
    }

    private List<BufferedImage> cargarDe(String raiz, Animacion anim) {
        List<BufferedImage> lista = new ArrayList<>();

        // 1) busca la carpeta en disco y lee TODAS las imágenes que tenga, sin importar cómo se llamen
        File dir = buscarCarpeta(raiz, anim);
        if (dir != null) {
            File[] archivos = dir.listFiles((d, n) -> n.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif)"));
            if (archivos != null) {
                Arrays.sort(archivos, (a, b) -> clave(a.getName()).compareTo(clave(b.getName())));
                for (File f : archivos) lista.addAll(leerFrames(f));
            }
        }

        // 2) si no (por ejemplo dentro de un .jar), prueba 0.png, 1.png... desde el classpath
        if (lista.isEmpty()) {
            String base = raiz + "/" + anim.carpeta + "/";
            int inicio = (leer(base + "0.png") != null) ? 0 : 1;
            for (int i = inicio; ; i++) {
                BufferedImage img = leer(base + i + ".png");
                if (img == null) break;
                lista.add(img);
            }
        }

        if (!lista.isEmpty()) {
            System.out.println("[Animador] " + raiz + "/" + anim.carpeta + ": " + lista.size() + " frames"
                    + (dir != null ? " desde " + dir.getAbsolutePath() : ""));
        }
        return lista;
    }

    /** Voltea una imagen horizontalmente (efecto espejo). */
    private static BufferedImage espejar(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = r.createGraphics();
        g.drawImage(img, w, 0, 0, h, 0, 0, w, h, null);
        g.dispose();
        return r;
    }

    /** Busca la subcarpeta de la animación (sin distinguir mayúsculas; acepta nombres que la contengan). */
    private File buscarCarpeta(String raizNombre, Animacion anim) {
        List<File> raices = new ArrayList<>();
        URL recurso = getClass().getResource("/" + raizNombre);
        if (recurso != null && "file".equals(recurso.getProtocol())) {
            try { raices.add(new File(recurso.toURI())); } catch (Exception e) { /* se ignora */ }
        }
        raices.add(new File(raizNombre));
        raices.add(new File("src/" + raizNombre));

        for (File r : raices) {
            File[] subs = r.listFiles(File::isDirectory);
            if (subs == null) continue;
            for (File d : subs) if (d.getName().equalsIgnoreCase(anim.carpeta)) return d;
            for (File d : subs) if (d.getName().toLowerCase().contains(anim.carpeta)) return d;
        }
        return null;
    }

    /** Convierte los números del nombre a 10 dígitos para ordenar bien (2.png antes que 10.png). */
    private static String clave(String nombre) {
        StringBuilder sb = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(nombre.toLowerCase());
        int ultimo = 0;
        while (m.find()) {
            sb.append(nombre.toLowerCase(), ultimo, m.start());
            sb.append(String.format("%010d", Long.parseLong(m.group())));
            ultimo = m.end();
        }
        sb.append(nombre.toLowerCase().substring(ultimo));
        return sb.toString();
    }

    /** Lee un archivo de imagen; si es un GIF animado devuelve todos sus fotogramas. */
    private List<BufferedImage> leerFrames(File archivo) {
        List<BufferedImage> resultado = new ArrayList<>();
        try {
            if (archivo.getName().toLowerCase().endsWith(".gif")) {
                try (ImageInputStream in = ImageIO.createImageInputStream(archivo)) {
                    Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
                    if (it.hasNext()) {
                        ImageReader reader = it.next();
                        reader.setInput(in);
                        for (int i = 0; i < reader.getNumImages(true); i++) resultado.add(reader.read(i));
                        reader.dispose();
                        return resultado;
                    }
                }
            }
            BufferedImage img = ImageIO.read(archivo);
            if (img != null) resultado.add(img);
        } catch (IOException e) {
            System.err.println("[Animador] No se pudo leer " + archivo + ": " + e.getMessage());
        }
        return resultado;
    }

    private BufferedImage leer(String ruta) {
        try {
            URL recurso = getClass().getResource("/" + ruta);
            if (recurso != null) return ImageIO.read(recurso);
            File archivo = new File(ruta);
            if (archivo.exists()) return ImageIO.read(archivo);
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ruta + ": " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Reproducción
    // ------------------------------------------------------------------

    /** Reproduce una animación. 'alTerminar' (puede ser null) se ejecuta al llegar al último frame. */
    public void reproducir(Animacion anim, Runnable alTerminar) {
        detener(); // corta la anterior sin ejecutar su 'alTerminar'

        List<BufferedImage> lista = frames.get(anim);
        if (lista == null || lista.isEmpty()) {
            if (alTerminar != null) alTerminar.run();
            return;
        }

        actual = lista;
        indice = 0;
        mantener = anim.mantenerUltimo;
        this.alTerminar = alTerminar;

        timer = new Timer(1000 / anim.fps, e -> avanzar());
        timer.start();
        destino.repaint();
    }

    private void avanzar() {
        if (indice < actual.size() - 1) {
            indice++;
            destino.repaint();
            return;
        }
        // llegó al último frame
        timer.stop();
        Runnable cb = alTerminar;
        alTerminar = null;
        if (!mantener) actual = null; // victoria/derrota se quedan en pantalla hasta detener()
        destino.repaint();
        if (cb != null) cb.run();
    }

    /** Corta la animación actual y la quita de la pantalla. */
    public void detener() {
        if (timer != null) timer.stop();
        actual = null;
        alTerminar = null;
        destino.repaint();
    }

    // ------------------------------------------------------------------
    // Posición y dibujo
    // ------------------------------------------------------------------

    /**
     * Define dónde "está parado" el personaje (escalado por su alto).
     * @param centroX x donde queda el centro del personaje
     * @param piesY   y donde quedan sus pies
     * @param alto    alto en píxeles que ocupa en pantalla
     */
    public void setAnclaje(int centroX, int piesY, int alto) {
        setAnclaje(centroX, piesY, alto, false);
    }

    /** Igual que el anterior, pero con 'porAncho' = true el tamaño indicado es el ANCHO en pantalla. */
    public void setAnclaje(int centroX, int piesY, int tamano, boolean porAncho) {
        this.centroX = centroX;
        this.piesY = piesY;
        this.tamano = tamano;
        this.porAncho = porAncho;
        recalcularEscala();
        destino.repaint();
    }

    private void recalcularEscala() {
        if (caja == null || tamano <= 0) return;
        escala = tamano / (double) (porAncho ? caja.width : caja.height);
    }

    /** Calcula la zona visible (sin transparencia) que abarcan todos los frames de todas las animaciones. */
    private void calcularCaja() {
        Rectangle total = null;
        for (List<BufferedImage> lista : frames.values()) {
            for (BufferedImage img : lista) {
                Rectangle r = cajaVisible(img);
                if (r != null) total = (total == null) ? r : total.union(r);
            }
        }
        caja = total;
        recalcularEscala();
    }

    private static Rectangle cajaVisible(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        if (!img.getColorModel().hasAlpha()) return new Rectangle(0, 0, w, h); // sin transparencia: todo el lienzo
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) > 128) { // ignora brillos/sombras casi transparentes
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return (maxX < 0) ? null : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** Dibuja el frame actual con los pies del personaje sobre (centroX, piesY). */
    public void pintar(Graphics2D g2) {
        if (actual == null || caja == null) return;
        BufferedImage img = actual.get(indice);

        int w = (int) Math.round(img.getWidth() * escala);
        int h = (int) Math.round(img.getHeight() * escala);
        int x = centroX - (int) Math.round(caja.getCenterX() * escala);
        int y = piesY - (int) Math.round((caja.y + caja.height) * escala);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x, y, w, h, null);
    }
}