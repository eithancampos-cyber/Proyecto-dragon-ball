import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Interfaz gráfica del Modo Patrones: solo MUESTRA una matriz de 5x5.
 * Cada celda es un JLabel con borde (no editable), igual que en InterfazMatrices.
 * Es un JPanel para mostrarse dentro de la ventana única que crea Main.
 */
public class MatrizPatrones extends JPanel {

    // Una etiqueta por cada celda de la matriz 5x5
    private JLabel[][] matriz5 = new JLabel[5][5];

    // Acción de navegación que Main le asigna (volver al menú)
    private Runnable alVolver;

    public MatrizPatrones() {
        setLayout(new BorderLayout());

        // Panel con margen que contiene la matriz
        JPanel principal = new JPanel(new BorderLayout());
        principal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        principal.add(crearPanelMatriz(matriz5, "Matriz 5x5 - Modo Patrones"), BorderLayout.CENTER);
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
