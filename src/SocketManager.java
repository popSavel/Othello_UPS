import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class SocketManager extends Thread{

    final String PREFIX = "KIVUPS";
    final String SKIP = "NO";
    static final String DEFAUL_IP = "127.0.0.1";
    static final int DEFAULT_PORT = 9999;
    private Socket socket;
    PrintWriter out;
    BufferedReader ins;
    String nickname;
    GameWindow gameWindow;
    QueueWindow queueWindow;

    EndGame endGame;
    String opp;

    boolean oppDisc = false;


    public SocketManager(String ip, int port) throws Exception {
        this.socket = new Socket(ip, port);
        InetAddress adresa = socket.getInetAddress();
        System.out.print("Pripojuju se na : "+adresa.getHostAddress()+" se jmenem : "+adresa.getHostName()+"\n" );
        ins = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        queueWindow = new QueueWindow(this, this.nickname);
        endGame = new EndGame(this);
        /* testovaci zprava po pripojeni */
        out.println("Hello");
    }

    public SocketManager(String ip) throws Exception {
        this(ip, DEFAULT_PORT);
    }

    public SocketManager() throws Exception {
        this(DEFAUL_IP, DEFAULT_PORT);
    }


    /**
     * posle na server login zpravu a ukaze queue window
     */
    public void sendLogin(String name){
        this.nickname = name;
        int totalLength = 14 + name.length();
        String message = PREFIX + "logn" + totalLength + "0" + name.length() + name;
        System.out.println("posilam: " + message);
        out.println(message);
        queueWindow.setNick(nickname);
        queueWindow.setVisible(true);
    }

    /**
     * posle na server join zpravu
     */
    public void joinGame() {
        int totalLength = 14 + this.nickname.length();
        String message = PREFIX + "join" + totalLength + "0" + this.nickname.length() + this.nickname;
        System.out.println("posilam: " + message);
        out.println(message);
    }

    /**
     * vlakno - v nekonecne smycce prijima zpravy a reaguje na ne podle typu
     */
    public void run() {
        String message;
        while(true) {
            try {
                message = ins.readLine();
                System.out.println("received: " + message);
                if (message.contains(PREFIX)) {
                    int start = message.indexOf(PREFIX);
                    message = message.substring(start);
                    /* subSt = typ zpravy */
                    String subSt;
                    subSt = message.substring(6, 10);
                    /* pokud je oppDisc true je zprava pouze pro oponenta po opetovnem pripojeni */
                    if (subSt.equals("game") && !oppDisc) {
                        gameWindow = new GameWindow(this);
                        subSt = message.substring(12, 14);
                        int length = Integer.parseInt(subSt);
                        String player = message.substring(14, 14 + length);

                        subSt = message.substring(14 + length, 14 + length + 2);
                        int length2 = Integer.parseInt(subSt);

                        String player2 = message.substring(14 + length + 2, 14 + length + 2 + length2);
                        if (player.equals(nickname)) {
                            gameWindow.setColor(Color.BLUE);
                            gameWindow.setOppColor(Color.RED);
                            opp = player2;
                            gameWindow.onTurn = true;
                            startGame();
                        }
                        if (player2.equals(nickname)) {
                            gameWindow.setColor(Color.RED);
                            gameWindow.setOppColor(Color.BLUE);
                            opp = player;
                            startGame();
                        }
                    }
                    if (subSt.equals("turn") && !oppDisc) {
                        subSt = message.substring(12, 14);
                        int length = Integer.parseInt(subSt);
                        String player = message.substring(14, 14 + length);
                        if (player.equals(opp)) {
                            subSt = message.substring(14 + length, 14 + length + 2);
                            if (!subSt.equals(SKIP)) {
                                length = Integer.parseInt(subSt);
                                gameWindow.oppTurn(length);
                            }
                            gameWindow.onTurn = true;
                            gameWindow.setTurn();
                        }
                        if(player.equals(nickname)){
                            subSt = message.substring(14 + length, 14 + length + 2);
                            if (!subSt.equals(SKIP)) {
                                length = Integer.parseInt(subSt);
                                gameWindow.myTurn(length);
                            }
                        }
                    }
                    /* ping zpravu pouze posle zpet ve stejne podobe */
                    if (subSt.equals("ping")) {
                        System.out.println("posilam: " + message);
                        out.println(message);
                    }

                    if (subSt.equals("disc")) {
                        subSt = message.substring(12, 14);
                        int length = Integer.parseInt(subSt);
                        String player = message.substring(14, 14 + length);
                        if (player.equals(opp)) {
                            /* po druhe zprave recn byl oponent odpojen trvale -> navrat do lobby */
                            if(oppDisc){
                                gameWindow.setVisible(false);
                                queueWindow = new QueueWindow(this, this.nickname);
                                queueWindow.setNick(nickname);
                                queueWindow.setVisible(true);
                                oppDisc = false;
                            }else{
                                gameWindow.oppDisc();
                                oppDisc = true;
                            }
                        }
                    }
                    if(subSt.equals("recn")){
                        subSt = message.substring(12, 14);
                        int length = Integer.parseInt(subSt);
                        String player = message.substring(14, 14 + length);
                        if(player.equals(nickname)){
                            gameWindow.onTurn = false;
                            gameWindow.setTurn();
                        }
                        if(player.equals(opp)){
                            oppDisc = false;
                            /* oponent po opetovnem pripojeni neni nikdy na tahu, pokud by mel byt posle se prazdny tah */
                            if(gameWindow.onTurn == false){
                                sendTurn(gameWindow.NON_TURN);
                            }
                            gameWindow.recn();
                        }
                    }
                } else {
                    System.out.println("received message was invalid");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * skryje queue window
     * pripravi a zobrazi game window
     */
    private void startGame() {
        gameWindow.setTurn();
        gameWindow.setNames(nickname, opp);
        queueWindow.setVisible(false);
        gameWindow.setVisible(true);
        gameWindow.repaint();
    }

    public void sendTurn(int index) {
        int totalLength = 16 + this.nickname.length();
        String turnIndex;
        if(index < 10){
            turnIndex = "0" + index;
        }else{
            if(index == gameWindow.NON_TURN){
                turnIndex = SKIP;
            }else{
                turnIndex = String.valueOf(index);
            }
        }
        String message = PREFIX + "turn" + totalLength + "0" + this.nickname.length() + this.nickname + turnIndex;
        System.out.println("posilam: " + message);
        out.println(message);
    }

    public void gameOver(int[] playerStones, int[] oppStones) {
        endGame.printResult(playerStones, oppStones, nickname, opp);
        endGame.setVisible(true);
    }

    public void backToLobby() {
        gameWindow.setVisible(false);
        endGame.setVisible(false);
        queueWindow.setNick(nickname);
        queueWindow.setVisible(true);
    }
}
