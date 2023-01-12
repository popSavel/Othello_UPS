import javax.swing.*;
import java.awt.*;

public class ServerAlert extends JFrame {

    JLabel infoLabel;

    public ServerAlert(){
        setSize(new Dimension(450, 250));
        setLocationRelativeTo(null);
        setTitle("KIV/UPS - Othello");
        setResizable(false);

        String alert = "Server stopped responding, waiting to reconnect";
        infoLabel = new JLabel(alert);
        infoLabel.setFont(new Font("Verdana", Font.BOLD, 15));
        int x = (getWidth() - infoLabel.getFontMetrics(infoLabel.getFont()).stringWidth(alert)) / 2;
        infoLabel.setBounds(x,100, infoLabel.getFontMetrics(infoLabel.getFont()).stringWidth(alert), 30);
        add(infoLabel);

        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(false);
    }
}
