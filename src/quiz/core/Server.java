/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.core;

import java.util.Set;
import quiz.messages.Message;
import quiz.messages.MessageType;
import quiz.channels.ChannelEndPoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import quiz.channels.ChannelEndPointList;
import quiz.util.GsonHelper;

/**
 *
 * @author higor
 */
final public class Server implements Runnable {

    public static final int MAX_TRANSFERS = 100;
    public static final int TIMEOUT = 5000;
    public static final int BUFFER_SIZE = 4096;
    private AtomicBoolean quit;
    private final Message loginFailMessage = new Message(MessageType.Login);
    private ChannelEndPointList clients;
    private ConcurrentMap<ChannelEndPoint, Player> players;
    private Selector selector;
    private long lastUpdate;
    private boolean gameStarted;
    private List<Question> questions = new ArrayList<Question>();

    //private SocketAddress bindTo;
    public Server(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port));
    }

    public Server(int port) throws IOException {
        this(new InetSocketAddress("0.0.0.0", port));
    }

    public Server(InetSocketAddress bindTo) throws IOException {
        quit = new AtomicBoolean(false);
        clients = new ChannelEndPointList();
        players = new ConcurrentHashMap<ChannelEndPoint, Player>();
        //this.bindTo = bindTo;

        selector = SelectorProvider.provider().openSelector();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(bindTo);
        ssc.configureBlocking(false);

        ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void finalize() throws Throwable {
        clients.clear();

        super.finalize();
    }

    @Override
    public void run() {

        players.clear();
        clients.clear();


        while (!getQuit()) {
            try {
                selector.select(100);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            if (!keys.hasNext()) {
                clients.refreshOps();

                cleanup();

                updatePlayers();
            }

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                try {
                    if (!key.isValid()) {
                        if (key.channel().isOpen()) {
                            unregisterClient((ChannelEndPoint) key.attachment());
                        }

                        key = null;
                        // xxx: talvez desconectar.
                        continue;
                    }

                    if (key.isAcceptable()) {

                        handleAccept(key);

                    } else if (key.isReadable()) {

                        handleRead(key);

                    } else if (key.isWritable()) {

                        handleWrite(key);

                    }

                    cleanup();

                    clients.refreshOps();

                    updatePlayers();



                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (Thread.interrupted()) {
                close();
                break;
            }
        }
    }

    public synchronized void close() {
        setQuit(true);

        for (ChannelEndPoint client : clients) {
            try {
                client.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        Set<SelectionKey> it = selector.keys();
        
        for (SelectionKey key : selector.keys()) {
            if (key.isValid()) {
                try {
                    key.channel().close();
                    key.cancel();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        try {
            selector.close();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void cleanup() {
        //ChannelEndPointList invalid = clients.getInvalid();

        for (ChannelEndPoint endPoint : clients) {
            Player player = players.get(endPoint);

            /* remove invalidos. */
            if (!endPoint.isValid()) {
                players.remove(endPoint);

                if (player != null) {
                    Message msg = new Message(MessageType.Leave);
                    msg.addArg(player);

                    clients.broadcast(msg);
                }

                continue;
            } else {
                if (player == null) {
                    continue;
                }

                if (player.lost()) {
                    players.remove(player);
                }
            }
        }

        if (players.isEmpty()) {
            gameStarted = false;
        }
    }

    private void handleAccept(SelectionKey serverKey) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) serverKey.channel();

        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        registerClient(clientChannel.register(selector, SelectionKey.OP_READ));
    }

    private void handleRead(SelectionKey key) throws IOException {
        ChannelEndPoint client = (ChannelEndPoint) key.attachment();

        try {
            if (!client.hold()) {
                unregisterClient(client);
                return;
            }

            while (client.hasUnread()) {
                handleCommand(client, client.read());
            }

            /*if (client.hasUnwritten()) {
            key.interestOps(SelectionKey.OP_WRITE);
            }*/
        } catch (IOException ex) {
            unregisterClient(client);
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ChannelEndPoint client = (ChannelEndPoint) key.attachment();

        client.flush();
    }

    private void handleCommand(ChannelEndPoint endPoint, Message inmsg) throws IOException {
        Message outmsg = null;
        Player player = players.get(endPoint);

        switch (inmsg.getType()) {
            case Login: {
                if (gameStarted) {
                    endPoint.write(new Message(MessageType.Login, null));
                    break;
                }
                if (!inmsg.hasArgs()) {
                    endPoint.write(loginFailMessage);
                    break;
                }

                String name = inmsg.getString(0);

                /* Já existe um jogador com esse nome. */
                if (findPlayer(name) != null) {
                    endPoint.write(loginFailMessage);
                    break;
                }

                player = new Player();
                player.setName(name);

                players.putIfAbsent(endPoint, player);

                outmsg = new Message(MessageType.Login);
                outmsg.addArg(player);

                endPoint.write(outmsg);

                /* Diz aos outros jogadores que existe um novo jogador. */

                outmsg = new Message(MessageType.Join);
                outmsg.addArg(player);

                clients.broadcast(outmsg);

                break;
            }

            case Logout: {
                if (player == null) {
                    break;
                }

                outmsg = new Message(MessageType.Leave);
                outmsg.addArg(player);

                unregisterClient(endPoint);

                clients.broadcast(outmsg);


                break;
            }
            case List: {

                outmsg = new Message(MessageType.List);

                outmsg.addArg(players.values());

                endPoint.write(outmsg);

                break;
            }

            case Ready: {

                player.setReady(true);

                outmsg = new Message(MessageType.Ready);
                outmsg.addArg(player);
                clients.broadcast(outmsg);

                if (allReady()) {
                    gameStarted = true;
                    lastUpdate = (int) (System.currentTimeMillis() / 1000L);
                    outmsg = new Message(MessageType.Start);
                    outmsg.addArg(questions);
                    clients.broadcast(outmsg);
                }

                break;
            }

            case Answer: {
                if (!inmsg.hasArgs()) {
                    break;
                }

                int i = inmsg.getInteger(0);
                
                if (player.getQuestion() > questions.size() - 1) {
                    unregisterClient(endPoint);
                    break;
                }

                Question q = questions.get(player.getQuestion());

                if (q.getCorrect() == i) {
                    player.nextQuestion();
                } else {
                    player.useContinue();
                }

                /* acabou. */
                if (player.getQuestion() > questions.size() - 1) {
                    outmsg = new Message(MessageType.End, player);
                    clients.broadcast(outmsg, true);
                }
                break;
            }
            default:
                System.err.println("Unknown command: " + GsonHelper.toJson(inmsg));
        }

        outmsg = null;
    }

    private synchronized ChannelEndPoint registerClient(SelectionKey key) throws ClosedChannelException {
        ChannelEndPoint endPoint = new ChannelEndPoint(key);


        key.attach(endPoint);

        clients.add(endPoint);

        return endPoint;
    }

    private synchronized void unregisterClient(ChannelEndPoint client) throws IOException {
        client.close();
        clients.remove(client);
        players.remove(client);
    }

    private Player findPlayer(String name) {
        for (Player player : players.values()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    private boolean allReady() {
        for (Player p : players.values()) {
            if (!p.isReady()) {
                return false;
            }
        }

        return true;
    }

    private void updatePlayers() {

        if (!gameStarted) {
            return;
        }

        long time = (long) (System.currentTimeMillis() / 1000L);
        long diff = Player.INITIAL_TIME - (time - lastUpdate);
        //lastUpdate = time;

        for (ChannelEndPoint client : clients) {
            Player p = players.get(client);

            if (p == null) {
                continue;
            }

            if (p.getQuestion() < 0) {
                p.nextQuestion();
            }

            long before = p.getTime();

            if (diff != before) {
                p.setTime(diff);

                client.write(new Message(MessageType.Idle, p));
            }
        }
    }

    public static void main(String[] args) {
        Server s;

        try {
            s = new Server(9595);

            s.run();
            //new Thread(s, "ServerWorker").start();

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean getQuit() {
        return quit.get();
    }

    public void setQuit(boolean quit) {
        this.quit.compareAndSet(!quit, quit);
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions.clear();
        this.questions.addAll(questions);
    }

    private void endMatch() {
        for (ChannelEndPoint client : clients) {
            try {
                client.flush();
                unregisterClient(client);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
