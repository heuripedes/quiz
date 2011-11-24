/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import quiz.messages.Message;
import quiz.util.GsonHelper;

/**
 *
 * @author higor
 */
public final class ChannelEndPoint implements MessageChannel {

    public static final int DEFAULT_BUFSIZE = 8192;
    public static final int DELAY = 200;
    public static final String DELIMITER = "\r\n";
    private LinkedBlockingQueue<Message> sendQueue;
    private LinkedBlockingQueue<Message> recvQueue;
    private SocketChannel channel;
    private ByteBuffer buffer;
    private SelectionKey key;

    public ChannelEndPoint(SelectionKey key, int bufSize) throws ClosedChannelException {
        this.sendQueue = new LinkedBlockingQueue<Message>();
        this.recvQueue = new LinkedBlockingQueue<Message>();
        this.buffer = ByteBuffer.allocate(bufSize);
        this.channel = (SocketChannel) key.channel();
        this.key = key;

        System.out.println("Nó " + this.channel.hashCode() + " conectado.");
    }

    public ChannelEndPoint(SelectionKey key) throws ClosedChannelException {
        this(key, DEFAULT_BUFSIZE);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        channel = null;
        buffer = null;
        sendQueue = null;
        recvQueue = null;
        super.finalize();
    }

    public void flush() throws IOException {
        flush(-1);
    }

    public synchronized void flush(int amount) throws IOException {
        Iterator<Message> messages = sendQueue.iterator();

        while (amount != 0 && messages.hasNext()) {
            Message message = messages.next();
            messages.remove();

            String json = GsonHelper.toJson(message, Message.class) + DELIMITER;

            getBuffer().clear();
            getBuffer().put(json.getBytes());

            int sent = -1;
            int attempts = 10;

            do {
                
                /* somente será zero se uma tentativa de escrita tiver sido feita. */
                if (sent == 0) {
                    attempts--;
                }

                if (attempts == 0) {
                    throw new IOException("Could not write.");
                }

                getBuffer().flip();
                sent = getChannel().write(getBuffer());
                //buffer.compact();

            } while (getBuffer().hasRemaining());

            //System.out.println(channel.hashCode() + " enviado: " + json.trim());

            amount--;
        }
    }

    public boolean hold() throws IOException {
        return hold(-1);
    }

    public synchronized boolean hold(int amount) throws IOException {

        if (!isOpen()) {
            throw new IOException("Channel is closed.");
        }

        do {
            int read = 0;

            getBuffer().clear();
            read = getChannel().read(getBuffer());
            getBuffer().flip();

            /* Cliente desconectou */
            if (read == -1) {
                close();
                return false;
            }

            /* Nada para ler */
            if (read == 0) {
                break;
            }

            String source = new String(getBuffer().array(), 0, read);

            Scanner scanner = new Scanner(source);

            scanner.useDelimiter(DELIMITER);

            while (scanner.hasNext()) {
                String json = scanner.next().trim();
                Message message = GsonHelper.fromJson(json, Message.class);

                //System.out.println(channel.hashCode() + " recebido: " + json);

                recvQueue.add(message);
            }

        } while (--amount != 0);

        return true;
    }

    public synchronized boolean hasUnread() {
        return !recvQueue.isEmpty();
    }

    public synchronized boolean hasUnwritten() {
        return !sendQueue.isEmpty();
    }

    @Override
    public synchronized boolean isOpen() {
        return getChannel().isOpen();
    }

    public synchronized boolean isValid() {
        return getKey().isValid();
    }

    @Override
    public synchronized void close() throws IOException {
        if (getChannel().isOpen()) {
            getChannel().close();
            System.out.println("Nó " + this.getChannel().hashCode() + " desconectado.");
        }

        if (getKey().isValid()) {
            getKey().cancel();
        }

        sendQueue.clear();

        recvQueue.clear();
    }

    @Override
    public synchronized Message read() {
        return recvQueue.poll();
    }

    @Override
    public synchronized void write(Message message) {
        try {
            sendQueue.put(message);
        } catch (InterruptedException ex) {
            Logger.getLogger(ChannelEndPoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public SelectionKey getKey() {
        return key;
    }
}
