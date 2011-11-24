/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.core;

import quiz.messages.Message;
import quiz.messages.MessageType;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import quiz.channels.ChannelEndPoint;

/**
 *
 * @author higor
 */
public class Client implements Runnable {

    private boolean leave;
    private Selector selector;
    private ChannelEndPoint server;
    private ClientListener listener;

    public Client(String host, int port) throws IOException {
        leave = false;
        selector = SelectorProvider.provider().openSelector();

        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress(host, port));

        sc.register(selector, SelectionKey.OP_CONNECT);
    }

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    @Override
    protected void finalize() throws Throwable {
        if (selector.isOpen()) {
            selector.close();
        }
        super.finalize();
    }

    @Override
    public void run() {

        while (!leave && selector.isOpen()) {
            int selected = 0;
            boolean write = true;

            try {
                selected = selector.select(300);

            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } finally {
                /*if (selected == 0 && selector.isOpen()) {
                System.out.println("Timeout.");
                listener.onTimeout(this);
                break;
                }*/
            }

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            SelectionKey key = null;

            /* só há um socket no selector e portanto somente uma chave. 
            não é necessário um loop aqui. */
            if (keys.hasNext()) {
                key = keys.next();
                keys.remove();
            }

            /* nenhuma chave selecionada */
            if (key != null) {
                /* só existe um canal. se falhar então acabou-se tudo. */
                if (!key.isValid()) {
                    disconnect();
                    break;
                }

                try {

                    if (key.isConnectable()) {
                        handleConnect(key);

                    } else if (key.isReadable()) {
                        handleRead(key);

                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }

                    if (!key.isValid()) {
                        break;
                    }

                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    disconnect();
                    break;
                }
            }

            if (server == null || !server.isOpen()) {
                continue;
            }

            if (!server.isValid()) {
                break;
            }

            if (server.hasUnwritten()) {
                server.getKey().interestOps(SelectionKey.OP_WRITE);
            } else {
                server.getKey().interestOps(SelectionKey.OP_READ);
            }

            if (Thread.interrupted()) {
                disconnect();
                continue;
            }
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();

        try {

            channel.finishConnect();

            Thread.sleep(110);

            server = new ChannelEndPoint(key);


            listener.onConnect(this, false);
            key.attach(server);
            key.interestOps(SelectionKey.OP_WRITE);

        } catch (InterruptedException ex) {
        } catch (ConnectException ex) {
            listener.onConnect(this, true);
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {

        if (!server.isOpen()) {
            return;
        }

        if (!server.hold()) {
            disconnect();
            return;
        }

        while (server.hasUnread()) {
            handleCommand(server.read());
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {

        if (!server.isOpen()) {
            return;
        }

        server.flush();
    }

    private void handleCommand(Message inmsg) {

        if (inmsg == null) {
            disconnect();
            return;
        }

        switch (inmsg.getType()) {
            case Login: {
                if (!inmsg.hasArgs()) {
                    synchronized (this) {
                        listener.onLogin(this, null);
                    }
                    break;
                }

                Player p = inmsg.getObject(0, Player.class);

                if (p == null) {
                    synchronized (this) {
                        listener.onDisconnect(this, true);
                    }
                    break;
                }

                synchronized (this) {
                    listener.onLogin(this, p);
                }
                break;
            }
            case Join: {
                Player p = null;

                p = inmsg.getObject(0, Player.class);

                if (p == null) {
                    break;
                }

                synchronized (this) {
                    listener.onJoin(this, p);
                }
                break;
            }
            case Leave: {
                Player p = null;

                p = inmsg.getObject(0, Player.class);

                if (p == null) {
                    break;
                }

                synchronized (this) {
                    listener.onLeave(this, p);
                }

                break;
            }
            case List: {
                List<Player> entries;

                entries = inmsg.getObject(0, List.class);

                if (entries == null) {
                    break;
                }

                synchronized (this) {
                    listener.onList(this, entries);
                }
                break;
            }
            case Ready: {
                Player p = inmsg.getObject(0, Player.class);

                if (p == null) {
                    break;
                }
                synchronized (this) {
                    listener.onReady(this, p);
                }
                break;
            }
            case Idle: {
                Player p = inmsg.getObject(0, Player.class);
                if (p == null) {
                    break;
                }

                synchronized (this) {
                    listener.onIdle(this, p);
                }
                break;
            }
            case Start: {
                List<Question> questions = inmsg.getObject(0, List.class);

                if (questions == null || questions.isEmpty()) {
                    break;
                }

                synchronized (this) {
                    listener.onStart(this, questions);
                }
                break;
            }
            case End: {
                Player p = inmsg.getObject(0, Player.class);
                if (p == null) {
                    break;
                }

                synchronized (this) {
                    listener.onEnd(this, p);
                }

                break;
            }
        }
    }

    public synchronized void disconnect() {
        if (leave == true || !selector.isOpen()) {
            return;
        }

        try {
            if (server != null) {
                server.close();
            }

            selector.close();
            leave = true;
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            listener.onDisconnect(this, false);
        }
    }

    public void ready() {
        server.write(new Message(MessageType.Ready));
    }

    public void login(String nome) {
        Message msg = new Message(MessageType.Login);
        msg.addArg(nome);

        server.write(msg);
    }

    public void list() {
        Message msg = new Message(MessageType.List);

        server.write(msg);
    }

    public void logout() {
        Message msg = new Message(MessageType.Logout);

        server.write(msg);
    }

    public void answer(int answer) {
        Message msg = new Message(MessageType.Answer);
        msg.addArg(answer);

        server.write(msg);
    }
}
