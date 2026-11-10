import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * CONTROLADOR de Login, Registro y reconocimiento facial.
 * Toma lo que escribió el usuario en las vistas, se lo pasa a los modelos
 * (ModeloUsuarios para la contraseña; AnalizadorRostro + ModeloRostros para la cara)
 * y muestra el resultado. Las vistas y los modelos no se conocen entre sí.
 *
 * Asociación rostro-usuario:
 *  - Registro: la cámara captura el rostro y se guarda como "pendiente" ligado al nombre escrito;
 *    solo se escribe en rostros.txt cuando "Guardar Registro" termina bien.
 *  - Login facial: se compara el rostro SOLO contra el rostro registrado del usuario escrito
 *    en el Login. Si no es la misma persona, se niega el acceso.
 */
public class ControladorAcceso {

    /** true = no se puede crear una cuenta sin rostro. false = el rostro es opcional al registrarse. */
    private static final boolean ROSTRO_OBLIGATORIO = false;
    private static final int MAX_INTENTOS_FACIALES = 3;

    private final ModeloUsuarios modelo;
    private final ModeloRostros rostros;
    private final Login login;
    private final Registro registro;
    private final ReconocimientoFacial facial;

    // rostro capturado en el Registro que todavía no se guarda (espera a "Guardar Registro")
    private String usuarioPendiente;
    private List<float[]> muestrasPendientes;

    private String usuarioIntentos;
    private int intentosFallidos;

    public ControladorAcceso(ModeloUsuarios modelo, ModeloRostros rostros,
                             Login login, Registro registro, ReconocimientoFacial facial) {
        this.modelo = modelo;
        this.rostros = rostros;
        this.login = login;
        this.registro = registro;
        this.facial = facial;

        login.getBtnIniciar().addActionListener(e -> iniciarSesion());
        registro.getBtnGuardar().addActionListener(e -> registrar());
        facial.setAlEscanear(this::procesarEscaneo);
    }

    // ------------------------------------------------------------------
    // Login y registro con contraseña
    // ------------------------------------------------------------------

    private void iniciarSesion() {
        String error = modelo.iniciarSesion(login.getUsuario(), login.getClave());
        if (error != null) {
            login.mostrarError(error);
            login.limpiarClave();
            return;
        }
        login.limpiar();
        login.entrar(); // Main muestra el menú principal
    }

    private void registrar() {
        String usuario = registro.getUsuario();

        // si cambió el nombre después de capturar el rostro, ese rostro ya no corresponde a este usuario
        if (usuarioPendiente != null && !usuarioPendiente.equals(usuario)) {
            registro.mostrarError("Cambiaste el nombre de usuario después de registrar el rostro.\n"
                    + "Vuelve a pulsar \"Registrar Rostro (Cámara)\" para asociarlo a \"" + usuario + "\".");
            return;
        }
        if (ROSTRO_OBLIGATORIO && muestrasPendientes == null) {
            registro.mostrarError("Debes registrar tu rostro con la cámara antes de guardar.");
            return;
        }

        String error = modelo.registrar(usuario, registro.getClave(), registro.getConfirmacion());
        if (error != null) {
            registro.mostrarError(error);
            return;
        }

        // el usuario quedó creado: ahora se guarda su rostro (o se limpia cualquier rostro viejo con ese nombre)
        String nota;
        try {
            if (muestrasPendientes != null) {
                rostros.guardar(usuario, muestrasPendientes);
                nota = "\nTu rostro quedó asociado a esta cuenta: ya puedes usar el Login Facial.";
            } else {
                rostros.eliminar(usuario);
                nota = "\nNo registraste tu rostro: por ahora solo podrás entrar con contraseña.";
            }
        } catch (IOException ex) {
            nota = "\nLa cuenta se creó, pero no se pudo guardar el rostro: " + ex.getMessage();
        }
        descartarRostroPendiente();

        registro.mostrarInfo("¡Usuario \"" + usuario + "\" registrado con éxito!" + nota);
        registro.limpiar();
        login.setUsuario(usuario);   // el Login ya aparece con el usuario escrito
        registro.registroExitoso();  // Main vuelve al Login
    }

