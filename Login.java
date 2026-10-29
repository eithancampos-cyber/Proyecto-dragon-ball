import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.net.URL;
import javax.swing.*;

/**
 * Pantalla de Login con estilo visual Dragon Ball y fondo ajustado.
 * Todos los elementos comparten el mismo bloque centrado (mismo borde izquierdo y derecho).
 */
public class Login extends JPanel {

    private static final int ANCHO = 960, ALTO = 540;

    private Runnable alIniciarSesion, alIrRegistro; // navegación que Main le asigna

    public Login() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        PanelFondo fondo = new PanelFondo(cargarImagen("fondo2.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        // Bloque centrado: todo se alinea a los mismos bordes izquierdo y derecho
        int anchoBloque = 360, separacion = 16;
        int izq = (ANCHO - anchoBloque) / 2;
        int y = 175; // inicio vertical (bloque centrado en la altura)
        int mitad = (anchoBloque - separacion) / 2;

        agregarFila(fondo, "Usuario:", new CampoTexto(), izq, y, anchoBloque);
        agregarFila(fondo, "Contraseña:", new CampoClave(), izq, y + 46, anchoBloque);

        y += 104; // debajo de las dos filas (46 + 34 de alto + 24 de aire)
        fondo.add(crearBoton("Iniciar Sesión", izq, y, mitad, () -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
            if (alIniciarSesion != null) alIniciarSesion.run();
        }));
        fondo.add(crearBoton("Login Facial", izq + mitad + separacion, y, mitad, () ->
            JOptionPane.showMessageDialog(this, "Aquí abriría la cámara para el reconocimiento facial")));
        fondo.add(crearBoton("¿No tienes cuenta? Regístrate", izq, y + 48, anchoBloque, () -> {
            if (alIrRegistro != null) alIrRegistro.run();
        }));
    }

    /** Agrega una fila "etiqueta + campo"; el campo termina justo en el borde derecho del bloque. */
    private static void agregarFila(JPanel destino, String texto, JTextField campo, int izq, int y, int anchoBloque) {
        int anchoEtiqueta = 115;
        EtiquetaDragonBall etiqueta = new EtiquetaDragonBall(texto);
        etiqueta.setBounds(izq, y, anchoEtiqueta, 34);
        campo.setBounds(izq + anchoEtiqueta + 5, y, anchoBloque - anchoEtiqueta - 5, 34);
        destino.add(etiqueta);
        destino.add(campo);
    }

    /** Crea un botón Dragon Ball de 38 px de alto con su acción. */
    private static JButton crearBoton(String texto, int x, int y, int ancho, Runnable accion) {
        JButton boton = new BotonDragonBall(texto);
        boton.setBounds(x, y, ancho, 38);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    /** Carga una imagen del classpath o de la carpeta del proyecto (también la usan Registro y MatrizPatrones). */
    static Image cargarImagen(String nombre) {
        URL url = Login.class.getResource("/" + nombre);
        if (url != null) return new ImageIcon(url).getImage();
        for (String ruta : new String[]{nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre}) {
            if (new File(ruta).exists()) return new ImageIcon(ruta).getImage();
        }
        System.err.println("No se encontró la imagen de fondo: " + nombre);
        return null;
    }

    // ------------------------------------------------------------------
    // Componentes con el estilo Dragon Ball (los colores vienen de BotonDragonBall)
    // ------------------------------------------------------------------

    private static final Color CREMA = new Color(255, 246, 205);

    /** Dibuja el fondo redondeado con borde negro grueso (naranja si tiene el foco). */
    private static void pintarCampo(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(CREMA);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 22, 22);
        g2.setColor(c.isFocusOwner() ? BotonDragonBall.NARANJA : Color.BLACK);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 22, 22);
        g2.dispose();
    }

    /** Aplica fuente, colores y margen internos comunes a los campos. */
    private static void estilizarCampo(JTextField campo) {
        campo.setOpaque(false); // el fondo lo pinta pintarCampo
        campo.setFont(new Font("Arial", Font.BOLD, 14));
        campo.setForeground(BotonDragonBall.CAFE_OSCURO);
        campo.setCaretColor(BotonDragonBall.CAFE_OSCURO);
        campo.setSelectionColor(BotonDragonBall.AMARILLO);
        campo.setSelectedTextColor(BotonDragonBall.CAFE_OSCURO);
        campo.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        campo.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { campo.repaint(); }
            @Override public void focusLost(FocusEvent e) { campo.repaint(); }
        });
    }

    /** Campo de usuario con estilo Dragon Ball. */
    static class CampoTexto extends JTextField {
        CampoTexto() { estilizarCampo(this); }
        @Override protected void paintComponent(Graphics g) { pintarCampo(g, this); super.paintComponent(g); }
    }

    /** Campo de contraseña con estilo Dragon Ball. */
    static class CampoClave extends JPasswordField {
        CampoClave() { estilizarCampo(this); }
        @Override protected void paintComponent(Graphics g) { pintarCampo(g, this); super.paintComponent(g); }
    }

    /** Etiqueta amarilla con contorno negro grueso, como las letras del anime. */
    static class EtiquetaDragonBall extends JLabel {
        EtiquetaDragonBall(String texto) {
            super(texto);
            setFont(new Font("Arial", Font.BOLD, 17));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = 3; // deja espacio para el contorno
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(Color.BLACK); // contorno: el texto desplazado en todas las direcciones
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    if (dx != 0 || dy != 0) g2.drawString(getText(), x + dx, y + dy);

            g2.setColor(BotonDragonBall.AMARILLO); // relleno encima
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Panel que estira la imagen a todo su tamaño (o pinta un color liso si no hay imagen). */
    static class PanelFondo extends JPanel {
        private final Image imagen;

        PanelFondo(Image imagen) { this.imagen = imagen; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imagen != null) {
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