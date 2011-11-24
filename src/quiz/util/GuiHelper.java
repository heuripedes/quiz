/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.util;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * @author higor
 */
public class GuiHelper {

    public static Object askForSingleInput(Component parent, String title, String message, String init) {
        return JOptionPane.showInputDialog(parent,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                null, null,
                init);
    }

    public static void showError(Component parent, String string) {
        JOptionPane.showMessageDialog(parent, string, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
