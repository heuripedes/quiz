/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.messages;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author higor
 */
public final class Message {
    final private MessageType type;
    final private List args;

    public Message() {
        this.args = null;
        this.type = MessageType.EOF;
    }

    public Message(Message message) {
        this.args = new ArrayList(message.args);
        this.type = message.type;
    }

    public Message(MessageType type) {
        this.args = new ArrayList();
        this.type = type;
    }

    public Message(MessageType type, Object arg) {
        this.args = new ArrayList();
        this.type = type;

        addArg(arg);
    }

    @Override
    protected void finalize() throws Throwable {
        args.clear();
        super.finalize();
    }

    public boolean hasArgs() {
        return !args.isEmpty();
    }

    public int getArgCount() {
        return args.size();
    }

    public int getInteger(int index) {
        return  ((Number)args.get(index)).intValue();
    }

    public String getString(int index) {
        if (index > args.size()-1) {
            return null;
        }
        return (String) args.get(index);
    }

    public float getFloat(int index) {
        return  ((Number)args.get(index)).floatValue();
    }

    public <T> T getObject(int index, Class<T> classOfT) {
        if (index > args.size()-1) {
            return null;
        }

        return classOfT.cast(args.get(index));
    }

    public void addArg(Object arg) {
        args.add(arg);
    }

    public void addArg(String arg) {
        args.add(arg);
    }
    
    private void addNumber(int arg) {
    }

    public void addArg(int arg) {
        args.add(arg);
    }

    public void addArg(float arg) {
        args.add(arg);
    }

    public void addArg(boolean arg) {
        args.add(arg);
    }

    public MessageType getType() {
        return type;
    }

    public boolean isEOF() {
        return type == MessageType.EOF;
    }

    /*public List getArgs() {
        return args;
    }*/
}
