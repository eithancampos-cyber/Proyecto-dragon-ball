import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.net.URL;
import javax.swing.*;

/**
 * VISTA del Login (estilo Dragon Ball, botón para ver la contraseña y acceso facial).
 * Fondo animado: fondoLogin.gif.
 *
 * Esta clase también guarda las piezas visuales que comparten Login y Registro
 * (cargarImagen, pintarFondo, PanelFondo, campos, etiqueta, botón del ojo y crearBoton),
 * para no tenerlas repetidas en cada pantalla.
 */
public class Login extends JPanel {

    private static final int ANCHO = 960, ALTO = 540;
    static final int ALTO_BOTON = 38;
    private static final Color CREMA = new Color(255, 246, 205);

    private Runnable alIniciarSesion, alIrRegistro, alLoginFacial;
    private final CampoTexto txtUsuario = new CampoTexto();
    private final CampoClave txtPassword = new CampoClave();
    private final JButton btnIniciar;

    public Login() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setLayout(null);

        PanelFondo fondo = new PanelFondo(cargarImagen("fondoLogin.gif"));
        fondo.setBounds(0, 0, ANCHO, ALTO);
        fondo.setLayout(null);
        add(fondo);

        int anchoBloque = 360, separacion = 16;
        int izq = 40; // pegado a la izquierda para no tapar a Goku, que está a la derecha del GIF (antes: (ANCHO - anchoBloque) / 2, centrado)
        int y = 165;
        int mitad = (anchoBloque - separacion) / 2;

        agregarFila(fondo, "Usuario:", txtUsuario, izq, y, anchoBloque);
        y += 46;
        agregarFila(fondo, "Contraseña:", txtPassword, izq, y, anchoBloque);

        y += 60;
        // "Iniciar Sesión" no tiene acción aquí: la asigna ControladorAcceso (valida con ModeloUsuarios)
        btnIniciar = crearBoton("Iniciar Sesión", izq, y, mitad);
        fondo.add(btnIniciar);

        JButton btnFacial = crearBoton("Login Facial", izq + mitad + separacion, y, mitad);
        btnFacial.addActionListener(e -> {
            if (alLoginFacial != null) alLoginFacial.run();
        });
        fondo.add(btnFacial);

        JButton btnRegistro = crearBoton("¿No tienes cuenta? Regístrate", izq, y + 48, anchoBloque);
        btnRegistro.addActionListener(e -> {
            limpiar();
            if (alIrRegistro != null) alIrRegistro.run();
        });
        fondo.add(btnRegistro);

