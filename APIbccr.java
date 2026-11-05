import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta el tipo de cambio del dólar al API del BCCR (SDDE) y calcula el premio.
 * Solo hay que pegar el token en la constante TOKEN.
 */
public class APIbccr {

    // ===== LO ÚNICO QUE TENÉS QUE CAMBIAR =====
    private static final String TOKEN = "SU TOKEN AQUI";
    // ==========================================

    private static final int INDICADOR = 317; // 317 = compra, 318 = venta

    private static double cache = -1; // último tipo de cambio obtenido

    private APIbccr() { }

    /** Consulta el tipo de cambio en un hilo aparte (no congela la ventana). Llamar al inicio. */
    public static void precargar() {
        new Thread(() -> {
            try {
                obtenerTipoCambio();
            } catch (Exception e) {
                System.err.println("No se pudo consultar el BCCR: " + e.getMessage());
            }
        }).start();
    }

    /** Devuelve el último tipo de cambio publicado por el BCCR (colones por dólar). */
    public static synchronized double obtenerTipoCambio() throws Exception {
        if (cache > 0) return cache; // ya se consultó antes

        // Se piden los últimos 10 días porque fines de semana y feriados no tienen dato
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String desde = URLEncoder.encode(LocalDate.now().minusDays(10).format(f), StandardCharsets.UTF_8);
        String hasta = URLEncoder.encode(LocalDate.now().format(f), StandardCharsets.UTF_8);

        String url = "https://apim.bccr.fi.cr/"
                + "SDDE/api/Bccr.GE.SDDE.Publico.Indicadores.API/"
                + "indicadoresEconomicos/" + INDICADOR + "/series"
                + "?fechaInicio=" + desde
                + "&fechaFin=" + hasta
                + "&idioma=ES";

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> respuesta = HttpClient.newHttpClient()
                .send(solicitud, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error del BCCR: " + respuesta.statusCode() + " (revisá el token)");
        }

        // Busca pares "fecha" + "valorDatoPorPeriodo" y se queda con el de la fecha más reciente
        Matcher m = Pattern.compile(
                "\"fecha\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"valorDatoPorPeriodo\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
                .matcher(respuesta.body());

        String fechaMax = "";
        double valor = -1;
        while (m.find()) {
            if (m.group(1).compareTo(fechaMax) > 0) {
                fechaMax = m.group(1);
                valor = Double.parseDouble(m.group(2));
            }
        }
        if (valor <= 0) {
            throw new RuntimeException("No se encontró el valor del tipo de cambio: " + respuesta.body());
        }

        cache = valor;
        return cache;
    }

    /**
     * Premio en colones: 1 / puntaje * 100 * tipoCambio (puntaje = intentos para ganar).
     * Devuelve -1 si no se pudo obtener el tipo de cambio.
     */
    public static double calcularPremio(int intentos) {
        if (intentos <= 0) return -1;
        try {
            return 1.0 / intentos * 100 * obtenerTipoCambio();
        } catch (Exception e) {
            System.err.println("No se pudo calcular el premio: " + e.getMessage());
            return -1;
        }
    }

    // Prueba rápida: ejecutá esta clase sola para ver si el token funciona
    public static void main(String[] args) {
        try {
            System.out.println("Tipo de cambio: ₡" + obtenerTipoCambio());
            System.out.println("Premio con 20 intentos: ₡" + calcularPremio(20));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}