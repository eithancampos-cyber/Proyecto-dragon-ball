import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;
<<<<<<< HEAD
<<<<<<< Updated upstream
=======
>>>>>>> origin/main
import java.awt.*;
import java.io.File;

public class Login extends JFrame {
    private Webcam webcam = null;
<<<<<<< HEAD
=======
import java.awt.Dimension;

/**
 * Pantalla de Login.
 * Ahora es un JPanel (antes era un JFrame) para poder mostrarse
 * dentro de la ventana única que crea Main.
 */
public class Login extends JPanel {

    // Acciones de navegación que Main le asigna (la clase no sabe a qué pantalla va)
    private Runnable alIniciarSesion; // se ejecuta después de "Iniciar Sesión"
    private Runnable alIrRegistro;    // se ejecuta al pulsar "Regístrate"
>>>>>>> Stashed changes
=======
>>>>>>> origin/main

    public Login() {
        setPreferredSize(new Dimension(350, 280)); // tamaño original de la ventana
        setLayout(null);                           // posicionamiento manual, igual que antes

        // Etiqueta y campo de usuario
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setBounds(30, 25, 80, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(110, 25, 180, 25);
        add(txtUsuario);

        // Etiqueta y campo de contraseña
        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 65, 80, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(110, 65, 180, 25);
        add(txtPassword);

        // Botón de login con credenciales (mantiene su mensaje original)
        JButton btnCredenciales = new JButton("Iniciar Sesión");
        btnCredenciales.setBounds(30, 115, 130, 30);
        btnCredenciales.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
            if (alIniciarSesion != null) alIniciarSesion.run(); // pasa al menú principal
        });
        add(btnCredenciales);

<<<<<<< HEAD
<<<<<<< Updated upstream
        // --- BOTÓN DE LOGIN FACIAL CON CÁMARA REAL ---
=======
        // Botón de login facial (mantiene su mensaje original)
>>>>>>> Stashed changes
=======
        // --- BOTÓN DE LOGIN FACIAL CON CÁMARA REAL ---
>>>>>>> origin/main
        JButton btnFacial = new JButton("Login Facial");
        btnFacial.setBounds(175, 115, 125, 30);
        btnFacial.addActionListener(e -> {
            abrirVentanaReconocimientoFacial();
        });
        add(btnFacial);

        // Botón para ir a la pantalla de registro
        JButton btnIrRegistro = new JButton("¿No tienes cuenta? Regístrate");
        btnIrRegistro.setBounds(30, 165, 270, 30);
        btnIrRegistro.addActionListener(e -> {
            if (alIrRegistro != null) alIrRegistro.run(); // Main muestra el Registro
        });
        add(btnIrRegistro);
    }

<<<<<<< HEAD
<<<<<<< Updated upstream
=======
>>>>>>> origin/main
    private void abrirVentanaReconocimientoFacial() {
        JFrame ventanaCamara = new JFrame("Reconocimiento Facial - Escaneando...");
        ventanaCamara.setSize(400, 350);
        ventanaCamara.setLocationRelativeTo(this);
        ventanaCamara.setLayout(new BorderLayout());

        webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this, "No se detectó ninguna cámara web.");
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        WebcamPanel panel = new WebcamPanel(webcam);
        ventanaCamara.add(panel, BorderLayout.CENTER);

        JButton btnEscanear = new JButton("Verificar mi Rostro");
        btnEscanear.setFont(new Font("Arial", Font.BOLD, 14));
        btnEscanear.addActionListener(ev -> {
            // Lógica de validación: Comprobamos si existen rostros registrados en la carpeta
            File carpetaRostros = new File("rostros_registrados");
            
            if (carpetaRostros.exists() && carpetaRostros.list().length > 0) {
                // Tomamos el primer usuario encontrado como validación exitosa de ejemplo
                File[] archivos = carpetaRostros.listFiles();
                if (archivos != null && archivos.length > 0) {
                    String nombreArchivo = archivos[0].getName();
                    String usuarioReconocido = nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.'));

                    webcam.close();
                    ventanaCamara.dispose();
                    this.dispose(); // Cierra el Login

                    // ¡ÉXITO! Lo manda al inicio de sesión / panel principal
                    JOptionPane.showMessageDialog(null, "¡Rostro reconocido con éxito!\nBienvenido al sistema, " + usuarioReconocido);
                    
                    // Aquí abrirías tu ventana principal del sistema:
                    // new VentanaPrincipal(usuarioReconocido).setVisible(true);
                }
            } else {
                webcam.close();
                ventanaCamara.dispose();
                JOptionPane.showMessageDialog(this, "Rostro no reconocido o ningún usuario registrado.", "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
            }
        });

        ventanaCamara.add(btnEscanear, BorderLayout.SOUTH);
        
        // Hilo para encender la cámara de forma fluida
        Thread t = new Thread(() -> webcam.open());
        t.setDaemon(true);
        t.start();

        ventanaCamara.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Login().setVisible(true);
        });
    }
=======
    // Setters para que Main conecte la navegación
    public void setAlIniciarSesion(Runnable accion) { this.alIniciarSesion = accion; }
    public void setAlIrRegistro(Runnable accion) { this.alIrRegistro = accion; }
>>>>>>> Stashed changes
}