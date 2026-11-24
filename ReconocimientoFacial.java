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

    // Medidas base de la pantalla y margen izquierdo para alinear los elementos
    private static final int ANCHO = 960;
    private static final int ALTO = 540;
    private static final int IZQ = 40;

    // Cuántas fotos se toman al escanear y cuánto se espera entre cada toma
    private static final int FOTOGRAMAS_REGISTRO = 5; // al registrar se toman más muestras
    private static final int FOTOGRAMAS_LOGIN = 4;
    private static final int PAUSA_ENTRE_FOTOGRAMAS_MS = 250;

    // Acciones que el controlador o Main conectan para reaccionar al escanear o terminar
    private Consumer<List<BufferedImage>> alEscanear;
    private Runnable alRegistroCompletado;
    private Runnable alLoginCompletado;
    private Runnable alVolver;

    // Referencias a la cámara y al panel de video (se guardan como Object porque se cargan por reflexión)
    private Object webcam;
    private Object webcamPanel;
    private JPanel contenedorCamara;
    private JLabel lblTitulo;
    private JButton btnCapturar;
    private String textoBoton = "Escanear y Validar";
    private String modoActual = "LOGIN";
    private boolean escaneando = false; // evita que se pulse el botón dos veces mientras toma fotos

    // Constructor: arma la pantalla visual de la cámara, el título, los botones y el consejo
    public ReconocimientoFacial() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        // Fondo animado de la pantalla
        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondoLogin.gif"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        // Título principal de la pantalla
        lblTitulo = new Login.EtiquetaDragonBall("IDENTIFICACIÓN FACIAL");
        lblTitulo.setBounds(IZQ, 25, 400, 40);
        fondo.add(lblTitulo);

        // Recuadro donde se va a mostrar la transmisión en vivo de la cámara
        contenedorCamara = new JPanel(new BorderLayout());
        contenedorCamara.setBounds(IZQ, 80, 400, 300);
        contenedorCamara.setBorder(BorderFactory.createLineBorder(BotonDragonBall.AMARILLO, 3));
        fondo.add(contenedorCamara);

        // Pequeña guía para que la persona se coloque bien frente a la cámara
        JLabel lblConsejo = new JLabel("Mira de frente, con buena luz y sin taparte la cara", SwingConstants.CENTER);
        lblConsejo.setForeground(Color.WHITE);
        lblConsejo.setFont(new Font("Arial", Font.BOLD, 12));
        lblConsejo.setBounds(IZQ, 382, 400, 16);
        fondo.add(lblConsejo);

        // Botón para tomar las fotos y mandarlas a validar
        btnCapturar = new BotonDragonBall("Escanear y Validar");
        btnCapturar.setBounds(IZQ, 400, 400, 40);
        btnCapturar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCapturar.addActionListener(e -> escanear());
        fondo.add(btnCapturar);

        // Botón para regresar a la pantalla anterior sin completar el proceso
        JButton btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds(IZQ, 450, 400, 38);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.addActionListener(e -> volver());
        fondo.add(btnVolver);
    }

    // ------------------------------------------------------------------
    // Escaneo: toma varios fotogramas sin congelar la ventana
    // ------------------------------------------------------------------

    // Toma varias fotos seguidas en segundo plano y luego se las pasa al controlador
    private void escanear() {
        // Si ya está escaneando, ignoramos nuevos clics
        if (escaneando) return;
        // Si no hay cámara lista, mostramos un aviso de error
        if (webcam == null) {
            mostrarAviso("Error", "No se detectó ninguna cámara. Verifica que esté conectada y que otro programa no la esté usando.",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Bloqueamos el botón mientras dura la captura
        escaneando = true;
        btnCapturar.setEnabled(false);
        btnCapturar.setText("Escaneando... no te muevas");
        final int cuantos = modoActual.equals("REGISTRO") ? FOTOGRAMAS_REGISTRO : FOTOGRAMAS_LOGIN;

        // Usamos SwingWorker para esperar entre fotos sin congelar la ventana de Swing
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
                // Restauramos el botón al terminar de capturar
                escaneando = false;
                btnCapturar.setEnabled(true);
                btnCapturar.setText(textoBoton);
                try {
                    List<BufferedImage> fotogramas = get();
                    // Entregamos las imágenes capturadas al controlador para que las analice
                    if (alEscanear != null) alEscanear.accept(fotogramas);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarAviso("Error", "Error al procesar el escaneo facial.", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Pide una foto actual a la webcam usando reflexión (sin importar la librería directo)
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

    // Ajusta textos y botones según si se usa para registrar un rostro o para iniciar sesión
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

    // Busca y enciende la cámara web, configurando resolución VGA y vista tipo espejo
    private void iniciarCamara() {
        // Primero cerramos cualquier cámara abierta anteriormente para evitar conflictos
        cerrarCamara();
        contenedorCamara.removeAll();

        try {
            // Cargamos las clases de Webcam Capture por reflexión para no depender de ellas en tiempo de compilación
            Class<?> webcamClass = Class.forName("com.github.sarxos.webcam.Webcam");
            webcam = webcamClass.getMethod("getDefault").invoke(null);
            if (webcam != null) {
                Class<?> resolutionClass = Class.forName("com.github.sarxos.webcam.WebcamResolution");
                Object vga = resolutionClass.getField("VGA").get(null);
                Dimension size = (Dimension) resolutionClass.getMethod("getSize").invoke(vga);
                webcamClass.getMethod("setViewSize", Dimension.class).invoke(webcam, size);

                // Creamos el panel visual de la cámara y lo ponemos en modo espejo para que sea más natural
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
            t.printStackTrace(); 
            webcam = null; // sin cámara utilizable: escanear() avisará
            mostrarMensajeError("Error al iniciar el controlador de la cámara.");
        }
        // Refrescamos el contenedor para que se vea el video o el mensaje de error
        contenedorCamara.revalidate();
        contenedorCamara.repaint();
    }

    // Muestra un texto centrado dentro del cuadro de la cámara si no se pudo iniciar
    private void mostrarMensajeError(String mensaje) {
        JLabel lblError = new JLabel(mensaje, SwingConstants.CENTER);
        lblError.setForeground(Color.WHITE);
        lblError.setFont(new Font("Arial", Font.BOLD, 13));
        contenedorCamara.setBackground(new Color(20, 20, 30));
        contenedorCamara.add(lblError, BorderLayout.CENTER);
    }

    // Detiene el panel de video y cierra la cámara para liberar el hardware
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

    // Muestra una ventana emergente con el título, mensaje e ícono que pida el controlador
    public void mostrarAviso(String titulo, String mensaje, int tipo) {
        JOptionPane.showMessageDialog(this, mensaje, titulo, tipo);
    }

    // Muestra un mensaje informativo estándar del reconocimiento facial
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

    // Métodos para que otras clases (como Main o ControladorAcceso) conecten qué hacer en cada evento
    public void setAlEscanear(Consumer<List<BufferedImage>> accion) { this.alEscanear = accion; }
    public void setAlRegistroCompletado(Runnable accion) { this.alRegistroCompletado = accion; }
    public void setAlLoginCompletado(Runnable accion) { this.alLoginCompletado = accion; }
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}