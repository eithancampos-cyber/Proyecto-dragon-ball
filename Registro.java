import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Pantalla de Registro con apartado de cámara.
 * Es un JPanel para mostrarse dentro de la ventana única que crea Main.
 * La cámara NO se enciende al crear el panel: Main llama a iniciarCamara()
 * cuando se muestra esta pantalla. Al guardar o volver, el propio panel
 * apaga la cámara antes de avisar a Main.
 */
public class Registro extends JPanel {

    // Loaded reflectively so the project can compile even when the optional
    // sarxos webcam library is not on the compile classpath.
    private Object webcam = null;
    private JPanel panelCamara;           // contenedor donde se dibuja la cámara en vivo
    private BufferedImage rostroCapturado; // foto de referencia tomada de la cámara

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    // Acciones de navegación que Main le asigna
    private Runnable alGuardar; // Registro -> Login tras guardar
    private Runnable alVolver;  // Registro -> Login sin guardar

    public Registro() {
        setPreferredSize(new Dimension(400, 450)); // mismo tamaño que Login
        setLayout(null);

        JLabel lblUsuario = new JLabel("Nuevo Usuario:");
        lblUsuario.setBounds(30, 20, 100, 25);
        add(lblUsuario);

        txtUsuario = new JTextField();
        txtUsuario.setBounds(140, 20, 200, 25);
        add(txtUsuario);

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 60, 100, 25);
        add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(140, 60, 200, 25);
        add(txtPassword);

        // Panel para mostrar la cámara en vivo (se llena en iniciarCamara())
        panelCamara = new JPanel(new BorderLayout());
        panelCamara.setBounds(30, 100, 310, 190);
        add(panelCamara);

        JButton btnCapturarRostro = new JButton("Registrar Rostro (Cámara)");
        btnCapturarRostro.setBounds(30, 300, 310, 30);
        btnCapturarRostro.addActionListener(e -> {
            if (camaraAbierta()) {
                try {
                    rostroCapturado = (BufferedImage) webcam.getClass().getMethod("getImage").invoke(webcam);
                } catch (ReflectiveOperationException ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo capturar la imagen.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Rostro capturado correctamente");
            } else {
                JOptionPane.showMessageDialog(this, "La cámara no está disponible todavía.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        });
        add(btnCapturarRostro);

        JButton btnGuardar = new JButton("Guardar Registro");
        btnGuardar.setBounds(30, 345, 310, 30);
        btnGuardar.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "¡Usuario registrado con éxito!");
            detenerCamara(); // Main asume que la cámara ya se apagó
            limpiarCampos();
            if (alGuardar != null) alGuardar.run();
        });
        add(btnGuardar);

        JButton btnVolver = new JButton("Volver al Login");
        btnVolver.setBounds(30, 390, 310, 30);
        btnVolver.addActionListener(e -> {
            detenerCamara(); // Main no apaga la cámara en este caso
            limpiarCampos();
            if (alVolver != null) alVolver.run();
        });
        add(btnVolver);
    }

    private void limpiarCampos() {
        txtUsuario.setText("");
        txtPassword.setText("");
        rostroCapturado = null;
    }

    /** Enciende la cámara. Main lo llama cuando se muestra esta pantalla. */
    public void iniciarCamara() {
        if (webcam == null) {
            try {
                Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
                webcam = webcamClass.getMethod("getDefault").invoke(null);
                if (webcam == null) {
                    JOptionPane.showMessageDialog(this, "No se encontró ninguna cámara web.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                webcamClass.getMethod("setViewSize", Dimension.class)
                        .invoke(webcam, new Dimension(640, 480));

                Class<?> webcamPanelClass = Class.forName("com.github.sarxos.webcam.WebcamPanel");
                Constructor<?> constructor = webcamPanelClass.getConstructor(webcamClass, boolean.class);
                Component webcamPanel = (Component) constructor.newInstance(webcam, false);
                webcamPanelClass.getMethod("setFPSDisplayed", boolean.class).invoke(webcamPanel, true);
                panelCamara.add(webcamPanel, BorderLayout.CENTER);
                panelCamara.revalidate();
            } catch (ReflectiveOperationException | LinkageError ex) {
                JOptionPane.showMessageDialog(this, "No se encontró ninguna cámara web.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Hilo separado para no congelar la ventana
        if (!camaraAbierta()) {
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
    }

    /** Apaga la cámara si está encendida. */
    public void detenerCamara() {
        if (camaraAbierta()) {
            try {
                webcam.getClass().getMethod("close").invoke(webcam);
            } catch (ReflectiveOperationException ex) {
                // Nothing else to do if the optional webcam library fails to close.
            }
        }
    }

    private boolean camaraAbierta() {
        if (webcam == null) return false;
        try {
            Method isOpen = webcam.getClass().getMethod("isOpen");
            return Boolean.TRUE.equals(isOpen.invoke(webcam));
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    // Setters para que Main conecte la navegación
    public void setAlGuardar(Runnable accion) { this.alGuardar = accion; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}