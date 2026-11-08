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
                Image imagenCapturada = obtenerImagenWebcam();
                
                if (imagenCapturada == null) {
                    JOptionPane.showMessageDialog(this, "⚠️ No se pudo capturar la imagen. Verifica la cámara.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // VALIDACIÓN BLINDADA: EXIGE ROSTRO COMPLETO, PROPORCIÓN Y SIMETRÍA REAL
                boolean rostroDetectado = esRostroHumanoCompletoReal(imagenCapturada);

                if (!rostroDetectado) {
                    JOptionPane.showMessageDialog(this, "Acceso denegado, rostro no encontrado", "Error de Validación", JOptionPane.WARNING_MESSAGE);
                    return; 
                }

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

    /**
     * Valida que el objeto frente a la cámara sea un rostro completo real mediante
     * el análisis de los límites (Bounding Box), su proporción de altura/ancho y 
     * su simetría horizontal estricta. Si tapas la mitad de la cara con la mano, 
     * la asimetría se dispara y se rechaza inmediatamente.
     */
    private boolean esRostroHumanoCompletoReal(Image img) {
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

            int minX = ancho, maxX = 0, minY = alto, maxY = 0;
            int totalPixelesPiel = 0;
            boolean[][] esPielMatriz = new boolean[ancho][alto];

            for (int y = 0; y < alto; y += 2) {
                for (int x = 0; x < ancho; x += 2) {
                    int rgb = bimage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    boolean esPiel = (r > 75 && g > 30 && b > 15) &&
                                     (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 12) &&
                                     (r > g) && (r > b);

                    if (esPiel) {
                        esPielMatriz[x][y] = true;
                        totalPixelesPiel++;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            // 1. Si no hay suficiente volumen de piel, rechazar
            if (totalPixelesPiel < 300) return false;

            int bboxAncho = maxX - minX;
            int bboxAlto = maxY - minY;

            // 2. Validar tamaño mínimo de la cara
            if (bboxAncho < 50 || bboxAlto < 70) return false;

            // 3. Validar proporción (Aspect Ratio): el rostro es más alto que ancho
            double aspectRatio = (double) bboxAlto / bboxAncho;
            if (aspectRatio < 1.0 || aspectRatio > 2.2) {
                return false; 
            }

            // 4. Validar Simetría Horizontal estricta
            int centroX = minX + bboxAncho / 2;
            int pielIzquierda = 0;
            int pielDerecha = 0;

            for (int y = minY; y <= maxY; y += 2) {
                for (int x = minX; x <= maxX; x += 2) {
                    if (x >= 0 && x < ancho && y >= 0 && y < alto && esPielMatriz[x][y]) {
                        if (x < centroX) {
                            pielIzquierda++;
                        } else if (x > centroX) {
                            pielDerecha++;
                        }
                    }
                }
            }

            int totalPielSimetria = pielIzquierda + pielDerecha;
            if (totalPielSimetria == 0) return false;

            double diferenciaSimetria = (double) Math.abs(pielIzquierda - pielDerecha) / totalPielSimetria;

            // REGLA ABSOLUTA: Si tapas la mitad de la cara, la asimetría supera el 22% y se rechaza al instante
            if (diferenciaSimetria > 0.22) {
                return false; 
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true; 
    }
}