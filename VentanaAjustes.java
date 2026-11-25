import java.awt.*;
import javax.swing.*;

/**
 * Diálogo modal de Ajustes: activar o apagar la música, abrir 'Acerca del equipo' y (solo en el menú
 * principal) cerrar sesión. No decide nada por sí misma: deja banderas y estado para que quien la abrió
 * actúe.
 */
public class VentanaAjustes extends JDialog {

    // Botones de la ventana. Son atributos para poder exponerlos con getters (estilo MVC del proyecto).
    private final BotonDragonBall btnMusica;
    private final BotonDragonBall btnAcerca;
    private final BotonDragonBall btnCerrar;
    private BotonDragonBall btnCerrarSesion; // solo existe cuando se muestra la opción (menú principal)
    // Copia local del estado de la música; sirve para mostrar el texto del botón (Activada / Apagada).
    private boolean musicaActiva;
    private boolean cerrarSesionSolicitado = false; // Bandera para saber si dio clic en cerrar sesión

    /** Ajustes del menú principal: música + acerca del equipo + cerrar sesión. */
    public VentanaAjustes(JFrame padre, boolean musicaActiva) {
        // Delegación: por defecto sí se muestra el botón de cerrar sesión (caso del menú principal).
        this(padre, musicaActiva, true);
    }

    /**
     * @param mostrarCerrarSesion true en el menú principal; false en Login y Registro
     *                            (ahí todavía no hay una sesión que cerrar).
     */
    public VentanaAjustes(JFrame padre, boolean musicaActiva, boolean mostrarCerrarSesion) {
        // super(padre, título, modal): modal = true bloquea la ventana de atrás hasta que se cierre
        // Ajustes.
        super(padre, "Ajustes", true);
        this.musicaActiva = musicaActiva;

        // Tamaño fijo. El alto se calcula más abajo según cuántos botones haya.
        setResizable(false);

        // Layout nulo (posiciones absolutas con setBounds) y colores del estilo Dragon Ball: fondo
        // morado oscuro y borde dorado.
        JComponent contenido = (JComponent) getContentPane();
        contenido.setLayout(null);
        contenido.setBackground(new Color(26, 16, 40));
        contenido.setBorder(BorderFactory.createLineBorder(new Color(255, 200, 0), 3));

        // Título
        JLabel lblTitulo = new JLabel("AJUSTES", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(255, 215, 0));
        lblTitulo.setBounds(0, 25, 400, 40);
        contenido.add(lblTitulo);

        // 'y' lleva la posición vertical del siguiente elemento; cada botón que se agrega la va
        // aumentando.
        int y = 90;

        // Opción de música
        JLabel lblMusica = new JLabel("Música de fondo");
        lblMusica.setFont(new Font("Arial", Font.BOLD, 16));
        lblMusica.setForeground(Color.WHITE);
        lblMusica.setBounds(40, y, 170, 40);
        contenido.add(lblMusica);

        // Texto vacío a propósito: actualizarBotonMusica() le pone 'Activada' o 'Apagada'.
        btnMusica = new BotonDragonBall("");
        btnMusica.setBounds(220, y, 140, 40);
        btnMusica.addActionListener(e -> {
            // Invierte el estado local y lo aplica de verdad en GestorMusica (static), que es quien
            // prende o apaga el audio.
            this.musicaActiva = !this.musicaActiva;
            GestorMusica.setActiva(this.musicaActiva); // enciende o apaga el audio
            actualizarBotonMusica();
        });
        contenido.add(btnMusica);
        actualizarBotonMusica();

        // Botón Acerca del equipo (About Team)
        y += 60;
        btnAcerca = new BotonDragonBall("Acerca del equipo");
        btnAcerca.setBounds(40, y, 320, 40);
        // Abre otro diálogo con 'this' como padre, para que aparezca encima de Ajustes.
        btnAcerca.addActionListener(e -> new AcercaDelEquipo(this).setVisible(true));
        contenido.add(btnAcerca);

        // Botón de Cerrar Sesión (solo en el menú principal)
        // En Login y Registro todavía no hay sesión que cerrar, por eso allí este botón no se crea
        // (btnCerrarSesion queda en null).
        if (mostrarCerrarSesion) {
            y += 60;
            btnCerrarSesion = new BotonDragonBall("Cerrar Sesión");
            btnCerrarSesion.setBounds(40, y, 320, 40);
            btnCerrarSesion.addActionListener(e -> {
                // Pide confirmación antes de cerrar sesión; YES_NO_OPTION muestra los botones Sí / No.
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "¿Estás seguro de que deseas cerrar sesión?",
                    "Cerrar Sesión",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    // Aquí no se cierra la sesión: solo se deja la bandera. Quien abrió Ajustes
                    // (VentanaPrincipal) la consulta y ejecuta la acción.
                    cerrarSesionSolicitado = true;
                    dispose(); // Cierra la ventana de ajustes
                }
            });
            contenido.add(btnCerrarSesion);
        }

        // Botón Cerrar Ventana
        y += 70;
        btnCerrar = new BotonDragonBall("Volver");
        btnCerrar.setBounds(125, y, 150, 40);
        btnCerrar.addActionListener(e -> dispose());
        contenido.add(btnCerrar);

        // Alto total = posición del último botón + su altura + margen inferior. pack() ajusta la ventana
        // a ese tamaño.
        contenido.setPreferredSize(new Dimension(400, y + 40 + 30));
        pack();
        // Centra el diálogo respecto a la ventana padre.
        setLocationRelativeTo(padre);
    }

    /** Refresca el texto y la apariencia del botón de música según el estado actual. */
    private void actualizarBotonMusica() {
        btnMusica.setText(musicaActiva ? "Activada" : "Apagada");
        // setApagado(true) pone el botón en gris (método de BotonDragonBall).
        btnMusica.setApagado(!musicaActiva);
    }

    // Para saber en tu clase Main si el usuario decidió cerrar sesión
    public boolean isCerrarSesionSolicitado() {
        return cerrarSesionSolicitado;
    }

    // Getters (MVC).
    public boolean isMusicaActiva() { return musicaActiva; }
    public JButton getBtnMusica() { return btnMusica; }
    public JButton getBtnCerrar() { return btnCerrar; }
}