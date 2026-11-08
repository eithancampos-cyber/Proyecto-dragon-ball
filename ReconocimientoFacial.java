import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class ReconocimientoFacial extends JPanel {

    private static final int ANCHO = 960;
    private static final int ALTO = 540;
    private static final int IZQ = 40; 
    private Runnable alFinalizar;
    private Runnable alEntrarAlJuego;

    private Object webcam;
    private Object webcamPanel;
    private JPanel contenedorCamara;
    private JLabel lblTitulo;
    private JButton btnCapturar;
    private String modoActual = "LOGIN";

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

        btnCapturar = new BotonDragonBall("Escanear y Validar");
        btnCapturar.setBounds(IZQ, 400, 400, 40);
        btnCapturar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCapturar.addActionListener(e -> {
            try {
                // 1. Obtener la imagen actual de la cámara
                Image imagenCapturada = obtenerImagenWebcam();
                
                if (imagenCapturada == null) {
                    JOptionPane.showMessageDialog(this, "⚠️ No se pudo capturar la imagen. Verifica la cámara.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 2. VALIDACIÓN ESTRICTA POR DETECCIÓN DE ROSTRO REAL
                boolean rostroDetectado = detectarRostroReal(imagenCapturada);

                if (!rostroDetectado) {
                    JOptionPane.showMessageDialog(this, "Acceso denegado, rostro no encontrado", "Error de Validación", JOptionPane.WARNING_MESSAGE);
                    return; // Bloquea totalmente el avance
                }

                // 3. Proceso exitoso
                cerrarCamara();
                
                if (modoActual.equals("REGISTRO")) {
                    JOptionPane.showMessageDialog(this, "¡Rostro registrado con éxito!");
                    if (alFinalizar != null) alFinalizar.run();
                } else {
                    JOptionPane.showMessageDialog(this, "¡Identificación facial exitosa!");
                    if (alEntrarAlJuego != null) {
                        alEntrarAlJuego.run();
                    } else if (alFinalizar != null) {
                        alFinalizar.run();
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "⚠️ Error al procesar el escaneo facial.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        fondo.add(btnCapturar);

        JButton btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds(IZQ, 450, 400, 38);
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.addActionListener(e -> {
            cerrarCamara();
            if (alFinalizar != null) alFinalizar.run();
        });
        fondo.add(btnVolver);
    }

    public void configurarModo(String modo) {
        this.modoActual = modo;
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
                webcamPanel.getClass().getMethod("stop").invoke(webcamPanel);
            }
            if (webcam != null && (Boolean) webcam.getClass().getMethod("isOpen").invoke(webcam)) {
                webcam.getClass().getMethod("close").invoke(webcam);
            }
        } catch (Exception ignored) {}
        webcamPanel = null;
        webcam = null;
    }

    public void setAlFinalizar(Runnable accion) {
        this.alFinalizar = accion;
    }

    public void setAlEntrarAlJuego(Runnable accion) {
        this.alEntrarAlJuego = accion;
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

    private boolean detectarRostroReal(Image img) {
        if (img == null) return false;
        
        try {
            BufferedImage bimage;
            if (img instanceof BufferedImage) {
                bimage = (BufferedImage) img;
            } else {
                bimage = new BufferedImage(
                    img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bimage.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
            }

            int ancho = bimage.getWidth();
            int alto = bimage.getHeight();

            int pixelesPiel = 0;
            int pixelesTotalesAnalizados = 0;

            int inicioX = ancho / 4;
            int finX = (ancho * 3) / 4;
            int inicioY = alto / 4;
            int finY = (alto * 3) / 4;

            for (int y = inicioY; y < finY; y += 4) {
                for (int x = inicioX; x < finX; x += 4) {
                    int rgb = bimage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    boolean esTonoPiel = (r > 95 && g > 40 && b > 20) &&
                                         (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 15) &&
                                         (Math.abs(r - g) > 15) && (r > g) && (r > b);

                    if (esTonoPiel) {
                        pixelesPiel++;
                    }
                    pixelesTotalesAnalizados++;
                }
            }

            if (pixelesTotalesAnalizados == 0) return false;

            double porcentajePiel = (double) pixelesPiel / pixelesTotalesAnalizados;

            if (porcentajePiel < 0.08) {
                return false; 
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true; 
    }
}