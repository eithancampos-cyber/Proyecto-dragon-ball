import java.awt.*;
import javax.swing.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class ReconocimientoFacial extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;
    private Runnable alFinalizar;
    
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private JPanel contenedorCamara;
    private JLabel lblTitulo;
    private JButton btnCapturar;

    public ReconocimientoFacial() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondo2.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        lblTitulo = new Login.EtiquetaDragonBall("IDENTIFICACIÓN FACIAL");
        lblTitulo.setBounds(280, 25, 400, 40);
        fondo.add(lblTitulo);

        contenedorCamara = new JPanel(new BorderLayout());
        contenedorCamara.setBounds(280, 80, 400, 300);
        contenedorCamara.setBorder(BorderFactory.createLineBorder(BotonDragonBall.AMARILLO, 3));
        fondo.add(contenedorCamara);

        btnCapturar = new BotonDragonBall("Escanear y Validar");
        btnCapturar.setBounds(280, 400, 400, 40);
        btnCapturar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCapturar.addActionListener(e -> {
            cerrarCamara();
            JOptionPane.showMessageDialog(this, "¡Proceso facial exitoso!");
            if (alFinalizar != null) alFinalizar.run();
        });
        fondo.add(btnCapturar);

        JButton btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds(280, 450, 400, 38);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.addActionListener(e -> {
            cerrarCamara();
            if (alFinalizar != null) alFinalizar.run();
        });
        fondo.add(btnVolver);
    }

    // Método para configurar el modo (LOGIN o REGISTRO) y abrir la cámara fresca cada vez que se entra
    public void configurarModo(String modo) {
        if (modo.equals("REGISTRO")) {
            lblTitulo.setText("REGISTRO FACIAL");
            btnCapturar.setText("Capturar y Guardar Rostro");
        } else {
            lblTitulo.setText("IDENTIFICACIÓN FACIAL");
            btnCapturar.setText("Escanear y Validar");
        }
        iniciarCamara();
    }

    private void iniciarCamara() {
        cerrarCamara(); // Asegura limpiar cualquier sesión anterior de la cámara
        contenedorCamara.removeAll();

        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcamPanel = new WebcamPanel(webcam);
                webcamPanel.setFPSDisplayed(false);
                webcamPanel.setDisplayDebugInfo(false);
                webcamPanel.setMirrored(true);
                contenedorCamara.add(webcamPanel, BorderLayout.CENTER);
            } else {
                mostrarMensajeError("⚠️ No se detectó ninguna cámara web.");
            }
        } catch (Throwable t) {
            mostrarMensajeError("⚠️ Error al iniciar el controlador de la cámara.");
        }
        contenedorCamara.revalidate();
        contenedorCamara.repaint();
    }

    private void mostrarMensajeError(String mensaje) {
        JLabel lblError = new JLabel(mensaje, SwingConstants.CENTER);
        lblError.setForeground(Color.WHITE);
        lblError.setFont(new Font("Arial", Font.BOLD, 13));
        contenedorCamara.setBackground(new Color(20, 20, 30));
        contenedorCamara.add(lblError, BorderLayout.CENTER);
    }

    public void cerrarCamara() {
        try {
            if (webcamPanel != null) {
                webcamPanel.stop();
            }
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (Exception ignored) {}
    }

    public void setAlFinalizar(Runnable accion) {
        this.alFinalizar = accion;
    }
}