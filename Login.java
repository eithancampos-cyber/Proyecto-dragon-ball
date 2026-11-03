import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.net.URL;
import javax.swing.*;

/**
 * Pantalla de Login con estilo visual Dragon Ball, botón para ver contraseña y acceso facial.
 */
public class Login extends JPanel {

    private static final int ANCHO = 960, ALTO = 540;

    private Runnable alIniciarSesion, alIrRegistro, alLoginFacial;
    private CampoTexto txtUsuario;
    private CampoClave txtPassword;
    private JButton btnIniciar;

    public Login() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        PanelFondo fondo = new PanelFondo(cargarImagen("fondo2.jpg"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        int anchoBloque = 360, separacion = 16;
        int izq = (ANCHO - anchoBloque) / 2;
        int y = 165; 
        int mitad = (anchoBloque - separacion) / 2;

        // Fila Usuario
        txtUsuario = new CampoTexto();
        agregarFilaUsuario(fondo, "Usuario:", txtUsuario, izq, y, anchoBloque);
        
        // Fila Contraseña con botón del ojo
        y += 46;
        txtPassword = new CampoClave();
        agregarFilaPasswordConOjo(fondo, "Contraseña:", txtPassword, izq, y, anchoBloque);

        y += 60; 
        // Iniciar Sesión lo maneja ControladorAcceso (valida con ModeloUsuarios)
        btnIniciar = crearBoton("Iniciar Sesión", izq, y, mitad, () -> { });
        fondo.add(btnIniciar);

        fondo.add(crearBoton("Login Facial", izq + mitad + separacion, y, mitad, () -> {
            if (alLoginFacial != null) alLoginFacial.run();
        }));

        fondo.add(crearBoton("¿No tienes cuenta? Regístrate", izq, y + 48, anchoBloque, () -> {
            limpiar();
            if (alIrRegistro != null) alIrRegistro.run();
        }));

        // Enter en el usuario pasa a la contraseña; Enter en la contraseña inicia sesión
        txtUsuario.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> btnIniciar.doClick());
    }

    private static void agregarFilaUsuario(JPanel destino, String texto, JTextField campo, int izq, int y, int anchoBloque) {
        int anchoEtiqueta = 115;
        EtiquetaDragonBall etiqueta = new EtiquetaDragonBall(texto);
        etiqueta.setBounds(izq, y, anchoEtiqueta, 34);
        campo.setBounds(izq + anchoEtiqueta + 5, y, anchoBloque - anchoEtiqueta - 5, 34);
        destino.add(etiqueta);
        destino.add(campo);
    }

    private static void agregarFilaPasswordConOjo(JPanel destino, String texto, CampoClave campo, int izq, int y, int anchoBloque) {
        int anchoEtiqueta = 115;
        EtiquetaDragonBall etiqueta = new EtiquetaDragonBall(texto);
        etiqueta.setBounds(izq, y, anchoEtiqueta, 34);
        
        int anchoCampoTotal = anchoBloque - anchoEtiqueta - 5;
        int anchoBotonOjo = 35;
        int anchoRealCampo = anchoCampoTotal - anchoBotonOjo - 5;

        campo.setBounds(izq + anchoEtiqueta + 5, y, anchoRealCampo, 34);

        JButton btnOjo = new JButton("👁");
        btnOjo.setBounds(izq + anchoEtiqueta + 5 + anchoRealCampo + 5, y, anchoBotonOjo, 34);
        btnOjo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnOjo.setFocusPainted(false);
        btnOjo.setBackground(new Color(255, 246, 205));
        btnOjo.setForeground(BotonDragonBall.CAFE_OSCURO);
        btnOjo.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        final boolean[] visible = {false};
        btnOjo.addActionListener(e -> {
            visible[0] = !visible[0];
            if (visible[0]) {
                campo.setEchoChar((char) 0);
                btnOjo.setText("🙈");
            } else {
                campo.setEchoChar('•');
                btnOjo.setText("👁");
            }
        });

        destino.add(etiqueta);
        destino.add(campo);
        destino.add(btnOjo);
    }

    private static JButton crearBoton(String texto, int x, int y, int ancho, Runnable accion) {
        JButton boton = new BotonDragonBall(texto);
        boton.setBounds(x, y, ancho, 38);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    static Image cargarImagen(String nombre) {
        URL url = Login.class.getResource("/" + nombre);
        if (url != null) return new ImageIcon(url).getImage();
        for (String ruta : new String[]{nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre}) {
            if (new File(ruta).exists()) return new ImageIcon(ruta).getImage();
        }
        return null;
    }

    private static final Color CREMA = new Color(255, 246, 205);

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

    private static void estilizarCampo(JTextField campo) {
        campo.setOpaque(false);
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

    static class CampoTexto extends JTextField {
        CampoTexto() { estilizarCampo(this); }
        @Override protected void paintComponent(Graphics g) { pintarCampo(g, this); super.paintComponent(g); }
    }

    static class CampoClave extends JPasswordField {
        CampoClave() { estilizarCampo(this); }
        @Override protected void paintComponent(Graphics g) { pintarCampo(g, this); super.paintComponent(g); }
    }

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
            int x = 3;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(Color.BLACK);
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    if (dx != 0 || dy != 0) g2.drawString(getText(), x + dx, y + dy);

            g2.setColor(BotonDragonBall.AMARILLO);
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

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

    // ------------------------------------------------------------------
    // API para el controlador (ControladorAcceso)
    // ------------------------------------------------------------------

    public String getUsuario() { return txtUsuario.getText().trim(); }
    public String getClave() { return new String(txtPassword.getPassword()); }
    public JButton getBtnIniciar() { return btnIniciar; }

    public void setUsuario(String usuario) { txtUsuario.setText(usuario); }
    public void limpiarClave() { txtPassword.setText(""); txtPassword.requestFocusInWindow(); }

    public void limpiar() {
        txtUsuario.setText("");
        txtPassword.setText("");
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo iniciar sesión", JOptionPane.WARNING_MESSAGE);
    }

    /** Login correcto: Main decide a qué pantalla se va. */
    public void entrar() {
        if (alIniciarSesion != null) alIniciarSesion.run();
    }

    public void setAlIniciarSesion(Runnable accion) { this.alIniciarSesion = accion; }
    public void setAlIrRegistro(Runnable accion) { this.alIrRegistro = accion; }
    public void setAlLoginFacial(Runnable accion) { this.alLoginFacial = accion; }
}