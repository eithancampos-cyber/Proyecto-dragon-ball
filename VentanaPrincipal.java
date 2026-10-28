import java.awt.Dimension;
import java.awt.Font;
import javax.swing.*;

/**
 * Menú principal del Memory Game.
 * Ahora es un JPanel (antes era un JFrame) para mostrarse en la ventana única.
 */
public class VentanaPrincipal extends JPanel {

    private final JButton btnAjustes;
    private final JButton btnMejoresJugadores;
    private final JButton btnSeleccionModo;

    // Acciones de navegación que Main le asigna (una por cada modo de juego)
    private Runnable alModoClasico;   // se ejecuta tras elegir "Modo Clásico"
    private Runnable alModoPatrones;  // se ejecuta tras elegir "Modo Patrones"

    public VentanaPrincipal() {
        setPreferredSize(new Dimension(500, 400)); // tamaño original de la ventana
        setLayout(null);                           // posicionamiento manual

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

        // Título del juego
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
                if (alModoClasico != null) alModoClasico.run(); // Main muestra las matrices 6x6
            } else if (opcion == 1) {
                JOptionPane.showMessageDialog(this, "Aquí iniciaría el modo patrones (1 jugador)");
                if (alModoPatrones != null) alModoPatrones.run(); // Main muestra la matriz 5x5
            }
        });
        add(btnSeleccionModo);
    }

    // Getters para que el controlador pueda asignar los listeners (MVC)
    public JButton getBtnAjustes() { return btnAjustes; }
    public JButton getBtnMejoresJugadores() { return btnMejoresJugadores; }
    public JButton getBtnSeleccionModo() { return btnSeleccionModo; }

    // Setter para que Main conecte la navegación
    public void setAlModoClasico(Runnable accion) { this.alModoClasico = accion; }
    public void setAlModoPatrones(Runnable accion) { this.alModoPatrones = accion; }
}