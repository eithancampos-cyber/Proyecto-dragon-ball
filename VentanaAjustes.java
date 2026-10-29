import java.awt.*;
import javax.swing.*;

public class VentanaAjustes extends JDialog {

    private final BotonDragonBall btnMusica;
    private final BotonDragonBall btnCerrar;
    private boolean musicaActiva;

    public VentanaAjustes(JFrame padre, boolean musicaActiva) {
        super(padre, "Ajustes", true);
        this.musicaActiva = musicaActiva;

        setResizable(false);

        JComponent contenido = (JComponent) getContentPane();
        contenido.setLayout(null);
        contenido.setBackground(new Color(26, 16, 40));
        contenido.setBorder(BorderFactory.createLineBorder(new Color(255, 200, 0), 3));
        contenido.setPreferredSize(new Dimension(400, 240));

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
        lblMusica.setBounds(40, 100, 170, 40);
        contenido.add(lblMusica);

        btnMusica = new BotonDragonBall("");
        btnMusica.setBounds(220, 100, 140, 40);
        btnMusica.addActionListener(e -> {
            this.musicaActiva = !this.musicaActiva;
            actualizarBotonMusica();
            // Aquí después se activará o pausará la música real
        });
        contenido.add(btnMusica);
        actualizarBotonMusica();

        // Cerrar
        btnCerrar = new BotonDragonBall("Cerrar");
        btnCerrar.setBounds(125, 170, 150, 40);
        btnCerrar.addActionListener(e -> dispose());
        contenido.add(btnCerrar);

        pack();
        setLocationRelativeTo(padre);
    }

    private void actualizarBotonMusica() {
        btnMusica.setText(musicaActiva ? "Activada" : "Apagada");
        btnMusica.setApagado(!musicaActiva);
    }

    // Getters (MVC)
    public boolean isMusicaActiva() { return musicaActiva; }
    public JButton getBtnMusica() { return btnMusica; }
    public JButton getBtnCerrar() { return btnCerrar; }
}