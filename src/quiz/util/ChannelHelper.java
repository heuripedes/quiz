/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import quiz.messages.Message;

/**
 *
 * @author aluno
 */
public class ChannelHelper {
    public static final int MAX_ATTEMPTS = 3;
    public static final Message nullMessage = new Message();

    public static synchronized int receive(SocketChannel channel, ByteBuffer buffer) throws IOException {
        int length = -1;

        buffer.clear();
        length = channel.read(buffer);

        buffer.flip();

        return length;
    }

    public static synchronized int send(SocketChannel channel, ByteBuffer buffer) throws IOException {
        int length = -1;
        int attempts = MAX_ATTEMPTS;

        do {
            buffer.flip();
            length = channel.write(buffer);
            buffer.compact();

        } while (buffer.hasRemaining() && --attempts != 0);


        return length;
    }

    public static String receiveString(SocketChannel channel, ByteBuffer buffer) throws IOException {
        if (receive(channel, buffer) == -1) {
            return null;
        }

        String input = new String(buffer.array(), 0, buffer.limit());

        System.out.println("Recebido : " + input);

        return input;
    }


    public static int sendString(SocketChannel channel, ByteBuffer buffer, String string) throws IOException {

        buffer.clear();
        buffer.put(string.getBytes());

        int length = send(channel, buffer);

        System.out.println("Enviado: " + string);

        return length;
    }

    public static Message receiveMessage(SocketChannel channel, ByteBuffer buffer) throws IOException {
        String str = receiveString(channel, buffer);

        if (str == null) {
            return nullMessage;
        }
        
        return GsonHelper.fromJson(str, Message.class);
    }

    public static int sendMessage(SocketChannel channel, ByteBuffer buffer, Message message) throws IOException {
        return sendString(channel, buffer, GsonHelper.toJson(message));
    }
}
