import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

public class GameWindow extends JFrame {

    final int CELL_SIZE = 80;
    final int BOARD_SIZE = 8;

    final int NON_TURN = 45689745;

    final String VERTICAL = "vertical";
    final String HORIZONTAL = "horizontal";
    final String DIAGONAL_DOWN = "diag_down";
    final String DIAGONAL_UP = "diag_up";

    JPanel panel;
    JLabel turnLabel;
    JLabel playerLabel;
    JLabel skipLabel;
    JLabel oppLabel;
    JButton skipTurn;

    SocketManager soc;

    boolean oppConnected = true;

    Rectangle2D cells [] = new Rectangle2D[BOARD_SIZE * BOARD_SIZE];

    int playerStones[] = new int[64];
    int oppStones[] = new int[64];
    Color color;
    Color oppColor;
    boolean onTurn;

    public GameWindow(SocketManager soc){
        onTurn = false;
        this.soc = soc;
        panel = new JPanel(){
            @Override
            public void paint(Graphics g){
                Graphics2D g2 = (Graphics2D) g.create();
                for (int i = 0; i < BOARD_SIZE; i++){
                    for (int j = 0; j  < BOARD_SIZE; j++){
                        if((i + j) % 2 == 0){
                            g2.setColor(Color.WHITE);
                        }else{
                            g2.setColor(Color.LIGHT_GRAY);
                        }
                        Rectangle2D rec = new Rectangle(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        cells[(i * BOARD_SIZE) + j] = rec;
                        g2.fill(rec);
                    }
                }
                g2.setColor(color);
                for(int i = 0; i < playerStones.length; i++){
                    if(playerStones[i] == 1){
                        g2.fillOval((int)cells[i].getX() + 10, (int)cells[i].getY() + 10, 60, 60);
                    }
                }

                g2.setColor(oppColor);
                for(int i = 0; i < oppStones.length; i++){
                    if(oppStones[i] == 1){
                        g2.fillOval((int)cells[i].getX() + 10, (int)cells[i].getY() + 10, 60, 60);
                    }
                }

                /* ztmavi okno pri vykreseleni pokud se odpoji oponent  */
                if(!oppConnected){
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.fillRect(0,0,getWidth(), getHeight());
                    g2.setColor(new Color(0, 0, 0));
                    String str = "Opponent disconnected, waiting to reconnect";
                    int x = (getWidth() - g2.getFontMetrics().stringWidth(str)) / 2;
                    g2.drawString(str, x, getHeight() / 2);
                }
            }

        };

        for (int i = 0; i < BOARD_SIZE; i++){
            for(int j = 0; j < BOARD_SIZE; j++){
                Rectangle2D rec = new Rectangle(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                cells[(i * BOARD_SIZE) + j] = rec;
            }
        }

        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(CELL_SIZE * 8, CELL_SIZE * 8));
        panel.setBounds(30, 50, CELL_SIZE * 8, CELL_SIZE * 8);
        panel.setFont(new Font("Verdana", Font.BOLD, 20));

        turnLabel = new JLabel();
        turnLabel.setBounds(270, 720, 200, 30);
        turnLabel.setFont(new Font("Verdana", Font.BOLD, 20));

        playerLabel = new JLabel();
        playerLabel.setFont(new Font("Verdana", Font.BOLD, 20));

        oppLabel = new JLabel();
        oppLabel.setFont(new Font("Verdana", Font.BOLD, 20));
        oppLabel.setForeground(Color.BLACK);

        skipLabel = new JLabel("When you can play, you must play!");
        skipLabel.setFont(new Font("Verdana", Font.BOLD, 12));
        skipLabel.setForeground(Color.RED);
        skipLabel.setBounds(450, 700, skipLabel.getFontMetrics(skipLabel.getFont()).stringWidth(skipLabel.getText()), 30);
        skipLabel.setVisible(false);

        skipTurn = new JButton("Skip");
        skipTurn.setBounds(530, 730, 100, 30);
        skipTurn.setBackground(Color.PINK);
        skipTurn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(onTurn){
                    if(canPlay()){
                        skipLabel.setVisible(true);
                    }else{
                        onTurn = false;
                        setTurn();
                        soc.sendTurn(NON_TURN);
                    }
                }
            }
        });

        add(panel);
        add(turnLabel);
        add(playerLabel);
        add(oppLabel);
        add(skipTurn);
        add(skipLabel);
        setSize(720, 820);
        setLayout(null);
        setLocationRelativeTo(null);
        setTitle("KIV/UPS - Othello");
        setResizable(false);
        getContentPane().setBackground(Color.darkGray);
        setVisible(false);

        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(onTurn && oppConnected){
                    int x = (e.getX() - 38) / 80;
                    int y = (e.getY() - 80) / 80;
                    int index = y * 8 + x;
                    if(oppStones[index] != 1 && playerStones[index] != 1) {
                        if(isOutFlanking(index, true)) {
                            Rectangle2D rec = cells[index];
                            Graphics g = panel.getGraphics();
                            g.setColor(color);
                            g.fillOval((int)rec.getX() + 10,(int) rec.getY() + 10, 60, 60);
                            onTurn = false;
                            setTurn();
                            soc.sendTurn(index);
                            playerStones[index] = 1;
                            skipLabel.setVisible(false);
                            if(gameOver()){
                                soc.gameOver(playerStones, oppStones);
                            }
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(false);
    }

    public void setColor(Color color){
        this.color = color;
        this.playerLabel.setForeground(color);
        if(this.color.equals(Color.RED)){
            playerStones[28] = 1;
            playerStones[35] = 1;
            oppStones[27] = 1;
            oppStones[36] = 1;
        }else{
            playerStones[27] = 1;
            playerStones[36] = 1;
            oppStones[28] = 1;
            oppStones[35] = 1;
        }
    }
    public void setOppColor(Color color) { this.oppColor = color; }


    public void oppTurn(int index) {
        Rectangle2D rec = cells[index];
        Graphics g = panel.getGraphics();
        g.setColor(oppColor);
        g.fillOval((int)rec.getX() + 10,(int) rec.getY() + 10, 60, 60);
        oppStones[index] = 1;
        playerStones[index] = 0;
        if(gameOver()){
            soc.gameOver(playerStones, oppStones);
        }
    }

    public void setTurn(){
        if(onTurn){
            turnLabel.setText("Your turn");
            turnLabel.setForeground(color);
        }else{
            turnLabel.setText("Opponents turn");
            turnLabel.setForeground(Color.BLACK);
        }
    }

    public void setNames(String nickname, String opp) {
        playerLabel.setText(nickname);
        oppLabel.setText(" vs. " + opp);
        playerLabel.setBounds(100, 5, playerLabel.getFontMetrics(playerLabel.getFont()).stringWidth(nickname), 30);
        oppLabel.setBounds(105 + playerLabel.getFontMetrics(playerLabel.getFont()).stringWidth(nickname), 5, oppLabel.getFontMetrics(oppLabel.getFont()).stringWidth(oppLabel.getText()), 30);
    }

    /**
     * metodda zjisti zda tah na pole index obarvi nejaka souperova pole
     * @param clickedStone pokud je true pole se rovnou obarvi
     */
    public boolean isOutFlanking(int index, boolean clickedStone){
        boolean outFlanked = false;

        //horizontal left
        if(index % 8 != 0){
            if(oppStones[index-1] == 1){
                int count = index % 8;
                for(int i = 0; i < count - 1; i++){
                    if(playerStones[index - (2+i)] == 1){
                        if(clickedStone){
                            outFlank( index - (2 + i), index, HORIZONTAL);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    }else if(oppStones[index - (1 + i)] != 1){
                        break;
                    }
                }
            }
        }

        //horizontal right
        if(index % 8 != 7){
            if(oppStones[index+1] == 1){
                int count = 8 - ((index % 8) + 1);
                for(int i = 0; i < count - 1; i++){
                    if(playerStones[index + (2+i)] == 1){
                        if(clickedStone){
                            outFlank( index, index + (1 + i), HORIZONTAL);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    }else if(oppStones[index + (1 + i)] != 1){
                        break;
                    }
                }
            }
        }

        //vertical up
        if( index - 8 >= 0){
            if(oppStones[index-8] == 1){
                int count = index/8;
                for(int i = 0; i < count - 1; i++){
                    if(playerStones[index - (8 * (i + 1))] == 1){
                        if(clickedStone){
                            outFlank(index - (8 * i), index - 8, VERTICAL);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    }else if(oppStones[index - (8 * (i + 1))] != 1){
                        break;
                    }
                }
            }
        }

        //vertical down
        if(index + 8 <= 63){
            if(oppStones[index+8] == 1){
                int count = 8 - (index / 8) - 1;
                for(int i = 0; i < count; i++){
                    if(playerStones[index + (8 * (i + 1))] == 1){
                        if(clickedStone){
                            outFlank(index + 8,index + (8 * i),  VERTICAL);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    }else if(oppStones[index + (8 * (i + 1))] != 1){
                        break;
                    }
                }
            }
        }


        //diagonal up left
        if(index % 8 != 0 && index - 8 >= 0){
            if(oppStones[index-(8 + 1)] == 1 ){
                int i = index - (8 + 1);
                while( i >= 9 && i % 8 != 0){
                    if(playerStones[i - 9] == 1){
                        if(clickedStone){
                            outFlank(i - 9,index -  (8 + 1), DIAGONAL_DOWN);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    }else if(oppStones[i - 9] != 1){
                        break;
                    }
                    i -= 9;
                }
            }
        }

        //diagonal up right
        if(index % 8 != 7 && index - 8 >= 0){
            if(oppStones[index-(8 - 1)] == 1 ){
                int i = index - (8 - 1);
                while(i > 6 && i % 8 != 7){
                    if(playerStones[i - 7] == 1){
                        if(clickedStone){
                            outFlank( i - 7,index - (8 - 1),   DIAGONAL_UP);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    } else if(oppStones[i - 7] != 1) {
                        break;
                    }
                    i-=7;
                }
            }
        }

        //diagonal down left
        if(index % 8 != 0 && index + 8 <= 63){
            if(oppStones[index+(8 - 1)] == 1 ){
                int i = index + (8 - 1);
                while(i < 56 && i % 8 != 0){
                    if(playerStones[i + 7] == 1){
                        if(clickedStone){
                            outFlank( index + (8 - 1), i + 7,  DIAGONAL_UP);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    } else if(oppStones[i + 7] != 1) {
                        break;
                    }
                    i+=7;
                }
            }
        }

        //diagonal down right
        if(index % 8 != 7 && index + 8 <= 63){
            if(oppStones[index+(8 + 1)] == 1){
                int i = index + (8 + 1);
                while(i < 56 && i % 8 != 7){
                    if(playerStones[i + 9] == 1){
                        if(clickedStone){
                            outFlank(index +  (8 + 1), i + 9,  DIAGONAL_DOWN);
                            outFlanked = true;
                            break;
                        }else{
                            return true;
                        }
                    } else if(oppStones[i + 9] != 1) {
                        break;
                    }
                    i+=9;
                }
            }
        }

        return outFlanked;
    }

    /**
     * metoda vykresli kameny na pole od indexu start do indexu end
     * @param direction smer vykresleni
     */
    private void outFlank(int start, int end, String direction) {
        Graphics g = panel.getGraphics();
        g.setColor(color);
        if(direction.equals(HORIZONTAL)){
            for(int i = start; i < end + 1; i++){
                g.fillOval((int) cells[i].getX() + 10, (int) cells[i].getY() + 10, 60, 60);
                playerStones[i] = 1;
                oppStones[i] = 0;
                soc.sendTurn(i);
            }
        }else if(direction.equals(VERTICAL)){
            for(int i = start; i < end + 1; i+=8){
                g.fillOval((int) cells[i].getX() + 10, (int) cells[i].getY() + 10, 60, 60);
                playerStones[i] = 1;
                oppStones[i] = 0;
                soc.sendTurn(i);
            }
        }else if(direction.equals(DIAGONAL_DOWN)){
            for(int i = start; i < end + 1; i+=9){
                g.fillOval((int) cells[i].getX() + 10, (int) cells[i].getY() + 10, 60, 60);
                playerStones[i] = 1;
                oppStones[i] = 0;
                soc.sendTurn(i);
            }
        }else if(direction.equals(DIAGONAL_UP)){
            for(int i = start; i < end + 1; i+=7){
                g.fillOval((int) cells[i].getX() + 10, (int) cells[i].getY() + 10, 60, 60);
                playerStones[i] = 1;
                oppStones[i] = 0;
                soc.sendTurn(i);
            }
        }
    }

    /**
     * @return zda hrac muze v danou chvili udelat nejaky tah
     */
    public boolean canPlay() {
        for(int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++){
            if(oppStones[i] != 1 && playerStones[i] != 1){
                if(isOutFlanking(i, false)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean gameOver(){
        int stonesCount = 0;
        for(int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++){
            if(oppStones[i] == 1){
                stonesCount++;
            }
            if(playerStones[i] == 1){
                stonesCount++;
            }
        }
        if(stonesCount < BOARD_SIZE * BOARD_SIZE){
            return false;
        }else{
            return true;
        }
    }

    public void oppDisc() {
        oppConnected = false;
        repaint();
    }

    /**
     * metoda vykresli po opetovnem pripojeni tah hracem zahrany pred odpojenim
     */
    public void myTurn(int index) {
        Rectangle2D rec = cells[index];
        Graphics g = panel.getGraphics();
        g.setColor(color);
        g.fillOval((int)rec.getX() + 10,(int) rec.getY() + 10, 60, 60);
        oppStones[index] = 0;
        playerStones[index] = 1;
    }

    public void recn() {
        oppConnected = true;
        repaint();
    }
}
