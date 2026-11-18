import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Detector de rostro y ojos con OpenCV (clasificadores Haar).
 *
 * OpenCV se usa por REFLEXIÓN (igual que la cámara en ReconocimientoFacial): esta clase compila aunque el editor
 * no tenga el jar de OpenCV en el classpath. En tiempo de ejecución el jar lo trae el pom.xml (org.openpnp:opencv).
 * Si OpenCV o los clasificadores no cargan, disponible() da false y AnalizadorRostro usa su método de respaldo
 * (color de piel), así el programa nunca se rompe.
 *
 * Los clasificadores son dos archivos .xml (no son librerías .jar):
 *   haarcascade_frontalface_default.xml  (rostro)   y   haarcascade_eye.xml  (ojos)
 * Se buscan en este orden: carpeta del proyecto, classpath, carpeta de caché (~/.dragonball-cascades) y,
 * si no están en ninguna, se descargan una sola vez desde el repositorio oficial de OpenCV.
 */
public final class DetectorOpenCV {

    /** Rostro encontrado: su caja y cuántos ojos se vieron en la mitad de arriba. */
    public static final class Deteccion {
        public final Rectangle rostro;
        public final int ojos;
        Deteccion(Rectangle rostro, int ojos) { this.rostro = rostro; this.ojos = ojos; }
    }

    private static final String URL_BASE = "https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/";
    private static final double MIN_TAM_ROSTRO = 0.15; // el rostro debe medir al menos 15% del lado menor de la imagen

    private static boolean intentado = false;
    private static boolean disponible = false;

    // clases y métodos de OpenCV (se resuelven una sola vez)
    private static Class<?> cMat, cMatOfRect, cRect, cSize, cImgproc, cCascade;
    private static Constructor<?> nuevaMat, nuevaMatVacia, nuevoMatOfRect, nuevoRect, nuevoSize, nuevoCascade;
    private static Method mPut, mSubmat, mRelease, mEcualizar, mDetectar, mToArray, mVacio;
    private static Object clasifRostro, clasifOjos;
    private static int tipoGris8;

    private DetectorOpenCV() { }

    public static synchronized boolean disponible() {
        if (!intentado) {
            intentado = true;
            try {
                Class.forName("nu.pattern.OpenCV").getMethod("loadLocally").invoke(null); // carga la librería nativa del jar

                cMat = Class.forName("org.opencv.core.Mat");
                cMatOfRect = Class.forName("org.opencv.core.MatOfRect");
                cRect = Class.forName("org.opencv.core.Rect");
                cSize = Class.forName("org.opencv.core.Size");
                cImgproc = Class.forName("org.opencv.imgproc.Imgproc");
                cCascade = Class.forName("org.opencv.objdetect.CascadeClassifier");

                tipoGris8 = Class.forName("org.opencv.core.CvType").getField("CV_8UC1").getInt(null);
                nuevaMat = cMat.getConstructor(int.class, int.class, int.class);
                nuevaMatVacia = cMat.getConstructor();
                nuevoMatOfRect = cMatOfRect.getConstructor();
                nuevoRect = cRect.getConstructor(int.class, int.class, int.class, int.class);
                nuevoSize = cSize.getConstructor(double.class, double.class);
                nuevoCascade = cCascade.getConstructor(String.class);

                mPut = cMat.getMethod("put", int.class, int.class, byte[].class);
                mSubmat = cMat.getMethod("submat", cRect);
                mRelease = cMat.getMethod("release");
                mEcualizar = cImgproc.getMethod("equalizeHist", cMat, cMat);
                mDetectar = cCascade.getMethod("detectMultiScale", cMat, cMatOfRect, double.class, int.class, int.class, cSize, cSize);
                mToArray = cMatOfRect.getMethod("toArray");
                mVacio = cCascade.getMethod("empty");

                clasifRostro = cargar("haarcascade_frontalface_default.xml");
                clasifOjos = cargar("haarcascade_eye.xml");
                disponible = clasifRostro != null && clasifOjos != null;
                System.out.println("[Facial] OpenCV " + (disponible ? "listo (deteccion Haar)" : "sin clasificadores: se usa el metodo de respaldo"));
            } catch (Throwable t) {
                Throwable causa = t.getCause() != null ? t.getCause() : t;
                System.err.println("[Facial] OpenCV no disponible (" + causa + "): se usa el metodo de respaldo");
            }
        }
        return disponible;
    }