        // Enter en el usuario pasa a la contraseña; Enter en la contraseña inicia sesión
        txtUsuario.addActionListener(e -> txtPassword.requestFocusInWindow());
        txtPassword.addActionListener(e -> btnIniciar.doClick());
    }

    /** Una fila "etiqueta + campo"; si el campo es de contraseña, le agrega el botón del ojo. */
    private static void agregarFila(JPanel destino, String texto, JTextField campo, int izq, int y, int anchoBloque) {
        int anchoEtiqueta = 115;
        int xCampo = izq + anchoEtiqueta + 5;
        int anchoCampo = anchoBloque - anchoEtiqueta - 5;

        EtiquetaDragonBall etiqueta = new EtiquetaDragonBall(texto);
        etiqueta.setBounds(izq, y, anchoEtiqueta, 34);
        destino.add(etiqueta);

        if (campo instanceof JPasswordField) {
            int anchoOjo = 35;
            anchoCampo -= anchoOjo + 5;
            JButton ojo = crearBotonOjo((JPasswordField) campo);
            ojo.setBounds(xCampo + anchoCampo + 5, y, anchoOjo, 34);
            destino.add(ojo);
        }
        campo.setBounds(xCampo, y, anchoCampo, 34);
        destino.add(campo);
    }

    /** Botón estándar del juego (Arial 13 en negrita, alto fijo). La acción la agrega quien lo llama. */
    static JButton crearBoton(String texto, int x, int y, int ancho) {
        JButton boton = new BotonDragonBall(texto);
        boton.setBounds(x, y, ancho, ALTO_BOTON);
        boton.setFont(new Font("Arial", Font.BOLD, 13));
        return boton;
    }

    /**
     * Botón del ojo: muestra u oculta la contraseña del campo. Quien lo usa le pone la posición.
     * (Antes este código estaba copiado en Login y en Registro. Además, al ocultar de nuevo se
     * restaura el carácter original del campo en vez de forzar '•'.)
     */
    static JButton crearBotonOjo(JPasswordField campo) {
        JButton btn = new JButton("👁");
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBackground(CREMA);
        btn.setForeground(BotonDragonBall.CAFE_OSCURO);
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        final char caracterOculto = campo.getEchoChar();
        btn.addActionListener(e -> {
            boolean estabaOculta = campo.getEchoChar() != 0;
            campo.setEchoChar(estabaOculta ? (char) 0 : caracterOculto);
            btn.setText(estabaOculta ? "🙈" : "👁");
        });
        return btn;
    }

    // ------------------------------------------------------------------
    // Fondos (compartidos por todas las pantallas)
    // ------------------------------------------------------------------

    /**
     * Carga una imagen del classpath o de la carpeta del proyecto. Sirve para JPG y para GIF.
     * Se usa ImageIcon (y NO ImageIO.read) a propósito: ImageIcon conserva la animación del GIF,
     * mientras que ImageIO.read solo devolvería el primer cuadro.
     */
    static Image cargarImagen(String nombre) {
        URL url = Login.class.getResource("/" + nombre);
        if (url != null) return new ImageIcon(url).getImage();
        for (String ruta : new String[]{nombre, "Proyecto-dragon-ball/" + nombre, "src/" + nombre}) {
            if (new File(ruta).exists()) return new ImageIcon(ruta).getImage();
        }
        // MEJORA: antes fallaba en silencio (solo se veía el color de respaldo); así se sabe qué archivo falta
        System.err.println("No se encontró la imagen de fondo: " + nombre);
        return null;
    }

    /**
     * Dibuja el fondo estirado a todo el componente; si no hay imagen, lo rellena con 'respaldo'.
     * Todos los GIF son 16:9, igual que la ventana de 960x540, así que estirar no los deforma
     * (antes MatrizPatrones calculaba un "cubrir y recortar" en cada cuadro sin necesitarlo).
     *
     * Importante: el componente se pasa como ImageObserver. Eso es lo que avisa a Swing de que
     * llegó un cuadro nuevo del GIF y hay que repintar.
     *
     * El color de 'respaldo' se pinta SIEMPRE antes (cuesta ~0,08 ms): sirve cuando falta la imagen
     * y también al inicio, porque el primer cuadro del GIF tarda un instante en estar listo y sin
     * ese relleno el panel mostraría restos de lo que había antes.
     *
     * MEJORA DE RENDIMIENTO: se quitó RenderingHints.KEY_INTERPOLATION = BILINEAR. Medido con estos
     * GIF a 960x540: ~7 ms por cuadro con bilineal contra ~0,5-2 ms sin ella, y ahora el fondo se
     * repinta varias veces por segundo. Si prefieres más suavidad en vez de velocidad, vuelve a
     * activarla aquí (un solo lugar para todas las pantallas).
     */
    static void pintarFondo(Graphics g, Image imagen, JComponent destino, Color respaldo) {
        g.setColor(respaldo);
        g.fillRect(0, 0, destino.getWidth(), destino.getHeight());
        if (imagen != null) g.drawImage(imagen, 0, 0, destino.getWidth(), destino.getHeight(), destino);
    }

    static class PanelFondo extends JPanel {
        private final Image imagen;
        PanelFondo(Image imagen) { this.imagen = imagen; }

        @Override
        protected void paintComponent(Graphics g) {
            pintarFondo(g, imagen, this, new Color(26, 16, 40));
        }
    }

    // ------------------------------------------------------------------
    // Campos y etiquetas con el estilo del juego
    // ------------------------------------------------------------------

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
        // el borde cambia de color con el foco, por eso hay que repintar al ganarlo y perderlo
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

        /**
         * NOTA (posible mejora, no la cambié para no alterar el aspecto): el contorno negro se
         * hace con 24 drawString por etiqueta, y como el fondo ahora es un GIF, cada cuadro nuevo
         * hace repintar estas etiquetas. Son pocas y cortas, así que casi no se nota, pero si algún
         * día hay lentitud se podría dibujar el texto una sola vez en un BufferedImage y reutilizarlo.
         */
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