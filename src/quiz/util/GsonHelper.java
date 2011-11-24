/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.util;

import quiz.messages.MessageTypeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.lang.reflect.Type;
import quiz.messages.Message;

/**
 *
 * @author higor
 */
public class GsonHelper {

    private static Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new MessageTypeAdapter())
                .serializeNulls()
                //.setPrettyPrinting()
                .create();
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    
    public static <T> T fromJson(Reader reader, Class<T> classOfT) {
        return gson.fromJson(reader, classOfT);
    }
    
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
    
    public static <T> T fromJson(Reader reader, Type typeOfT) {
        return gson.fromJson(reader, typeOfT);
    }

    public static <T> String toJson(T src) {
        return gson.toJson(src, src.getClass());
    }

    public static <T> String toJson(T src, Type typeOfT) {
        return gson.toJson(src, typeOfT);
    }
}
