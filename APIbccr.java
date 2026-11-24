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
    // Token de autorización para conectarse al servicio web del Banco Central de Costa Rica (BCCR)
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJCQ0NSLVNEREUiLCJzdWIiOiJlZWl0aGFuY3JAZ21haWwuY29tIiwiYXVkIjoiU0RERS1TaXRpb0V4dGVybm8iLCJleHAiOjI1MzQwMjMwMDgwMCwibmJmIjoxNzkzMjg0NDY1LCJpYXQiOjE3OTMyODQ0NjUsImp0aSI6ImYyZGY5ZGU5LTA4NmQtNDUzOC04NDA2LTA3OTNlN2FjMDJmYyIsImVtYWlsIjoiZWVpdGhhbmNyQGdtYWlsLmNvbSJ9._Iyct4wHhbggb3RQGzifGNVghsx9xuRD96YW86-bOYA";

    // Código del indicador económico en el BCCR (317 = compra del dólar, 318 = venta)
    private static final int INDICADOR = 317; // 317 = compra, 318 = venta

    // Guarda el último valor obtenido en memoria para no volver a pedirlo a internet cada vez
    private static double cache = -1; // último tipo de cambio obtenido

    // Constructor privado: todos los métodos son estáticos, no hace falta crear instancias de APIbccr
    private APIbccr() { }

    /** Consulta el tipo de cambio en un hilo aparte (no congela la ventana). Llamar al inicio. */
    public static void precargar() {
        // Lanzamos un hilo secundario para que la petición de red no trabe la interfaz al arrancar
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
        // Si ya tenemos un tipo de cambio guardado en caché, lo devolvemos directo sin ir a internet
        if (cache > 0) return cache; // ya se consultó antes

        // Se piden los últimos 10 días porque fines de semana y feriados no tienen dato
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String desde = URLEncoder.encode(LocalDate.now().minusDays(10).format(f), StandardCharsets.UTF_8);
        String hasta = URLEncoder.encode(LocalDate.now().format(f), StandardCharsets.UTF_8);

        // Armamos la dirección (URL) del API del BCCR con el indicador y el rango de fechas
        String url = "https://apim.bccr.fi.cr/"
                + "SDDE/api/Bccr.GE.SDDE.Publico.Indicadores.API/"
                + "indicadoresEconomicos/" + INDICADOR + "/series"
                + "?fechaInicio=" + desde
                + "&fechaFin=" + hasta
                + "&idioma=ES";

        // Preparamos la solicitud HTTP GET enviando el token en el encabezado Authorization
        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();

        // Enviamos la solicitud y recibimos el texto de respuesta del servidor
        HttpResponse<String> respuesta = HttpClient.newHttpClient()
                .send(solicitud, HttpResponse.BodyHandlers.ofString());

        // El código 200 significa que la consulta salió bien; cualquier otro indica error (ej. token inválido)
        if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error del BCCR: " + respuesta.statusCode() + " (revisá el token)");
        }

        // Busca pares "fecha" + "valorDatoPorPeriodo" y se queda con el de la fecha más reciente
        Matcher m = Pattern.compile(
                "\"fecha\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"valorDatoPorPeriodo\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
                .matcher(respuesta.body());

        String fechaMax = "";
        double valor = -1;
        // Recorremos todas las coincidencias para quedarnos con el dato más actual
        while (m.find()) {
            if (m.group(1).compareTo(fechaMax) > 0) {
                fechaMax = m.group(1);
                valor = Double.parseDouble(m.group(2));
            }
        }
        // Si no pudimos extraer un número válido, lanzamos un aviso
        if (valor <= 0) {
            throw new RuntimeException("No se encontró el valor del tipo de cambio: " + respuesta.body());
        }

        // Guardamos el resultado en caché y lo devolvemos
        cache = valor;
        return cache;
    }

    /**
     * Premio en colones: 1 / puntaje * 100 * tipoCambio (puntaje = intentos para ganar).
     * Devuelve -1 si no se pudo obtener el tipo de cambio.
     */
    public static double calcularPremio(int intentos) {
        // Si los intentos son 0 o negativos, no es válido calcular premio
        if (intentos <= 0) return -1;
        try {
            // Entre menos intentos se usen, mayor es el premio en colones según el tipo de cambio actual
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