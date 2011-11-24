/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import quiz.core.Player;
import quiz.core.Question;

/**
 *
 * @author aluno
 */
public class MessageTypeAdapter implements JsonDeserializer<Message> {

    private static final Type playerListType = new TypeToken<List<Player>>() {
    }.getType();
    private static final Type questionListType = new TypeToken<List<Question>>() {
    }.getType();

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Message msg = new Message((MessageType) context.deserialize(obj.get("type"), MessageType.class));
        JsonArray arr = obj.getAsJsonArray("args");

        Iterator<JsonElement> it = arr.iterator();
        //System.out.println(json.toString());

        while (it.hasNext()) {
            JsonElement el = it.next();

            if (el.isJsonNull()) {
                msg.addArg(null);
            } else if (el.isJsonPrimitive()) {
                JsonPrimitive primitive = el.getAsJsonPrimitive();

                if (primitive.isString()) {
                    msg.addArg(primitive.getAsString());
                } else if (primitive.isBoolean()) {
                    msg.addArg(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    msg.addArg(primitive.getAsNumber().floatValue());
                }
            } else {
                switch (msg.getType()) {
                    case Login:
                    case Join:
                    case Idle:
                    case Ready:
                    case End:
                        msg.addArg(context.deserialize(el, Player.class));
                        break;

                    case List:
                        msg.addArg(context.deserialize(el, playerListType));
                        break;
                    case Start:
                        msg.addArg(context.deserialize(el, questionListType));
                        break;
                }
            }
        }

        return msg;
    }
}
