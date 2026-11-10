import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * MODELO (sin Swing ni archivos de datos): analiza una imagen de la cámara.
 *
 *  1. Decide si hay un rostro completo frente a la cámara (y si no, POR QUÉ no).
 *  2. Si lo hay, calcula la "huella" del rostro: un descriptor LBP por zonas
 *     (el mismo principio del LBPH de OpenCV, hecho en Java puro, sin librerías).
 *
 * La huella NO guarda la foto: son histogramas de texturas locales de 6x6 zonas de la cara.
 * Dos huellas se comparan con distancia(); entre más baja, más parecidas.
 */
public class AnalizadorRostro {

    public enum Estado { OK, SIN_IMAGEN, MUY_OSCURA, SIN_ROSTRO, ROSTRO_INCOMPLETO }

    /** Resultado de analizar UNA imagen. Si estado == OK trae los descriptores. */
    public static final class Resultado {
        private final Estado estado;
        private final List<float[]> descriptores;

        private Resultado(Estado estado, List<float[]> descriptores) {
            this.estado = estado;
            this.descriptores = descriptores;
        }

        public Estado getEstado() { return estado; }
        public boolean isOk() { return estado == Estado.OK; }

        /** [0] = recorte centrado (el que se guarda al registrar); el resto, variantes algo desplazadas. */
        public List<float[]> getDescriptores() { return descriptores; }
        public float[] getDescriptor() { return descriptores.isEmpty() ? null : descriptores.get(0); }
    }

    // ---- Parámetros del descriptor ----
    private static final int LADO = 96;       // la cara se normaliza a 96x96
    private static final int REJILLA = 6;     // 6x6 zonas (cada una de 16x16 px)
    private static final int BINS = 59;       // LBP "uniforme": 58 patrones + 1 para el resto
    // Un vecino cuenta como "más claro" solo si supera al centro por más de esto (0-255).
    private static final int TOLERANCIA_RUIDO = 4;
    public static final int TAM_DESCRIPTOR = REJILLA * REJILLA * BINS;
    private static final int[] TABLA_LBP = crearTablaUniforme();

    // Variantes del recorte (fracción del ancho/alto): compensan que el recorte nunca cae exacto
    private static final double[][] DESPLAZAMIENTOS = {{0, 0}, {-0.06, 0}, {0.06, 0}, {0, -0.06}, {0, 0.06}};

    // ---- Parámetros de la detección ----
    private static final int PASO = 4;                  // se analiza 1 de cada 4 píxeles (rápido)
    private static final double LUZ_MINIMA = 35;        // brillo medio (0-255) por debajo = muy oscuro
    private static final double MIN_ANCHO = 0.12;       // el rostro debe medir al menos 12% del ancho de la imagen
    private static final double MIN_ALTO = 0.15;        // y 15% del alto
    private static final double MIN_AREA = 0.02;        // y tener al menos 2% de la imagen en tono piel
    private static final double MAX_AREA = 0.65;        // más de 65% = demasiado cerca / mano en la lente
    private static final double PROPORCION_MIN = 0.40;  // alto/ancho del rostro (con algo de cuello)
    private static final double PROPORCION_MAX = 2.4;
    private static final double RELLENO_MIN = 0.20;     // un rostro llena buena parte de su caja
    private static final double ASIMETRIA_MAX = 0.40;   // diferencia izquierda/derecha (tapar media cara la dispara)

    private AnalizadorRostro() { }

    // ------------------------------------------------------------------
    // Detección + huella
    // ------------------------------------------------------------------

