import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de Login con apartado de cámara.
 * Es un JPanel para mostrarse dentro de la ventana única que crea Main.
 * La cámara NO se enciende al crear el panel: Main llama a iniciarCamara()
 * cuando se muestra esta pantalla y a detenerCamara() cuando se sale de ella.
 */
public class Login extends JPanel {

    // Object is used so the project can compile even when the optional webcam
    // library is not on the classpath.
    private Object webcam = null;
    private JPanel panelCamara; // contenedor donde se dibuja la cámara en vivo

    // Acciones de navegación que Main le asigna (la clase no sabe a qué pantalla va)
    private Runnable alIniciarSesion; // se ejecuta después de "Iniciar Sesión"
    private Runnable alIrRegistro;    // se ejecuta al pulsar "Regístrate"

    public Login() {
        setPreferredSize(new Dimension(400, 450)); // mismo tamaño que la pantalla de Registro
        setLayout(null);                           // posicionamiento manual, igual que antes

        // Etiqueta y campo de usuario
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setBounds(30, 20, 100, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(140, 20, 200, 25);
        add(txtUsuario);

        // Etiqueta y campo de contraseña
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 60, 100, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(140, 60, 200, 25);
        add(txtPassword);

        // Panel para mostrar la cámara en vivo (se llena en iniciarCamara())
        panelCamara = new JPanel();
        panelCamara.setBounds(30, 100, 310, 230);
        panelCamara.setLayout(new BorderLayout());
        add(panelCamara);

        // Botón de login con credenciales (mantiene su mensaje original)
        JButton btnCredenciales = new JButton("Iniciar Sesión");
        btnCredenciales.setBounds(30, 345, 150, 30);
        btnCredenciales.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
            if (alIniciarSesion != null) alIniciarSesion.run(); // pasa al menú principal
        });
        add(btnCredenciales);

        // Botón de login facial (mantiene su mensaje original)
        JButton btnFacial = new JButton("Login Facial");
        btnFacial.setBounds(190, 345, 150, 30);
        btnFacial.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí abriría la cámara para el reconocimiento facial");
        });
        add(btnFacial);

        // Botón para ir a la pantalla de registro
        JButton btnIrRegistro = new JButton("¿No tienes cuenta? Regístrate");
        btnIrRegistro.setBounds(30, 390, 310, 30);
        btnIrRegistro.addActionListener(e -> {
            if (alIrRegistro != null) alIrRegistro.run(); // Main muestra el Registro
        });
        add(btnIrRegistro);
    }

    /** Enciende la cámara. Main lo llama cuando se muestra esta pantalla. */
    public void iniciarCamara() {
        // Inicializar la webcam predeterminada de la PC (solo la primera vez)
        if (webcam == null) {
            try {
                Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
                webcam = webcamClass.getMethod("getDefault").invoke(null);
                if (webcam == null) {
                    JOptionPane.showMessageDialog(this, "No se encontró ninguna cámara web.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Class<?> resolutionClass = Class.forName("com.github.sarxos.webcam.WebcamResolution");
                Object vga = resolutionClass.getField("VGA").get(null);
                Dimension size = (Dimension) resolutionClass.getMethod("getSize").invoke(vga);
                webcamClass.getMethod("setViewSize", Dimension.class).invoke(webcam, size);

                Class<?> webcamPanelClass = Class.forName("com.github.sarxos.webcam.WebcamPanel");
                JPanel webcamPanel = (JPanel) webcamPanelClass
                        .getConstructor(webcamClass).newInstance(webcam);
                webcamPanelClass.getMethod("setFPSDisplayed", boolean.class).invoke(webcamPanel, true);
                panelCamara.add(webcamPanel, BorderLayout.CENTER);
                panelCamara.revalidate();
            } catch (ReflectiveOperationException | LinkageError ex) {
                webcam = null;
                JOptionPane.showMessageDialog(this,
                        "La cámara requiere la biblioteca webcam-capture en el classpath.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Hilo separado para iniciar la cámara sin congelar la ventana
        try {
            boolean isOpen = (Boolean) webcam.getClass().getMethod("isOpen").invoke(webcam);
            if (!isOpen) {
                Thread t = new Thread(() -> {
                    try {
                        webcam.getClass().getMethod("open").invoke(webcam);
                    } catch (ReflectiveOperationException ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                                "No se pudo iniciar la cámara.", "Error", JOptionPane.ERROR_MESSAGE));
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        } catch (ReflectiveOperationException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo iniciar la cámara.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Apaga la cámara si está encendida (se libera para que la use otra pantalla). */
    public void detenerCamara() {
        if (webcam != null) {
            try {
                boolean isOpen = (Boolean) webcam.getClass().getMethod("isOpen").invoke(webcam);
                if (isOpen) webcam.getClass().getMethod("close").invoke(webcam);
            } catch (ReflectiveOperationException ex) {
                // Ignore shutdown errors; the screen is closing regardless.
            }
        }
    }

    // Setters para que Main conecte la navegación
    public void setAlIniciarSesion(Runnable accion) { this.alIniciarSesion = accion; }
    public void setAlIrRegistro(Runnable accion) { this.alIrRegistro = accion; }
}