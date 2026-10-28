import java.awt.Font;
import javax.swing.*;

public class VentanaPrincipal extends JFrame {

    private final JButton btnAjustes;
    private final JButton btnMejoresJugadores;
    private final JButton btnSeleccionModo;

    public VentanaPrincipal() {
        setTitle("Memory Game - Menú Principal");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(null);

        // Extremo superior izquierdo: Ajustes
        btnAjustes = new JButton("Ajustes");
        btnAjustes.setBounds(15, 15, 110, 30);
        btnAjustes.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se abrirían los ajustes (sonido, etc.)");
        });
        add(btnAjustes);

        // Extremo superior derecho: Mejores jugadores
        btnMejoresJugadores = new JButton("Mejores Jugadores");
        btnMejoresJugadores.setBounds(320, 15, 150, 30);
        btnMejoresJugadores.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se mostraría el top 5 con sus premios");
        });
        add(btnMejoresJugadores);

        // Título
        JLabel lblTitulo = new JLabel("MEMORY GAME", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 30));
        lblTitulo.setBounds(100, 100, 300, 40);
        add(lblTitulo);

        // Centro: Selección de modo
        btnSeleccionModo = new JButton("Selección de Modo");
        btnSeleccionModo.setBounds(150, 190, 200, 50);
        btnSeleccionModo.setFont(new Font("Arial", Font.BOLD, 14));
        btnSeleccionModo.addActionListener(e -> {
            String[] modos = {"Modo Clásico", "Modo Patrones"};
            int opcion = JOptionPane.showOptionDialog(
                    this,
                    "Elige un modo de juego",
                    "Selección de Modo",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, modos, modos[0]);
            if (opcion == 0) {
                JOptionPane.showMessageDialog(this, "Aquí iniciaría el modo clásico (2 jugadores)");
            } else if (opcion == 1) {
                JOptionPane.showMessageDialog(this, "Aquí iniciaría el modo patrones (1 jugador)");
            }
        });
        add(btnSeleccionModo);
    }

    // Getters para que el controlador pueda asignar los listeners (MVC)
    public JButton getBtnAjustes() { return btnAjustes; }
    public JButton getBtnMejoresJugadores() { return btnMejoresJugadores; }
    public JButton getBtnSeleccionModo() { return btnSeleccionModo; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaPrincipal().setVisible(true);
        });
    }
}