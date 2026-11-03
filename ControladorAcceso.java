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
}