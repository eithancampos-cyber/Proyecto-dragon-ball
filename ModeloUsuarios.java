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
 * MODELO de usuarios: registro y login con sus validaciones.
 * No sabe nada de Swing: solo guarda los usuarios y aplica las reglas.
 *
 * Reglas del usuario:  3 a 15 caracteres, empieza con letra, solo letras (sin tildes), números y "_".
 * Reglas de la clave:  6 a 30 caracteres, sin espacios, con al menos una letra y un número,
 *                      y distinta del nombre de usuario.
 * El nombre de usuario no distingue mayúsculas ("Goku" y "goku" son el mismo usuario).
 *
 * Los usuarios se guardan en el archivo usuarios.txt (una línea por usuario: nombre;sal;hash).
 * La contraseña NUNCA se guarda en texto plano: se guarda su hash PBKDF2 con una sal aleatoria.
 *
 * Los métodos de validación devuelven null si todo está bien, o el mensaje de error para mostrar.
 */
public class ModeloUsuarios {

    public static final int USUARIO_MIN = 3;
    public static final int USUARIO_MAX = 15;
    public static final int CLAVE_MIN = 6;
    public static final int CLAVE_MAX = 30;

    private static final int ITERACIONES = 65536;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Path archivo;
    // llave: usuario en minúsculas -> {nombre original, sal en Base64, hash en Base64}
    private final Map<String, String[]> usuarios = new LinkedHashMap<>();
    private String usuarioActual;

    public ModeloUsuarios() {
        this(Paths.get("usuarios.txt"));
    }

    public ModeloUsuarios(Path archivo) {
        this.archivo = archivo;
        cargar();
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    /** Valida solo el formato del nombre de usuario (no revisa si ya existe). */
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

    /** Valida el formato de la contraseña para el registro. */
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

    // ------------------------------------------------------------------
    // Registro y login
    // ------------------------------------------------------------------

    /** Registra un usuario nuevo. Devuelve null si se registró, o el mensaje de error. */
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
            Base64.getEncoder().encodeToString(derivar(clave, sal))
        };

        usuarios.put(llave(usuario), registro);
        if (!guardar()) {
            usuarios.remove(llave(usuario)); // no se queda registrado en memoria si no se pudo guardar
            return "No se pudo guardar el registro en el archivo " + archivo + ".";
        }
        return null;
    }

    /** Valida las credenciales. Devuelve null si son correctas (y deja el usuario como activo). */
    public String iniciarSesion(String usuario, String clave) {
        usuario = (usuario == null) ? "" : usuario.trim();

        if (usuario.isEmpty()) return "Escribe tu usuario.";
        if (clave == null || clave.isEmpty()) return "Escribe tu contraseña.";

        String[] registro = usuarios.get(llave(usuario));
        // El mismo mensaje si el usuario no existe o la clave está mal: no se revela cuál falló
        if (registro == null || !claveCorrecta(registro, clave)) {
            return "Usuario o contraseña incorrectos.";
        }
        usuarioActual = registro[0];
        return null;
    }

    /** Nombre del usuario que inició sesión (null si nadie lo ha hecho). */
    public String getUsuarioActual() {
        return usuarioActual;
    }

    public void cerrarSesion() {
        usuarioActual = null;
    }

    // ------------------------------------------------------------------
    // Hash y archivo
    // ------------------------------------------------------------------

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
        return MessageDigest.isEqual(esperado, derivar(clave, sal)); // comparación de tiempo constante
    }

    private void cargar() {
        if (!Files.exists(archivo)) return;
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String[] partes = linea.split(";");
                if (partes.length == 3) {
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
            lineas.add(registro[0] + ";" + registro[1] + ";" + registro[2]);
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