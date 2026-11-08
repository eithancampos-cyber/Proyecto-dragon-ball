import java.awt.*;
import javax.swing.*;

public class Main {

    private static final String LOGIN = "LOGIN";
    private static final String REGISTRO = "REGISTRO";
    private static final String MENU = "MENU";
    private static final String MATRICES = "MATRICES";
    private static final String PATRONES = "PATRONES";
    private static final String FACIAL = "FACIAL";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            GestorMusica.iniciar(); 
            APIbccr.precargar();    

            JFrame ventana = new JFrame("Memory Game");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.getContentPane().setPreferredSize(new Dimension(960, 540));
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setResizable(false);

            CardLayout cartas = new CardLayout();
            JPanel contenedor = new JPanel(cartas);

            ModeloUsuarios usuarios = new ModeloUsuarios();

            Login login = new Login();
            Registro registro = new Registro();
            VentanaPrincipal menu = new VentanaPrincipal();
            InterfazMatrices matrices = new InterfazMatrices();
            MatrizPatrones patrones = new MatrizPatrones();
            
            ReconocimientoFacial reconocimientoFacial = new ReconocimientoFacial(usuarios);

            contenedor.add(centrar(login), LOGIN);
            contenedor.add(centrar(registro), REGISTRO);
            contenedor.add(centrar(menu), MENU);
            contenedor.add(centrar(matrices), MATRICES);
            contenedor.add(centrar(patrones), PATRONES);
            contenedor.add(centrar(reconocimientoFacial), FACIAL);

            ControladorAcceso controladorAcceso = new ControladorAcceso(usuarios, login, registro);

            login.setAlIrRegistro(() -> cartas.show(contenedor, REGISTRO));          
            registro.setAlGuardar(() -> cartas.show(contenedor, LOGIN));             
            registro.setAlVolver(() -> cartas.show(contenedor, LOGIN));              
            login.setAlIniciarSesion(() -> cartas.show(contenedor, MENU));          
            
            login.setAlLoginFacial(() -> {
                String usr = login.getUsuario();
                if (controladorAcceso.validarUsuarioParaFacial()) {
                    reconocimientoFacial.configurarModo("LOGIN", usr);
                    cartas.show(contenedor, FACIAL);
                }
            });
            
            registro.setAlCapturarRostro(() -> {
                String usr = registro.getUsuario();
                if (usr == null || usr.trim().isEmpty()) {
                    registro.mostrarError("⚠️ Escribe un nombre de usuario antes de registrar tu rostro.");
                    return;
                }
                reconocimientoFacial.configurarModo("REGISTRO", usr);
                cartas.show(contenedor, FACIAL);
            });

            reconocimientoFacial.setAlFinalizar(() -> {
                reconocimientoFacial.cerrarCamara();
                cartas.show(contenedor, LOGIN);
            });

            reconocimientoFacial.setAlEntrarAlJuego(() -> {
                reconocimientoFacial.cerrarCamara();
                cartas.show(contenedor, MENU);
            });

            menu.setAlModoClasico(() -> cartas.show(contenedor, MATRICES));          
            menu.setAlModoPatrones(() -> cartas.show(contenedor, PATRONES));         
            matrices.setAlVolver(() -> cartas.show(contenedor, MENU));               
            patrones.setAlVolver(() -> cartas.show(contenedor, MENU));               

            menu.setAlCerrarSesion(() -> {
                usuarios.cerrarSesion();
                cartas.show(contenedor, LOGIN);
            });

            ventana.add(contenedor);
            cartas.show(contenedor, LOGIN); 
            ventana.setVisible(true);
        });
    }

    private static JPanel centrar(JPanel panel) {
        JPanel envoltorio = new JPanel(new GridBagLayout()); 
        envoltorio.add(panel);
        return envoltorio;
    }
}