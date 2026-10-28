import javax.swing.*;
import java.awt.*;

/**
 * Clase principal: crea la ÚNICA ventana del programa, instancia todas las
 * pantallas (cada una en su propia clase) y define cómo se navega entre ellas.
 * Flujo: Login -> (Registro) -> Menú principal -> Matrices (clásico) o Matriz 5x5 (patrones) -> Menú principal
 */
public class Main {

    // Nombres de cada "tarjeta" del CardLayout
    private static final String LOGIN = "LOGIN";
    private static final String REGISTRO = "REGISTRO";
    private static final String MENU = "MENU";
    private static final String MATRICES = "MATRICES";   // Modo Clásico (6x6)
    private static final String PATRONES = "PATRONES";   // Modo Patrones (5x5)

    public static void main(String[] args) {
        // Swing debe ejecutarse en el hilo de eventos (EDT)
        SwingUtilities.invokeLater(() -> {

            // --- Ventana única ---
            JFrame ventana = new JFrame("Memory Game");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setSize(700, 480);
            ventana.setLocationRelativeTo(null);
            ventana.setResizable(false);

            // --- Contenedor que muestra una pantalla a la vez ---
            CardLayout cartas = new CardLayout();
            JPanel contenedor = new JPanel(cartas);

            // --- Pantallas (cada una vive en su clase) ---
            Login login = new Login();
            Registro registro = new Registro();
            VentanaPrincipal menu = new VentanaPrincipal();
            InterfazMatrices matrices = new InterfazMatrices();
            MatrizPatrones patrones = new MatrizPatrones(); // matriz 5x5 del Modo Patrones

            // Se agregan centradas para que se vean bien dentro de la ventana
            contenedor.add(centrar(login), LOGIN);
            contenedor.add(centrar(registro), REGISTRO);
            contenedor.add(centrar(menu), MENU);
            contenedor.add(centrar(matrices), MATRICES);
            contenedor.add(centrar(patrones), PATRONES);

            // --- Navegación entre pantallas ---
            login.setAlIrRegistro(() -> cartas.show(contenedor, REGISTRO));          // Login -> Registro
            registro.setAlGuardar(() -> cartas.show(contenedor, LOGIN));             // Registro -> Login
            login.setAlIniciarSesion(() -> cartas.show(contenedor, MENU));           // Login -> Menú
            menu.setAlModoClasico(() -> cartas.show(contenedor, MATRICES));          // Menú -> Matrices (clásico)
            menu.setAlModoPatrones(() -> cartas.show(contenedor, PATRONES));         // Menú -> Matriz 5x5 (patrones)
            matrices.setAlVolver(() -> cartas.show(contenedor, MENU));               // Matrices -> Menú
            patrones.setAlVolver(() -> cartas.show(contenedor, MENU));               // Matriz 5x5 -> Menú

            // --- Mostrar ---
            ventana.add(contenedor);
            cartas.show(contenedor, LOGIN); // la primera pantalla es el Login
            ventana.setVisible(true);
        });
    }

    /** Envuelve un panel en otro que lo mantiene centrado y con su tamaño original. */
    private static JPanel centrar(JPanel panel) {
        JPanel envoltorio = new JPanel(new GridBagLayout()); // GridBagLayout centra por defecto
        envoltorio.add(panel);
        return envoltorio;
    }
}