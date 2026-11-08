import javax.swing.JOptionPane;

/**
 * CONTROLADOR de Login y Registro.
 * Toma lo que escribió el usuario en las vistas, se lo pasa a ModeloUsuarios
 * para validar y muestra el resultado. Las vistas y el modelo no se conocen entre sí.
 */
public class ControladorAcceso {

    private final ModeloUsuarios modelo;
    private final Login login;
    private final Registro registro;

    public ControladorAcceso(ModeloUsuarios modelo, Login login, Registro registro) {
        this.modelo = modelo;
        this.login = login;
        this.registro = registro;

        login.getBtnIniciar().addActionListener(e -> iniciarSesion());
        registro.getBtnGuardar().addActionListener(e -> registrar());
    }

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
        String error = modelo.registrar(usuario, registro.getClave(), registro.getConfirmacion());
        if (error != null) {
            registro.mostrarError(error);
            return;
        }
        registro.mostrarInfo("¡Usuario \"" + usuario + "\" registrado con éxito!");
        registro.limpiar();
        login.setUsuario(usuario);   // el Login ya aparece con el usuario escrito
        registro.registroExitoso();  // Main vuelve al Login
    }

    /**
     * Valida si el usuario existe antes de permitirle usar el reconocimiento facial.
     */
    public boolean validarUsuarioParaFacial() {
        String usuarioIngresado = login.getUsuario();

        // 1. Validar que no esté vacío
        if (usuarioIngresado == null || usuarioIngresado.trim().isEmpty()) {
            JOptionPane.showMessageDialog(login, 
                "⚠️ Por favor, escribe tu nombre de usuario primero.", 
                "Usuario Requerido", 
                JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // 2. Verificar si el usuario NO está registrado usando el método existe() del modelo
        if (!modelo.existe(usuarioIngresado)) {
            JOptionPane.showMessageDialog(login, 
                "❌ Acceso denegado: ¡El usuario no está registrado! Tienes que registrarte primero.", 
                "Registro Requerido", 
                JOptionPane.WARNING_MESSAGE);
            return false; // Bloquea la cámara
        }

        return true; // El usuario sí existe, abre la cámara con normalidad
    }
}