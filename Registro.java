import javax.swing.*;

public class Registro extends JFrame {
    public Registro() {
        setTitle("Registro de Nuevo Usuario");
        setSize(350, 270);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel lblUsuario = new JLabel("Nuevo Usuario:");
        lblUsuario.setBounds(30, 20, 100, 25);
        add(lblUsuario);

        JTextField txtUsuario = new JTextField();
        txtUsuario.setBounds(140, 20, 160, 25);
        add(txtUsuario);

        JLabel lblPassword = new JLabel("Contraseña:");
        lblPassword.setBounds(30, 60, 100, 25);
        add(lblPassword);

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setBounds(140, 60, 160, 25);
        add(txtPassword);

        JButton btnCapturarRostro = new JButton("Registrar Rostro (Cámara)");
        btnCapturarRostro.setBounds(30, 110, 270, 30);
        btnCapturarRostro.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Aquí se abriría la cámara para tomar la foto de referencia");
        });
        add(btnCapturarRostro);

        JButton btnGuardar = new JButton("Guardar Registro");
        btnGuardar.setBounds(30, 160, 270, 30);
        btnGuardar.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "¡Usuario registrado con éxito!");
            dispose(); 
        });
        add(btnGuardar);
    }
}