    public static Resultado analizar(BufferedImage img) {
        if (img == null || img.getWidth() < 64 || img.getHeight() < 64) {
            return new Resultado(Estado.SIN_IMAGEN, Collections.<float[]>emptyList());
        }
        int w = img.getWidth(), h = img.getHeight();
        int[] rgb = img.getRGB(0, 0, w, h, null, 0, w);

        // luminosidad de toda la imagen (sirve para el descriptor y para detectar "muy oscuro")
        int[] luma = new int[w * h];
        long suma = 0;
        for (int i = 0; i < luma.length; i++) {
            int p = rgb[i];
            luma[i] = (299 * ((p >> 16) & 0xFF) + 587 * ((p >> 8) & 0xFF) + 114 * (p & 0xFF)) / 1000;
            suma += luma[i];
        }
        if (suma / (double) luma.length < LUZ_MINIMA) {
            System.out.println("[Facial] rechazo: muy oscura");
            return new Resultado(Estado.MUY_OSCURA, Collections.<float[]>emptyList());
        }

        // máscara de piel en una cuadrícula gruesa
        int gw = w / PASO, gh = h / PASO;
        boolean[] piel = new boolean[gw * gh];
        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                int p = rgb[(gy * PASO + PASO / 2) * w + gx * PASO + PASO / 2];
                piel[gy * gw + gx] = esPiel((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);
            }
        }

