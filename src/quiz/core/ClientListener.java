/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.core;

import java.util.List;

/**
 *
 * @author higor
 */
public interface ClientListener {

    public void onConnect(Client source, boolean fail);

    public void onDisconnect(Client source, boolean busy);

    public void onTimeout(Client source);

    public void onLogin(Client source, Player player);

    public void onReady(Client source, Player player);

    public void onList(Client source, List<Player> players);

    public void onJoin(Client source, Player player);

    public void onLeave(Client source, Player player);

    public void onStart(Client source, List<Question> questions);

    public void onIdle(Client source, Player player);

    public void onEnd(Client source, Player player);
    
}
