import java.awt.*;
import javax.swing.*;

/**
 * VISTA del Registro: usuario, contraseña con confirmación, captura facial y guardado.
 * Fondo animado: fondoRegistro.gif.
 * Los campos, etiquetas, botones y el botón del ojo vienen de Login (se compartían copiados).
 */
public class Registro extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;

    private final Login.CampoTexto txtUsuario = new Login.CampoTexto();
    private final Login.CampoClave txtPassword = new Login.CampoClave();
    private final Login.CampoClave txtConfirmPassword = new Login.CampoClave();
    private final JButton btnGuardar;

    private Runnable alGuardar;
    private Runnable alVolver;
    private Runnable alCapturarRostro;

    public Registro() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondoLogin.gif"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        int anchoBloque = 430;
        int izq = 55;
        int y = 125;

        int anchoEtiqueta = 185; // ancho para que quepa "Confirmar Contraseña:" sin recortarse
        int xCampo = izq + anchoEtiqueta + 5;
        int anchoCampoTotal = anchoBloque - anchoEtiqueta - 5;
        int anchoOjo = 35;
        int anchoCampoClave = anchoCampoTotal - anchoOjo - 5; // las contraseñas dejan sitio al ojo
        int altoFila = 34;
        int pasoFila = altoFila + 10;

        agregarFila(fondo, "Nuevo Usuario:", txtUsuario, izq, xCampo, y, anchoEtiqueta, anchoCampoTotal, altoFila);
        y += pasoFila;
        agregarFila(fondo, "Contraseña:", txtPassword, izq, xCampo, y, anchoEtiqueta, anchoCampoClave, altoFila);
        agregarOjo(fondo, txtPassword, xCampo + anchoCampoClave + 5, y, anchoOjo, altoFila);
        y += pasoFila;
        agregarFila(fondo, "Confirmar Contraseña:", txtConfirmPassword, izq, xCampo, y, anchoEtiqueta, anchoCampoClave, altoFila);
        agregarOjo(fondo, txtConfirmPassword, xCampo + anchoCampoClave + 5, y, anchoOjo, altoFila);

        // Botones
        y += altoFila + 18;
        JButton btnCapturarRostro = Login.crearBoton("Registrar Rostro (Cámara)", izq, y, anchoBloque);
        btnCapturarRostro.addActionListener(e -> {
            if (alCapturarRostro != null) alCapturarRostro.run();
        });
        fondo.add(btnCapturarRostro);

        y += Login.ALTO_BOTON + 10;
        // "Guardar Registro" no tiene acción aquí: la asigna ControladorAcceso (valida con ModeloUsuarios)
        btnGuardar = Login.crearBoton("Guardar Registro", izq, y, anchoBloque);
        fondo.add(btnGuardar);

        y += Login.ALTO_BOTON + 10;
        JButton btnVolver = Login.crearBoton("Volver al Login", izq, y, anchoBloque);
        btnVolver.addActionListener(e -> {
            limpiar();
            if (alVolver != null) alVolver.run();
        });
        fondo.add(btnVolver);

        // Enter pasa al siguiente campo; Enter en el último guarda
        txtUsuario.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> txtConfirmPassword.requestFocusInWindow());
        txtConfirmPassword.addActionListener(e -> btnGuardar.doClick());
    }

    /** Una fila "etiqueta + campo" (el ancho del campo ya viene calculado por quien llama). */
    private static void agregarFila(JPanel destino, String texto, JTextField campo,
                                    int xEtiqueta, int xCampo, int y, int anchoEtiqueta, int anchoCampo, int alto) {
        Login.EtiquetaDragonBall etiqueta = new Login.EtiquetaDragonBall(texto);
        etiqueta.setBounds(xEtiqueta, y, anchoEtiqueta, alto);
        campo.setBounds(xCampo, y, anchoCampo, alto);
        destino.add(etiqueta);
        destino.add(campo);
    }

    private static void agregarOjo(JPanel destino, JPasswordField campo, int x, int y, int ancho, int alto) {
        JButton ojo = Login.crearBotonOjo(campo);
        ojo.setBounds(x, y, ancho, alto);
        destino.add(ojo);
    }

    // ------------------------------------------------------------------
    // API para el controlador (ControladorAcceso)
    // ------------------------------------------------------------------

    public String getUsuario() { return txtUsuario.getText().trim(); }
    public String getClave() { return new String(txtPassword.getPassword()); }
    public String getConfirmacion() { return new String(txtConfirmPassword.getPassword()); }
    public JButton getBtnGuardar() { return btnGuardar; }

    public void limpiar() {
        txtUsuario.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revisa tus datos", JOptionPane.WARNING_MESSAGE);
    }

    public void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Registro", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Registro correcto: Main decide a qué pantalla se va (el Login). */
    public void registroExitoso() {
        if (alGuardar != null) alGuardar.run();
    }

    public void setAlGuardar(Runnable accion) { this.alGuardar = accion; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
    public void setAlCapturarRostro(Runnable accion) { this.alCapturarRostro = accion; }
}