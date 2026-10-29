import java.awt.*;
import javax.swing.*;

/**
 * Pantalla de Registro con estilo Dragon Ball y fondo fondo6.jpg.
 * Es un JPanel para mostrarse dentro de la ventana única que crea Main.
 * El formulario va a la izquierda para no tapar a los personajes del fondo.
 */
public class Registro extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;

    private Login.CampoTexto txtUsuario;
    private Login.CampoClave txtPassword;

    // Acciones de navegación que Main le asigna
    private Runnable alGuardar; // Registro -> Login tras guardar
    private Runnable alVolver;  // Registro -> Login sin guardar

    public Registro() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        // --- Panel de Fondo (reutiliza el de Login) ---
        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondo6.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        // --- Medidas del bloque: todo se alinea a estos bordes ---
        int anchoBloque = 400;
        int izq = 59;  // bloque centrado en la zona izquierda libre del fondo
        int y = 151;   // inicio vertical (el bloque queda centrado en la altura)

        int anchoEtiqueta = 150;
        int anchoCampo = anchoBloque - anchoEtiqueta - 5; // el campo termina en el borde derecho
        int altoFila = 34;
        int separacionFilas = 12;

        // Fila Nuevo Usuario
        Login.EtiquetaDragonBall lblUsuario = new Login.EtiquetaDragonBall("Nuevo Usuario:");
        lblUsuario.setBounds(izq, y, anchoEtiqueta, altoFila);
        fondo.add(lblUsuario);

        txtUsuario = new Login.CampoTexto();
        txtUsuario.setBounds(izq + anchoEtiqueta + 5, y, anchoCampo, altoFila);
        fondo.add(txtUsuario);

        // Fila Contraseña
        y += altoFila + separacionFilas;
        Login.EtiquetaDragonBall lblPassword = new Login.EtiquetaDragonBall("Contraseña:");
        lblPassword.setBounds(izq, y, anchoEtiqueta, altoFila);
        fondo.add(lblPassword);

        txtPassword = new Login.CampoClave();
        txtPassword.setBounds(izq + anchoEtiqueta + 5, y, anchoCampo, altoFila);
        fondo.add(txtPassword);

        // Botones: misma altura y mismos bordes izquierdo/derecho
        int altoBoton = 38;
        int separacionBotones = 10;
        y += altoFila + 24;

        JButton btnCapturarRostro = new BotonDragonBall("Registrar Rostro (Cámara)");
        btnCapturarRostro.setBounds(izq, y, anchoBloque, altoBoton);
        btnCapturarRostro.setFont(new Font("Arial", Font.BOLD, 13));
        btnCapturarRostro.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se abriría la cámara para tomar la foto de referencia");
        });
        fondo.add(btnCapturarRostro);

        y += altoBoton + separacionBotones;
        JButton btnGuardar = new BotonDragonBall("Guardar Registro");
        btnGuardar.setBounds(izq, y, anchoBloque, altoBoton);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 13));
        btnGuardar.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "¡Usuario registrado con éxito!");
            limpiarCampos();
            if (alGuardar != null) alGuardar.run();
        });
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
    }

    private void limpiarCampos() {
        txtUsuario.setText("");
        txtPassword.setText("");
    }

    // Setters para que Main conecte la navegación
    public void setAlGuardar(Runnable accion) { this.alGuardar = accion; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}