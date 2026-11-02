import java.awt.*;
import javax.swing.*;

public class VentanaAjustes extends JDialog {

    private final BotonDragonBall btnMusica;
    private final BotonDragonBall btnCerrar;
    private final BotonDragonBall btnCerrarSesion; // Nuevo botón para cerrar sesión
    private boolean musicaActiva;
    private boolean cerrarSesionSolicitado = false; // Bandera para saber si dio clic en cerrar sesión

    public VentanaAjustes(JFrame padre, boolean musicaActiva) {
        super(padre, "Ajustes", true);
        this.musicaActiva = musicaActiva;

        setResizable(false);

        JComponent contenido = (JComponent) getContentPane();
        contenido.setLayout(null);
        contenido.setBackground(new Color(26, 16, 40));
        contenido.setBorder(BorderFactory.createLineBorder(new Color(255, 200, 0), 3));
        contenido.setPreferredSize(new Dimension(400, 310)); // Un poco más alto para que quepan los botones

        // Título
        JLabel lblTitulo = new JLabel("AJUSTES", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(255, 215, 0));
        lblTitulo.setBounds(0, 25, 400, 40);
        contenido.add(lblTitulo);

        // Opción de música
        JLabel lblMusica = new JLabel("Música de fondo");
        lblMusica.setFont(new Font("Arial", Font.BOLD, 16));
        lblMusica.setForeground(Color.WHITE);
        lblMusica.setBounds(40, 90, 170, 40);
        contenido.add(lblMusica);

        btnMusica = new BotonDragonBall("");
        btnMusica.setBounds(220, 90, 140, 40);
        btnMusica.addActionListener(e -> {
            this.musicaActiva = !this.musicaActiva;
            actualizarBotonMusica();
        });
        contenido.add(btnMusica);
        actualizarBotonMusica();

        // Botón de Cerrar Sesión
        btnCerrarSesion = new BotonDragonBall("Cerrar Sesión");
        btnCerrarSesion.setBounds(40, 150, 320, 40);
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

        // Botón Cerrar Ventana
        btnCerrar = new BotonDragonBall("Volver");
        btnCerrar.setBounds(125, 220, 150, 40);
        btnCerrar.addActionListener(e -> dispose());
        contenido.add(btnCerrar);

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