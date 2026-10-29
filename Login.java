import java.awt.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;

/**
 * Pantalla de Login con apartado de cámara, estilo visual Dragon Ball y fondo ajustado.
 */
public class Login extends JPanel {

    private static final int ANCHO = 700;
    private static final int ALTO = 540;

    private Object webcam = null;
    private JPanel panelCamara; // Contenedor donde se dibuja la cámara en vivo

    private Runnable alIniciarSesion; 
    private Runnable alIrRegistro;    

    public Login() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        // --- Panel de Fondo ---
        PanelFondo fondo = new PanelFondo(cargarImagen("fondo2.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        // Eje X central para alinear de forma simétrica en la ventana de 700px
        int xCentro = 175;
        // Bajamos el bloque general un poco más hacia el centro de la pantalla
        int yBase = 60; 

        // Etiqueta Usuario
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setFont(new Font("Arial", Font.BOLD, 14));
        lblUsuario.setBounds(xCentro, yBase, 100, 25);
        fondo.add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(xCentro + 90, yBase, 260, 30);
        fondo.add(txtUsuario);

        // Etiqueta Contraseña
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setForeground(Color.WHITE);
        lblPassword.setFont(new Font("Arial", Font.BOLD, 14));
        lblPassword.setBounds(xCentro, yBase + 40, 100, 25);
        fondo.add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(xCentro + 90, yBase + 40, 260, 30);
        fondo.add(txtPassword);

        // Panel para mostrar la cámara en vivo
        panelCamara = new JPanel();
        panelCamara.setBounds(xCentro + 30, yBase + 85, 290, 180);
        panelCamara.setLayout(new BorderLayout());
        panelCamara.setOpaque(false);
        fondo.add(panelCamara);

        // Botones temáticos usando BotonDragonBall
        JButton btnCredenciales = new BotonDragonBall("Iniciar Sesión");
        btnCredenciales.setBounds(xCentro + 20, yBase + 275, 145, 38);
        btnCredenciales.setFont(new Font("Arial", Font.BOLD, 13));
        btnCredenciales.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
            if (alIniciarSesion != null) alIniciarSesion.run();
        });
        fondo.add(btnCredenciales);

        JButton btnFacial = new BotonDragonBall("Login Facial");
        btnFacial.setBounds(xCentro + 175, yBase + 275, 145, 38);
        btnFacial.setFont(new Font("Arial", Font.BOLD, 13));
        btnFacial.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí abriría la cámara para el reconocimiento facial");
        });
        fondo.add(btnFacial);

        JButton btnIrRegistro = new BotonDragonBall("¿No tienes cuenta? Regístrate");
        btnIrRegistro.setBounds(xCentro + 20, yBase + 322, 300, 38);
        btnIrRegistro.setFont(new Font("Arial", Font.BOLD, 13));
        btnIrRegistro.addActionListener(e -> {
            if (alIrRegistro != null) alIrRegistro.run();
        });
        fondo.add(btnIrRegistro);
    }

    /** Enciende la cámara. */
    public void iniciarCamara() {
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
                
                webcamPanel.setOpaque(false);
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

    /** Apaga la cámara si está encendida. */
    public void detenerCamara() {
        if (webcam != null) {
            try {
                boolean isOpen = (Boolean) webcam.getClass().getMethod("isOpen").invoke(webcam);
                if (isOpen) webcam.getClass().getMethod("close").invoke(webcam);
            } catch (ReflectiveOperationException ex) {
                // Ignore shutdown errors
            }
        }
    }

    private Image cargarImagen(String nombre) {
        URL url = getClass().getResource("/" + nombre);
        if (url != null) {
            return new ImageIcon(url).getImage();
        }
        String[] rutas = {nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre};
        for (String ruta : rutas) {
            if (new File(ruta).exists()) {
                return new ImageIcon(ruta).getImage();
            }
        }
        System.err.println("No se encontró la imagen de fondo: " + nombre);
        return null;
    }

    private static class PanelFondo extends JPanel {
        private final Image imagen;

        PanelFondo(Image imagen) {
            this.imagen = imagen;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagen != null) {
                // Asegura que la imagen estire exactamente a todo el ancho y alto del panel (700x540)
                g.drawImage(imagen, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(new Color(26, 16, 40));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    public void setAlIniciarSesion(Runnable accion) { this.alIniciarSesion = accion; }
    public void setAlIrRegistro(Runnable accion) { this.alIrRegistro = accion; }
}