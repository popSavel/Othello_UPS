import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EndGame extends JFrame {
    JLabel infoLabel;
    JLabel cgLabel;
    JButton ok;


    public EndGame(SocketManager soc){

        setSize(new Dimension(450, 250));
        setLocationRelativeTo(null);
        setTitle("KIV/UPS - Othello");
        setResizable(false);

        infoLabel = new JLabel();
        cgLabel = new JLabel();
        ok = new JButton("OK");
        ok.setBounds((getWidth() - 35) / 2, 150, 70, 35);
       /* back to lobby button */
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                soc.backToLobby();
            }
        });
        add(infoLabel);
        add(cgLabel);
        add(ok);

        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(false);
    }

    /**
     * print out results of game
     */
    public void printResult(int[] playerStones, int[] oppStones, String name, String opp) {
        int playerScore = 0;
        int oppScore = 0;
        for (int i = 0; i < playerStones.length; i++){
            if(playerStones[i] == 1){
                playerScore++;
            }
            if(oppStones[i] == 1){
                oppScore++;
            }
        }
        String result = name + "  " + playerScore + " : " + oppScore + "  " + opp;
        infoLabel.setText(result);
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 20));
        int x = (getWidth() - infoLabel.getFontMetrics(infoLabel.getFont()).stringWidth(result)) / 2;
        infoLabel.setBounds(x,35, infoLabel.getFontMetrics(infoLabel.getFont()).stringWidth(result), 30);
        String cg;
        if(playerScore > oppScore){
            cg = "Winner: " + name;
        }else if(oppScore > playerScore){
            cg = "Winner: " + opp;
        }else{
            cg = "It's a tie";
        }
        cgLabel.setText(cg);
        cgLabel.setFont(new Font("Verdana", Font.BOLD, 25));
        x = (getWidth() - cgLabel.getFontMetrics(cgLabel.getFont()).stringWidth(cg)) / 2;
        cgLabel.setBounds(x,100, cgLabel.getFontMetrics(cgLabel.getFont()).stringWidth(cg), 30);
    }
}
