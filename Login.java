import javax.swing.*;

public class Login extends JFrame {

    public Login() {
        setTitle("Login - Sistema");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setBounds(30, 25, 80, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(110, 25, 180, 25);
        add(txtUsuario);

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 65, 80, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(110, 65, 180, 25);
        add(txtPassword);

        JButton btnCredenciales = new JButton("Iniciar Sesión");
        btnCredenciales.setBounds(30, 115, 130, 30);
        btnCredenciales.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí validaría las credenciales");
        });
        add(btnCredenciales);

        JButton btnFacial = new JButton("Login Facial");
        btnFacial.setBounds(175, 115, 125, 30);
        btnFacial.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí abriría la cámara para el reconocimiento facial");
        });
        add(btnFacial);

        JButton btnIrRegistro = new JButton("¿No tienes cuenta? Regístrate");
        btnIrRegistro.setBounds(30, 165, 270, 30);
        btnIrRegistro.addActionListener(e -> {
            Registro ventanaRegistro = new Registro();
            ventanaRegistro.setVisible(true);
        });
        add(btnIrRegistro);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Login().setVisible(true);
        });
    }
}