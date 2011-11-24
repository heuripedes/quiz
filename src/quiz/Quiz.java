/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package quiz;

import quiz.dialogs.QuizMain;

/**
 *
 * @author aluno
 */
public class Quiz {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new QuizMain().setVisible(true);
            }
        });
    }
}