    /** Devuelve el rostro más grande de la imagen, o null si no hay ninguno. */
    public static synchronized Deteccion detectar(BufferedImage img) {
        if (!disponible()) return null;
        int w = img.getWidth(), h = img.getHeight();

        // imagen en grises -> Mat (sin depender de códecs ni de conversiones de OpenCV)
        BufferedImage gris = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gris.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        byte[] datos = ((DataBufferByte) gris.getRaster().getDataBuffer()).getData();

        Object mat = null, ecualizada = null, rostros = null, ojos = null, zonaOjos = null;
        try {
            mat = nuevaMat.newInstance(h, w, tipoGris8);
            mPut.invoke(mat, 0, 0, datos);
            ecualizada = nuevaMatVacia.newInstance();
            mEcualizar.invoke(null, mat, ecualizada); // compensa poca luz / luz desigual
            rostros = nuevoMatOfRect.newInstance();
            int minimo = (int) (Math.min(w, h) * MIN_TAM_ROSTRO);
            mDetectar.invoke(clasifRostro, ecualizada, rostros, 1.1, 4, 0,
                    nuevoSize.newInstance((double) minimo, (double) minimo), nuevoSize.newInstance(0.0, 0.0));

            int[] mejor = null; // x, y, ancho, alto
            for (Object r : (Object[]) mToArray.invoke(rostros)) {
                int[] c = coordenadas(r);
                if (mejor == null || c[2] * c[3] > mejor[2] * mejor[3]) mejor = c;
            }
            if (mejor == null) return null;

            // ojos: se buscan en la mitad de arriba del rostro
            Object arriba = nuevoRect.newInstance(mejor[0], mejor[1], mejor[2], (int) (mejor[3] * 0.55));
            zonaOjos = mSubmat.invoke(ecualizada, arriba);
            ojos = nuevoMatOfRect.newInstance();
            double minOjo = Math.max(8, (int) (mejor[2] * 0.10));
            mDetectar.invoke(clasifOjos, zonaOjos, ojos, 1.1, 3, 0,
                    nuevoSize.newInstance(minOjo, minOjo), nuevoSize.newInstance(0.0, 0.0));
            int cuantosOjos = ((Object[]) mToArray.invoke(ojos)).length;

            return new Deteccion(new Rectangle(mejor[0], mejor[1], mejor[2], mejor[3]), cuantosOjos);
        } catch (Throwable t) {
            Throwable causa = t.getCause() != null ? t.getCause() : t;
            System.err.println("[Facial] error en OpenCV: " + causa);
            return null;
        } finally {
            liberar(zonaOjos);
            liberar(mat);
            liberar(ecualizada);
            liberar(rostros);
            liberar(ojos);
        }
    }

    private static int[] coordenadas(Object rect) throws ReflectiveOperationException {
        return new int[]{
            cRect.getField("x").getInt(rect), cRect.getField("y").getInt(rect),
            cRect.getField("width").getInt(rect), cRect.getField("height").getInt(rect)
        };
    }

    private static void liberar(Object mat) {
        if (mat == null) return;
        try { mRelease.invoke(mat); } catch (Throwable ignorada) { }
    }

    // ------------------------------------------------------------------
    // Carga de los clasificadores .xml
    // ------------------------------------------------------------------

    private static Object cargar(String nombre) throws ReflectiveOperationException {
        Path ruta = localizar(nombre);
        if (ruta == null) {
            System.err.println("[Facial] no se encontro " + nombre);
            return null;
        }
        Object c = nuevoCascade.newInstance(ruta.toString());
        if ((Boolean) mVacio.invoke(c)) {
            System.err.println("[Facial] no se pudo leer " + ruta);
            return null;
        }
        return c;
    }

    private static Path localizar(String nombre) {
        // 1) carpeta del proyecto (donde está Main.java)
        for (String carpeta : new String[]{".", "Proyecto-dragon-ball", "src", "src/main/resources"}) {
            Path p = Paths.get(carpeta, nombre);
            if (Files.isRegularFile(p)) return p;
        }
        // 2) classpath (por ejemplo, si el .xml está en src/main/resources)
        try (InputStream in = DetectorOpenCV.class.getResourceAsStream("/" + nombre)) {
            if (in != null) {
                Path tmp = Files.createTempFile("cascada-", ".xml");
                tmp.toFile().deleteOnExit();
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                return tmp;
            }
        } catch (IOException ignorada) { }
        // 3) caché en la carpeta del usuario; si no está, se descarga una vez
        Path cache = Paths.get(System.getProperty("user.home"), ".dragonball-cascades", nombre);
        if (Files.isRegularFile(cache)) return cache;
        try {
            Files.createDirectories(cache.getParent());
            URLConnection con = new URL(URL_BASE + nombre).openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(15000);
            Path parcial = cache.resolveSibling(nombre + ".part");
            try (InputStream in = con.getInputStream()) {
                Files.copy(in, parcial, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(parcial) < 10_000) { Files.deleteIfExists(parcial); return null; } // no es un xml válido
            Files.move(parcial, cache, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Facial] descargado " + nombre + " en " + cache);
            return cache;
        } catch (IOException e) {
            System.err.println("[Facial] no se pudo descargar " + nombre + ": " + e.getMessage());
            return null;
        }
    }
}