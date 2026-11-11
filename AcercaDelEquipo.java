import java.awt.*;
import javax.swing.*;

/**
 * Ventana "Acerca del equipo" (About Team) con el estilo Dragon Ball.
 * Se abre desde el botón "Acerca del equipo" de VentanaAjustes, así que está disponible
 * en el Login, en el Registro y en el Menú principal.
 *
 * Para cambiar los datos, edita solo las constantes de aquí abajo.
 */
public class AcercaDelEquipo extends JDialog {

    // ---------- DATOS A EDITAR ----------
    private static final String PROYECTO = "Memory Game - Dragon Ball";
    private static final String DESCRIPCION =
            "Juego de memoria con modo Clásico (matriz 6x6) y modo Patrones (matriz 5x5), "
            + "con registro de usuarios y acceso por reconocimiento facial.";
    private static final String[] INTEGRANTES = {
            "Eithan Campos",
            "Eidan Alvarado",
            "Julian Gómez",
            "Eithan Mora"
    };
    private static final String PIE = "Universidad de Costa Rica  -  2026";
    // ------------------------------------

    private static final Color FONDO = new Color(26, 16, 40);
    private static final Color DORADO = new Color(255, 215, 0);

    public AcercaDelEquipo(Window padre) {
        super(padre, "Acerca del equipo", ModalityType.APPLICATION_MODAL);
        setResizable(false);

        JComponent contenido = (JComponent) getContentPane();
        contenido.setLayout(null);
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createLineBorder(new Color(255, 200, 0), 3));

        int ancho = 440;
        int y = 20;

        JLabel lblTitulo = new JLabel("ACERCA DEL EQUIPO", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 26));
        lblTitulo.setForeground(DORADO);
        lblTitulo.setBounds(0, y, ancho, 40);
        contenido.add(lblTitulo);

        y += 55;
        JLabel lblProyecto = new JLabel(PROYECTO, SwingConstants.CENTER);
        lblProyecto.setFont(new Font("Arial", Font.BOLD, 18));
        lblProyecto.setForeground(Color.WHITE);
        lblProyecto.setBounds(0, y, ancho, 28);
        contenido.add(lblProyecto);

        y += 32;
        JLabel lblDescripcion = new JLabel(
                "<html><div style='text-align:center; width:350px'>" + DESCRIPCION + "</div></html>",
                SwingConstants.CENTER);
        lblDescripcion.setFont(new Font("Arial", Font.PLAIN, 13));
        lblDescripcion.setForeground(new Color(220, 220, 220));
        lblDescripcion.setBounds(0, y, ancho, 60);
        contenido.add(lblDescripcion);

        y += 75;
        JLabel lblIntegrantes = new JLabel("Integrantes", SwingConstants.CENTER);
        lblIntegrantes.setFont(new Font("Arial", Font.BOLD, 16));
        lblIntegrantes.setForeground(BotonDragonBall.NARANJA);
        lblIntegrantes.setBounds(0, y, ancho, 26);
        contenido.add(lblIntegrantes);

        y += 30;
        for (String nombre : INTEGRANTES) {
            JLabel lblNombre = new JLabel(nombre, SwingConstants.CENTER);
            lblNombre.setFont(new Font("Arial", Font.BOLD, 16));
            lblNombre.setForeground(BotonDragonBall.AMARILLO);
            lblNombre.setBounds(0, y, ancho, 26);
            contenido.add(lblNombre);
            y += 28;
        }

        y += 10;
        JLabel lblPie = new JLabel(PIE, SwingConstants.CENTER);
        lblPie.setFont(new Font("Arial", Font.ITALIC, 12));
        lblPie.setForeground(new Color(180, 180, 180));
        lblPie.setBounds(0, y, ancho, 20);
        contenido.add(lblPie);

        y += 35;
        BotonDragonBall btnVolver = new BotonDragonBall("Volver");
        btnVolver.setBounds((ancho - 150) / 2, y, 150, 40);
        btnVolver.addActionListener(e -> dispose());
        contenido.add(btnVolver);

        y += 40 + 25;
        contenido.setPreferredSize(new Dimension(ancho, y));

        pack();
        setLocationRelativeTo(padre);
    }
}