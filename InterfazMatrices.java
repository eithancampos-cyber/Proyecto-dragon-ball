import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Interfaz gráfica que solo MUESTRA dos matrices (cada una de 6x6 según el código original).
 * Cada celda es un JLabel con borde (no editable y sin botones de juego).
 * Ahora es un JPanel (antes era un JFrame) para mostrarse en la ventana única.
 */
public class InterfazMatrices extends JPanel {

    // Una etiqueta por cada celda de cada matriz
    private JLabel[][] matrizA = new JLabel[6][6];
    private JLabel[][] matrizB = new JLabel[6][6];

    // Acción de navegación que Main le asigna (volver al menú)
    private Runnable alVolver;

    public InterfazMatrices() {
        setLayout(new BorderLayout());

        // Panel principal: 1 fila y 2 columnas (una matriz en cada una)
        JPanel principal = new JPanel(new GridLayout(1, 2, 15, 0));
        principal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        principal.add(crearPanelMatriz(matrizA, "Matriz 6x6"));
        principal.add(crearPanelMatriz(matrizB, "Matriz 6x6"));
        add(principal, BorderLayout.CENTER);

        // Botón de navegación para no quedar atrapado en esta pantalla
        JButton btnVolver = new JButton("Volver al menú");
        btnVolver.addActionListener(e -> {
            if (alVolver != null) alVolver.run(); // Main muestra el menú principal
        });
        JPanel inferior = new JPanel();
        inferior.add(btnVolver);
        add(inferior, BorderLayout.SOUTH);
    }

    /** Crea un panel con una cuadrícula de n x n celdas. */
    private JPanel crearPanelMatriz(JLabel[][] celdas, String titulo) {
        int n = celdas.length;

        // GridLayout(filas, columnas, espacioH, espacioV)
        JPanel panel = new JPanel(new GridLayout(n, n, 3, 3));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), titulo,
                TitledBorder.CENTER, TitledBorder.TOP));

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                JLabel celda = new JLabel("", SwingConstants.CENTER); // texto centrado
                celda.setPreferredSize(new Dimension(45, 35));         // tamaño de cada celda
                celda.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                celdas[i][j] = celda;
                panel.add(celda); // GridLayout las acomoda fila por fila
            }
        }
        return panel;
    }

    // Setter para que Main conecte la navegación
    public void setAlVolver(Runnable accion) { this.alVolver = accion; }
}