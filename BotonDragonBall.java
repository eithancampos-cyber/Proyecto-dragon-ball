import java.awt.*;
import javax.swing.*;

// Botón con el estilo de la imagen: amarillo/naranja, borde negro, rojo al pasar el mouse
public class BotonDragonBall extends JButton {

    // Paleta del estilo Dragon Ball: la reutilizan los demás componentes (Login, etc.)
    public static final Color AMARILLO = new Color(255, 228, 50);
    public static final Color NARANJA = new Color(255, 150, 0);
    public static final Color CAFE_OSCURO = new Color(60, 10, 0);

    private boolean apagado = false;

    public BotonDragonBall(String texto) {
        super(texto);
        setFont(new Font("Arial", Font.BOLD, 15));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // Estado visual "apagado" (gris), útil para opciones que se activan y desactivan
    public void setApagado(boolean apagado) {
        this.apagado = apagado;
        repaint();
    }

    public boolean isApagado() {
        return apagado;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color arriba;
        Color abajo;
        Color colorTexto;
        if (getModel().isPressed()) {
            arriba = new Color(160, 0, 0);
            abajo = new Color(110, 0, 0);
            colorTexto = Color.WHITE;
        } else if (getModel().isRollover()) {
            arriba = new Color(255, 70, 40);
            abajo = new Color(190, 0, 0);
            colorTexto = Color.WHITE;
        } else if (apagado) {
            arriba = new Color(160, 160, 160);
            abajo = new Color(90, 90, 90);
            colorTexto = new Color(235, 235, 235);
        } else {
            arriba = AMARILLO;
            abajo = NARANJA;
            colorTexto = CAFE_OSCURO;
        }

        // Relleno con degradado
        g2.setPaint(new GradientPaint(0, 0, arriba, 0, h, abajo));
        g2.fillRoundRect(0, 0, w - 1, h - 1, 26, 26);

        // Borde negro grueso
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, 26, 26);

        // Texto centrado
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(colorTexto);
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}