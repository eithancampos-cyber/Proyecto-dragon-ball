import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Registro extends JFrame {
    private Webcam webcam = null;

    public Registro() {
        setTitle("Registro de Nuevo Usuario con Cámara");
        setSize(400, 450);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblUsuario = new JLabel("Nuevo Usuario:");
        lblUsuario.setBounds(30, 20, 100, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(140, 20, 200, 25);
        add(txtUsuario);

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 60, 100, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(140, 60, 200, 25);
        add(txtPassword);

        // Panel para mostrar la cámara en vivo
        JPanel panelCamara = new JPanel();
        panelCamara.setBounds(30, 100, 310, 230);
        panelCamara.setLayout(new BorderLayout());
        add(panelCamara);

        // Inicializar la webcam predeterminada de la PC
        webcam = Webcam.getDefault();
        if (webcam != null) {
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            WebcamPanel webcamPanel = new WebcamPanel(webcam);
            webcamPanel.setFPSDisplayed(true);
            panelCamara.add(webcamPanel, BorderLayout.CENTER);
            
            // Hilo separado para iniciar la cámara sin congelar la ventana
            Thread t = new Thread(() -> webcam.open());
            t.setDaemon(true);
            t.start();
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró ninguna cámara web.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        JButton btnGuardar = new JButton("Capturar Rostro y Registrarse");
        btnGuardar.setBounds(30, 350, 310, 30);
        btnGuardar.addActionListener(e -> {
            String usuario = txtUsuario.getText().trim();
            if (usuario.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe un nombre de usuario primero.");
                return;
            }

            if (webcam != null && webcam.isOpen()) {
                // Crear carpeta para guardar los rostros
                File carpeta = new File("rostros_registrados");
                if (!carpeta.exists()) {
                    carpeta.mkdir();
                }

                // Tomar la foto actual de la cámara
                java.awt.image.BufferedImage imagen = webcam.getImage();
                File archivoSalida = new File(carpeta, usuario + ".jpg");

                try {
                    ImageIO.write(imagen, "JPG", archivoSalida);
                    JOptionPane.showMessageDialog(this, "¡Rostro capturado y registrado con éxito para " + usuario + "!");
                    
                    // Apagar cámara antes de cerrar
                    webcam.close();
                    dispose();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error al guardar la foto: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "La cámara no está activa.");
            }
        });
        add(btnGuardar);
        
        // Asegurarse de apagar la cámara si cierran la ventana con la 'X'
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                }
            }
        });
    }
}