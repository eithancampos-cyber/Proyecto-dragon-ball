import java.awt.*;
import javax.swing.*;

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
    private static final String MATRICES = "MATRICES";    // Modo Clásico (6x6)
    private static final String PATRONES = "PATRONES";    // Modo Patrones (4x4)
    private static final String FACIAL = "FACIAL";          // Pantalla única reutilizable para la Cámara

    public static void main(String[] args) {
        // Swing debe ejecutarse en el hilo de eventos (EDT)
        SwingUtilities.invokeLater(() -> {

            GestorMusica.iniciar(); // arranca la música de fondo (musica.wav)
            APIbccr.precargar();    // consulta el tipo de cambio del BCCR en segundo plano

            // --- Ventana única ---
            JFrame ventana = new JFrame("Memory Game");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Tamaño inicial: 960x540, el mismo tamaño de las pantallas con fondo.
            ventana.getContentPane().setPreferredSize(new Dimension(960, 540));
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setMinimumSize(new Dimension(640, 360)); // tamaño mínimo al achicar
            ventana.setResizable(true);                       // se puede agrandar/maximizar a voluntad

            // --- Contenedor que muestra una pantalla a la vez ---
            CardLayout cartas = new CardLayout();
            JPanel contenedor = new JPanel(cartas);

            // Instancia de todas las pantallas base
            Login login = new Login();
            Registro registro = new Registro();
            VentanaPrincipal menu = new VentanaPrincipal();
            InterfazMatrices matrices = new InterfazMatrices();
            MatrizPatrones patrones = new MatrizPatrones(); // matriz 5x5 del Modo Patrones

            // Instancia ÚNICA para el Reconocimiento Facial (se adapta para Login o Registro)
            ReconocimientoFacial reconocimientoFacial = new ReconocimientoFacial();

            // Se agregan escaladas: cada pantalla se estira para llenar la ventana
            contenedor.add(centrar(login), LOGIN);
            contenedor.add(centrar(registro), REGISTRO);
            contenedor.add(centrar(menu), MENU);
            contenedor.add(centrar(matrices), MATRICES);
            contenedor.add(centrar(patrones), PATRONES);
            contenedor.add(centrar(reconocimientoFacial), FACIAL);

            // --- Registro y login con sus validaciones (modelo + controlador) ---
            ModeloUsuarios usuarios = new ModeloUsuarios();
            ModeloRostros rostros = new ModeloRostros("rostros.txt"); // rostro de cada usuario (aparte de usuarios.txt)
            ControladorAcceso controladorAcceso = new ControladorAcceso(usuarios, rostros, login, registro, reconocimientoFacial);

            // --- Navegación entre pantallas ---
            login.setAlIrRegistro(() -> cartas.show(contenedor, REGISTRO));          // Login -> Registro
            registro.setAlGuardar(() -> cartas.show(contenedor, LOGIN));             // Registro -> Login (tras guardar)
            registro.setAlVolver(() -> {                                             // Registro -> Login sin guardar
                controladorAcceso.descartarRostroPendiente();                        // el rostro capturado no se guarda
                cartas.show(contenedor, LOGIN);
            });
            login.setAlIniciarSesion(() -> cartas.show(contenedor, MENU));          // Login -> Menú

            // --- Navegación de Reconocimiento Facial (Con validación de usuario registrado) ---
            login.setAlLoginFacial(() -> {
                // Valida a través del controlador si el usuario existe antes de abrir la cámara
                if (controladorAcceso.validarUsuarioParaFacial()) {
                    reconocimientoFacial.configurarModo("LOGIN");
                    cartas.show(contenedor, FACIAL);
                }
            });

            registro.setAlCapturarRostro(() -> {
                // el rostro se asocia al nombre escrito: tiene que haber un usuario nuevo antes de abrir la cámara
                if (controladorAcceso.validarUsuarioParaRegistroFacial()) {
                    reconocimientoFacial.configurarModo("REGISTRO");
                    cartas.show(contenedor, FACIAL);
                }
            });

            // Rostro capturado en el Registro: vuelve al Registro (con el formulario intacto) para pulsar "Guardar"
            reconocimientoFacial.setAlRegistroCompletado(() -> cartas.show(contenedor, REGISTRO));

            // Login Facial exitoso (el rostro coincide con el usuario): entra directo al juego (Menú principal)
            reconocimientoFacial.setAlLoginCompletado(() -> cartas.show(contenedor, MENU));

            // Botón "Volver" de la cámara (o demasiados intentos): regresa a la pantalla de donde vino
            reconocimientoFacial.setAlVolver(() ->
                    cartas.show(contenedor, "REGISTRO".equals(reconocimientoFacial.getModo()) ? REGISTRO : LOGIN));

            // Navegación de los modos de juego
            menu.setAlModoClasico(() -> cartas.show(contenedor, MATRICES));          // Menú -> Matrices (clásico)
            menu.setAlModoPatrones(() -> cartas.show(contenedor, PATRONES));         // Menú -> Matriz 5x5 (patrones)
            matrices.setAlVolver(() -> cartas.show(contenedor, MENU));               // Matrices -> Menú
            patrones.setAlVolver(() -> cartas.show(contenedor, MENU));               // Matriz 5x5 -> Menú

            // Conexión para que el botón de Cerrar Sesión en Ajustes devuelva al Login
            menu.setAlCerrarSesion(() -> {
                usuarios.cerrarSesion();
                cartas.show(contenedor, LOGIN);
            });

            // --- Mostrar ---
            ventana.add(contenedor);
            cartas.show(contenedor, LOGIN); // la primera pantalla es el Login
            ventana.setVisible(true);
        });
    }

    /** Envuelve un panel en un PanelEscalable: se ve igual en cualquier tamaño de ventana. */
    private static JPanel centrar(JPanel panel) {
        // PanelEscalable estira la pantalla (960x540) para llenar la ventana, manteniendo la proporción
        return new PanelEscalable(panel, 960, 540);
    }
}