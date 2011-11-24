/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package quiz.channels;

import java.nio.channels.Channel;
import quiz.messages.Message;

/**
 *
 * @author higor
 */
public interface MessageChannel extends Channel {
    public Message read();
    public void write(Message message);
}