        // se unen zonas de piel separadas por poco (gafas, barba, sombras): dilatación de 1 celda
        boolean[] dil = new boolean[piel.length];
        for (int gy = 0; gy < gh; gy++) {
            for (int gx = 0; gx < gw; gx++) {
                if (!piel[gy * gw + gx]) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = gx + dx, ny = gy + dy;
                        if (nx >= 0 && nx < gw && ny >= 0 && ny < gh) dil[ny * gw + nx] = true;
                    }
                }
            }
        }

        // se queda con la mancha de piel más grande (la cara)
        int[] etiqueta = new int[piel.length];
        int[] pila = new int[piel.length];
        int id = 0, mejorId = 0, mejorPiel = 0;
        int bMinX = 0, bMaxX = -1, bMinY = 0, bMaxY = -1;
        for (int s = 0; s < piel.length; s++) {
            if (!dil[s] || etiqueta[s] != 0) continue;
            id++;
            int tope = 0;
            pila[tope++] = s;
            etiqueta[s] = id;
            int cuenta = 0, minX = gw, maxX = -1, minY = gh, maxY = -1;
            while (tope > 0) {
                int p = pila[--tope];
                int x = p % gw, y = p / gw;
                if (piel[p]) {
                    cuenta++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
                if (x > 0 && dil[p - 1] && etiqueta[p - 1] == 0) { etiqueta[p - 1] = id; pila[tope++] = p - 1; }
                if (x < gw - 1 && dil[p + 1] && etiqueta[p + 1] == 0) { etiqueta[p + 1] = id; pila[tope++] = p + 1; }
                if (y > 0 && dil[p - gw] && etiqueta[p - gw] == 0) { etiqueta[p - gw] = id; pila[tope++] = p - gw; }
                if (y < gh - 1 && dil[p + gw] && etiqueta[p + gw] == 0) { etiqueta[p + gw] = id; pila[tope++] = p + gw; }
            }
            if (cuenta > mejorPiel) {
                mejorPiel = cuenta; mejorId = id;
                bMinX = minX; bMaxX = maxX; bMinY = minY; bMaxY = maxY;
            }
        }

        // 1) ¿hay algo con tamaño de rostro? si no, "no hay nadie"
        int bw = bMaxX - bMinX + 1, bh = bMaxY - bMinY + 1;
        if (mejorPiel == 0 || bw < gw * MIN_ANCHO || bh < gh * MIN_ALTO || mejorPiel < gw * gh * MIN_AREA) {
            System.out.println("[Facial] rechazo: sin rostro. piel=" + mejorPiel + " de " + (gw * gh)
                    + " ancho=" + bw + "/" + gw + " alto=" + bh + "/" + gh);
            return new Resultado(Estado.SIN_ROSTRO, Collections.<float[]>emptyList());
        }
        // 2) ¿demasiado cerca o algo cubriendo la lente?
        if (mejorPiel > gw * gh * MAX_AREA || (bw > gw * 0.92 && bh > gh * 0.92)) {
            System.out.println("[Facial] rechazo: demasiado cerca/area. piel=" + mejorPiel
                    + " de " + (gw * gh) + " ancho=" + bw + "/" + gw + " alto=" + bh + "/" + gh);
            return new Resultado(Estado.ROSTRO_INCOMPLETO, Collections.<float[]>emptyList());
        }
        // 3) proporción y relleno propios de un rostro
        double proporcion = bh / (double) bw;
        double relleno = mejorPiel / (double) (bw * bh);
        if (proporcion < PROPORCION_MIN || proporcion > PROPORCION_MAX || relleno < RELLENO_MIN) {
            System.out.printf(Locale.ROOT, "[Facial] rechazo: forma. proporcion=%.2f (%.2f-%.2f) relleno=%.2f (min %.2f)%n",
                    proporcion, PROPORCION_MIN, PROPORCION_MAX, relleno, RELLENO_MIN);
            return new Resultado(Estado.ROSTRO_INCOMPLETO, Collections.<float[]>emptyList());
        }
        // 4) simetría izquierda/derecha: si tapas media cara, se dispara
        double centro = bMinX + bw / 2.0;
        int izq = 0, der = 0;
        for (int gy = bMinY; gy <= bMaxY; gy++) {
            for (int gx = bMinX; gx <= bMaxX; gx++) {
                int p = gy * gw + gx;
                if (piel[p] && etiqueta[p] == mejorId) {
                    if (gx + 0.5 < centro) izq++; else if (gx + 0.5 > centro) der++;
                }
            }
        }
        double asimetria = (izq + der) == 0 ? 1 : Math.abs(izq - der) / (double) (izq + der);
        if (asimetria > ASIMETRIA_MAX) {
            System.out.printf(Locale.ROOT, "[Facial] rechazo: asimetria=%.2f (max %.2f) izq=%d der=%d%n",
                    asimetria, ASIMETRIA_MAX, izq, der);
            return new Resultado(Estado.ROSTRO_INCOMPLETO, Collections.<float[]>emptyList());
        }

        // recorte del rostro: ancho de la mancha (sin los bordes, donde hay pelo/fondo) y alto fijo
        // = 1,25 x ancho, anclado en la frente. Así el cuello no cambia el recorte entre capturas.
        int caraX = bMinX * PASO, caraY = bMinY * PASO, caraAncho = bw * PASO;
        int ancho = (int) (caraAncho * 0.88);
        int alto = (int) (ancho * 1.25);
        int x0 = caraX + (int) (caraAncho * 0.06);

        List<float[]> descriptores = new ArrayList<>();
        for (double[] d : DESPLAZAMIENTOS) {
            Rectangle r = ajustar(new Rectangle(x0 + (int) (d[0] * ancho), caraY + (int) (d[1] * alto), ancho, alto), w, h);
            descriptores.add(descriptor(luma, w, r));
        }
        return new Resultado(Estado.OK, descriptores);
    }

    /** Tono de piel en el espacio YCbCr (funciona mejor que RGB con distintas luces y tonos). */
    private static boolean esPiel(int r, int g, int b) {
        double y = 0.299 * r + 0.587 * g + 0.114 * b;
        double cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
        double cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
        // Cr hasta 180 (el rango clásico llega a 173): con luz cálida o piel rosada el lado más
        // iluminado de la cara se salía del rango y se rechazaba por "asimétrico".
        return y >= 40 && cr >= 133 && cr <= 180 && cb >= 77 && cb <= 127;
    }

    /** Mantiene el rectángulo dentro de la imagen (lo desplaza y, si no cabe, lo encoge). */
    private static Rectangle ajustar(Rectangle r, int w, int h) {
        int rw = Math.min(r.width, w), rh = Math.min(r.height, h);
        int x = Math.max(0, Math.min(r.x, w - rw));
        int y = Math.max(0, Math.min(r.y, h - rh));
        return new Rectangle(x, y, rw, rh);
    }

    // ------------------------------------------------------------------
    // Descriptor LBP por zonas
    // ------------------------------------------------------------------

    /** Huella de la zona 'r' de una imagen en grises (luma de w píxeles de ancho). */
    static float[] descriptor(int[] luma, int w, Rectangle r) {
        // 1) el recorte se lleva a (LADO+2)x(LADO+2) promediando cajas de píxeles (suaviza el ruido de la cámara)
        int d = LADO + 2;
        int[] parche = new int[d * d];
        for (int j = 0; j < d; j++) {
            int ys = r.y + j * r.height / d;
            int ye = Math.max(ys + 1, r.y + (j + 1) * r.height / d);
            for (int i = 0; i < d; i++) {
                int xs = r.x + i * r.width / d;
                int xe = Math.max(xs + 1, r.x + (i + 1) * r.width / d);
                long s = 0;
                for (int yy = ys; yy < ye; yy++) {
                    for (int xx = xs; xx < xe; xx++) s += luma[yy * w + xx];
                }
                parche[j * d + i] = (int) (s / ((long) (ye - ys) * (xe - xs)));
            }
        }

        // 1b) suavizado 3x3: quita el ruido que queda antes de buscar bordes
        int[] suave = new int[d * d];
        for (int y = 0; y < d; y++) {
            for (int x = 0; x < d; x++) {
                int s = 0, n = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int yy = y + dy, xx = x + dx;
                        if (yy >= 0 && yy < d && xx >= 0 && xx < d) { s += parche[yy * d + xx]; n++; }
                    }
                }
                suave[y * d + x] = s / n;
            }
        }
        parche = suave;

        // 2) LBP: cada píxel se compara con sus 8 vecinos; el patrón se cuenta en el histograma de su zona
        float[] desc = new float[TAM_DESCRIPTOR];
        int celda = LADO / REJILLA;
        for (int y = 1; y <= LADO; y++) {
            for (int x = 1; x <= LADO; x++) {
                int c = parche[y * d + x] + TOLERANCIA_RUIDO;
                int codigo = 0;
                if (parche[(y - 1) * d + (x - 1)] >= c) codigo |= 1;
                if (parche[(y - 1) * d + x] >= c) codigo |= 2;
                if (parche[(y - 1) * d + (x + 1)] >= c) codigo |= 4;
                if (parche[y * d + (x + 1)] >= c) codigo |= 8;
                if (parche[(y + 1) * d + (x + 1)] >= c) codigo |= 16;
                if (parche[(y + 1) * d + x] >= c) codigo |= 32;
                if (parche[(y + 1) * d + (x - 1)] >= c) codigo |= 64;
                if (parche[y * d + (x - 1)] >= c) codigo |= 128;
                int zona = ((y - 1) / celda) * REJILLA + (x - 1) / celda;
                desc[zona * BINS + TABLA_LBP[codigo]]++;
            }
        }
        // 3) cada zona se normaliza (suma 1) para que no dependa del tamaño
        float tam = celda * celda;
        for (int i = 0; i < desc.length; i++) desc[i] /= tam;
        return desc;
    }

    /**
     * Distancia entre dos huellas (chi-cuadrado promedio por zona): 0 = idénticas, hasta 2 = totalmente distintas.
     */
    public static double distancia(float[] a, float[] b) {
        double total = 0;
        for (int z = 0; z < REJILLA * REJILLA; z++) {
            int base = z * BINS;
            for (int k = 0; k < BINS; k++) {
                double x = a[base + k], y = b[base + k], s = x + y;
                if (s > 0) total += (x - y) * (x - y) / s;
            }
        }
        return total / (REJILLA * REJILLA);
    }

    /** LBP "uniforme": los 58 patrones con a lo sumo 2 cambios 0/1 tienen su casilla; los demás comparten la 59. */
    private static int[] crearTablaUniforme() {
        int[] tabla = new int[256];
        int siguiente = 0;
        for (int c = 0; c < 256; c++) {
            int cambios = 0;
            for (int b = 0; b < 8; b++) {
                if (((c >> b) & 1) != ((c >> ((b + 1) % 8)) & 1)) cambios++;
            }
            tabla[c] = cambios <= 2 ? siguiente++ : -1;
        }
        for (int c = 0; c < 256; c++) if (tabla[c] == -1) tabla[c] = siguiente;
        return tabla;
    }
}