    /** Main lo llama al salir del Registro sin guardar: el rostro capturado se descarta. */
    public void descartarRostroPendiente() {
        usuarioPendiente = null;
        muestrasPendientes = null;
        registro.marcarRostroCapturado(false);
    }

    // ------------------------------------------------------------------
    // Validaciones antes de abrir la cámara
    // ------------------------------------------------------------------

    /** Registro: antes de abrir la cámara debe haber un nombre de usuario nuevo al que asociar el rostro. */
    public boolean validarUsuarioParaRegistroFacial() {
        String usuario = registro.getUsuario();
        if (usuario.isEmpty()) {
            registro.mostrarError("Escribe primero el nombre del usuario que vas a registrar.\n"
                    + "El rostro se asocia a ese nombre.");
            return false;
        }
        if (modelo.existe(usuario)) {
            registro.mostrarError("El usuario \"" + usuario + "\" ya existe. Elige otro nombre antes de registrar el rostro.");
            return false;
        }
        return true;
    }

    /** Login facial: el usuario debe existir Y tener un rostro registrado. */
    public boolean validarUsuarioParaFacial() {
        String usuarioIngresado = login.getUsuario();

        if (usuarioIngresado == null || usuarioIngresado.trim().isEmpty()) {
            JOptionPane.showMessageDialog(login,
                "Por favor, escribe tu nombre de usuario primero.",
                "Usuario Requerido",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (!modelo.existe(usuarioIngresado)) {
            JOptionPane.showMessageDialog(login,
                "Acceso denegado: ¡El usuario no está registrado! Tienes que registrarte primero.",
                "Registro Requerido",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (!rostros.tieneRostro(usuarioIngresado)) {
            JOptionPane.showMessageDialog(login,
                "El usuario \"" + usuarioIngresado + "\" no tiene un rostro registrado.\n"
                + "Inicia sesión con tu contraseña (o crea una cuenta nueva registrando tu rostro).",
                "Rostro no registrado",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Fotogramas de la cámara
    // ------------------------------------------------------------------

    private void procesarEscaneo(List<BufferedImage> fotogramas) {
        List<AnalizadorRostro.Resultado> resultados = new ArrayList<>();
        for (BufferedImage f : fotogramas) resultados.add(AnalizadorRostro.analizar(f));

        if ("REGISTRO".equals(facial.getModo())) {
            procesarRegistroFacial(resultados);
        } else {
            procesarLoginFacial(resultados);
        }
    }

    private void procesarRegistroFacial(List<AnalizadorRostro.Resultado> resultados) {
        String usuario = registro.getUsuario();

        List<float[]> muestras = new ArrayList<>();
        for (AnalizadorRostro.Resultado r : resultados) {
            if (r.isOk()) muestras.add(r.getDescriptor());
        }

        if (muestras.size() < ModeloRostros.MIN_MUESTRAS_REGISTRO) {
            facial.mostrarAviso("No se pudo registrar el rostro",
                    motivoDeFallo(resultados) + "\n(Se necesitan al menos " + ModeloRostros.MIN_MUESTRAS_REGISTRO
                    + " capturas buenas; hubo " + muestras.size() + ".)", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!ModeloRostros.muestrasConsistentes(muestras)) {
            facial.mostrarAviso("No se pudo registrar el rostro",
                    "Las capturas no se parecen entre sí. No te muevas durante el escaneo, mantén la luz estable y repite.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        usuarioPendiente = usuario;
        muestrasPendientes = muestras;
        registro.marcarRostroCapturado(true);
        facial.mostrarInfo("Rostro capturado para el usuario \"" + usuario + "\".\n"
                + "Ahora pulsa \"Guardar Registro\" para terminar.");
        facial.registroCompletado(); // Main vuelve al Registro (con el formulario intacto)
    }

    private void procesarLoginFacial(List<AnalizadorRostro.Resultado> resultados) {
        String usuario = login.getUsuario();

        // 1) ¿hay un rostro frente a la cámara? Si no hay nada, se avisa (y no cuenta como intento fallido)
        boolean hayRostro = false;
        for (AnalizadorRostro.Resultado r : resultados) hayRostro |= r.isOk();
        if (!hayRostro) {
            facial.mostrarAviso("Acceso denegado, rostro no encontrado", motivoDeFallo(resultados), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2) ¿es el rostro de ESTE usuario?
        ModeloRostros.Verificacion v = rostros.verificar(usuario, resultados);
        switch (v.getEstado()) {
            case COINCIDE:
                intentosFallidos = 0;
                login.limpiar();
                facial.mostrarInfo("¡Identificación facial exitosa! Bienvenido, " + usuario + ".");
                facial.loginCompletado(); // Main entra al juego
                break;

            case INSUFICIENTE:
                facial.mostrarAviso("Acceso denegado, rostro no encontrado",
                        "No se logró ver tu rostro completo y estable. Mírate de frente, sin moverte, y vuelve a escanear.",
                        JOptionPane.WARNING_MESSAGE);
                break;

            case SIN_ROSTRO_REGISTRADO:
                facial.mostrarAviso("Rostro no registrado",
                        "El usuario \"" + usuario + "\" no tiene un rostro registrado.", JOptionPane.WARNING_MESSAGE);
                facial.volver();
                break;

            default: // NO_COINCIDE
                if (!usuario.equals(usuarioIntentos)) { usuarioIntentos = usuario; intentosFallidos = 0; }
                intentosFallidos++;
                if (intentosFallidos >= MAX_INTENTOS_FACIALES) {
                    intentosFallidos = 0;
                    facial.mostrarAviso("Acceso denegado",
                            "El rostro no coincide con el usuario \"" + usuario + "\".\n"
                            + "Demasiados intentos fallidos: inicia sesión con tu contraseña.", JOptionPane.ERROR_MESSAGE);
                    facial.volver();
                } else {
                    facial.mostrarAviso("Acceso denegado",
                            "El rostro no coincide con el usuario \"" + usuario + "\".\n"
                            + "Intentos restantes: " + (MAX_INTENTOS_FACIALES - intentosFallidos), JOptionPane.ERROR_MESSAGE);
                }
                break;
        }
    }

    /** Explica en palabras por qué no se pudo usar la cámara (según lo que más se repitió en los fotogramas). */
    private static String motivoDeFallo(List<AnalizadorRostro.Resultado> resultados) {
        Map<AnalizadorRostro.Estado, Integer> cuenta = new EnumMap<>(AnalizadorRostro.Estado.class);
        for (AnalizadorRostro.Resultado r : resultados) {
            if (!r.isOk()) cuenta.merge(r.getEstado(), 1, Integer::sum);
        }
        AnalizadorRostro.Estado principal = AnalizadorRostro.Estado.SIN_IMAGEN;
        int max = -1;
        for (Map.Entry<AnalizadorRostro.Estado, Integer> e : cuenta.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); principal = e.getKey(); }
        }
        switch (principal) {
            case MUY_OSCURA:
                return "Hay muy poca luz o la cámara está tapada. Busca un lugar mejor iluminado.";
            case SIN_ROSTRO:
                return "No se detecta ningún rostro frente a la cámara.\nColócate frente a ella, con buena luz y a una distancia normal.";
            case ROSTRO_INCOMPLETO:
                return "Rostro incompleto: muestra toda la cara de frente, sin taparla ni acercarte demasiado.";
            default:
                return "No se pudo leer la imagen de la cámara. Verifica que esté conectada y que otro programa no la use.";
        }
    }
}