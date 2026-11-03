import java.awt.*;
import javax.swing.*;

/**
 * Pantalla de Registro con Confirmación de contraseña en español, botones de ojo y captura facial.
 */
public class Registro extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;

    private Login.CampoTexto txtUsuario;
    private Login.CampoClave txtPassword;
    private Login.CampoClave txtConfirmPassword;
    private JButton btnGuardar;

    private Runnable alGuardar; 
    private Runnable alVolver;  
    private Runnable alCapturarRostro;

    public Registro() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondo6.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        int anchoBloque = 430;
        int izq = 55;  
        int y = 125;   

        int anchoEtiqueta = 185; // Ampliado para que quepa "Confirmar Contraseña:" sin recortarse
        int anchoCampoTotal = anchoBloque - anchoEtiqueta - 5;
        int altoFila = 34;
        int separacionFilas = 10;

        // Fila Nuevo Usuario
        Login.EtiquetaDragonBall lblUsuario = new Login.EtiquetaDragonBall("Nuevo Usuario:");
        lblUsuario.setBounds(izq, y, anchoEtiqueta, altoFila);
        fondo.add(lblUsuario);

        txtUsuario = new Login.CampoTexto();
        txtUsuario.setBounds(izq + anchoEtiqueta + 5, y, anchoCampoTotal, altoFila);
        fondo.add(txtUsuario);

        // Fila Contraseña con Ojo
        y += altoFila + separacionFilas;
        Login.EtiquetaDragonBall lblPassword = new Login.EtiquetaDragonBall("Contraseña:");
        lblPassword.setBounds(izq, y, anchoEtiqueta, altoFila);
        fondo.add(lblPassword);

        txtPassword = new Login.CampoClave();
        int anchoBotonOjo = 35;
        int anchoRealCampo = anchoCampoTotal - anchoBotonOjo - 5;
        txtPassword.setBounds(izq + anchoEtiqueta + 5, y, anchoRealCampo, altoFila);
        fondo.add(txtPassword);
        fondo.add(crearBotonOjo(txtPassword, izq + anchoEtiqueta + 5 + anchoRealCampo + 5, y, anchoBotonOjo, altoFila));

        // Fila Confirmar Contraseña con Ojo (En Español completo)
        y += altoFila + separacionFilas;
        Login.EtiquetaDragonBall lblConfirm = new Login.EtiquetaDragonBall("Confirmar Contraseña:");
        lblConfirm.setBounds(izq, y, anchoEtiqueta, altoFila);
        fondo.add(lblConfirm);

        txtConfirmPassword = new Login.CampoClave();
        txtConfirmPassword.setBounds(izq + anchoEtiqueta + 5, y, anchoRealCampo, altoFila);
        fondo.add(txtConfirmPassword);
        fondo.add(crearBotonOjo(txtConfirmPassword, izq + anchoEtiqueta + 5 + anchoRealCampo + 5, y, anchoBotonOjo, altoFila));

        // Botones
        int altoBoton = 38;
        int separacionBotones = 10;
        y += altoFila + 18;

        JButton btnCapturarRostro = new BotonDragonBall("Registrar Rostro (Cámara)");
        btnCapturarRostro.setBounds(izq, y, anchoBloque, altoBoton);
        btnCapturarRostro.setFont(new Font("Arial", Font.BOLD, 13));
        btnCapturarRostro.addActionListener(e -> {
            if (alCapturarRostro != null) alCapturarRostro.run();
        });
        fondo.add(btnCapturarRostro);

        y += altoBoton + separacionBotones;
        btnGuardar = new BotonDragonBall("Guardar Registro");
        btnGuardar.setBounds(izq, y, anchoBloque, altoBoton);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 13));
        // Su acción la maneja ControladorAcceso (valida con ModeloUsuarios)
        fondo.add(btnGuardar);

        y += altoBoton + separacionBotones;
        JButton btnVolver = new BotonDragonBall("Volver al Login");
        btnVolver.setBounds(izq, y, anchoBloque, altoBoton);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.addActionListener(e -> {
            limpiarCampos();
            if (alVolver != null) alVolver.run();
        });
        fondo.add(btnVolver);

        // Enter pasa al siguiente campo; Enter en el último guarda
        txtUsuario.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> txtConfirmPassword.requestFocusInWindow());
        txtConfirmPassword.addActionListener(e -> btnGuardar.doClick());
    }

    private JButton crearBotonOjo(JPasswordField campo, int x, int y, int ancho, int alto) {
        JButton btnOjo = new JButton("👁");
        btnOjo.setBounds(x, y, ancho, alto);
        btnOjo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnOjo.setFocusPainted(false);
        btnOjo.setBackground(new Color(255, 246, 205));
        btnOjo.setForeground(BotonDragonBall.CAFE_OSCURO);
        btnOjo.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        final boolean[] visible = {false};
        btnOjo.addActionListener(e -> {
            visible[0] = !visible[0];
            if (visible[0]) {
                campo.setEchoChar((char) 0);
                btnOjo.setText("🙈");
            } else {
                campo.setEchoChar('•');
                btnOjo.setText("👁");
            }
        });
        return btnOjo;
    }

    private void limpiarCampos() {
        txtUsuario.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
    }

    // ------------------------------------------------------------------
    // API para el controlador (ControladorAcceso)
    // ------------------------------------------------------------------

    public String getUsuario() { return txtUsuario.getText().trim(); }
    public String getClave() { return new String(txtPassword.getPassword()); }
    public String getConfirmacion() { return new String(txtConfirmPassword.getPassword()); }
    public JButton getBtnGuardar() { return btnGuardar; }

    public void limpiar() { limpiarCampos(); }

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