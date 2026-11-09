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
    private String usuarioObjetivo = ""; 
    private final ModeloUsuarios modeloUsuarios;

    public ReconocimientoFacial() {
        this(new ModeloUsuarios());
    }

    public ReconocimientoFacial(ModeloUsuarios modeloUsuarios) {
        this.modeloUsuarios = modeloUsuarios != null ? modeloUsuarios : new ModeloUsuarios();
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        Login.PanelFondo fondo = new Login.PanelFondo(Login.cargarImagen("fondoLogin.gif"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        lblTitulo = new Login.EtiquetaDragonBall("IDENTIFICACIÓN FACIAL");
        lblTitulo.setBounds(IZQ, 25, 500, 40);
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
                    JOptionPane.showMessageDialog(this, "⚠️ No se pudo capturar la imagen. Verifica la cámara.", "Error de Cámara", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                DatosRostro datosRostro = analizarYExtraerFirmaRostro(imagenCapturada);
                if (!datosRostro.valido) {
                    JOptionPane.showMessageDialog(this, "⚠️ No se detecta tu rostro con claridad.\nColócate enfrente de la cámara.", "Rostro no detectado", JOptionPane.WARNING_MESSAGE);
                    return; 
                }

                if (modoActual.equals("REGISTRO")) {
                    if (usuarioObjetivo == null || usuarioObjetivo.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "⚠️ Error: No hay un usuario asociado para el registro facial.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    boolean guardado = this.modeloUsuarios.guardarRostroUsuario(usuarioObjetivo, datosRostro.firma);
                    if (guardado) {
                        JOptionPane.showMessageDialog(this, "¡Rostro registrado con éxito para " + usuarioObjetivo + "!");
                        cerrarCamara();
                        if (alFinalizar != null) alFinalizar.run();
                    } else {
                        JOptionPane.showMessageDialog(this, "❌ Error al guardar el rostro en el archivo.", "Error de Guardado", JOptionPane.ERROR_MESSAGE);
                    }

                } else {
                    if (usuarioObjetivo == null || usuarioObjetivo.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "⚠️ Por favor ingresa primero tu usuario en la pantalla de login.", "Usuario Requerido", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (!this.modeloUsuarios.tieneRostroRegistrado(usuarioObjetivo)) {
                        JOptionPane.showMessageDialog(this, "❌ El usuario '" + usuarioObjetivo + "' no tiene ningún rostro registrado todavía. Regístrate primero.", "Sin Registro Facial", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    boolean identidadValida = this.modeloUsuarios.validarRostroUsuario(usuarioObjetivo, datosRostro.firma);

                    if (!identidadValida) {
                        JOptionPane.showMessageDialog(this, "❌ ACCESO DENEGADO: El rostro frente a la cámara NO coincide con el usuario '" + usuarioObjetivo + "'.", "Fallo de Autenticación", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    JOptionPane.showMessageDialog(this, "¡Identificación facial exitosa. Bienvenido, " + usuarioObjetivo + "!");
                    cerrarCamara();
                    if (alEntrarAlJuego != null) {
                        alEntrarAlJuego.run();
                    } else if (alFinalizar != null) {
                        alFinalizar.run();
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "⚠️ Error crítico al procesar el escaneo facial.", "Error", JOptionPane.ERROR_MESSAGE);
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
        configurarModo(modo, this.usuarioObjetivo);
    }

    public void configurarModo(String modo, String usuario) {
        this.modoActual = modo;
        if (usuario != null && !usuario.trim().isEmpty()) {
            this.usuarioObjetivo = usuario;
        }
        if (modo.equals("REGISTRO")) {
            lblTitulo.setText("REGISTRO FACIAL: " + this.usuarioObjetivo);
            btnCapturar.setText("Capturar y Guardar Rostro");
        } else {
            lblTitulo.setText("LOGIN FACIAL: " + this.usuarioObjetivo);
            btnCapturar.setText("Escanear y Validar Identidad");
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

    public void setAlFinalizar(Runnable accion) { this.alFinalizar = accion; }
    public void setAlEntrarAlJuego(Runnable accion) { this.alEntrarAlJuego = accion; }

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

    private static class DatosRostro {
        boolean valido;
        String firma;
        DatosRostro(boolean valido, String firma) {
            this.valido = valido;
            this.firma = firma;
        }
    }

    private DatosRostro analizarYExtraerFirmaRostro(Image img) {
        if (img == null) return new DatosRostro(false, "");
        
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

            int minRoiX = (int) (ancho * 0.10);
            int maxRoiX = (int) (ancho * 0.90);
            int minRoiY = (int) (alto * 0.05);
            int maxRoiY = (int) (alto * 0.95);

            int minX = maxRoiX, maxX = minRoiX, minY = maxRoiY, maxY = minRoiY;
            int totalPixelesPiel = 0;

            for (int y = minRoiY; y < maxRoiY; y += 2) {
                for (int x = minRoiX; x < maxRoiX; x += 2) {
                    int rgb = bimage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    boolean esPiel = (r > 50 && g > 25 && b > 10) &&
                                     (r > g) && (r > b) &&
                                     (Math.abs(r - g) > 8);

                    if (esPiel) {
                        totalPixelesPiel++;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (totalPixelesPiel < 120) return new DatosRostro(false, "");

            int bboxAncho = maxX - minX;
            int bboxAlto = maxY - minY;

            if (bboxAncho < 35 || bboxAlto < 45) return new DatosRostro(false, "");

            StringBuilder sb = new StringBuilder();
            int stepX = Math.max(1, bboxAncho / 3);
            int stepY = Math.max(1, bboxAlto / 3);

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int subMinX = minX + col * stepX;
                    int subMaxX = (col == 2) ? maxX : subMinX + stepX;
                    int subMinY = minY + row * stepY;
                    int subMaxY = (row == 2) ? maxY : subMinY + stepY;

                    int count = 0;
                    int area = Math.max(1, (subMaxX - subMinX) * (subMaxY - subMinY) / 2);
                    for (int y = subMinY; y <= subMaxY; y += 2) {
                        for (int x = subMinX; x <= subMaxX; x += 2) {
                            if (x >= 0 && x < ancho && y >= 0 && y < alto) {
                                int rgb = bimage.getRGB(x, y);
                                int r = (rgb >> 16) & 0xFF;
                                int g = (rgb >> 8) & 0xFF;
                                int b = rgb & 0xFF;
                                if ((r > 50 && g > 25 && b > 10) && (r > g) && (r > b)) {
                                    count++;
                                }
                            }
                        }
                    }
                    double ratio = (double) count / area;
                    sb.append(String.format(java.util.Locale.US, "%.3f", ratio)).append(",");
                }
            }

            return new DatosRostro(true, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new DatosRostro(false, "");
        }
    }
}