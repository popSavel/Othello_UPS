import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QueueWindow extends JFrame {

    JLabel nick;

    JPanel panel;

    JButton button;

    JLabel waiting;

    public QueueWindow(SocketManager soc, String nickname) {
        setSize(new Dimension(400, 200));
        setLocationRelativeTo(null);
        setTitle("KIV/UPS - Othello");
        setResizable(false);

        panel = new JPanel();
        panel.setLayout(null);

        nick = new JLabel();
        nick.setBounds(125, 10, 125, 40);
        panel.add(nick);

        waiting = new JLabel("--Waiting for game--");
        waiting.setBounds(125, 60, 200, 40);
        waiting.setVisible(false);
        waiting.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        panel.add(waiting);

        button = new JButton("Join game");
        button.setBounds(125, 60, 125, 40);
        button.setBackground(Color.BLUE);
        button.setForeground(Color.WHITE);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.setVisible(false);
                waiting.setVisible(true);
                repaint();
                soc.joinGame();
            }
        });
        panel.add(button);

        this.getContentPane().add(panel, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(false);
    }

    public void setNick(String nickname) {
        nick.setText("User: " + nickname);
    }
}
