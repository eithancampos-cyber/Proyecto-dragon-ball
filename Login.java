import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Login extends JFrame {
    private Webcam webcam = null;

    public Login() {
        setTitle("Login - Sistema");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setBounds(30, 25, 80, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(110, 25, 180, 25);
        add(txtUsuario);

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 65, 80, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(110, 65, 180, 25);
        add(txtPassword);

        JButton btnCredenciales = new JButton("Iniciar Sesión");
        btnCredenciales.setBounds(30, 115, 130, 30);
        btnCredenciales.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
        });
        add(btnCredenciales);

        // --- BOTÓN DE LOGIN FACIAL CON CÁMARA REAL ---
        JButton btnFacial = new JButton("Login Facial");
        btnFacial.setBounds(175, 115, 125, 30);
        btnFacial.addActionListener(e -> {
            abrirVentanaReconocimientoFacial();
        });
        add(btnFacial);

        JButton btnIrRegistro = new JButton("¿No tienes cuenta? Regístrate");
        btnIrRegistro.setBounds(30, 165, 270, 30);
        btnIrRegistro.addActionListener(e -> {
            Registro ventanaRegistro = new Registro();
            ventanaRegistro.setVisible(true);
        });
        add(btnIrRegistro);
    }

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
}