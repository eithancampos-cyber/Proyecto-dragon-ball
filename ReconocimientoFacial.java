import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * VISTA de la cámara (registro facial y login facial).
 * Solo muestra la cámara, toma varios fotogramas cuando se pulsa el botón y se los entrega al
 * controlador (ControladorAcceso) con setAlEscanear. La decisión de si el rostro es válido o si
 * coincide con el usuario NO se toma aquí: la toma el controlador con AnalizadorRostro y ModeloRostros.
 */
public class ReconocimientoFacial extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;
    private static final int IZQ = 40;

    private static final int FOTOGRAMAS_REGISTRO = 5; // al registrar se toman más muestras
    private static final int FOTOGRAMAS_LOGIN = 4;
    private static final int PAUSA_ENTRE_FOTOGRAMAS_MS = 250;

    private Consumer<List<BufferedImage>> alEscanear;
    private Runnable alRegistroCompletado;
    private Runnable alLoginCompletado;
    private Runnable alVolver;

    private Object webcam;
    private Object webcamPanel;
    private JPanel contenedorCamara;
    private JLabel lblTitulo;
    private JButton btnCapturar;
    private String textoBoton = "Escanear y Validar";
    private String modoActual = "LOGIN";
    private boolean escaneando = false;

    public ReconocimientoFacial() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondoLogin.gif"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        lblTitulo = new Login.EtiquetaDragonBall("IDENTIFICACIÓN FACIAL");
        lblTitulo.setBounds(IZQ, 25, 400, 40);
        fondo.add(lblTitulo);

        contenedorCamara = new JPanel(new BorderLayout());
        contenedorCamara.setBounds(IZQ, 80, 400, 300);
        contenedorCamara.setBorder(BorderFactory.createLineBorder(BotonDragonBall.AMARILLO, 3));
        fondo.add(contenedorCamara);

        JLabel lblConsejo = new JLabel("Mira de frente, con buena luz y sin taparte la cara", SwingConstants.CENTER);
        lblConsejo.setForeground(Color.WHITE);
        lblConsejo.setFont(new Font("Arial", Font.BOLD, 12));
        lblConsejo.setBounds(IZQ, 382, 400, 16);
        fondo.add(lblConsejo);

        btnCapturar = new BotonDragonBall("Escanear y Validar");
        btnCapturar.setBounds(IZQ, 400, 400, 40);
        btnCapturar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCapturar.addActionListener(e -> escanear());
        fondo.add(btnCapturar);

        JButton btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds(IZQ, 450, 400, 38);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.addActionListener(e -> volver());
        fondo.add(btnVolver);
    }

    // ------------------------------------------------------------------
    // Escaneo: toma varios fotogramas sin congelar la ventana
    // ------------------------------------------------------------------

    private void escanear() {
        if (escaneando) return;
        if (webcam == null) {
            mostrarAviso("Error", "No se detectó ninguna cámara. Verifica que esté conectada y que otro programa no la esté usando.",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        escaneando = true;
        btnCapturar.setEnabled(false);
        btnCapturar.setText("Escaneando... no te muevas");
        final int cuantos = modoActual.equals("REGISTRO") ? FOTOGRAMAS_REGISTRO : FOTOGRAMAS_LOGIN;

        new SwingWorker<List<BufferedImage>, Void>() {
            @Override
            protected List<BufferedImage> doInBackground() throws Exception {
                List<BufferedImage> fotogramas = new ArrayList<>();
                for (int i = 0; i < cuantos; i++) {
                    Image img = obtenerImagenWebcam();
                    if (img instanceof BufferedImage) fotogramas.add((BufferedImage) img);
                    Thread.sleep(PAUSA_ENTRE_FOTOGRAMAS_MS);
                }
                return fotogramas;
            }

            @Override
            protected void done() {
                escaneando = false;
                btnCapturar.setEnabled(true);
                btnCapturar.setText(textoBoton);
                try {
                    List<BufferedImage> fotogramas = get();
                    if (alEscanear != null) alEscanear.accept(fotogramas);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarAviso("Error", "Error al procesar el escaneo facial.", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private Image obtenerImagenWebcam() {
        try {
            if (webcam != null) {
                return (Image) webcam.getClass().getMethod("getImage").invoke(webcam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Modo y cámara
    // ------------------------------------------------------------------

    public void configurarModo(String modo) {
        this.modoActual = modo;
        if (modo.equals("REGISTRO")) {
            lblTitulo.setText("REGISTRO FACIAL");
            textoBoton = "Capturar y Guardar Rostro";
        } else {
            lblTitulo.setText("IDENTIFICACIÓN FACIAL");
            textoBoton = "Escanear y Validar";
        }
        btnCapturar.setText(textoBoton);
        iniciarCamara();
    }

    /** "REGISTRO" o "LOGIN": Main lo usa para saber a qué pantalla volver. */
    public String getModo() {
        return modoActual;
    }

    private void iniciarCamara() {
        cerrarCamara();
        contenedorCamara.removeAll();

        try {
            Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
            webcam = webcamClass.getMethod("getDefault").invoke(null);
            if (webcam != null) {
                Class<?> resolutionClass = Class.forName("com.github.sarxos.webcam.WebcamResolution");
                Object vga = resolutionClass.getField("VGA").get(null);
                Dimension size = (Dimension) resolutionClass.getMethod("getSize").invoke(vga);
                webcamClass.getMethod("setViewSize", Dimension.class).invoke(webcam, size);

                Class<?> panelClass = Class.forName("com.github.sarxos.webcam.WebcamPanel");
                webcamPanel = panelClass.getConstructor(webcamClass).newInstance(webcam);
                panelClass.getMethod("setFPSDisplayed", boolean.class).invoke(webcamPanel, false);
                panelClass.getMethod("setDisplayDebugInfo", boolean.class).invoke(webcamPanel, false);
                panelClass.getMethod("setMirrored", boolean.class).invoke(webcamPanel, true);
                contenedorCamara.add((Component) webcamPanel, BorderLayout.CENTER);
            } else {
                mostrarMensajeError("No se detectó ninguna cámara web.");
            }
        } catch (Throwable t) {
            webcam = null; // sin cámara utilizable: escanear() avisará
            mostrarMensajeError("Error al iniciar el controlador de la cámara.");
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
                webcamPanel.getClass().getMethod("stop").invoke(webcamPanel);
            }
            if (webcam != null && (Boolean) webcam.getClass().getMethod("isOpen").invoke(webcam)) {
                webcam.getClass().getMethod("close").invoke(webcam);
            }
        } catch (Exception ignored) {}
        webcamPanel = null;
        webcam = null;
    }

    // ------------------------------------------------------------------
    // API para el controlador y para Main
    // ------------------------------------------------------------------

    public void mostrarAviso(String titulo, String mensaje, int tipo) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipo);
    }

    public void mostrarInfo(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Reconocimiento facial", JOptionPane.INFORMATION_MESSAGE);
    }

    /** El rostro se capturó bien: se cierra la cámara y Main vuelve a la pantalla de Registro. */
    public void registroCompletado() {
        cerrarCamara();
        if (alRegistroCompletado != null) alRegistroCompletado.run();
    }

    /** El rostro coincide con el usuario: se cierra la cámara y Main entra al juego. */
    public void loginCompletado() {
        cerrarCamara();
        if (alLoginCompletado != null) alLoginCompletado.run();
    }

    /** Cierra la cámara y Main regresa a la pantalla de donde se vino (Registro o Login). */
    public void volver() {
        cerrarCamara();
        if (alVolver != null) alVolver.run();
    }

    public void setAlEscanear(Consumer<List<BufferedImage>> accion) { this.alEscanear = accion; }
    public void setAlRegistroCompletado(Runnable accion) { this.alRegistroCompletado = accion; }
    public void setAlLoginCompletado(Runnable accion) { this.alLoginCompletado = accion; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}