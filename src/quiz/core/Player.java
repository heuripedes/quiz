/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package quiz.core;

import java.io.Serializable;

/**
 *
 * @author higor
 */
public class Player implements Serializable {
    public static final int INITIAL_TIME = 300;
    private String name;
    private int points    = 0;
    private int question  = -1;
    private int continues = 3;
    private long time      = INITIAL_TIME;
    private boolean ready = false;
    private boolean done  = false;

    @Override
    public boolean equals(Object obj) {

        if (obj == null || !Player.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        Player p = (Player) obj;

        return p.getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    
    public Player() {

    }

    public Player(String name) {
        setName(name);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getQuestion() {
        return question;
    }

    public void setQuestion(int question) {
        this.question = question;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getContinues() {
        return continues;
    }

    public void setContinues(int continues) {
        this.continues = continues;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public synchronized void update(Player p) {
        if (!name.equals(p.name)) {
            return;
        }

        name = p.name;
        continues = p.continues;
        points = p.points;
        question = p.question;
        done = p.done;
        ready = p.ready;
        time = p.time;
    }

    public void nextQuestion() {
        question++;
    }

    public void useContinue() {
        continues--;
    }

    public boolean lost() {
        return continues >= 0;
    }
}
