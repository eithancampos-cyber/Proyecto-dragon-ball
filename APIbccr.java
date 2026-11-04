package cr.ucr.bccr.app;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APIbccr {

    public static float obtenerTipoCambio(String token)
            throws Exception {

        String url = "https://apim.bccr.fi.cr/"
                + "SDDE/api/Bccr.GE.SDDE.Publico.Indicadores.API/"
                + "indicadoresEconomicos/317/series"
                + "?fechaInicio=2026%2F10%2F29"
                + "&fechaFin=2026%2F10%2F29"
                + "&idioma=ES";

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> respuesta =
                HttpClient.newHttpClient().send(
                        solicitud,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (respuesta.statusCode() != 200) {
            throw new RuntimeException(
                    "Error del BCCR: " + respuesta.statusCode()
            );
        }

        String json = respuesta.body();
        //System.out.println(json);
        Pattern patron = Pattern.compile(
                "\"valorDatoPorPeriodo\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)"
        );

        Matcher matcher = patron.matcher(json);

        if (matcher.find()) {
            return Float.parseFloat(matcher.group(1));
        }

        throw new RuntimeException(
                "No se encontró el valor del tipo de cambio: " + json
        );
    }

    public static void main(String[] args) {
        String token = "SU TOKEN AQUI";

        try {
            float tipoCambio = obtenerTipoCambio(token);
            System.out.println(tipoCambio);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}