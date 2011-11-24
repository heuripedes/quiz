/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.channels;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import quiz.messages.Message;

/**
 *
 * @author higor
 */
public class ChannelEndPointList extends CopyOnWriteArrayList<ChannelEndPoint> {

    public ChannelEndPointList() {
        super();
    }

    public void broadcast(Message message) {
        broadcast(message, false);
    }

    public void broadcast(Message message, boolean flush) {

        for (ChannelEndPoint endPoint : this) {

            endPoint.write(new Message(message));
            if (flush) {
                try {
                    endPoint.flush();
                } catch (IOException ex) {
                    Logger.getLogger(ChannelEndPointList.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public ChannelEndPointList getInvalid() {
        ChannelEndPointList list = new ChannelEndPointList();

        for (ChannelEndPoint endPoint : this) {
            if (!endPoint.isValid()) {
                list.add(endPoint);
                remove(endPoint);
            }
        }

        return list;
    }

    public void refreshOps() {
        for (ChannelEndPoint endPoint : this) {

            if (endPoint.hasUnwritten()) {
                endPoint.getKey().interestOps(SelectionKey.OP_WRITE);
            } else {
                endPoint.getKey().interestOps(SelectionKey.OP_READ);
            }
        }
    }
}
