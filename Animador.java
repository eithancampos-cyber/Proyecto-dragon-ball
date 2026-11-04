import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.w3c.dom.Node;

/**
 * Reproduce las animaciones del juego (parte de la VISTA).
 * Cada animación es un archivo GIF con el nombre de la animación:
 *
 *   animaciones/intro.gif
 *   animaciones/victoria.gif
 *   animaciones/derrota.gif
 *
 * También se acepta una subcarpeta con ese nombre que tenga un GIF dentro
 * (animaciones/intro/lo_que_sea.gif).
 * La carpeta "animaciones" va junto a fondo4.jpg (en el classpath o en la carpeta del proyecto).
 *
 * La velocidad la define el propio GIF (cada cuadro dura lo que diga el GIF) y se reproduce
 * una sola vez, sin importar cuántas repeticiones traiga.
 *
 * Hay un Animador por personaje:
 *  - Jugador 1: carpeta "animaciones".
 *  - Jugador 2: carpeta "animaciones2" con los mismos GIF. Si un GIF no existe ahí, usa el de
 *    "animaciones" volteado en espejo, para que mire hacia el otro lado.
 * Si una animación no tiene GIF, se salta y se ejecuta de inmediato lo que venía después,
 * así el juego nunca se queda trabado.
 */
public class Animador {

    /** Las animaciones: nombre del GIF y si se queda en el último cuadro al terminar. */
    public enum Animacion {
        INTRO("intro", false),
        VICTORIA("victoria", true),
        DERROTA("derrota", true);

        final String nombre;
        final boolean mantenerUltimo;

        Animacion(String nombre, boolean mantenerUltimo) {
            this.nombre = nombre;
            this.mantenerUltimo = mantenerUltimo;
        }
    }

    // Si un GIF trae un tiempo de 0 o 1 centésima, los navegadores lo tratan como 100 ms; aquí igual
    private static final int RETRASO_MINIMO_MS = 20;
    private static final int RETRASO_POR_DEFECTO_MS = 100;

    /** Un GIF ya decodificado: un cuadro completo por cada paso, con lo que dura cada uno. */
    private static class Secuencia {
        final List<BufferedImage> cuadros = new ArrayList<>();
        final List<Integer> retrasosMs = new ArrayList<>();

        boolean isEmpty() { return cuadros.isEmpty(); }
        int size() { return cuadros.size(); }
    }

    private final String raiz;          // carpeta principal de las animaciones
    private final String raizRespaldo;  // si falta algo en 'raiz', se usa esta carpeta volteada en espejo (puede ser null)

    private final JComponent destino; // el componente donde se dibuja (para llamar a repaint)
    private final Map<Animacion, Secuencia> secuencias = new EnumMap<>(Animacion.class);

    private Secuencia actual; // null = no hay nada que dibujar
    private int indice;
    private boolean mantener;
    private Timer timer;
    private Runnable alTerminar;
    private int generacion; // sube con cada reproducir()/detener() para ignorar avisos viejos del timer

    // Posición del personaje: se ancla por lo que realmente se ve (píxeles no transparentes)
    private Rectangle caja;          // zona visible que abarca el personaje en todos los cuadros
    private double escala = 1;
    private int centroX, piesY, tamano;

    /** Animador del jugador 1 (todas las animaciones de personaje, carpeta "animaciones"). */
    public Animador(JComponent destino) {
        this(destino, "animaciones", null,
                EnumSet.of(Animacion.INTRO, Animacion.VICTORIA, Animacion.DERROTA));
    }

