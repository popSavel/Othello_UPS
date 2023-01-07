import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginWindow extends JFrame{

    JPanel panel;
    JLabel wrongLabel;
    JTextField textArea;
    JLabel loginLabel;
    JButton button;

    public LoginWindow(SocketManager soc){

        setSize(new Dimension(400, 200));
        setLocationRelativeTo(null);
        setTitle("KIV/UPS - Othello");
        setResizable(false);

        panel = new JPanel();
        panel.setLayout(null);

        loginLabel = new JLabel("Insert login");
        loginLabel.setBounds(100, 8, 120, 20);
        panel.add(loginLabel);

        textArea = new JTextField();
        textArea.setBounds(100, 35, 190, 28);
        panel.add(textArea);

        button = new JButton("Login");
        button.setBounds(150, 75, 90, 25);
        button.setBackground(Color.BLUE);
        button.setForeground(Color.WHITE);

        /**
         * okno se samo zavre po potvrzeni loginu
         */
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = textArea.getText();
                if(!(name.length() < 1)){
                    if(name.length() > 9){
                        if(wrongLabel.isVisible() != true){
                            wrongLabel.setVisible(true);
                            repaint();
                        }
                    }else{
                        soc.sendLogin(name);
                        setVisible(false);
                        repaint();
                    }
                }
            }
        });
        panel.add(button);

        wrongLabel = new JLabel("Max 9 znaků!!");
        wrongLabel.setForeground(Color.RED);
        wrongLabel.setBounds(100, 110, 90, 20);
        wrongLabel.setVisible(false);
        panel.add(wrongLabel);

        panel.setBorder(new EmptyBorder(10,10,10,10));
        panel.add(wrongLabel);
        this.getContentPane().add(panel, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
}
