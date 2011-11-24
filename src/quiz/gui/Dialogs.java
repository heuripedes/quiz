/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package quiz.gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import quiz.dialogs.QuizMatch;

/**
 *
 * @author higor
 */
public class Dialogs {

    public static JOptionPane connectDialog(Component owner) {
        Object[] options = {"Cancel"};

        JOptionPane dialog = new JOptionPane(
                "Conectando, aguarde...",
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null, 
                options,
                options[0]
                );
        dialog.setVisible(false);

        return dialog;
    }

    public static void error(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "Informação", JOptionPane.INFORMATION_MESSAGE);
    }
}