    /**
     * @param raiz         carpeta donde están los GIF de las animaciones
     * @param raizRespaldo carpeta que se usa, volteada en espejo, si en 'raiz' no está el GIF (null = no hay respaldo)
     * @param cuales       qué animaciones carga este animador
     */
    public Animador(JComponent destino, String raiz, String raizRespaldo, Set<Animacion> cuales) {
        this.destino = destino;
        this.raiz = raiz;
        this.raizRespaldo = raizRespaldo;
        for (Animacion a : cuales) {
            secuencias.put(a, cargar(a));
        }
        calcularCaja();
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    private Secuencia cargar(Animacion anim) {
        Secuencia sec = cargarDe(raiz, anim, false);

        // sin GIF propio: usa el de la carpeta de respaldo, volteado para que mire al otro lado
        if (sec.isEmpty() && raizRespaldo != null) {
            sec = cargarDe(raizRespaldo, anim, true);
            if (!sec.isEmpty()) {
                System.out.println("[Animador] " + raiz + "/" + anim.nombre + ".gif no existe: se usa "
                        + raizRespaldo + "/" + anim.nombre + ".gif en espejo");
            }
        }

        if (sec.isEmpty()) {
            System.err.println("[Animador] NO se encontró el GIF '" + raiz + "/" + anim.nombre
                    + ".gif'. Carpeta de trabajo: " + new File(".").getAbsolutePath());
        }
        return sec;
    }

    private Secuencia cargarDe(String raiz, Animacion anim, boolean espejo) {
        Secuencia sec = new Secuencia();
        URL url = buscarGif(raiz, anim);
        if (url == null) return sec;

        try {
            sec = leerGif(url);
        } catch (IOException e) {
            System.err.println("[Animador] No se pudo leer " + url + ": " + e.getMessage());
            return new Secuencia();
        }
        if (espejo) {
            for (int i = 0; i < sec.cuadros.size(); i++) sec.cuadros.set(i, espejar(sec.cuadros.get(i)));
        }

        System.out.println("[Animador] " + raiz + "/" + anim.nombre + ".gif: " + sec.size() + " cuadros"
                + (espejo ? " (espejo)" : "") + " desde " + url);
        return sec;
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

    /**
     * Busca el GIF de la animación (sin distinguir mayúsculas): primero un archivo "nombre.gif" y,
     * si no está, una subcarpeta con ese nombre que tenga un GIF dentro. Mira en disco y luego en el classpath.
     */
    private URL buscarGif(String raizNombre, Animacion anim) {
        List<File> raices = new ArrayList<>();
        URL recurso = getClass().getResource("/" + raizNombre);
        if (recurso != null && "file".equals(recurso.getProtocol())) {
            try { raices.add(new File(recurso.toURI())); } catch (Exception e) { /* se ignora */ }
        }
        raices.add(new File(raizNombre));
        raices.add(new File("src/" + raizNombre));

        for (File r : raices) {
            File[] archivos = r.listFiles(File::isFile);
            if (archivos != null) {
                for (File f : archivos) {
                    if (f.getName().equalsIgnoreCase(anim.nombre + ".gif")) return aUrl(f);
                }
            }
            File[] subs = r.listFiles(File::isDirectory);
            if (subs == null) continue;
            for (File d : subs) {
                if (!d.getName().equalsIgnoreCase(anim.nombre)) continue;
                File[] gifs = d.listFiles((dir, n) -> n.toLowerCase().endsWith(".gif"));
                if (gifs != null && gifs.length > 0) {
                    Arrays.sort(gifs);
                    return aUrl(gifs[0]);
                }
            }
        }

        // dentro de un .jar no se puede listar la carpeta: se prueba el nombre exacto desde el classpath
        return getClass().getResource("/" + raizNombre + "/" + anim.nombre + ".gif");
    }

    private static URL aUrl(File f) {
        try {
            return f.toURI().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lee todos los cuadros del GIF y los compone sobre un lienzo del tamaño completo,
     * respetando la posición de cada cuadro, la transparencia y el modo de "disposal"
     * (qué hacer con el cuadro antes de dibujar el siguiente). Guarda también cuánto dura cada uno.
     */
    private static Secuencia leerGif(URL url) throws IOException {
        Iterator<ImageReader> lectores = ImageIO.getImageReadersByFormatName("gif");
        if (!lectores.hasNext()) throw new IOException("Java no tiene lector de GIF");
        ImageReader lector = lectores.next();

        try (InputStream in = url.openStream();
             ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            lector.setInput(iis, false);
            int total = lector.getNumImages(true);
            if (total == 0) throw new IOException("el GIF no tiene cuadros");

            // tamaño del lienzo completo del GIF
            int ancho = 0, alto = 0;
            IIOMetadata flujo = lector.getStreamMetadata();
            if (flujo != null) {
                Node raizFlujo = flujo.getAsTree("javax_imageio_gif_stream_1.0");
                Node pantalla = hijo(raizFlujo, "LogicalScreenDescriptor");
                ancho = entero(pantalla, "logicalScreenWidth", 0);
                alto = entero(pantalla, "logicalScreenHeight", 0);
            }
            if (ancho <= 0 || alto <= 0) { // respaldo: se usa el tamaño del primer cuadro
                BufferedImage primero = lector.read(0);
                ancho = primero.getWidth();
                alto = primero.getHeight();
            }

            Secuencia sec = new Secuencia();
            BufferedImage compuesto = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);

            for (int i = 0; i < total; i++) {
                BufferedImage cuadro = lector.read(i);
                Node raiz = lector.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                Node descriptor = hijo(raiz, "ImageDescriptor");
                Node control = hijo(raiz, "GraphicControlExtension");

                int x = entero(descriptor, "imageLeftPosition", 0);
                int y = entero(descriptor, "imageTopPosition", 0);
                int retraso = entero(control, "delayTime", 0) * 10; // viene en centésimas de segundo
                sec.retrasosMs.add(retraso < RETRASO_MINIMO_MS ? RETRASO_POR_DEFECTO_MS : retraso);
                String disposal = texto(control, "disposalMethod", "none");

                // si el cuadro se deshace volviendo al anterior, se guarda cómo estaba antes de dibujarlo
                BufferedImage antes = "restoreToPrevious".equals(disposal) ? copiar(compuesto) : null;

                Graphics2D g = compuesto.createGraphics();
                g.drawImage(cuadro, x, y, null);
                g.dispose();
                sec.cuadros.add(copiar(compuesto));

                // qué se hace con este cuadro antes de dibujar el siguiente
                if ("restoreToBackgroundColor".equals(disposal)) {
                    Graphics2D b = compuesto.createGraphics();
                    b.setComposite(AlphaComposite.Clear);
                    b.fillRect(x, y, cuadro.getWidth(), cuadro.getHeight());
                    b.dispose();
                } else if (antes != null) {
                    compuesto.setData(antes.getRaster());
                }
            }
            return sec;
        } finally {
            lector.dispose();
        }
    }

    private static BufferedImage copiar(BufferedImage origen) {
        BufferedImage copia = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copia.setData(origen.getRaster());
        return copia;
    }

    // ---- ayudas para leer los metadatos del GIF ----

    private static Node hijo(Node padre, String nombre) {
        if (padre == null) return null;
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeName().equals(nombre)) return n;
        }
        return null;
    }

    private static String texto(Node nodo, String atributo, String porDefecto) {
        if (nodo == null || nodo.getAttributes() == null) return porDefecto;
        Node a = nodo.getAttributes().getNamedItem(atributo);
        return (a == null) ? porDefecto : a.getNodeValue();
    }

    private static int entero(Node nodo, String atributo, int porDefecto) {
        try {
            return Integer.parseInt(texto(nodo, atributo, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    // ------------------------------------------------------------------
    // Reproducción
    // ------------------------------------------------------------------

    /** Reproduce una animación. 'alTerminar' (puede ser null) se ejecuta al terminar el último cuadro. */
    public void reproducir(Animacion anim, Runnable alTerminar) {
        detener(); // corta la anterior sin ejecutar su 'alTerminar'

        Secuencia sec = secuencias.get(anim);
        if (sec == null || sec.isEmpty()) {
            if (alTerminar != null) alTerminar.run();
            return;
        }

        actual = sec;
        indice = 0;
        mantener = anim.mantenerUltimo;
        this.alTerminar = alTerminar;

        programarSiguiente();
        destino.repaint();
    }

    /** Espera lo que dura el cuadro actual (según el GIF) y entonces avanza. */
    private void programarSiguiente() {
        final int miGeneracion = generacion;
        timer = new Timer(actual.retrasosMs.get(indice), e -> {
            if (miGeneracion != generacion || actual == null) return; // aviso viejo: la animación ya se cortó
            avanzar();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void avanzar() {
        if (indice < actual.size() - 1) {
            indice++;
            programarSiguiente();
            destino.repaint();
            return;
        }
        // terminó el último cuadro
        Runnable cb = alTerminar;
        alTerminar = null;
        if (!mantener) actual = null; // victoria/derrota se quedan en pantalla hasta detener()
        destino.repaint();
        if (cb != null) cb.run();
    }

    /** Corta la animación actual y la quita de la pantalla (no ejecuta su 'alTerminar'). */
    public void detener() {
        generacion++;
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
     * @param alto    alto en píxeles que ocupa en pantalla (0 = tamaño original del GIF)
     */
    public void setAnclaje(int centroX, int piesY, int alto) {
        this.centroX = centroX;
        this.piesY = piesY;
        this.tamano = alto;
        recalcularEscala();
        destino.repaint();
    }

    private void recalcularEscala() {
        if (caja == null || tamano <= 0) return;
        escala = tamano / (double) caja.height;
    }

    /** Calcula la zona visible (sin transparencia) que abarcan todos los cuadros de todas las animaciones. */
    private void calcularCaja() {
        Rectangle total = null;
        for (Secuencia sec : secuencias.values()) {
            for (BufferedImage img : sec.cuadros) {
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

    /** Dibuja el cuadro actual con los pies del personaje sobre (centroX, piesY). */
    public void pintar(Graphics2D g2) {
        if (actual == null || caja == null) return;
        BufferedImage img = actual.cuadros.get(indice);

        int w = (int) Math.round(img.getWidth() * escala);
        int h = (int) Math.round(img.getHeight() * escala);
        int x = centroX - (int) Math.round(caja.getCenterX() * escala);
        int y = piesY - (int) Math.round((caja.y + caja.height) * escala);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, x, y, w, h, null);
    }
}