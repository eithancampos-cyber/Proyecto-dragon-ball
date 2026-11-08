import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * MODELO de usuarios: registro y login con sus validaciones y soporte facial.
 * No sabe nada de Swing: solo guarda los usuarios y aplica las reglas.
 */
public class ModeloUsuarios {

    public static final int USUARIO_MIN = 3;
    public static final int USUARIO_MAX = 15;
    public static final int CLAVE_MIN = 6;
    public static final int CLAVE_MAX = 30;

    private static final int ITERACIONES = 65536;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Path archivo;
    // llave: usuario en minúsculas -> arreglo con [nombre, sal, hashClave, firmaFacial (opcional)]
    private final Map<String, String[]> usuarios = new LinkedHashMap<>();
    private String usuarioActual;

    public ModeloUsuarios() {
        this(Paths.get("usuarios.txt"));
    }

    public ModeloUsuarios(Path archivo) {
        this.archivo = archivo;
        cargar();
    }

    public String validarUsuario(String usuario) {
        if (usuario == null || usuario.isEmpty()) {
            return "Escribe un nombre de usuario.";
        }
        if (usuario.length() < USUARIO_MIN || usuario.length() > USUARIO_MAX) {
            return "El usuario debe tener entre " + USUARIO_MIN + " y " + USUARIO_MAX + " caracteres.";
        }
        if (!usuario.matches("[A-Za-z][A-Za-z0-9_]*")) {
            return "El usuario debe empezar con una letra y solo puede tener letras (sin tildes), "
                    + "números y guion bajo (_).";
        }
        return null;
    }

    public String validarClave(String usuario, String clave) {
        if (clave == null || clave.isEmpty()) {
            return "Escribe una contraseña.";
        }
        if (clave.length() < CLAVE_MIN || clave.length() > CLAVE_MAX) {
            return "La contraseña debe tener entre " + CLAVE_MIN + " y " + CLAVE_MAX + " caracteres.";
        }
        for (int i = 0; i < clave.length(); i++) {
            if (Character.isWhitespace(clave.charAt(i))) {
                return "La contraseña no puede tener espacios.";
            }
        }
        boolean hayLetra = false;
        boolean hayNumero = false;
        for (int i = 0; i < clave.length(); i++) {
            char c = clave.charAt(i);
            if (Character.isLetter(c)) hayLetra = true;
            if (Character.isDigit(c)) hayNumero = true;
        }
        if (!hayLetra || !hayNumero) {
            return "La contraseña debe tener al menos una letra y un número.";
        }
        if (usuario != null && clave.equalsIgnoreCase(usuario)) {
            return "La contraseña no puede ser igual al nombre de usuario.";
        }
        return null;
    }

    public boolean existe(String usuario) {
        return usuario != null && usuarios.containsKey(llave(usuario));
    }

    public String registrar(String usuario, String clave, String confirmacion) {
        usuario = (usuario == null) ? "" : usuario.trim();

        String error = validarUsuario(usuario);
        if (error != null) return error;

        error = validarClave(usuario, clave);
        if (error != null) return error;

        if (!clave.equals(confirmacion)) {
            return "Las contraseñas no coinciden.";
        }
        if (existe(usuario)) {
            return "El usuario \"" + usuario + "\" ya está registrado. Elige otro nombre.";
        }

        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        String[] registro = {
            usuario,
            Base64.getEncoder().encodeToString(sal),
            Base64.getEncoder().encodeToString(derivar(clave, sal)),
            "" // Firma facial vacía inicialmente
        };

        usuarios.put(llave(usuario), registro);
        if (!guardar()) {
            usuarios.remove(llave(usuario));
            return "No se pudo guardar el registro en el archivo " + archivo + ".";
        }
        return null;
    }

    public String iniciarSesion(String usuario, String clave) {
        usuario = (usuario == null) ? "" : usuario.trim();

        if (usuario.isEmpty()) return "Escribe tu usuario.";
        if (clave == null || clave.isEmpty()) return "Escribe tu contraseña.";

        String[] registro = usuarios.get(llave(usuario));
        if (registro == null || !claveCorrecta(registro, clave)) {
            return "Usuario o contraseña incorrectos.";
        }
        usuarioActual = registro[0];
        return null;
    }

    public String getUsuarioActual() {
        return usuarioActual;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    /** Guarda o actualiza la firma facial asociada a un usuario específico en el archivo. */
    public boolean guardarRostroUsuario(String usuario, String firmaFacial) {
        String llaveUsr = llave(usuario);
        String[] registro = usuarios.get(llaveUsr);
        if (registro == null) return false;

        String[] nuevoRegistro = new String[]{
            registro[0], 
            registro[1], 
            registro[2], 
            firmaFacial != null ? firmaFacial : ""
        };
        usuarios.put(llaveUsr, nuevoRegistro);
        return guardar();
    }

    /** Valida si la firma facial capturada coincide con la registrada para el usuario. */
    public boolean validarRostroUsuario(String usuario, String firmaCapturada) {
        String[] registro = usuarios.get(llave(usuario));
        if (registro == null || registro.length < 4) return false;
        String firmaGuardada = registro[3];
        if (firmaGuardada == null || firmaGuardada.isEmpty()) return false;

        return compararFirmasFaciales(firmaGuardada, firmaCapturada);
    }

    public boolean tieneRostroRegistrado(String usuario) {
        String[] registro = usuarios.get(llave(usuario));
        return registro != null && registro.length >= 4 && registro[3] != null && !registro[3].isEmpty();
    }

    private boolean compararFirmasFaciales(String f1, String f2) {
        try {
            String[] p1 = f1.split(",");
            String[] p2 = f2.split(",");
            if (p1.length != p2.length) return false;
            double diferenciaTotal = 0;
            for (int i = 0; i < p1.length; i++) {
                double val1 = Double.parseDouble(p1[i]);
                double val2 = Double.parseDouble(p2[i]);
                diferenciaTotal += Math.abs(val1 - val2);
            }
            double promedioDiferencia = diferenciaTotal / p1.length;
            return promedioDiferencia < 0.15;
        } catch (Exception e) {
            return false;
        }
    }

    private static String llave(String usuario) {
        return usuario.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] derivar(String clave, byte[] sal) {
        try {
            PBEKeySpec especificacion = new PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }

    private static boolean claveCorrecta(String[] registro, String clave) {
        byte[] sal = Base64.getDecoder().decode(registro[1]);
        byte[] esperado = Base64.getDecoder().decode(registro[2]);
        return MessageDigest.isEqual(esperado, derivar(clave, sal));
    }

    private void cargar() {
        if (!Files.exists(archivo)) return;
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(";", -1); // -1 para conservar campos vacíos finales
                if (partes.length >= 3) {
                    usuarios.put(llave(partes[0]), partes);
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }

    private boolean guardar() {
        List<String> lineas = new ArrayList<>();
        for (String[] registro : usuarios.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append(registro[0]).append(";")
              .append(registro[1]).append(";")
              .append(registro[2]).append(";");
            if (registro.length >= 4) {
                sb.append(registro[3]);
            }
            lineas.add(sb.toString());
        }
        try {
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo escribir " + archivo + ": " + e.getMessage());
            return false;
        }
    }
}