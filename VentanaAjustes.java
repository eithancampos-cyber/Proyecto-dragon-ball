import java.awt.*;
import javax.swing.*;

public class VentanaAjustes extends JDialog {

    private final BotonDragonBall btnMusica;
    private final BotonDragonBall btnAcerca;
    private final BotonDragonBall btnCerrar;
    private BotonDragonBall btnCerrarSesion; // solo existe cuando se muestra la opción (menú principal)
    private boolean musicaActiva;
    private boolean cerrarSesionSolicitado = false; // Bandera para saber si dio clic en cerrar sesión

    /** Ajustes del menú principal: música + acerca del equipo + cerrar sesión. */
    public VentanaAjustes(JFrame padre, boolean musicaActiva) {
        this(padre, musicaActiva, true);
    }

    /**
     * @param mostrarCerrarSesion true en el menú principal; false en Login y Registro
     *                            (ahí todavía no hay una sesión que cerrar).
     */
    public VentanaAjustes(JFrame padre, boolean musicaActiva, boolean mostrarCerrarSesion) {
        super(padre, "Ajustes", true);
        this.musicaActiva = musicaActiva;

        setResizable(false);

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

        int y = 90;

        // Opción de música
        JLabel lblMusica = new JLabel("Música de fondo");
        lblMusica.setFont(new Font("Arial", Font.BOLD, 16));
        lblMusica.setForeground(Color.WHITE);
        lblMusica.setBounds(40, y, 170, 40);
        contenido.add(lblMusica);

        btnMusica = new BotonDragonBall("");
        btnMusica.setBounds(220, y, 140, 40);
        btnMusica.addActionListener(e -> {
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
        btnAcerca.addActionListener(e -> new AcercaDelEquipo(this).setVisible(true));
        contenido.add(btnAcerca);

        // Botón de Cerrar Sesión (solo en el menú principal)
        if (mostrarCerrarSesion) {
            y += 60;
            btnCerrarSesion = new BotonDragonBall("Cerrar Sesión");
            btnCerrarSesion.setBounds(40, y, 320, 40);
            btnCerrarSesion.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "¿Estás seguro de que deseas cerrar sesión?",
                    "Cerrar Sesión",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
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

        contenido.setPreferredSize(new Dimension(400, y + 40 + 30));
        pack();
        setLocationRelativeTo(padre);
    }

    private void actualizarBotonMusica() {
        btnMusica.setText(musicaActiva ? "Activada" : "Apagada");
        btnMusica.setApagado(!musicaActiva);
    }

    // Para saber en tu clase Main si el usuario decidió cerrar sesión
    public boolean isCerrarSesionSolicitado() {
        return cerrarSesionSolicitado;
    }

    public boolean isMusicaActiva() { return musicaActiva; }
    public JButton getBtnMusica() { return btnMusica; }
    public JButton getBtnCerrar() { return btnCerrar; }
}